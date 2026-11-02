param(
    [string]$Version = "0.2.0",
    [string]$Output = "output",
    [switch]$Plan,
    [switch]$KeepWork
)
# Collects the complete corresponding source for every component bundled by
# build.ps1 (FFmpeg + the libraries the pinned BtbN recipe links into the
# win64-lgpl-shared variant, yt-dlp, Deno) and writes a manifest with hashes.
# Fails loudly when any source cannot be fetched at its pinned revision.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$binaries = Get-Content binaries.json -Raw | ConvertFrom-Json
if ($binaries.ffmpeg.url -notmatch '/download/(?<tag>autobuild-[\d-]+)/ffmpeg-n(?<ver>[\d.]+)-\d+-g(?<commit>[0-9a-f]+)-(?<target>win64)-(?<variant>lgpl)-shared') {
    throw "binaries.json ffmpeg url does not match the expected BtbN naming: $($binaries.ffmpeg.url)"
}
$recipeTag = $Matches.tag
$ffCommit = $Matches.commit
$ffVer = [int]([version]$Matches.ver).Major * 100 + [int]([version]$Matches.ver).Minor
$variant = $Matches.variant
if ($binaries.'yt-dlp'.url -notmatch '/download/(?<tag>[\d.]+)/yt-dlp.exe') { throw 'Unexpected yt-dlp url.' }
$ytTag = $Matches.tag
if ($binaries.deno.url -notmatch '/download/(?<tag>v[\d.]+)/') { throw 'Unexpected deno url.' }
$denoTag = $Matches.tag
$buildconf = (& vendor\ffmpeg.exe -hide_banner -buildconf 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Run build.ps1 first so vendor\ffmpeg.exe exists.' }
$enabledFlags = [regex]::Matches($buildconf, '--enable-([a-z0-9_-]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique

$work = Join-Path $PSScriptRoot '.cache\sources'
$recipeDir = Join-Path $work 'recipe'
New-Item -ItemType Directory -Force $work, $Output | Out-Null

function Fetch($url, $target) {
    if (Test-Path $target) { return }
    $tmp = "$target.part"
    for ($attempt = 1; ; $attempt++) {
        try { Invoke-WebRequest $url -OutFile $tmp -UseBasicParsing; break }
        catch { if ($attempt -ge 3) { throw }; Start-Sleep 5 }
    }
    Move-Item $tmp $target -Force
}
function Sha($path) { (Get-FileHash $path -Algorithm SHA256).Hash.ToLowerInvariant() }
# Native stderr becomes a terminating error under $ErrorActionPreference='Stop'
# when redirected with 2>&1 in Windows PowerShell; git prints harmless notices
# there ("Skipping submodule ..."), so run it with errors downgraded.
function Invoke-Git($dir) {
    $ErrorActionPreference = 'Continue'
    & git -C $dir @args 2>&1 | Out-Null
}
function Export-SvnTree($url, $dir) {
    New-Item -ItemType Directory -Force $dir | Out-Null
    $html = (Invoke-WebRequest $url -UseBasicParsing).Content
    foreach ($m in [regex]::Matches($html, '<li><a href="(?<href>[^"]+)">')) {
        $href = [uri]::UnescapeDataString($m.Groups['href'].Value)
        if ($href -eq '../' -or $href -match '[/\\]\.\.|^\.\.$|^[/\\]|:') { continue }
        if ($href.EndsWith('/')) { Export-SvnTree "$url$($m.Groups['href'].Value)" (Join-Path $dir $href.TrimEnd('/')) }
        else { Fetch "$url$($m.Groups['href'].Value)" (Join-Path $dir $href) }
    }
}

# 1. Recipe (BtbN/FFmpeg-Builds at the exact autobuild tag) and FFmpeg source.
$recipeZip = Join-Path $work "FFmpeg-Builds-$recipeTag.zip"
Fetch "https://github.com/BtbN/FFmpeg-Builds/archive/refs/tags/$recipeTag.zip" $recipeZip
if (!(Test-Path $recipeDir)) {
    Expand-Archive $recipeZip "$recipeDir.tmp" -Force
    Move-Item (Get-ChildItem "$recipeDir.tmp" -Directory | Select-Object -First 1).FullName $recipeDir
    Remove-Item "$recipeDir.tmp" -Recurse -Force
}
$ffmpegZip = Join-Path $work "FFmpeg-$ffCommit.zip"
Fetch "https://github.com/FFmpeg/FFmpeg/archive/$ffCommit.zip" $ffmpegZip

# 2. Resolve which recipe scripts take part in this variant/target.
function Get-Block($text, $name) {
    if ($text -match "(?s)$name\(\)\s*\{(.*?)\n\}") { return $Matches[1] }
    return $null
}
$components = @()
foreach ($script in Get-ChildItem (Join-Path $recipeDir 'scripts.d') -Recurse -Filter *.sh | Sort-Object FullName) {
    $text = Get-Content $script.FullName -Raw
    if ($text -notmatch '(?m)^SCRIPT_REPO="([^"]+)"') { continue }
    $repo = $Matches[1]
    $rev = $null; $kind = $null
    foreach ($candidate in 'SCRIPT_COMMIT', 'SCRIPT_TAG', 'SCRIPT_REV', 'SCRIPT_BRANCH', 'SCRIPT_TAGFILTER') {
        if ($text -match "(?m)^$candidate=`"?([^`"\r\n]+)`"?") { $rev = $Matches[1]; $kind = $candidate; break }
    }
    $name = $script.BaseName -replace '^\d+-', ''
    $enabled = Get-Block $text 'ffbuild_enabled'
    $configure = Get-Block $text 'ffbuild_configure'
    $reason = 'dependency without configure flag'
    $included = $true
    if ($null -ne $configure) {
        $flags = [regex]::Matches($configure, '--enable-([a-z0-9_-]+)') | ForEach-Object { $_.Groups[1].Value }
        if ($flags.Count) {
            $hit = @($flags | Where-Object { $enabledFlags -contains $_ })
            $included = $hit.Count -gt 0
            $reason = if ($included) { "buildconf has --enable-$($hit[0])" } else { "buildconf lacks --enable-$($flags[0])" }
        }
    }
    if ($enabled) {
        $first = ($enabled -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -notmatch '^#' } | Select-Object -First 1)
        if ($first -eq 'return -1') { $included = $false; $reason = 'disabled in recipe' }
        if ($enabled -match '\$VARIANT == lgpl\*\s*\]\]\s*&&\s*return -1' -and $variant -eq 'lgpl') { $included = $false; $reason = 'gpl-only' }
        if ($enabled -match '\$VARIANT == nonfree\*\s*\]\]\s*\|\|\s*return -1') { $included = $false; $reason = 'nonfree-only' }
        if ($enabled -match '\$VARIANT == \*?gpl\*\s*\]\]\s*\|\|\s*return -1' -and $variant -eq 'lgpl') { $included = $false; $reason = 'gpl-only' }
        if ($enabled -match '\$TARGET == linux\*\s*\]\]\s*\|\|\s*return -1' -or $enabled -match '\$TARGET != linux\*\s*\]\]\s*&&\s*return -1') { $included = $false; $reason = 'linux-only' }
        if ($enabled -match '\$TARGET == win\*\s*\]\]\s*&&\s*return -1') { $included = $false; $reason = 'not for windows' }
        foreach ($m in [regex]::Matches($enabled, '\(\(\s*\$\(ffbuild_ffver\)\s*(>=|>|<=|<)\s*(\d+)\s*\)\)\s*\|\|\s*return -1')) {
            $ok = switch ($m.Groups[1].Value) { '>=' { $ffVer -ge [int]$m.Groups[2].Value } '>' { $ffVer -gt [int]$m.Groups[2].Value } '<=' { $ffVer -le [int]$m.Groups[2].Value } '<' { $ffVer -lt [int]$m.Groups[2].Value } }
            if (-not $ok) { $included = $false; $reason = "ffmpeg version gate $($m.Value)" }
        }
    }
    if ($repo -match 'mingw-w64') { $included = $false; $reason = 'toolchain runtime, not linked into FFmpeg DLLs (sources at upstream mirror)' }
    $components += [pscustomobject]@{ Name = $name; Script = $script.FullName.Substring($recipeDir.Length + 1); Repo = $repo; Kind = $kind; Rev = $rev; Included = $included; Reason = $reason }
}
$components | Format-Table Name, Included, Reason, Kind, Rev -AutoSize | Out-String -Width 220 | Write-Host
$missing = $components | Where-Object { $_.Included -and -not $_.Rev }
if ($missing) { throw "Recipe scripts without pinned revision: $($missing.Name -join ', ')" }
if ($Plan) { return }

# 3. Fetch every included dependency at its pinned revision (with submodules).
$depsDir = Join-Path $work 'deps'
New-Item -ItemType Directory -Force $depsDir | Out-Null
$failures = @()
$manifest = [System.Collections.Generic.List[string]]::new()
$manifest.Add("My Super App Downloader $Version - corresponding source manifest")
$manifest.Add("Generated $(Get-Date -Format o) by collect-sources.ps1")
$manifest.Add('')
function Add-Entry($file, $what, $origin) {
    $manifest.Add("$what`n  origin: $origin`n  file: $([IO.Path]::GetFileName($file))`n  sha256: $(Sha $file)`n  bytes: $((Get-Item $file).Length)")
}
Add-Entry $recipeZip 'BtbN/FFmpeg-Builds build recipe (scripts, Dockerfiles, patches)' "https://github.com/BtbN/FFmpeg-Builds/tree/$recipeTag"
Add-Entry $ffmpegZip "FFmpeg source at commit $ffCommit" "https://github.com/FFmpeg/FFmpeg/commit/$ffCommit"
foreach ($dep in $components | Where-Object Included) {
    $safeRev = ($dep.Rev -replace '[^A-Za-z0-9._-]', '_')
    $zip = Join-Path $depsDir "$($dep.Name)-$safeRev.zip"
    if (Test-Path $zip) { Add-Entry $zip "$($dep.Name) ($($dep.Kind) $($dep.Rev))" $dep.Repo; continue }
    Write-Host "Fetching $($dep.Name) @ $($dep.Rev)"
    $dir = Join-Path $depsDir $dep.Name
    if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
    try {
        if ($dep.Repo -like '*svn*') {
            # No svn client is assumed: walk the mod_dav_svn baseline collection
            # (!svn/bc/<rev>/<path>) so the tree is pinned to the exact revision.
            if ($dep.Repo -notmatch '^(?<root>https://svn\.code\.sf\.net/p/[^/]+/svn)(?<path>/.*)$') { throw "Unsupported svn host: $($dep.Repo)" }
            $svnRoot = $Matches.root; $svnPath = $Matches.path.TrimEnd('/')
            if ($dep.Rev -notmatch '^\d+$') { throw "svn revision must be numeric: $($dep.Rev)" }
            Export-SvnTree "$svnRoot/!svn/bc/$($dep.Rev)$svnPath/" $dir
            tar -a -cf $zip -C $depsDir $dep.Name
            if ($LASTEXITCODE) { throw 'archive failed' }
            Remove-Item $dir -Recurse -Force
        } else {
            git clone --quiet --filter=blob:none --no-checkout $dep.Repo $dir
            if ($LASTEXITCODE) { throw 'clone failed' }
            git -C $dir checkout --quiet $dep.Rev
            if ($LASTEXITCODE) { throw 'checkout failed' }
            Invoke-Git $dir -c protocol.version=2 submodule update --quiet --init --recursive --depth 1
            if ($LASTEXITCODE) {
                Invoke-Git $dir submodule update --quiet --init --recursive
                if ($LASTEXITCODE) { throw 'submodules failed' }
            }
            Get-ChildItem $dir -Recurse -Force -Directory -Filter .git | Remove-Item -Recurse -Force
            Get-ChildItem $dir -Recurse -Force -File -Filter .git | Remove-Item -Force
            tar -a -cf $zip -C $depsDir $dep.Name
            if ($LASTEXITCODE) { throw 'archive failed' }
            Remove-Item $dir -Recurse -Force
        }
        Add-Entry $zip "$($dep.Name) ($($dep.Kind) $($dep.Rev))" $dep.Repo
    } catch {
        $failures += "$($dep.Name): $($dep.Repo) @ $($dep.Rev) - $($_.Exception.Message)"
        if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
    }
}
if ($failures) {
    Write-Host "UNRESOLVED SOURCES:`n$($failures -join "`n")" -ForegroundColor Red
    throw 'Source collection incomplete; do not publish.'
}

# 4. yt-dlp and Deno sources plus notices.
$ytZip = Join-Path $work "yt-dlp-$ytTag.zip"
Fetch "https://github.com/yt-dlp/yt-dlp/archive/refs/tags/$ytTag.zip" $ytZip
Add-Entry $ytZip "yt-dlp $ytTag" "https://github.com/yt-dlp/yt-dlp/tree/$ytTag"
$denoZip = Join-Path $work "deno-$denoTag.zip"
Fetch "https://github.com/denoland/deno/archive/refs/tags/$denoTag.zip" $denoZip
Add-Entry $denoZip "Deno $denoTag (Rust dependencies are pinned by its Cargo.lock)" "https://github.com/denoland/deno/tree/$denoTag"
$manifest.Add('')
$manifest.Add("Binaries built from these sources are listed in binaries.json (SHA256 of each download).")
$manifest.Add("ffmpeg -buildconf of the bundled binary:`n$buildconf")
$manifest.Add("Excluded recipe components (not part of this build): " + (($components | Where-Object { -not $_.Included } | ForEach-Object { "$($_.Name) [$($_.Reason)]" }) -join ', '))

$stage = Join-Path $work 'stage'
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Force "$stage\deps" | Out-Null
Copy-Item $recipeZip, $ffmpegZip, $ytZip, $denoZip $stage
Copy-Item "$depsDir\*.zip" "$stage\deps"
Copy-Item THIRD-PARTY-NOTICES.txt, binaries.json, build.ps1, collect-sources.ps1, requirements-build.txt $stage
Copy-Item vendor\licenses "$stage\licenses" -Recurse
Set-Content "$stage\SOURCES-MANIFEST.txt" ($manifest -join "`n`n") -Encoding utf8
$bundle = Join-Path $Output "MySuperAppDownloader-$Version-sources.zip"
if (Test-Path $bundle) { Remove-Item $bundle }
tar -a -cf $bundle -C $stage .
if ($LASTEXITCODE) { throw 'Could not create the sources bundle.' }
if (-not $KeepWork) { Remove-Item $stage, $depsDir -Recurse -Force }
Get-FileHash $bundle -Algorithm SHA256

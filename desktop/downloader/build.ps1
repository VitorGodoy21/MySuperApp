param(
    [string]$Python = "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
    [string]$InnoSetup = "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    [switch]$SkipSources
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not [Environment]::Is64BitOperatingSystem -or $env:PROCESSOR_ARCHITECTURE -eq 'ARM64') {
    throw 'This build supports Windows x64 only.'
}
if (!(Test-Path $Python) -or !(Test-Path $InnoSetup)) {
    throw 'Install Python 3.12 x64 (with Tkinter) and Inno Setup 6, or pass their paths.'
}
function Run-Python {
    & $script:BuildPython @args
    if ($LASTEXITCODE -ne 0) { throw "Python failed: $args" }
}
if (!(Test-Path '.venv\Scripts\python.exe')) {
    & $Python -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create build environment.' }
}
$script:BuildPython = "$PSScriptRoot\.venv\Scripts\python.exe"
Run-Python -m pip install --disable-pip-version-check -r requirements-build.txt
New-Item -ItemType Directory -Force .cache,vendor,vendor\licenses | Out-Null
$binaries = Get-Content binaries.json -Raw | ConvertFrom-Json
foreach ($entry in $binaries.PSObject.Properties) {
    $item = $entry.Value
    $target = Join-Path '.cache' $item.file
    if (!(Test-Path $target)) { Invoke-WebRequest $item.url -OutFile $target }
    if ((Get-FileHash $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $item.sha256) {
        throw "SHA256 mismatch: $target. Do not use this download."
    }
    if ($item.file -eq 'yt-dlp.exe') {
        Copy-Item $target vendor\yt-dlp.exe -Force
    } else {
        $extract = Join-Path '.cache' $entry.Name
        Expand-Archive $target $extract -Force
        if ($entry.Name -eq 'deno') {
            Copy-Item "$extract\deno.exe" vendor\deno.exe -Force
        } else {
            $ff = Get-ChildItem $extract -Directory | Select-Object -First 1
            Copy-Item "$($ff.FullName)\bin\*" vendor -Force
            Copy-Item "$($ff.FullName)\LICENSE*" vendor\licenses -Force
        }
    }
}
$licenseUrls = @{
    'yt-dlp-LICENSE.txt' = 'https://raw.githubusercontent.com/yt-dlp/yt-dlp/2026.10.26/LICENSE'
    'yt-dlp-THIRD_PARTY_LICENSES.txt' = 'https://raw.githubusercontent.com/yt-dlp/yt-dlp/2026.10.26/THIRD_PARTY_LICENSES.txt'
    'deno-LICENSE.txt' = 'https://raw.githubusercontent.com/denoland/deno/v2.9.8/LICENSE.md'
}
foreach ($name in $licenseUrls.Keys) {
    $target = "vendor\licenses\$name"
    if (!(Test-Path $target)) { Invoke-WebRequest $licenseUrls[$name] -OutFile $target }
}
Copy-Item binaries.json vendor\licenses\binaries.json -Force
Copy-Item THIRD-PARTY-NOTICES.txt vendor\licenses -Force
Run-Python -m unittest discover -s tests -v
Run-Python -m PyInstaller --noconfirm --clean --windowed --onedir --name MySuperAppDownloader `
    --add-data 'public;public' --add-data 'vendor;vendor' app.py
& $InnoSetup /Q installer.iss
if ($LASTEXITCODE -ne 0) { throw 'Installer build failed.' }
Get-FileHash output\MySuperAppDownloader-Setup.exe -Algorithm SHA256
if ($SkipSources) {
    Write-Host 'Sources bundle skipped (-SkipSources). Run collect-sources.ps1 before publishing.'
} else {
    # Corresponding source for the (L)GPL components must be published next to the installer.
    & "$PSScriptRoot\collect-sources.ps1"
}

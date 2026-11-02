#define AppName "My Super App Downloader"
#define AppVersion "0.2.0"

[Setup]
AppId={{B1A708ED-3B66-40DD-A711-6DF5E8712659}
AppName={#AppName}
AppVersion={#AppVersion}
DefaultDirName={localappdata}\Programs\MySuperAppDownloader
DefaultGroupName={#AppName}
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=output
OutputBaseFilename=MySuperAppDownloader-Setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\MySuperAppDownloader.exe
CloseApplications=yes
SetupMutex=MySuperAppDownloaderSetup

[Files]
Source: "dist\MySuperAppDownloader\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\MySuperAppDownloader.exe"
Name: "{group}\Desinstalar"; Filename: "{uninstallexe}"

[Registry]
Root: HKCU; Subkey: "Software\Classes\mysuperapp-downloader"; ValueType: string; ValueData: "URL:My Super App Downloader"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\mysuperapp-downloader"; ValueType: string; ValueName: "URL Protocol"; ValueData: ""
Root: HKCU; Subkey: "Software\Classes\mysuperapp-downloader\DefaultIcon"; ValueType: string; ValueData: """{app}\MySuperAppDownloader.exe"",0"
Root: HKCU; Subkey: "Software\Classes\mysuperapp-downloader\shell\open\command"; ValueType: string; ValueData: """{app}\MySuperAppDownloader.exe"" ""%1"""

[Run]
Filename: "{app}\MySuperAppDownloader.exe"; Description: "Abrir Downloader MP3 e MP4"; Flags: nowait postinstall skipifsilent

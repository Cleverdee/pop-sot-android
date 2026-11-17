<#
Builds the native Android APK (arm64) of Prince of Persia: Warrior Within from the recompiled POP2.EXE.
Reuses the toolchain the Cricket 07 port already downloaded (E:\cricket-apk\port\third_party: Android SDK + NDK r27,
SDL2 source, wgpu-native for aarch64-linux-android, JDK).

Inputs (prepared once, see ..\..\README-ANDROID.md):
  ..\..\work\pop2.exe        GOG pop2.exe after `upx -d` (no section fix needed)
  ..\..\work\functions.json  Ghidra export (ghidra\ExportFunctions.java)
  ..\out_all\                translator output (x86toc.py + tools\patch_generated.py); made here if missing

Options:
  -Install        install the APK on the phone connected over USB
  -PushGame       also copy the game folder to the phone (Android/data/<package>/files/game)
  -Retranslate    redo the translation even if out_all\ exists
  -Opt "-O3"      optimisation for the game code (default -O2)
  -Debug          debuggable build with -O1 (faster to compile)
#>
param(
    [switch]$Install,
    [switch]$PushGame,
    [switch]$Retranslate,
    [string]$GameDir = "E:\cricket-apk\Prince of Persia Warrior Within",
    [string]$Opt = "-O2",
    [switch]$Debug
)

$ErrorActionPreference = "Stop"
$Android = $PSScriptRoot
$Repo = Split-Path $Android -Parent                     # pop-ww\src
$Top = Split-Path $Repo -Parent                         # pop-ww
$Work = Join-Path $Top "work"
$CricketTP = "E:\cricket-apk\port\third_party"
$Tools = Join-Path $CricketTP "tools"
$Package = "com.cleverdee.popww"
$NdkVersion = "27.2.12479018"

function Step($msg) { Write-Host ""; Write-Host "== $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host ""; Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }
function Run($exe, [string[]]$argv) {
    # tools write progress to stderr; with output redirected, "Stop" would turn that into a terminating error
    $old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
    & $exe @argv 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $old
    if ($LASTEXITCODE -ne 0) { Fail "$(Split-Path $exe -Leaf) failed (exit code $LASTEXITCODE)" }
}

Step "Toolchain (from the Cricket 07 port)"
$Sdk = Join-Path $Tools "android-sdk"
if (-not (Test-Path (Join-Path $Sdk "ndk\$NdkVersion"))) { Fail "no NDK $NdkVersion in ${Sdk}: run the Cricket port's android\build.ps1 once" }
$Jdk = Join-Path $Tools "jdk21"
# Java's NIO pipe is an AF_UNIX socket in %TEMP%; a long TEMP path breaks it ("Unable to establish loopback connection")
$ShortTmp = Join-Path $Top "tmp"
New-Item -ItemType Directory -Force $ShortTmp | Out-Null
$env:TEMP = $ShortTmp; $env:TMP = $ShortTmp
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=$ShortTmp -Djdk.net.unixdomain.tmpdir=$ShortTmp"
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
$env:ANDROID_HOME = $Sdk
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$env:SDL2_SRC = Join-Path $CricketTP "SDL2"
$env:RC_WGPU = Join-Path $CricketTP "wgpu"
foreach ($p in @($env:SDL2_SRC, (Join-Path $env:RC_WGPU "lib\libwgpu_native.a"))) { if (-not (Test-Path $p)) { Fail "missing $p" } }

Step "Recompiled code"
$Exe = Join-Path $Work "pop2.exe"
$Funcs = Join-Path $Work "functions.json"
$OutAll = Join-Path $Repo "out_all"
foreach ($p in @($Exe, $Funcs)) { if (-not (Test-Path $p)) { Fail "missing $p (see README-ANDROID.md)" } }
$Hash = (Get-FileHash $Exe -Algorithm SHA256).Hash.Substring(0, 16).ToLower()
$env:RC_EXE_HASH = ""   # the GOG exe is preprocessed: the app can't compare the player's pop2.exe against it
$Stamp = Join-Path $OutAll ".exe-hash"
if ($Retranslate -or -not (Test-Path $Stamp) -or (Get-Content $Stamp) -ne $Hash) {
    if (Test-Path $OutAll) { Remove-Item -Recurse -Force $OutAll }
    Run "python" @("-I", (Join-Path $Repo "translator\x86toc.py"), $Exe, $Funcs, $OutAll, "--all", "--per-file", "300")
    Run "python" @("-I", (Join-Path $Repo "tools\patch_generated.py"), $OutAll)
    Set-Content -Path $Stamp -Value $Hash -Encoding ascii
}
Write-Host "  $((Get-ChildItem $OutAll -Filter *.c).Count) generated C files in out_all\"

Step "Building the APK (first build compiles ~2M translated instructions: 20-60 minutes)"
Set-Content -Path (Join-Path $Android "local.properties") -Value ("sdk.dir=" + ($Sdk -replace "\\", "/")) -Encoding ascii
$env:RC_NDK_VERSION = $NdkVersion
$env:RC_OPT = if ($Debug) { "-O1" } else { $Opt }
$env:RC_LTO = "OFF"
Remove-Item Env:RC_BUNDLE_DIR -ErrorAction SilentlyContinue
$task = if ($Debug) { "assembleDebug" } else { "assembleRelease" }
Push-Location $Android
try { Run (Join-Path $Android "gradlew.bat") @("--no-daemon", $task) } finally { Pop-Location }

$variant = if ($Debug) { "debug" } else { "release" }
$built = Join-Path $Android "app\build\outputs\apk\$variant\app-$variant.apk"
$OutDir = Join-Path $Android "output"
New-Item -ItemType Directory -Force $OutDir | Out-Null
$Apk = Join-Path $OutDir "PrinceOfPersiaWW.apk"
Copy-Item -Force $built $Apk
Write-Host "  built $Apk ($([math]::Round((Get-Item $Apk).Length / 1MB)) MB)" -ForegroundColor Green

$PhoneGame = "/sdcard/Android/data/$Package/files/game"
if ($Install -or $PushGame) {
    Step "Phone"
    $devices = & $Adb devices | Select-String "\tdevice$"
    if (-not $devices) { Fail "no phone found over adb. Plug it in with USB debugging on and accept the prompt on the phone." }
    Run $Adb @("install", "-r", $Apk)
    Write-Host "  installed"
    if ($PushGame) {
        Write-Host "  copying the game to $PhoneGame (1.5 GB: several minutes)"
        Run $Adb @("shell", "mkdir", "-p", $PhoneGame)
        Run $Adb @("push", "--sync", "$GameDir\.", $PhoneGame)
        # the recompiled code is the unpacked exe; the game folder keeps it under the name the host loads
        Run $Adb @("push", $Exe, "$PhoneGame/POP2.EXE")
    }
}
Write-Host ""
Write-Host "Done. Game files on the phone: Android/data/$Package/files/game/" -ForegroundColor Green

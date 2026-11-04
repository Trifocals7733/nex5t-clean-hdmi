$ErrorActionPreference = 'Continue'
function ok { if ($LASTEXITCODE) { throw "step failed ($LASTEXITCODE)" } }
Set-Location $PSScriptRoot
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt = "$sdk\build-tools\36.0.0"
$jar = "$sdk\platforms\android-35\android.jar"
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
New-Item -ItemType Directory build\stubs, build\classes, build\dex | Out-Null

javac -nowarn -source 8 -target 8 -bootclasspath $jar -d build\stubs (Get-ChildItem -Recurse stubs -Filter *.java).FullName; ok
javac -nowarn -source 8 -target 8 -bootclasspath $jar -cp build\stubs -d build\classes (Get-ChildItem -Recurse src -Filter *.java).FullName; ok
& "$bt\d8.bat" --min-api 10 --lib $jar --classpath build\stubs --output build\dex (Get-ChildItem -Recurse build\classes -Filter *.class).FullName; ok
& "$bt\aapt2.exe" link --manifest AndroidManifest.xml -I $jar --min-sdk-version 10 --target-sdk-version 10 -o build\unsigned.apk; ok
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$PWD\build\unsigned.apk", 'Update')
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$PWD\build\dex\classes.dex", 'classes.dex') | Out-Null
$zip.Dispose()
& "$bt\zipalign.exe" -f 4 build\unsigned.apk build\aligned.apk; ok
if (-not (Test-Path debug.keystore)) {
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias key -keyalg RSA -keysize 2048 -sigalg SHA1withRSA -validity 10000 -dname "CN=cleanhdmi"
}
& "$bt\apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --min-sdk-version 10 --v2-signing-enabled false --v3-signing-enabled false --out CleanHdmi.apk build\aligned.apk; ok
Write-Output "Built $PWD\CleanHdmi.apk"

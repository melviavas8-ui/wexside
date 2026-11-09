param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $gradleArguments = @('build', 'writeVerificationClasspath', '--console=plain')
    if ($Offline) { $gradleArguments += '--offline' }
    & .\gradlew.bat @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' }
    $verificationRoot = Join-Path $projectRoot ('build\verification\' + [Guid]::NewGuid().ToString('N'))
    $dependencies = Join-Path $verificationRoot 'dependencies'
    $classes = Join-Path $verificationRoot 'tools'
    New-Item -ItemType Directory -Force -Path $dependencies, $classes | Out-Null
    $classpath = Get-Content -LiteralPath 'build\recovery\runtime-classpath.txt'
    $index = 0
    $asmLibraries = @()
    foreach ($entry in $classpath) {
        if (-not $entry.EndsWith('.jar')) { continue }
        $destination = Join-Path $dependencies ('{0:D3}.jar' -f $index)
        [System.IO.File]::Copy(('\\?\' + $entry), ('\\?\' + $destination))
        if ([System.IO.Path]::GetFileName($entry) -match '^asm(?:-|\.)') { $asmLibraries += $destination }
        $index++
    }
    $asmClasspath = $asmLibraries -join [System.IO.Path]::PathSeparator
    & javac -cp $asmClasspath -d $classes '.\tools\VerifyRecovered.java' '.\tools\LinkageAudit.java'
    if ($LASTEXITCODE -ne 0) { throw 'Verifier compilation failed' }
    $toolClasspath = $classes + [System.IO.Path]::PathSeparator + $asmClasspath
    $ownClasses = Join-Path $verificationRoot 'own-classes'
    New-Item -ItemType Directory -Path $ownClasses | Out-Null
    foreach ($sourceSet in @('main', 'recovery')) {
        $sourceRoot = Join-Path $projectRoot ('build\classes\java\' + $sourceSet)
        foreach ($classFile in Get-ChildItem -LiteralPath $sourceRoot -Filter '*.class' -File -Recurse) {
            $relative = $classFile.FullName.Substring($sourceRoot.Length + 1)
            $target = Join-Path $ownClasses $relative
            if (Test-Path -LiteralPath $target) { throw "Duplicate own class: $relative" }
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
            [System.IO.File]::Copy(('\\?\' + $classFile.FullName), ('\\?\' + $target))
        }
    }
    & java -Xverify:all -cp $toolClasspath VerifyRecovered $ownClasses $dependencies --report-dir (Join-Path $verificationRoot 'bytecode')
    if ($LASTEXITCODE -ne 0) { throw 'Bytecode verification failed' }
    & java -cp $toolClasspath LinkageAudit $ownClasses $dependencies (Join-Path $verificationRoot 'linkage-failures.tsv')
    if ($LASTEXITCODE -ne 0) { throw 'Member linkage verification failed' }
} finally {
    Pop-Location
}

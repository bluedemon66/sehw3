<#
.SYNOPSIS
Builds English API documentation and validates every visible Javadoc declaration.
.DESCRIPTION
Includes private strategies and constructors. DocLint warnings fail the command.
The generated pages are written to target/apidocs.
P.S. Missing-comment diagnostics are disabled because they also check implicit
constructors.
#>
$ErrorActionPreference = 'Stop'
$projectPath = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javadoc.exe'))) {
    $javaDocCommand = Get-Command (Join-Path $env:JAVA_HOME 'bin/javadoc.exe') -ErrorAction Stop
} else {
    $javaDocCommand = Get-Command javadoc -ErrorAction Stop
}
$classpath = Join-Path $projectPath 'target/takeout-system-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $classpath)) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'maven.ps1') '-DskipTests' 'package'
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$outputPath = Join-Path $projectPath 'target\apidocs'
$argumentPath = Join-Path $projectPath 'target\javadoc.args'
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$argumentLines = @(
    '-locale', 'en', '-quiet', '-private', '--release', '17', '-Xdoclint:all,-missing', '-Werror',
    '-encoding', 'UTF-8', '-docencoding', 'UTF-8', '-charset', 'UTF-8',
    '-classpath', ('"' + $classpath.Replace('\', '/') + '"'),
    '-notimestamp', '--no-platform-links', '-d', ('"' + $outputPath.Replace('\', '/') + '"')
)
$sources = Get-ChildItem -LiteralPath (Join-Path $projectPath 'src\main\java') -Recurse -File -Filter '*.java'
foreach ($source in ($sources | Sort-Object FullName)) {
    $argumentLines += '"' + $source.FullName.Replace('\', '/') + '"'
}
[IO.File]::WriteAllLines($argumentPath, $argumentLines, [Text.UTF8Encoding]::new($false))
& $javaDocCommand.Source '-J-Duser.language=en' '-J-Duser.country=US' ('@' + $argumentPath)
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
Write-Output "API documentation generated at $outputPath"

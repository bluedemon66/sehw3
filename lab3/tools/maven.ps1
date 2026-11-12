param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArguments = @('verify')
)
$ErrorActionPreference = 'Stop'
$projectPath = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$cachePath = 'E:\softwares\sehw3-tools\maven-repository'
$taskTempPath = 'E:\softwares\sehw3-tools\tmp'
New-Item -ItemType Directory -Force -Path $cachePath, $taskTempPath | Out-Null
$env:TEMP = $taskTempPath
$env:TMP = $taskTempPath
Push-Location $projectPath
try {
    & (Join-Path $projectPath 'mvnw.cmd') "-Dmaven.repo.local=$cachePath" @MavenArguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}

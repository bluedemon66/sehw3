<#
.SYNOPSIS
Exports the working Java project and CI workflow to the course repository.
.PARAMETER RepositoryPath
Existing Git repository that will receive lab3 and the root workflow.
.DESCRIPTION
Copies source, build configuration, engineering documentation, and tools.
Records source and exported SHA-256 hashes in lab3/SYNC.json.
P.S. Review removed source files with git rm in the destination repository.
This command only creates or updates files; staging and publishing are separate.
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$RepositoryPath
)
$ErrorActionPreference = 'Stop'
$sourceRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$repositoryRoot = (Resolve-Path -LiteralPath $RepositoryPath).Path
if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot '.git'))) {
    throw 'RepositoryPath must point to an existing Git repository or worktree.'
}
$exportRoot = [IO.Path]::GetFullPath((Join-Path $repositoryRoot 'lab3'))
if ($sourceRoot.TrimEnd('\', '/') -eq $exportRoot.TrimEnd('\', '/')) {
    throw 'Run this command from the sehw3 working project, outside the exported lab3 directory.'
}
$utf8 = [Text.UTF8Encoding]::new($false, $true)
$textExtensions = @('.java', '.xml', '.md', '.json', '.ps1', '.py', '.properties', '.yml', '.yaml', '.txt', '.cmd')
$excludedExtensions = @('.class', '.log', '.tmp', '.zip', '.7z', '.rar', '.gz', '.pyc')
$sources = [System.Collections.Generic.List[IO.FileInfo]]::new()
foreach ($relative in @('.gitignore', '.gitattributes', 'README.md', 'pom.xml', 'mvnw', 'mvnw.cmd')) {
    $sources.Add((Get-Item -LiteralPath (Join-Path $sourceRoot $relative)))
}
foreach ($relative in @('.mvn', 'src', 'config', 'doc', 'tools')) {
    foreach ($file in (Get-ChildItem -LiteralPath (Join-Path $sourceRoot $relative) -File -Recurse -Force)) {
        if ($file.Extension -notin $excludedExtensions -and $file.FullName -notmatch '[\\/]__pycache__[\\/]') {
            $sources.Add($file)
        }
    }
}
$mappings = foreach ($file in ($sources | Sort-Object FullName)) {
    $relative = $file.FullName.Substring($sourceRoot.Length + 1).Replace('\', '/')
    @{ Source = $file.FullName; Relative = $relative; Target = 'lab3/' + $relative }
}
$mappings += @{
    Source = Join-Path $sourceRoot 'sync/sehw3-ci.yml'
    Relative = 'sync/sehw3-ci.yml'
    Target = '.github/workflows/sehw3-ci.yml'
}
$entries = [System.Collections.Generic.List[object]]::new()
$updated = 0
foreach ($mapping in $mappings) {
    $destination = [IO.Path]::GetFullPath((Join-Path $repositoryRoot $mapping.Target))
    if (-not $destination.StartsWith($repositoryRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'An export destination escaped the repository.'
    }
    $inputBytes = [IO.File]::ReadAllBytes($mapping.Source)
    $outputBytes = $inputBytes
    $extension = [IO.Path]::GetExtension($mapping.Source)
    if ($extension -in $textExtensions -or [IO.Path]::GetFileName($mapping.Source) -in @('mvnw', '.gitignore', '.gitattributes')) {
        $content = $utf8.GetString($inputBytes).Replace("`r`n", "`n")
        $outputBytes = $utf8.GetBytes($content)
    }
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $sourceHash = [BitConverter]::ToString($sha.ComputeHash($inputBytes)).Replace('-', '').ToLowerInvariant()
        $exportHash = [BitConverter]::ToString($sha.ComputeHash($outputBytes)).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }
    $oldHash = if (Test-Path -LiteralPath $destination) { (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() } else { '' }
    if ($oldHash -ne $exportHash) {
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
        [IO.File]::WriteAllBytes($destination, $outputBytes)
        $updated++
    }
    $entries.Add([ordered]@{
        source = $mapping.Relative
        target = $mapping.Target
        source_sha256 = $sourceHash
        exported_sha256 = $exportHash
        transform = $(if ($sourceHash -eq $exportHash) { 'byte-copy' } else { 'utf8-lf' })
    })
}
$generatedAt = [DateTimeOffset]::Now.ToOffset([TimeSpan]::FromHours(8)).ToString('yyyy-MM-ddTHH:mm:sszzz')
$record = [ordered]@{ schema_version = 1; generated_at = $generatedAt; source = 'sehw3'; repository = 'bluedemon66/sehw1'; files = @($entries.ToArray()) }
$manifestPath = Join-Path $exportRoot 'SYNC.json'
if (Test-Path -LiteralPath $manifestPath) {
    $previous = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if (($previous.files | ConvertTo-Json -Depth 5 -Compress) -eq ($record.files | ConvertTo-Json -Depth 5 -Compress)) {
        $record.generated_at = $previous.generated_at
    }
}
$json = ($record | ConvertTo-Json -Depth 6).Replace("`r`n", "`n") + "`n"
$markdown = @(
    '# Lab 3 repository sync', '',
    'Working source: sehw3/. Repository copy: sehw1/lab3/.', '',
    'Workflow: sync/sehw3-ci.yml -> .github/workflows/sehw3-ci.yml.', '',
    ('Exported files: ' + $entries.Count + '. Source and exported SHA-256 hashes are recorded in [SYNC.json](SYNC.json).'), '',
    'Run tools/sync-repository.ps1 from the working project before reviewing and committing repository changes.', '',
    'P.S. Source text is exported as UTF-8 with LF line endings. Build output, assignment instructions, and archives stay in the working directory. Review source deletions in the destination repository.', ''
) -join "`n"
foreach ($directory in @($exportRoot, (Join-Path $sourceRoot 'sync'))) {
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    [IO.File]::WriteAllText((Join-Path $directory 'SYNC.json'), $json, $utf8)
    [IO.File]::WriteAllText((Join-Path $directory 'SYNC.md'), $markdown, $utf8)
}
Write-Output ("Exported {0} files; updated {1} file contents. Manifest: {2}" -f $entries.Count, $updated, $manifestPath)

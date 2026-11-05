param([Parameter(Mandatory=$true)][string]$Folder)
$ErrorActionPreference = 'Stop'
$Folder = (Resolve-Path -LiteralPath $Folder).Path
$catalogPath = Join-Path $Folder 'alarms.json'
$previous = @{}
if (Test-Path -LiteralPath $catalogPath) {
    Copy-Item -LiteralPath $catalogPath -Destination ($catalogPath + '.backup') -Force
    try {
        $oldCatalog = Get-Content -LiteralPath $catalogPath -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($oldCatalog.schemaVersion -eq 1) { foreach ($sound in $oldCatalog.sounds) { $previous[$sound.id] = $sound } }
    } catch { Write-Warning 'Existing JSON was backed up; a new catalog will be generated.' }
}
$sounds = @(Get-ChildItem -LiteralPath $Folder -File -Filter '*.mp3' | Sort-Object Name | ForEach-Object {
    $soundFile = $_
    if ($soundFile.Name.Contains('..') -or $soundFile.Name -cnotmatch "^[A-Za-z0-9][A-Za-z0-9 _.'()!-]{0,100}\.mp3$") { throw "Unsupported file name: $($soundFile.Name)" }
    if ($soundFile.Length -lt 1 -or $soundFile.Length -gt 20MB) { throw "Invalid file size: $($soundFile.Name)" }
    $nameHash = [System.Security.Cryptography.SHA256]::Create()
    try { $idHash = $nameHash.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($soundFile.Name.ToLowerInvariant())) } finally { $nameHash.Dispose() }
    $soundId = 'sound-' + ([BitConverter]::ToString($idHash).Replace('-','').ToLowerInvariant()).Substring(0,16)
    $digest = (Get-FileHash -LiteralPath $soundFile.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $version = 1
    if ($previous.ContainsKey($soundId)) {
        $version = [int]$previous[$soundId].version
        if ($previous[$soundId].sha256 -cne $digest) { $version++ }
    }
    [ordered]@{ id=$soundId; name=$soundFile.BaseName; file=$soundFile.Name; version=$version; size=$soundFile.Length; sha256=$digest }
})
if ($sounds.Count -gt 500) { throw 'Maximum 500 sounds per catalog.' }
$result = [ordered]@{ schemaVersion=1; sounds=$sounds } | ConvertTo-Json -Depth 5
[System.IO.File]::WriteAllText($catalogPath, $result, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Created $catalogPath with $($sounds.Count) sounds. Upload alarms.json and the MP3 files to infinitimeilia/infinitime/alarm on main."

# JMeter uniquement sur backend Quittances de test deja demarre et confirme.
# Ce script NE cree pas de base et NE demarre pas Spring Boot.
[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21.0.12.1',
    [string]$JMeterHome = "$env:USERPROFILE\Tools\apache-jmeter-5.6.3",
    [switch]$IsolationConfirmee
)

$ErrorActionPreference = 'Stop'
if (-not $IsolationConfirmee) {
    throw 'Refus: confirmer d abord que le processus sur 18084 utilise une base PostgreSQL temporaire _test, puis passer -IsolationConfirmee.'
}
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$plan = Join-Path $PSScriptRoot 'plan-test-quittances.jmx'
$jmeter = Join-Path $JMeterHome 'bin\jmeter.bat'
$java = Join-Path $JavaHome 'bin\java.exe'
foreach ($path in @($plan, $jmeter, $java)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Fichier requis introuvable : $path" }
}
# Interdire tout autre serveur ou port, meme lors d une execution manuelle.
$listener = @(Get-NetTCPConnection -State Listen -LocalPort 18084 -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalAddress -in @('127.0.0.1', '0.0.0.0', '::1', '::') })
if ($listener.Count -eq 0) {
    throw 'Aucun serveur de TEST en ecoute sur 18084. Ne pas utiliser le port Docker 8084.'
}
$serverPids = @($listener | Select-Object -ExpandProperty OwningProcess -Unique)
foreach ($processusServiceId in $serverPids) {
    $proc = Get-Process -Id $processusServiceId -ErrorAction SilentlyContinue
    if ($null -eq $proc -or $proc.ProcessName -notmatch '^java(w)?$') {
        throw "Le processus sur 18084 n est pas un processus Java verifie (PID $processusServiceId). Refus."
    }
}
$oldJava = $env:JAVA_HOME
$oldPath = $env:Path
try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$oldPath"
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $reportParent = Join-Path $repo 'qualite-logicielle\quittances\rapports\jmeter'
    $runDir = Join-Path $reportParent "execution-$stamp"
    if (Test-Path -LiteralPath $runDir) { throw 'Dossier de sortie deja present; ne pas ecraser.' }
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    $jtl = Join-Path $runDir 'resultats.jtl'
    $html = Join-Path $runDir 'rapport-html'
    $log = Join-Path $runDir 'jmeter.log'
    & $jmeter -n -t $plan -l $jtl -e -o $html -j $log
    if ($LASTEXITCODE -ne 0) { throw "JMeter a echoue avec le code $LASTEXITCODE" }
    if (-not (Test-Path -LiteralPath $jtl)) { throw 'Le fichier JTL attendu n existe pas.' }
    $rows = @(Import-Csv -LiteralPath $jtl)
    if ($rows.Count -eq 0) { throw 'JMeter n a enregistre aucune requete.' }
    $failed = @($rows | Where-Object { $_.success -ne 'true' }).Count
    Write-Host "Rapport : $html"
    Write-Host "Requetes : $($rows.Count) ; echecs : $failed"
    Write-Host 'Paliers : 5 puis 10 utilisateurs virtuels, 25 secondes chacun.'
    if ($failed -gt 0) { throw "$failed echantillons sont en echec. Lire le JTL et le rapport." }
}
finally {
    $env:JAVA_HOME = $oldJava
    $env:Path = $oldPath
}

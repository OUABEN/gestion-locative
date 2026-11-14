param([switch]$Reconstruire)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
function Controle([string]$Action) {
    if ($LASTEXITCODE -ne 0) { throw "Echec : $Action. Arret du script." }
}
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
foreach ($f in @('.env','.local-jwt/public.der','.local-signature/darloc-demo.p12','docker/stubs/serveur_demo.py','storage/quittances')) {
    if (-not (Test-Path -LiteralPath $f)) { throw "Fichier ou dossier absent : $f" }
}
foreach ($volume in @('gestionlocative-quittances-dev_quittances_pgdata','darloc-mailpit_mailpit_data')) {
    docker volume inspect $volume --format '{{.Name}}'
    Controle "volume $volume"
}
if ($Reconstruire) {
    & mvn '-DskipTests' package
    Controle 'compilation Maven'
    docker build --tag darloc-quittances:local .
    Controle 'construction image'
}
docker image inspect darloc-quittances:local --format '{{.Id}}'
Controle 'image DarLoc'
$applicationDocker = @(docker ps --filter 'label=com.docker.compose.project=darloc-local' --filter 'label=com.docker.compose.service=quittances' --format '{{.ID}}')
Controle 'inspection application'
if ($applicationDocker.Count -eq 0 -and (Get-NetTCPConnection -LocalPort 8084 -State Listen -ErrorAction SilentlyContinue)) {
    throw 'Arrete Spring Boot Windows avec Ctrl+C ou Stop dans IntelliJ, puis relance ce script.'
}
$secret = Read-Host 'Mot de passe du certificat de signature existant' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try { $env:DARLOC_SIGNATURE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
try {
    & "$env:JAVA_HOME\bin\keytool.exe" -list -keystore '.local-signature/darloc-demo.p12' -storetype PKCS12 -alias darloc-demo '-storepass:env' DARLOC_SIGNATURE_PASSWORD | Out-Null
    Controle 'mot de passe du certificat'
    docker compose --env-file .env -f compose.darloc.yml config --quiet
    Controle 'configuration Compose'
    docker compose --env-file .env -f compose.darloc.yml pull quittances-db mailpit services-demo
    Controle 'telechargement des images'
    $ancienDb = 'gestionlocative-quittances-dev-quittances-db-1'
    $ancienMail = 'darloc-mailpit-mailpit-1'
    $actifs = @(docker ps --format '{{.Names}}')
    Controle 'inspection conteneurs'
    if ($actifs -contains $ancienDb) {
        $dossier = Join-Path $PSScriptRoot ('sauvegardes/docker-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
        New-Item -ItemType Directory -Path $dossier -Force | Out-Null
        docker exec $ancienDb sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/darloc-avant-docker.dump'
        Controle 'sauvegarde PostgreSQL'
        docker cp "${ancienDb}:/tmp/darloc-avant-docker.dump" (Join-Path $dossier 'quittances.dump')
        Controle 'copie sauvegarde PostgreSQL'
        Write-Output "Sauvegarde PostgreSQL : $dossier"
    }
    $existants = @(docker ps -a --format '{{.Names}}')
    Controle 'inspection anciens conteneurs'
    foreach ($nom in @($ancienDb,$ancienMail)) {
        if ($actifs -contains $nom) { docker stop $nom; Controle "arret $nom" }
        if ($existants -contains $nom) {
            docker update --restart=no $nom | Out-Null
            Controle "desactivation redemarrage $nom"
        }
    }
    foreach ($volume in @('gestionlocative-quittances-dev_quittances_pgdata','darloc-mailpit_mailpit_data')) {
        $ids = @(docker ps --filter "volume=$volume" --format '{{.ID}}')
        Controle 'verification des volumes actifs'
        foreach ($id in $ids) {
            $info = docker inspect $id | ConvertFrom-Json
            Controle 'inspection utilisateur du volume'
            if ($info[0].Config.Labels.'com.docker.compose.project' -ne 'darloc-local') {
                throw "Le volume $volume est encore utilise par un autre conteneur. Demarrage bloque."
            }
        }
    }
    docker compose --env-file .env -f compose.darloc.yml up -d
    Controle 'demarrage Docker'
    $pret = $false
    for ($essai=0; $essai -lt 45; $essai++) {
        try {
            $etat = Invoke-RestMethod -Uri 'http://127.0.0.1:8084/api/v1/systeme/etat' -TimeoutSec 3
            if ($etat.statut -eq 'OPERATIONNEL') { $pret=$true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    docker compose --env-file .env -f compose.darloc.yml ps
    if (-not $pret) {
        docker compose --env-file .env -f compose.darloc.yml logs --tail 100 quittances
        throw 'Application non prete. Copie les journaux affiches.'
    }
    $etat | ConvertTo-Json
    Write-Output 'DarLoc : http://127.0.0.1:8084/quittances'
    Write-Output 'Mailpit : http://127.0.0.1:8025'
} finally {
    Remove-Item Env:DARLOC_SIGNATURE_PASSWORD -ErrorAction SilentlyContinue
    $secret = $null
}

param([switch]$Reconstruire, [string]$LocataireIntegrationId)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
function Controle([string]$Action) {
    if ($LASTEXITCODE -ne 0) { throw "Echec : $Action. Arret du script." }
}
$composeArgs = @('--env-file', '.env', '-f', 'compose.darloc.yml')
if ($LocataireIntegrationId) {
    $uuid = [guid]::Empty
    if (-not [guid]::TryParse($LocataireIntegrationId, [ref]$uuid)) { throw 'UUID locataire invalide.' }
    $fiche = Invoke-RestMethod "http://127.0.0.1:18082/api/v1/locataires/$LocataireIntegrationId/quittance-summary"
    if ($fiche.id -ne $LocataireIntegrationId) { throw 'Locataire non confirme.' }
    $reseaux = @(docker network ls --format '{{.Name}}')
    Controle 'lecture reseaux'
    if ($reseaux -notcontains 'darloc-locataires-integration') {
        docker network create darloc-locataires-integration | Out-Null
        Controle 'creation reseau integration'
    }
    $infoLocataires = docker inspect groupe16-locataires-integration | ConvertFrom-Json
    Controle 'inspection locataires'
    if (-not $infoLocataires[0].NetworkSettings.Networks.'darloc-locataires-integration') {
        docker network connect --alias locataires-integration darloc-locataires-integration groupe16-locataires-integration
        Controle 'connexion reseau locataires'
    }
    $env:DARLOC_LOCATAIRE_INTEGRATION_ID = $LocataireIntegrationId
    $composeArgs += @('-f', 'compose.locataires-integration.yml')
    Write-Output "Test hybride : bail BAIL-INTEGRATION-$LocataireIntegrationId ; octobre 2026."
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
    & mvn test package
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
    docker compose @composeArgs config --quiet
    Controle 'configuration Compose'
    docker compose @composeArgs pull quittances-db mailpit services-demo
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
    docker compose @composeArgs up -d
    Controle 'demarrage Docker'
    $pret = $false
    for ($essai=0; $essai -lt 45; $essai++) {
        try {
            $etat = Invoke-RestMethod -Uri 'http://127.0.0.1:8084/api/v1/systeme/etat' -TimeoutSec 3
            if ($etat.statut -eq 'OPERATIONNEL') { $pret=$true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    docker compose @composeArgs ps
    if (-not $pret) {
        docker compose @composeArgs logs --tail 100 quittances
        throw 'Application non prete. Copie les journaux affiches.'
    }
    $etat | ConvertTo-Json
    Write-Output 'DarLoc : http://127.0.0.1:8084/quittances'
    Write-Output 'Mailpit : http://127.0.0.1:8025'
} finally {
    Remove-Item Env:DARLOC_SIGNATURE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:DARLOC_LOCATAIRE_INTEGRATION_ID -ErrorAction SilentlyContinue
    $secret = $null
}

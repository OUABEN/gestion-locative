$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
# Stop n'utilise pas ce secret : valeur requise uniquement pour lire le fichier Compose.
$env:DARLOC_SIGNATURE_PASSWORD = 'inutilise-pour-arret'
try {
    docker compose --env-file .env -f compose.darloc.yml stop
    if ($LASTEXITCODE -ne 0) { throw 'Echec arret Docker.' }
} finally { Remove-Item Env:DARLOC_SIGNATURE_PASSWORD -ErrorAction SilentlyContinue }

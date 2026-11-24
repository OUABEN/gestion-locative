param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$JavaHome,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$PostgresBin
)

$ErrorActionPreference = "Stop"

$racineDepot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pom = Join-Path $racineDepot "backend\backend_gestion_quittances\pom.xml"
$dossierSurefire = Join-Path $racineDepot `
    "backend\backend_gestion_quittances\target\surefire-reports"
$dossierJacoco = Join-Path $racineDepot `
    "qualite-logicielle\quittances\rapports\jacoco"

$executables = @{
    java = Join-Path $JavaHome "bin\java.exe"
    initdb = Join-Path $PostgresBin "initdb.exe"
    pg_ctl = Join-Path $PostgresBin "pg_ctl.exe"
    createdb = Join-Path $PostgresBin "createdb.exe"
}

foreach ($nom in $executables.Keys) {
    if (-not (Test-Path -LiteralPath $executables[$nom])) {
        throw "Exécutable requis introuvable : $($executables[$nom])"
    }
}

if (-not (Test-Path -LiteralPath $pom)) {
    throw "Le pom.xml Quittances est introuvable : $pom"
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $maven) {
    throw "Maven (mvn) doit être accessible dans PATH."
}

$variablesTemporaires = @(
    "JAVA_HOME",
    "Path",
    "QUITTANCES_DB_HOST",
    "QUITTANCES_DB_PORT",
    "QUITTANCES_DB_NAME",
    "QUITTANCES_DB_USER",
    "QUITTANCES_DB_PASSWORD",
    "SPRING_APPLICATION_JSON",
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "SPRING_DATASOURCE_HIKARI_JDBC_URL",
    "SPRING_DATASOURCE_HIKARI_USERNAME",
    "SPRING_DATASOURCE_HIKARI_PASSWORD",
    "PGHOST",
    "PGPORT",
    "PGUSER"
)
$variablesTemporaires += @(
    Get-ChildItem Env: |
        Where-Object { $_.Name -like "SPRING_DATASOURCE_*" } |
        Select-Object -ExpandProperty Name
)
$variablesTemporaires = @($variablesTemporaires | Select-Object -Unique)
$valeursInitiales = @{}
foreach ($nom in $variablesTemporaires) {
    $valeursInitiales[$nom] =
        [Environment]::GetEnvironmentVariable($nom, "Process")
}

$identifiant = [Guid]::NewGuid().ToString("N")
$suffixe = $identifiant.Substring(0, 8)
$nomBase = "quittances_qualite_${suffixe}_test"
$utilisateurBase = "quittances_qualite"
$dossierTemporaire = Join-Path $env:TEMP "quittances-qualite-$identifiant"
$dossierDonnees = Join-Path $dossierTemporaire "data"
$journalPostgres = Join-Path $dossierTemporaire "postgres.log"
$serveurDemarre = $false
$port = $null

try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" + $valeursInitiales["Path"]

    Push-Location $racineDepot
    try {
        $versionMaven = (& $maven.Source -version 2>&1 | Out-String)
        if ($versionMaven -notmatch "Java version:\s+21(?:\.|\s)") {
            throw "Maven is not using Java 21. Check JAVA_HOME."
        }

        $ecouteur = [System.Net.Sockets.TcpListener]::new(
            [System.Net.IPAddress]::Loopback,
            0
        )
        $ecouteur.Start()
        $port = $ecouteur.LocalEndpoint.Port
        $ecouteur.Stop()

        if ($nomBase -notmatch "^[a-z0-9_]+_test$") {
            throw "Refusing to use a database name without the _test suffix."
        }

        New-Item -ItemType Directory -Path $dossierTemporaire | Out-Null

        if (Test-Path -LiteralPath $dossierJacoco) {
            Remove-Item -LiteralPath $dossierJacoco -Recurse -Force
        }

        & $executables.initdb `
            -D $dossierDonnees `
            -U $utilisateurBase `
            --auth-local=trust `
            --auth-host=trust `
            --encoding=UTF8 `
            --no-locale
        if ($LASTEXITCODE -ne 0) {
            throw "initdb failed with exit code $LASTEXITCODE."
        }

        & $executables.pg_ctl `
            -D $dossierDonnees `
            -l $journalPostgres `
            -o "-h 127.0.0.1 -p $port -c listen_addresses=127.0.0.1" `
            -w start
        if ($LASTEXITCODE -ne 0) {
            throw "Starting temporary PostgreSQL failed."
        }
        $serveurDemarre = $true

        $env:PGHOST = "127.0.0.1"
        $env:PGPORT = "$port"
        $env:PGUSER = $utilisateurBase
        & $executables.createdb $nomBase
        if ($LASTEXITCODE -ne 0) {
            throw "Creating the temporary database failed."
        }

        $env:QUITTANCES_DB_HOST = "127.0.0.1"
        $env:QUITTANCES_DB_PORT = "$port"
        $env:QUITTANCES_DB_NAME = $nomBase
        $env:QUITTANCES_DB_USER = $utilisateurBase
        $env:QUITTANCES_DB_PASSWORD = $identifiant

        foreach ($nom in $variablesTemporaires) {
            if ($nom -like "SPRING_DATASOURCE_*") {
                [Environment]::SetEnvironmentVariable($nom, $null, "Process")
            }
        }
        [Environment]::SetEnvironmentVariable(
            "SPRING_APPLICATION_JSON",
            $null,
            "Process"
        )

        $urlJdbc = "jdbc:postgresql://127.0.0.1:$port/$nomBase"
        $env:SPRING_DATASOURCE_URL = $urlJdbc
        $env:SPRING_DATASOURCE_USERNAME = $utilisateurBase
        $env:SPRING_DATASOURCE_PASSWORD = $identifiant
        $env:SPRING_DATASOURCE_HIKARI_JDBC_URL = $urlJdbc
        $env:SPRING_DATASOURCE_HIKARI_USERNAME = $utilisateurBase
        $env:SPRING_DATASOURCE_HIKARI_PASSWORD = $identifiant

        & $maven.Source -f $pom clean verify
        if ($LASTEXITCODE -ne 0) {
            throw "mvn clean verify failed with exit code $LASTEXITCODE."
        }

        Write-Host "Surefire/JUnit : $dossierSurefire"
        Write-Host "JaCoCo HTML/XML : $dossierJacoco"
        Write-Host "Base temporaire utilisée : $nomBase (127.0.0.1:$port)"
    }
    finally {
        Pop-Location
    }
}
finally {
    $supprimerCluster = $true
    if (-not $serveurDemarre -and (Test-Path -LiteralPath $dossierDonnees)) {
        & $executables.pg_ctl -D $dossierDonnees status *> $null
        if ($LASTEXITCODE -eq 0) {
            $serveurDemarre = $true
        }
    }

    if ($serveurDemarre) {
        & $executables.pg_ctl `
            -D $dossierDonnees `
            -m fast `
            -w stop
        if ($LASTEXITCODE -eq 0) {
            $serveurDemarre = $false
        }
        else {
            $supprimerCluster = $false
            Write-Warning "PostgreSQL could not be stopped. Data retained at: $dossierTemporaire"
        }
    }

    if ($supprimerCluster -and (Test-Path -LiteralPath $dossierTemporaire)) {
        Remove-Item -LiteralPath $dossierTemporaire -Recurse -Force
    }

    foreach ($nom in $variablesTemporaires) {
        [Environment]::SetEnvironmentVariable(
            $nom,
            $valeursInitiales[$nom],
            "Process"
        )
    }
}

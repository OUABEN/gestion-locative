param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$JavaHome,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$PostgresBin
)

$ErrorActionPreference = "Stop"

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    $encoded = [Convert]::ToBase64String($Bytes)
    return $encoded.TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-TestJwt {
    param(
        [string]$Secret,
        [string]$Subject,
        [string]$Role
    )

    $issuedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $claims = [ordered]@{
        sub  = $Subject
        role = $Role
        iat  = $issuedAt
        exp  = $issuedAt + 600
    }

    $header = ConvertTo-Base64Url `
        ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
    $payload = ConvertTo-Base64Url `
        ([Text.Encoding]::UTF8.GetBytes(
            (ConvertTo-Json -InputObject $claims -Compress)
        ))
    $message = "$header.$payload"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new(
        [Text.Encoding]::UTF8.GetBytes($Secret)
    )

    try {
        $signatureBytes = $hmac.ComputeHash(
            [Text.Encoding]::UTF8.GetBytes($message)
        )
        $signature = ConvertTo-Base64Url `
            -Bytes $signatureBytes
    }
    finally {
        $hmac.Dispose()
    }

    return "$message.$signature"
}

function Protect-NewmanValue {
    param(
        [object]$Value,
        [string]$PropertyName = ""
    )

    if ($PropertyName -match "(?i)authorization|token|secret|jwt") {
        return "[REDACTED]"
    }

    if ($null -eq $Value) {
        return $null
    }

    if ($Value -is [string]) {
        $safe = [regex]::Replace(
            $Value,
            "(?i)Bearer\s+[A-Za-z0-9._~-]+",
            "Bearer [REDACTED]"
        )
        return [regex]::Replace(
            $safe,
            "eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+",
            "[REDACTED_JWT]"
        )
    }

    if ($Value -is [System.Array]) {
        for ($index = 0; $index -lt $Value.Count; $index++) {
            $Value[$index] = Protect-NewmanValue $Value[$index]
        }
        return ,$Value
    }

    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $keyProperty = $Value.PSObject.Properties["key"]
        $valueProperty = $Value.PSObject.Properties["value"]
        if ($keyProperty -and $valueProperty -and
                $keyProperty.Value -match
                "(?i)authorization|token|secret|jwt") {
            $valueProperty.Value = "[REDACTED]"
        }

        foreach ($property in @($Value.PSObject.Properties)) {
            $property.Value = Protect-NewmanValue `
                $property.Value $property.Name
        }
        return $Value
    }

    return $Value
}

$racineDepot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$module = Join-Path $racineDepot "backend\backend_gestion_quittances"
$pom = Join-Path $module "pom.xml"
$jar = Join-Path $module "target\service-quittances-0.0.1-SNAPSHOT.jar"
$collection = Join-Path $PSScriptRoot "Quittances-api.postman_collection.json"
$environmentFile = Join-Path $PSScriptRoot "Quittances-test.postman_environment.json"
$newman = Join-Path $PSScriptRoot "node_modules\.bin\newman.cmd"
$rapportDirectory = Join-Path $racineDepot "docs\qualite\quittances\postman"
$rapportJson = Join-Path $rapportDirectory "newman-report.json"
$rapportHtml = Join-Path $rapportDirectory "newman-report.html"
$java = Join-Path $JavaHome "bin\java.exe"
$executables = @{
    initdb = Join-Path $PostgresBin "initdb.exe"
    pg_ctl = Join-Path $PostgresBin "pg_ctl.exe"
    createdb = Join-Path $PostgresBin "createdb.exe"
    psql = Join-Path $PostgresBin "psql.exe"
}

foreach ($required in @($java, $pom, $collection, $environmentFile, $newman)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Fichier ou outil requis introuvable : $required"
    }
}
foreach ($executable in $executables.Values) {
    if (-not (Test-Path -LiteralPath $executable)) {
        throw "Exécutable PostgreSQL introuvable : $executable"
    }
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $maven) {
    throw "Maven doit être accessible dans PATH."
}

$portProbe = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    18084
)
try {
    $portProbe.Start()
}
catch {
    throw "Le port 18084 est déjà utilisé. Refus de contacter ou arrêter son propriétaire."
}
finally {
    $portProbe.Stop()
}

$tempId = [Guid]::NewGuid().ToString("N")
$suffix = $tempId.Substring(0, 8)
$database = "quittances_postman_${suffix}_test"
$databaseUser = "quittances_postman"
$tempRoot = Join-Path $env:TEMP "quittances-postman-$tempId"
$dataDirectory = Join-Path $tempRoot "postgres"
$postgresLog = Join-Path $tempRoot "postgres.log"
$appStdout = Join-Path $tempRoot "app.stdout.log"
$appStderr = Join-Path $tempRoot "app.stderr.log"
$runtimeEnvironment = Join-Path $tempRoot "newman-environment.json"
$rawReport = Join-Path $tempRoot "newman-report.raw.json"
$fixtureSql = Join-Path $tempRoot "fixtures.sql"
$dbPort = $null
$postgresStarted = $false
$appProcess = $null
$secret = $null
$jwtBailleur = $null
$jwtGestionnaire = $null
$newmanExitCode = $null
$reportWritten = $false
$deleteTempRoot = $true

$environmentNames = @(
    "JAVA_HOME",
    "Path",
    "JWT_MODE",
    "JWT_SECRET",
    "SERVER_ADDRESS",
    "SERVER_PORT",
    "QUITTANCES_DB_HOST",
    "QUITTANCES_DB_PORT",
    "QUITTANCES_DB_NAME",
    "QUITTANCES_DB_USER",
    "QUITTANCES_DB_PASSWORD",
    "QUITTANCES_STORAGE_PATH",
    "PAIEMENTS_MODE",
    "PAIEMENTS_BASE_URL",
    "PAIEMENTS_TOKEN",
    "LOCATAIRES_MODE",
    "LOCATAIRES_BASE_URL",
    "LOCATAIRES_TOKEN",
    "BIENS_MODE",
    "BIENS_BASE_URL",
    "BIENS_TOKEN",
    "BAUX_MODE",
    "BAUX_BASE_URL",
    "BAUX_TOKEN",
    "PGHOST",
    "PGPORT",
    "PGUSER",
    "PGPASSWORD",
    "SPRING_APPLICATION_JSON",
    "SPRING_PROFILES_ACTIVE",
    "SPRING_CONFIG_LOCATION",
    "SPRING_CONFIG_ADDITIONAL_LOCATION"
)
$environmentNames += @(
    Get-ChildItem Env: |
        Where-Object { $_.Name -like "SPRING_DATASOURCE_*" } |
        Select-Object -ExpandProperty Name
)
$environmentNames = @($environmentNames | Select-Object -Unique)
$oldEnvironment = @{}
foreach ($name in $environmentNames) {
    $oldEnvironment[$name] =
        [Environment]::GetEnvironmentVariable($name, "Process")
}

try {
    New-Item -ItemType Directory -Path $tempRoot | Out-Null

    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" +
        $oldEnvironment["Path"]
    $mavenVersion = (& $maven.Source -version 2>&1 | Out-String)
    if ($mavenVersion -notmatch "Java version:\s+21(?:\.|\s)") {
        throw "Maven ne s'exécute pas avec Java 21."
    }

    Push-Location $racineDepot
    try {
        & $maven.Source -f $pom -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "La construction Maven a échoué ($LASTEXITCODE)."
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Le JAR exécutable Quittances est introuvable après le package."
    }

    $tcpListener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback,
        0
    )
    $tcpListener.Start()
    $dbPort = $tcpListener.LocalEndpoint.Port
    $tcpListener.Stop()

    if ($database -notmatch "^[a-z0-9_]+_test$") {
        throw "Refus d'utiliser une base dont le nom ne finit pas par _test."
    }

    & $executables.initdb `
        -D $dataDirectory `
        -U $databaseUser `
        --auth-local=trust `
        --auth-host=trust `
        --encoding=UTF8 `
        --no-locale
    if ($LASTEXITCODE -ne 0) {
        throw "L'initialisation du cluster PostgreSQL temporaire a échoué."
    }

    & $executables.pg_ctl `
        -D $dataDirectory `
        -l $postgresLog `
        -o "-h 127.0.0.1 -p $dbPort -c listen_addresses=127.0.0.1" `
        -w start
    if ($LASTEXITCODE -ne 0) {
        throw "Le démarrage du cluster PostgreSQL temporaire a échoué."
    }
    $postgresStarted = $true

    $env:PGHOST = "127.0.0.1"
    $env:PGPORT = "$dbPort"
    $env:PGUSER = $databaseUser
    $env:PGPASSWORD = ""
    & $executables.createdb `
        -h 127.0.0.1 -p $dbPort -U $databaseUser $database
    if ($LASTEXITCODE -ne 0) {
        throw "La création de la base PostgreSQL suffixée _test a échoué."
    }

    $randomBytes = New-Object byte[] 48
    $randomGenerator =
        [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $randomGenerator.GetBytes($randomBytes)
        $secret = [Convert]::ToBase64String($randomBytes)
    }
    finally {
        $randomGenerator.Dispose()
        [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    }

    $bailleurId = [Guid]::NewGuid().ToString()
    $documentBailleurId = [Guid]::NewGuid().ToString()
    $documentAutreBailleurId = [Guid]::NewGuid().ToString()
    $bailleurEtrangerId = [Guid]::NewGuid().ToString()
    $jwtBailleur = New-TestJwt $secret $bailleurId "BAILLEUR"
    $jwtGestionnaire = New-TestJwt `
        $secret ([Guid]::NewGuid().ToString()) "GESTIONNAIRE"

    $env:QUITTANCES_DB_HOST = "127.0.0.1"
    $env:QUITTANCES_DB_PORT = "$dbPort"
    $env:QUITTANCES_DB_NAME = $database
    $env:QUITTANCES_DB_USER = $databaseUser
    $env:QUITTANCES_DB_PASSWORD = [Guid]::NewGuid().ToString("N")
    $env:QUITTANCES_STORAGE_PATH = Join-Path $tempRoot "storage"
    $env:JWT_MODE = "COMMUN"
    $env:JWT_SECRET = $secret
    $env:SERVER_ADDRESS = "127.0.0.1"
    $env:SERVER_PORT = "18084"
    $env:PAIEMENTS_MODE = "SIMULATION"
    $env:PAIEMENTS_BASE_URL = "http://127.0.0.1:1"
    $env:PAIEMENTS_TOKEN = ""
    $env:LOCATAIRES_MODE = "SIMULATION"
    $env:LOCATAIRES_BASE_URL = "http://127.0.0.1:1"
    $env:LOCATAIRES_TOKEN = ""
    $env:BIENS_MODE = "SIMULATION"
    $env:BIENS_BASE_URL = "http://127.0.0.1:1"
    $env:BIENS_TOKEN = ""
    $env:BAUX_MODE = "SIMULATION"
    $env:BAUX_BASE_URL = "http://127.0.0.1:1"
    $env:BAUX_TOKEN = ""
    $env:SPRING_APPLICATION_JSON = $null
    $env:SPRING_PROFILES_ACTIVE = $null
    $env:SPRING_CONFIG_LOCATION = $null
    $env:SPRING_CONFIG_ADDITIONAL_LOCATION = $null
    foreach ($name in @(
            Get-ChildItem Env: |
                Where-Object { $_.Name -like "SPRING_DATASOURCE_*" } |
                Select-Object -ExpandProperty Name
        )) {
        [Environment]::SetEnvironmentVariable($name, $null, "Process")
    }

    $appProcess = Start-Process `
        -FilePath $java `
        -ArgumentList "-jar `"$jar`"" `
        -WorkingDirectory $tempRoot `
        -RedirectStandardOutput $appStdout `
        -RedirectStandardError $appStderr `
        -PassThru

    $applicationReady = $false
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($appProcess.HasExited) {
            break
        }

        $tcpClient = [System.Net.Sockets.TcpClient]::new()
        try {
            $connection = $tcpClient.BeginConnect(
                "127.0.0.1",
                18084,
                $null,
                $null
            )
            if ($connection.AsyncWaitHandle.WaitOne(500) -and
                    $tcpClient.Connected) {
                $tcpClient.EndConnect($connection)
                $applicationReady = $true
                break
            }
        }
        catch {
            # The loop retries while this newly started application initializes.
        }
        finally {
            $tcpClient.Dispose()
        }

        Start-Sleep -Seconds 1
    }

    if (-not $applicationReady) {
        $tail = @()
        if (Test-Path -LiteralPath $appStdout) {
            $tail += Get-Content -LiteralPath $appStdout -Tail 30
        }
        if (Test-Path -LiteralPath $appStderr) {
            $tail += Get-Content -LiteralPath $appStderr -Tail 30
        }
        throw "Spring Boot n'a pas ouvert le port 18084. Sortie recente: $($tail -join ' ')"
    }

    $sql = @"
INSERT INTO quittance (
    id, bail_id, locataire_id, bien_id, bailleur_id,
    periode_debut, periode_fin, loyer_du, charges_dues,
    loyer_paye, charges_payees, total_paye, devise, type_document,
    statut, statut_signature, donnees_figees, version
) VALUES
(
    '$documentBailleurId', 'bail-test-proprietaire', 'locataire-test-001',
    'bien-test-001', '$bailleurId', DATE '2026-10-01', DATE '2026-10-31',
    1000.00, 100.00, 1000.00, 100.00, 1100.00, 'MAD',
    'QUITTANCE_INTEGRALE', 'BROUILLON', 'NON_SIGNE',
    '{"bailleurNom":"Bailleur synthetique","locataireNom":"Locataire synthetique","locatairePrenom":"Test","bienAdresse":"Adresse de test"}',
    0
),
(
    '$documentAutreBailleurId', 'bail-test-tiers', 'locataire-test-002',
    'bien-test-002', '$bailleurEtrangerId', DATE '2026-10-01', DATE '2026-10-31',
    800.00, 80.00, 800.00, 80.00, 880.00, 'MAD',
    'QUITTANCE_INTEGRALE', 'BROUILLON', 'NON_SIGNE',
    '{"bailleurNom":"Autre bailleur synthetique","locataireNom":"Autre locataire synthetique","locatairePrenom":"Test","bienAdresse":"Autre adresse de test"}',
    0
);
"@
    [IO.File]::WriteAllText(
        $fixtureSql,
        $sql,
        [Text.UTF8Encoding]::new($false)
    )
    & $executables.psql `
        -h 127.0.0.1 -p $dbPort -U $databaseUser -d $database `
        -v ON_ERROR_STOP=1 -f $fixtureSql
    if ($LASTEXITCODE -ne 0) {
        throw "L'insertion des fixtures synthétiques a échoué."
    }
    Remove-Item -LiteralPath $fixtureSql -Force

    $runtime = Get-Content -LiteralPath $environmentFile -Raw |
        ConvertFrom-Json
    foreach ($variable in $runtime.values) {
        switch ($variable.key) {
            "baseUrl" { $variable.value = "http://127.0.0.1:18084" }
            "tokenBailleur" { $variable.value = $jwtBailleur }
            "tokenGestionnaire" { $variable.value = $jwtGestionnaire }
            "documentBailleurId" {
                $variable.value = $documentBailleurId
            }
            "documentAutreBailleurId" {
                $variable.value = $documentAutreBailleurId
            }
        }
    }
    [IO.File]::WriteAllText(
        $runtimeEnvironment,
        (ConvertTo-Json -InputObject $runtime -Depth 10),
        [Text.UTF8Encoding]::new($false)
    )

    $collectionObject = Get-Content -LiteralPath $collection -Raw |
        ConvertFrom-Json
    function Get-PostmanRequests {
        param([object[]]$Items)

        foreach ($item in $Items) {
            if ($item.request) {
                $item
            }
            elseif ($item.item) {
                Get-PostmanRequests $item.item
            }
        }
    }

    $requests = @(Get-PostmanRequests $collectionObject.item)
    $configuredAssertions = 0
    foreach ($request in $requests) {
        if ($request.request.method -ne "GET") {
            throw "Le runner refuse une requête qui n'est pas GET."
        }
        foreach ($event in $request.event) {
            if ($event.listen -eq "test") {
                $scriptText = $event.script.exec -join "`n"
                $configuredAssertions += [regex]::Matches(
                    $scriptText,
                    "pm\.test\s*\("
                ).Count
            }
        }
    }
    if ($requests.Count -ne 9 -or $configuredAssertions -ne 19) {
        throw "La collection a changé : attentes actuelles 9 requêtes GET / 19 tests configurés."
    }

    New-Item -ItemType Directory -Path $rapportDirectory -Force |
        Out-Null
    $newmanExitCode = 0
    & $newman run $collection `
        --environment $runtimeEnvironment `
        --reporters cli,json `
        --reporter-json-export $rawReport
    $newmanExitCode = $LASTEXITCODE

    if (-not (Test-Path -LiteralPath $rawReport)) {
        throw "Newman n'a pas produit le rapport JSON attendu."
    }

    $newmanReport = Get-Content -LiteralPath $rawReport -Raw |
        ConvertFrom-Json
    $executionAssertions = @(
        foreach ($execution in $newmanReport.run.executions) {
            foreach ($assertion in $execution.assertions) {
                $assertion
            }
        }
    )
    $executedAssertions = $executionAssertions.Count
    $failedAssertions = @(
        $executionAssertions |
            Where-Object { $null -ne $_.error }
    ).Count
    $passedAssertions = $executedAssertions - $failedAssertions
    $skippedAssertions = [Math]::Max(
        0,
        $configuredAssertions - $executedAssertions
    )

    $safeReport = Protect-NewmanValue $newmanReport
    $safeJson = ConvertTo-Json -InputObject $safeReport -Depth 100
    if ($safeJson -match
            "eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+" -or
            ($secret -and $safeJson.Contains($secret)) -or
            ($jwtBailleur -and $safeJson.Contains($jwtBailleur)) -or
            ($jwtGestionnaire -and $safeJson.Contains($jwtGestionnaire))) {
        throw "Le rapport Newman contient encore une valeur sensible; il n'est pas conservé."
    }
    [IO.File]::WriteAllText(
        $rapportJson,
        $safeJson,
        [Text.UTF8Encoding]::new($false)
    )
    $html = [Text.StringBuilder]::new()
    [void]$html.AppendLine("<!doctype html>")
    [void]$html.AppendLine('<html lang="fr"><head><meta charset="utf-8">')
    [void]$html.AppendLine(
        "<title>Rapport Newman - Quittances</title>"
    )
    [void]$html.AppendLine(
        "<style>body{font:15px Arial,sans-serif;margin:2rem;color:#222}table{border-collapse:collapse;width:100%}th,td{border:1px solid #bbb;padding:.45rem;text-align:left}th{background:#eee}.ok{color:#176b2c}</style>"
    )
    [void]$html.AppendLine("</head><body>")
    [void]$html.AppendLine("<h1>Tests API Quittances - Newman</h1>")
    [void]$html.AppendLine(
        "<p>Synthese generee a partir du rapport JSON Newman; aucune reponse brute n'est incluse.</p>"
    )
    [void]$html.AppendLine(
        "<p>Requetes : $($newmanReport.run.stats.requests.total); assertions configurees : $configuredAssertions; executees : $executedAssertions; reussies : $passedAssertions; echouees : $failedAssertions; ignorees/non executees : $skippedAssertions.</p>"
    )
    [void]$html.AppendLine(
        "<table><thead><tr><th>Requete</th><th>HTTP</th><th>Assertion</th><th>Resultat</th></tr></thead><tbody>"
    )
    foreach ($execution in $safeReport.run.executions) {
        $requestName = [Net.WebUtility]::HtmlEncode(
            [string]$execution.item.name
        )
        $statusCode = [Net.WebUtility]::HtmlEncode(
            [string]$execution.response.code
        )
        foreach ($assertion in $execution.assertions) {
            $assertionName = [Net.WebUtility]::HtmlEncode(
                [string]$assertion.assertion
            )
            $resultat = if ($assertion.error) {
                "ECHEC"
            }
            else {
                "REUSSITE"
            }
            $class = if ($assertion.error) { "" } else { ' class="ok"' }
            [void]$html.AppendLine(
                "<tr><td>$requestName</td><td>$statusCode</td><td>$assertionName</td><td$class>$resultat</td></tr>"
            )
        }
    }
    [void]$html.AppendLine("</tbody></table></body></html>")
    [IO.File]::WriteAllText(
        $rapportHtml,
        $html.ToString(),
        [Text.UTF8Encoding]::new($false)
    )
    $reportWritten = $true
    Remove-Item -LiteralPath $rawReport -Force
    Remove-Item -LiteralPath $runtimeEnvironment -Force

    Write-Host "Base temporaire suffixee _test : $database"
    Write-Host "Requetes Newman executees : $($newmanReport.run.stats.requests.total)"
    Write-Host "Assertions configurees : $configuredAssertions"
    Write-Host "Assertions executees : $executedAssertions"
    Write-Host "Assertions reussies : $passedAssertions"
    Write-Host "Assertions echouees : $failedAssertions"
    Write-Host "Assertions ignorees/non executees : $skippedAssertions"
    Write-Host "Rapport JSON assaini : $rapportJson"
    Write-Host "Synthese HTML assainie : $rapportHtml"
}
finally {
    if ($appProcess -and -not $appProcess.HasExited) {
        try {
            Stop-Process -Id $appProcess.Id -Force -ErrorAction Stop
            Wait-Process -Id $appProcess.Id -Timeout 20 -ErrorAction Stop
        }
        catch {
            $remainingApp = Get-Process -Id $appProcess.Id `
                -ErrorAction SilentlyContinue
            if ($remainingApp) {
                $deleteTempRoot = $false
                Write-Warning "L'instance Java PID $($appProcess.Id) est encore active; fichiers temporaires conserves."
            }
        }
    }

    if (-not $postgresStarted -and
            (Test-Path -LiteralPath $dataDirectory)) {
        & $executables.pg_ctl -D $dataDirectory status *> $null
        if ($LASTEXITCODE -eq 0) {
            $postgresStarted = $true
        }
    }

    if ($postgresStarted) {
        & $executables.pg_ctl `
            -D $dataDirectory `
            -m fast `
            -w stop
        if ($LASTEXITCODE -eq 0) {
            $postgresStarted = $false
        }
        else {
            $deleteTempRoot = $false
            Write-Warning "PostgreSQL n'a pas pu etre arrete; donnees temporaires conservees."
        }
    }

    if (Test-Path -LiteralPath $runtimeEnvironment) {
        Remove-Item -LiteralPath $runtimeEnvironment -Force
    }
    if (Test-Path -LiteralPath $rawReport) {
        Remove-Item -LiteralPath $rawReport -Force
    }
    if ($deleteTempRoot -and (Test-Path -LiteralPath $tempRoot)) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }

    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable(
            $name,
            $oldEnvironment[$name],
            "Process"
        )
    }

    $secret = $null
    $jwtBailleur = $null
    $jwtGestionnaire = $null
}

if ($null -ne $newmanExitCode -and $newmanExitCode -ne 0) {
    exit $newmanExitCode
}
if (-not $reportWritten) {
    exit 1
}

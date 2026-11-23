# Phase 5 / Quittances : execution reproductible de Cypress.
# Lance uniquement le frontend React compile et les API simulees.
# Ce lanceur arrete uniquement ses propres processus de test.
# Aucun acces aux backends, aucun Git commit/push.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$frontendRoot = Join-Path $repoRoot 'frontend'
$qaRoot = Join-Path $repoRoot 'qualite-logicielle\quittances\e2e-cypress'
$reportRoot = Join-Path $repoRoot 'qualite-logicielle\quittances\rapports\cypress'
$testSpec = Join-Path $qaRoot 'cypress\e2e\quittances.cy.js'
$cypressConfig = Join-Path $qaRoot 'cypress.config.cjs'
$cypressCli = Join-Path $qaRoot 'node_modules\cypress\bin\cypress'
$viteCli = Join-Path $frontendRoot 'node_modules\vite\bin\vite.js'
$serverPort = 13003
$workRoot = Join-Path $env:TEMP ('quittances-cypress-reprise-' + [guid]::NewGuid().ToString('N'))
$buildRoot = Join-Path $workRoot 'build'
$viteOut = Join-Path $workRoot 'vite-out.log'
$viteErr = Join-Path $workRoot 'vite-err.log'
$cypressOut = Join-Path $workRoot 'cypress-out.log'
$cypressErr = Join-Path $workRoot 'cypress-err.log'
$previewProcess = $null
$cypressProcess = $null
$oldApiBase = $env:VITE_QUITTANCES_API_BASE_URL
$completedReport = $null

function Stop-OwnedProcessTree {
    param([int]$RootProcessId)
    if ($RootProcessId -le 0) { return }
    $snapshot = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $pending = @($RootProcessId)
    $visited = @{}
    $sequence = New-Object 'System.Collections.Generic.List[int]'
    while ($pending.Count -gt 0) {
        $current = [int]$pending[0]
        if ($pending.Count -eq 1) { $pending = @() }
        else { $pending = @($pending | Select-Object -Skip 1) }
        if ($visited.ContainsKey($current)) { continue }
        $visited[$current] = $true
        $sequence.Add($current)
        $children = @($snapshot | Where-Object { $_.ParentProcessId -eq $current })
        foreach ($child in $children) { $pending += [int]$child.ProcessId }
    }
    for ($i = $sequence.Count - 1; $i -ge 0; $i--) {
        Stop-Process -Id $sequence[$i] -Force -ErrorAction SilentlyContinue
    }
}

function Read-CypressCases {
    param([string]$ReportPath)
    try {
        [xml]$xmlContent = Get-Content -LiteralPath $ReportPath -Raw -Encoding UTF8 -ErrorAction Stop
        return @($xmlContent.SelectNodes('//testcase'))
    }
    catch { return @() }
}

Set-Location $repoRoot
foreach ($required in @($testSpec, $cypressConfig, $cypressCli, $viteCli)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
        throw "Fichier requis introuvable : $required"
    }
}
$nodeExe = (Get-Command node.exe -ErrorAction Stop).Source
$npmExe = (Get-Command npm.cmd -ErrorAction Stop).Source

Write-Host '===== 1. CONTROLE DU PORT DE TEST ====='
if (@(Get-NetTCPConnection -State Listen -LocalPort $serverPort -ErrorAction SilentlyContinue).Count -gt 0) {
    throw "Le port $serverPort est occupe. Aucune connexion a un serveur inconnu."
}
New-Item -ItemType Directory -Path $workRoot -Force | Out-Null
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
$previousReports = @{}
Get-ChildItem -LiteralPath $reportRoot -Filter 'TEST-cypress-*.xml' -File -ErrorAction SilentlyContinue |
    ForEach-Object { $previousReports[$_.Name] = $true }
$env:VITE_QUITTANCES_API_BASE_URL = '/api/quittances/api/v1'

try {
    Write-Host '===== 2. COMPILATION REACT HORS DU DEPOT ====='
    Push-Location $frontendRoot
    try {
        & $npmExe run build -- --outDir $buildRoot --emptyOutDir
        if ($LASTEXITCODE -ne 0) { throw "Build React echoue : code $LASTEXITCODE" }
    }
    finally { Pop-Location }

    Write-Host '===== 3. SERVEUR PREVIEW INDEPENDANT ====='
    $viteArgs = ('"{0}" preview --host 127.0.0.1 --port {1} --strictPort --outDir "{2}"' -f $viteCli, $serverPort, $buildRoot)
    $previewProcess = Start-Process -FilePath $nodeExe -ArgumentList $viteArgs `
        -WorkingDirectory $frontendRoot -PassThru `
        -RedirectStandardOutput $viteOut -RedirectStandardError $viteErr

    $listening = $false
    for ($i = 0; $i -lt 80; $i++) {
        $previewProcess.Refresh()
        if ($previewProcess.HasExited) { throw 'Vite preview a quitte prematurement.' }
        try {
            $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$serverPort/login" `
                -UseBasicParsing -TimeoutSec 3
            if ($resp.StatusCode -eq 200) { $listening = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 350
    }
    if (-not $listening) { throw 'Le serveur preview ne repond pas.' }
    Write-Host "OK : http://127.0.0.1:$serverPort/login"

    Write-Host '===== 4. EXECUTION DES 7 SCENARIOS CYPRESS ====='
    Write-Host 'Navigateur : Electron ; API interceptees ; aucun backend reel.'
    $cliArgs = ('"{0}" run --browser electron --project "{1}" --config-file "{2}" --config "baseUrl=http://127.0.0.1:{3}" --spec "{4}"' -f $cypressCli,$qaRoot,$cypressConfig,$serverPort,$testSpec)
    $cypressProcess = Start-Process -FilePath $nodeExe -ArgumentList $cliArgs `
        -WorkingDirectory $qaRoot -PassThru `
        -RedirectStandardOutput $cypressOut -RedirectStandardError $cypressErr

    # Sous Windows, Cypress peut rester actif apres avoir ecrit le JUnit.
    # On considere la generation d'un XML complet de 7 cas comme signal de fin.
    $endAt = (Get-Date).AddMinutes(5)
    $processExitSeen = $null
    while ((Get-Date) -lt $endAt) {
        $possible = @(
            Get-ChildItem -LiteralPath $reportRoot -Filter 'TEST-cypress-*.xml' -File -ErrorAction SilentlyContinue |
                Where-Object { -not $previousReports.ContainsKey($_.Name) } |
                Sort-Object LastWriteTime -Descending
        )
        foreach ($candidate in $possible) {
            $cases = @(Read-CypressCases -ReportPath $candidate.FullName)
            if ($cases.Count -eq 7 -and (Get-Date) -gt $candidate.LastWriteTime.AddSeconds(1)) {
                $completedReport = $candidate
                break
            }
        }
        if ($null -ne $completedReport) { break }
        $cypressProcess.Refresh()
        if ($cypressProcess.HasExited) {
            if ($null -eq $processExitSeen) { $processExitSeen = Get-Date }
            elseif ((Get-Date) -gt $processExitSeen.AddSeconds(5)) { break }
        }
        Start-Sleep -Milliseconds 900
    }

    if ($null -eq $completedReport) {
        Write-Host '===== DERNIERES LIGNES DU PROCESSUS CYPRESS ====='
        if (Test-Path -LiteralPath $cypressOut) { Get-Content -LiteralPath $cypressOut -Tail 65 }
        if (Test-Path -LiteralPath $cypressErr) { Get-Content -LiteralPath $cypressErr -Tail 30 }
        throw 'Aucun nouveau rapport complet de 7 scenarios. Ne pas creer de commit.'
    }

    Write-Host '===== 5. RESULTATS DES SEPT SCENARIOS ====='
    Write-Host "Rapport : $($completedReport.FullName)"
    $scenarios = @(Read-CypressCases -ReportPath $completedReport.FullName)
    $passed = 0
    $failed = 0
    $skipped = 0
    foreach ($scenario in $scenarios) {
        $failureNode = $scenario.SelectSingleNode('./failure | ./error')
        $skippedNode = $scenario.SelectSingleNode('./skipped')
        $scenarioName = $scenario.GetAttribute('name')
        if ($null -ne $failureNode) {
            $failed++
            Write-Host "[ECHEC] $scenarioName"
            $message = $failureNode.GetAttribute('message')
            if ([string]::IsNullOrWhiteSpace($message)) { $message = $failureNode.InnerText }
            if ($message.Length -gt 1400) { $message = $message.Substring(0,1400) }
            Write-Host "ERREUR : $message"
        }
        elseif ($null -ne $skippedNode) {
            $skipped++
            Write-Host "[IGNORE] $scenarioName"
        }
        else {
            $passed++
            Write-Host "[REUSSI] $scenarioName"
        }
    }
    Write-Host "SYNTHESE CYPRESS : total=$($scenarios.Count), reussis=$passed, echecs=$failed, ignores=$skipped"
    if ($failed -gt 0 -or $skipped -gt 0 -or $passed -ne 7) {
        throw 'La suite doit etre corrigee avant le commit Cypress.'
    } else {
        Write-Host 'SUCCES : les 7 scenarios ont reussi sur React compile.'
    }
}
finally {
    Write-Host '===== 6. NETTOYAGE DE NOS PROCESSUS UNIQUEMENT ====='
    if ($null -ne $cypressProcess) { Stop-OwnedProcessTree -RootProcessId ([int]$cypressProcess.Id) }
    if ($null -ne $previewProcess) { Stop-OwnedProcessTree -RootProcessId ([int]$previewProcess.Id) }
    $env:VITE_QUITTANCES_API_BASE_URL = $oldApiBase
    if (@(Get-NetTCPConnection -State Listen -LocalPort $serverPort -ErrorAction SilentlyContinue).Count -eq 0) {
        Write-Host "Port $serverPort libre."
    } else {
        Write-Warning "Port $serverPort encore occupe : ne pas toucher aux autres services."
    }
    Set-Location $repoRoot
    Write-Host '===== ETAT GIT ====='
    git status -sb
    Write-Host "Journaux temporaires : $workRoot"
    Write-Host 'Aucun push ni commit effectue.'
}

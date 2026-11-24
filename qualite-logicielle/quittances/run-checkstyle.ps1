param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$JavaHome
)

$ErrorActionPreference = "Stop"

$racineDepot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$pom = Join-Path $racineDepot "backend\backend_gestion_quittances\pom.xml"
$dossierRapport = Join-Path $racineDepot "qualite-logicielle\quittances\rapports\checkstyle"

if (-not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "JDK executable not found under JavaHome."
}
if (-not (Test-Path -LiteralPath $pom)) {
    throw "The Quittances pom.xml was not found."
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $maven) {
    throw "Maven (mvn) must be available in PATH."
}

$oldJavaHome = $env:JAVA_HOME
$oldPath = $env:Path
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + ";" + $oldPath

    $versionMaven = (& $maven.Source -version 2>&1 | Out-String)
    if ($versionMaven -notmatch "Java version:\s+21(?:\.|\s)") {
        throw "Maven is not using Java 21. Check JAVA_HOME."
    }

    if (Test-Path -LiteralPath $dossierRapport) {
        Remove-Item -LiteralPath $dossierRapport -Recurse -Force
    }
    New-Item -ItemType Directory -Path $dossierRapport -Force | Out-Null
    Push-Location $racineDepot
    try {
        & $maven.Source -f $pom checkstyle:check checkstyle:checkstyle
        if ($LASTEXITCODE -ne 0) {
            throw "Checkstyle goals failed with exit code $LASTEXITCODE."
        }

        $xmlRapport = Join-Path $dossierRapport "checkstyle-result.xml"
        $htmlRapport = Join-Path $dossierRapport "checkstyle.html"
        if (-not (Test-Path -LiteralPath $xmlRapport)) {
            throw "Checkstyle XML report was not generated."
        }
        if (-not (Test-Path -LiteralPath $htmlRapport)) {
            throw "Checkstyle HTML report was not generated."
        }

        $documentXml = New-Object System.Xml.XmlDocument
        $documentXml.Load($xmlRapport)
        $prefixeDepot = [IO.Path]::GetFullPath($racineDepot).TrimEnd(
            [char]'\',
            [char]'/'
        ) + [IO.Path]::DirectorySeparatorChar
        foreach ($fichier in $documentXml.SelectNodes("/checkstyle/file")) {
            $cheminFichier = [IO.Path]::GetFullPath($fichier.GetAttribute("name"))
            if (-not $cheminFichier.StartsWith(
                $prefixeDepot,
                [StringComparison]::OrdinalIgnoreCase
            )) {
                throw "Checkstyle reported a file outside the repository."
            }
            $cheminRelatif = $cheminFichier.Substring($prefixeDepot.Length)
            $fichier.SetAttribute("name", $cheminRelatif.Replace('\', '/'))
        }
        $documentXml.Save($xmlRapport)

        foreach ($rapport in @($xmlRapport, $htmlRapport)) {
            if (Select-String -LiteralPath $rapport -SimpleMatch $racineDepot -Quiet) {
                throw "A report contains a local workspace path."
            }
        }

        Write-Host "Checkstyle XML/HTML reports: $dossierRapport"
    }
    finally {
        Pop-Location
    }
}
finally {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $oldJavaHome, "Process")
    [Environment]::SetEnvironmentVariable("Path", $oldPath, "Process")
}

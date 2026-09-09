param(
    [switch]$CompileOnly
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsDirectory = Join-Path $projectRoot '.tools'
$portableJdkDirectory = Join-Path $toolsDirectory 'jdk'
$outputDirectory = Join-Path $projectRoot 'out'

function Find-StudyFlowJdk {
    $pathJavac = Get-Command javac -ErrorAction SilentlyContinue
    $pathJava = Get-Command java -ErrorAction SilentlyContinue
    if ($pathJavac -and $pathJava) {
        return [pscustomobject]@{
            Javac = $pathJavac.Path
            Java = $pathJava.Path
        }
    }

    $candidateBins = New-Object System.Collections.Generic.List[string]

    if ($env:JAVA_HOME) {
        $candidateBins.Add((Join-Path $env:JAVA_HOME 'bin'))
    }

    $programFilesDirectory = [Environment]::GetFolderPath('ProgramFiles')
    $localAppDataDirectory = [Environment]::GetFolderPath('LocalApplicationData')
    $searchRoots = @(
        $portableJdkDirectory,
        (Join-Path $programFilesDirectory 'Java'),
        (Join-Path $programFilesDirectory 'Eclipse Adoptium'),
        (Join-Path $programFilesDirectory 'Microsoft'),
        (Join-Path $programFilesDirectory 'Android\Android Studio\jbr'),
        (Join-Path $localAppDataDirectory 'Programs\Eclipse Adoptium')
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }

        $candidateBins.Add((Join-Path $root 'bin'))
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $candidateBins.Add((Join-Path $_.FullName 'bin'))
        }
    }

    foreach ($binDirectory in $candidateBins) {
        $javacPath = Join-Path $binDirectory 'javac.exe'
        $javaPath = Join-Path $binDirectory 'java.exe'
        if ((Test-Path -LiteralPath $javacPath) -and (Test-Path -LiteralPath $javaPath)) {
            return [pscustomobject]@{
                Javac = $javacPath
                Java = $javaPath
            }
        }
    }

    return $null
}

function Install-PortableStudyFlowJdk {
    $architecture = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
    $downloadUrl = "https://aka.ms/download-jdk/microsoft-jdk-21-windows-$architecture.zip"
    $checksumUrl = "$downloadUrl.sha256sum.txt"
    $archivePath = Join-Path $toolsDirectory 'microsoft-openjdk-21.zip'
    $checksumPath = Join-Path $toolsDirectory 'microsoft-openjdk-21.sha256.txt'

    New-Item -ItemType Directory -Force -Path $toolsDirectory | Out-Null

    Write-Host ''
    Write-Host 'Java was not found. StudyFlow will download a portable Microsoft OpenJDK 21.' -ForegroundColor Yellow
    Write-Host 'This one-time download is approximately 200 MB and stays inside the project folder.' -ForegroundColor DarkGray
    Write-Host ''

    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing
        Invoke-WebRequest -Uri $checksumUrl -OutFile $checksumPath -UseBasicParsing

        $expectedHash = ((Get-Content -LiteralPath $checksumPath -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
        $actualHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($expectedHash -ne $actualHash) {
            throw 'The downloaded JDK checksum did not match the official checksum.'
        }

        if (Test-Path -LiteralPath $portableJdkDirectory) {
            Remove-Item -LiteralPath $portableJdkDirectory -Recurse -Force
        }
        New-Item -ItemType Directory -Force -Path $portableJdkDirectory | Out-Null
        Write-Host 'Extracting Java...' -ForegroundColor DarkGray
        Expand-Archive -LiteralPath $archivePath -DestinationPath $portableJdkDirectory -Force
    }
    catch {
        throw "Java could not be downloaded automatically. Install JDK 21 from https://learn.microsoft.com/java/openjdk/download and run this script again. Details: $($_.Exception.Message)"
    }
    finally {
        if (Test-Path -LiteralPath $archivePath) {
            Remove-Item -LiteralPath $archivePath -Force
        }
        if (Test-Path -LiteralPath $checksumPath) {
            Remove-Item -LiteralPath $checksumPath -Force
        }
    }
}

$jdk = Find-StudyFlowJdk
if (-not $jdk) {
    Install-PortableStudyFlowJdk
    $jdk = Find-StudyFlowJdk
}

if (-not $jdk) {
    throw 'Java was downloaded but its compiler could not be located. Delete the .tools folder and run this script again.'
}

$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' |
    Select-Object -ExpandProperty FullName

if (-not $sourceFiles) {
    throw 'No Java source files were found in the src folder.'
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
Write-Host 'Compiling StudyFlow...' -ForegroundColor DarkGray
& $jdk.Javac -encoding UTF-8 -d $outputDirectory $sourceFiles

if ($LASTEXITCODE -ne 0) {
    throw 'Compilation failed. Review the Java compiler messages above.'
}

if ($CompileOnly) {
    Write-Host 'Compilation completed successfully.' -ForegroundColor Green
    exit 0
}

Write-Host 'Launching StudyFlow...' -ForegroundColor Green
& $jdk.Java -cp $outputDirectory com.studyflow.AdaptiveStudyPlanner

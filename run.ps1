param(
    [switch]$CompileOnly,
    [switch]$Desktop,
    [switch]$Web,
    [switch]$Lan,
    [ValidateRange(1, 65535)][int]$Port = 8080,
    [string]$BindAddress = '127.0.0.1',
    [string]$PublicUrl = '',
    [string]$DataDirectory = ''
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsDirectory = Join-Path $projectRoot '.tools'
$portableJdkDirectory = Join-Path $toolsDirectory 'jdk'
$outputDirectory = Join-Path $projectRoot 'out'

function Find-StudyFlowLanAddress {
    foreach ($network in [Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces()) {
        if ($network.OperationalStatus -ne [Net.NetworkInformation.OperationalStatus]::Up) { continue }
        $properties = $network.GetIPProperties()
        $hasIpv4Gateway = $properties.GatewayAddresses | Where-Object {
            $_.Address.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork -and
            -not $_.Address.Equals([Net.IPAddress]::Any)
        }
        if (-not $hasIpv4Gateway) { continue }
        foreach ($address in $properties.UnicastAddresses) {
            $ip = $address.Address
            if ($ip.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork -and
                -not [Net.IPAddress]::IsLoopback($ip) -and
                -not $ip.IPAddressToString.StartsWith('169.254.')) {
                return $ip.IPAddressToString
            }
        }
    }
    return $null
}

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

$libraryDirectory = Join-Path $toolsDirectory 'lib'
New-Item -ItemType Directory -Force -Path $libraryDirectory | Out-Null
$dependencies = Get-Content -LiteralPath (Join-Path $projectRoot 'dependencies.json') -Raw | ConvertFrom-Json
foreach ($dependency in $dependencies) {
    $libraryPath = Join-Path $libraryDirectory $dependency.name
    if (-not (Test-Path -LiteralPath $libraryPath)) {
        Write-Host "Downloading $($dependency.name)..." -ForegroundColor DarkGray
        Invoke-WebRequest -UseBasicParsing -Uri $dependency.url -OutFile $libraryPath
    }
    $libraryHash = (Get-FileHash -LiteralPath $libraryPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($libraryHash -ne $dependency.sha256) {
        throw "Checksum mismatch for $($dependency.name). Remove that file from .tools/lib and run again."
    }
}
$classPath = "$outputDirectory;$libraryDirectory/*"

if (-not $sourceFiles) {
    throw 'No Java source files were found in the src folder.'
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
Write-Host 'Compiling StudyFlow...' -ForegroundColor DarkGray
& $jdk.Javac --release 21 -encoding UTF-8 -cp "$libraryDirectory/*" -d $outputDirectory $sourceFiles

if ($LASTEXITCODE -ne 0) {
    throw 'Compilation failed. Review the Java compiler messages above.'
}

if ($CompileOnly) {
    Write-Host 'Compilation completed successfully.' -ForegroundColor Green
    exit 0
}

if ($Lan) {
    $Web = $true
    $BindAddress = '0.0.0.0'
    if (-not $PublicUrl) {
        $lanAddress = Find-StudyFlowLanAddress
        if (-not $lanAddress) {
            throw 'No active Wi-Fi or Ethernet address was found. Connect this computer to the same network as the other devices and try again.'
        }
        $PublicUrl = "http://${lanAddress}:$Port"
    }
}

if (-not $Web) {
    Write-Host 'Launching the StudyFlow desktop application...' -ForegroundColor Green
    & $jdk.Java -cp $classPath com.studyflow.AdaptiveStudyPlanner
} else {
    $env:STUDYFLOW_PORT = [string]$Port
    $env:STUDYFLOW_HOST = $BindAddress
    if ($PublicUrl) { $env:STUDYFLOW_ORIGIN = $PublicUrl }
    elseif (-not $env:STUDYFLOW_ORIGIN) { $env:STUDYFLOW_ORIGIN = "http://localhost:$Port" }
    if ($DataDirectory) { $env:STUDYFLOW_DATA_DIR = [IO.Path]::GetFullPath($DataDirectory) }
    elseif (-not $env:STUDYFLOW_DATA_DIR) { $env:STUDYFLOW_DATA_DIR = Join-Path $projectRoot 'data' }
    $env:STUDYFLOW_WEB_DIR = Join-Path $projectRoot 'web'
    if ($Lan) {
        Write-Host ''
        Write-Host 'Same-Wi-Fi address for phones and laptops:' -ForegroundColor Yellow
        Write-Host "  $PublicUrl" -ForegroundColor Cyan
        Write-Host 'Keep this window open while people use StudyFlow.' -ForegroundColor DarkGray
        Write-Host ''
    }
    Write-Host 'Starting StudyFlow. Open the URL printed below in your browser.' -ForegroundColor Green
    & $jdk.Java -cp $classPath com.studyflow.server.StudyFlowServer
}
if ($LASTEXITCODE -ne 0) { throw 'StudyFlow stopped with an error. Review the message above.' }

# scripts/generate-certs.ps1
param (
    [Parameter(Mandatory=$true)]
    [System.Security.SecureString]$Password
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CertsDir = Join-Path $ScriptDir "../certs"

if (!(Test-Path $CertsDir)) {
    New-Item -ItemType Directory -Path $CertsDir
}

Push-Location $CertsDir

# Convert SecureString to plain string for the temp file (securely handled in cleanup)
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password)
$PlainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)

$PassFile = Join-Path $CertsDir ".pass"
$PlainPassword | Out-File -FilePath $PassFile -Encoding ASCII -NoNewline

try {
    $DnameBase = "OU=Education, O=Example, L=Stockholm, S=SE, C=SE"

    # 1. Generate Root CA
    Write-Host "Generating Root CA..." -ForegroundColor Cyan
    keytool -genkeypair -alias rootca -keyalg RSA -keysize 4096 -ext bc:c -validity 3650 `
        -keystore ca.jks -storepass:file "$PassFile" -keypass:file "$PassFile" -dname "CN=Labb-Root-CA, $DnameBase"

    # 2. Export Root CA certificate
    keytool -exportcert -alias rootca -keystore ca.jks -storepass:file "$PassFile" -file ca.crt -rfc

    # 3. Create Truststore (common for all services)
    Write-Host "Creating Truststore..." -ForegroundColor Cyan
    keytool -importcert -alias rootca -file ca.crt -keystore truststore.jks -storepass:file "$PassFile" -noprompt

    # 4. Function to generate service keystore
    function New-ServiceCert($name) {
        Write-Host "Generating certificate for $name..." -ForegroundColor Cyan
        $serviceDname = "CN=$name, $DnameBase"
        
        # Create keystore and keypair
        keytool -genkeypair -alias $name -keyalg RSA -keysize 2048 -validity 365 `
            -keystore "$name.jks" -storepass:file "$PassFile" -keypass:file "$PassFile" -dname $serviceDname `
            -ext "san=dns:$name,dns:localhost"
        
        # Generate CSR
        keytool -certreq -alias $name -keystore "$name.jks" -storepass:file "$PassFile" -file "$name.csr"
        
        # Sign with Root CA
        keytool -gencert -alias rootca -keystore ca.jks -storepass:file "$PassFile" -infile "$name.csr" -outfile "$name.crt" `
            -ext "san=dns:$name,dns:localhost" -rfc -validity 365
        
        # Import Root CA certificate (required for the chain)
        keytool -importcert -alias rootca -file ca.crt -keystore "$name.jks" -storepass:file "$PassFile" -noprompt
        
        # Import the signed certificate
        keytool -importcert -alias $name -file "$name.crt" -keystore "$name.jks" -storepass:file "$PassFile" -noprompt
        
        # Cleanup temp files
        if (Test-Path "$name.csr") { Remove-Item "$name.csr" }
    }

    New-ServiceCert "user-service"
    New-ServiceCert "auth-service"
    New-ServiceCert "message-service"

    Write-Host "Certificates generated successfully in certs/ directory." -ForegroundColor Green
}
finally {
    Pop-Location
    if (Test-Path $PassFile) {
        Remove-Item $PassFile -Force
    }
}

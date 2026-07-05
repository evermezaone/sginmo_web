# Deploy de sginmo-web a la VPS (build + scp + redeploy por scanner).
# Uso:  powershell -File tools\deploy-vps.ps1            (build + deploy)
#       powershell -File tools\deploy-vps.ps1 -SoloDeploy (sube el WAR ya compilado)
# Requiere: alias SSH "sginmo-vps" (clave ~/.ssh/sginmo_vps) y WildFly corriendo en la VPS
# (~/apps/sginmo/start-wildfly.sh). El scanner de deployments hace el redeploy solo.
param([switch]$SoloDeploy)

$ErrorActionPreference = 'Stop'
$mig = Split-Path -Parent $PSScriptRoot
$proyecto = Join-Path $mig 'Desarrollo\sginmo-web'
$war = Join-Path $proyecto 'target\sginmo-web.war'

if (-not $SoloDeploy) {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-23'
    $env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
    Set-Location $proyecto
    & (Join-Path $mig 'herramientas\apache-maven-3.9.9\bin\mvn.cmd') -q package
    if ($LASTEXITCODE -ne 0) { Write-Error 'Build fallo'; exit 1 }
    Write-Output 'Build OK'
}

if (-not (Test-Path $war)) { Write-Error ('No existe ' + $war); exit 1 }

# Subida ATOMICA: primero a un nombre temporal (el scanner ignora .tmp) y luego mv.
# Si se sube directo al .war el scanner puede tomar el archivo a medio copiar
# ("Unexpected end of ZLIB input stream") y dejar corriendo la version vieja.
ssh -o BatchMode=yes sginmo-vps 'rm -f ~/apps/wildfly-40.0.0.Final/standalone/deployments/sginmo-web.war.deployed ~/apps/wildfly-40.0.0.Final/standalone/deployments/sginmo-web.war.failed'
scp -O $war sginmo-vps:apps/wildfly-40.0.0.Final/standalone/deployments/sginmo-web.war.tmp
if ($LASTEXITCODE -ne 0) { Write-Error 'scp fallo'; exit 1 }

ssh -o BatchMode=yes sginmo-vps 'd=~/apps/wildfly-40.0.0.Final/standalone/deployments; mv $d/sginmo-web.war.tmp $d/sginmo-web.war; for i in $(seq 1 60); do [ -f $d/sginmo-web.war.deployed ] && { echo "Redeploy OK"; break; }; [ -f $d/sginmo-web.war.failed ] && { echo "REDEPLOY FALLO:"; cat $d/sginmo-web.war.failed; exit 1; }; sleep 2; done; [ -f $d/sginmo-web.war.deployed ] || { echo "Timeout esperando redeploy"; exit 1; }; curl -s -o /dev/null -w "Verificacion VPS: HTTP %{http_code}\n" http://localhost:8080/sginmo-web/'
if ($LASTEXITCODE -ne 0) { Write-Error 'Redeploy fallo en la VPS'; exit 1 }
Write-Output 'Deploy completado.'

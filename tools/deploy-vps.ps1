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
    # Build multi-modulo desde el padre (onesystem-security + sginmo-web)
    Set-Location (Join-Path $mig 'Desarrollo')
    # clean SIEMPRE: clases de fuentes borrados/movidos en target provocan
    # DuplicateMappingException u otros fantasmas al desplegar
    & (Join-Path $mig 'herramientas\apache-maven-3.9.9\bin\mvn.cmd') -q clean package
    if ($LASTEXITCODE -ne 0) { Write-Error 'Build fallo'; exit 1 }
    Write-Output 'Build OK'
}

if (-not (Test-Path $war)) { Write-Error ('No existe ' + $war); exit 1 }

# Subida ATOMICA: primero a un nombre temporal (el scanner ignora .tmp), luego mv y
# .dodeploy para forzar el redeploy. NUNCA borrar .deployed antes de subir: el scanner
# lo interpreta como pedido de UNDEPLOY y deja la app fuera de linea.
scp -O $war sginmo-vps:apps/wildfly-40.0.0.Final/standalone/deployments/sginmo-web.war.tmp
if ($LASTEXITCODE -ne 0) { Write-Error 'scp fallo'; exit 1 }

ssh -o BatchMode=yes sginmo-vps 'd=~/apps/wildfly-40.0.0.Final/standalone/deployments; mv $d/sginmo-web.war.tmp $d/sginmo-web.war; rm -f $d/sginmo-web.war.deployed $d/sginmo-web.war.failed $d/sginmo-web.war.undeployed; touch $d/sginmo-web.war.dodeploy; for i in $(seq 1 60); do [ -f $d/sginmo-web.war.deployed ] && { echo "Redeploy OK"; break; }; [ -f $d/sginmo-web.war.failed ] && { echo "REDEPLOY FALLO:"; cat $d/sginmo-web.war.failed; exit 1; }; sleep 2; done; [ -f $d/sginmo-web.war.deployed ] || { echo "Timeout esperando redeploy"; exit 1; }; curl -s -o /dev/null -w "Verificacion VPS: HTTP %{http_code}\n" http://localhost:8080/sginmo-web/articulos.xhtml'
if ($LASTEXITCODE -ne 0) { Write-Error 'Redeploy fallo en la VPS'; exit 1 }
Write-Output 'Deploy completado.'

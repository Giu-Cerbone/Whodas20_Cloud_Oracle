# Wrapper: la logica unica e' in setup-keycloak.ps1 (Admin REST API dirette).
# La vecchia versione di questo file usava endpoint /auth/, body JSON non valido
# (newline scritte come `n) e un token "Bearer TOKEN" letterale.
$scriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
& (Join-Path $scriptDir 'setup-keycloak.ps1') @args
exit $LASTEXITCODE
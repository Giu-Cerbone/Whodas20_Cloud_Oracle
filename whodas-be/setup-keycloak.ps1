# Keycloak Setup - WHODAS 2.0 (PowerShell)
# Provisioning idempotente tramite Keycloak Admin REST API.
# Sorgente di verita: keycloak-realm.json
#
#   powershell -ExecutionPolicy Bypass -File setup-keycloak.ps1
#   powershell -ExecutionPolicy Bypass -File setup-keycloak.ps1 -VerifyOnly
#
# Override opzionali: $env:KEYCLOAK_URL, $env:KC_ADMIN_USER, $env:KC_ADMIN_PASS, $env:REALM_FILE

[CmdletBinding()]
param(
    [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'

$KC_URL = if ($env:KEYCLOAK_URL) { $env:KEYCLOAK_URL.TrimEnd('/') } else { 'http://localhost:8080' }
$ADMIN_USER = if ($env:KC_ADMIN_USER) { $env:KC_ADMIN_USER } else { 'admin' }
$ADMIN_PASS = if ($env:KC_ADMIN_PASS) { $env:KC_ADMIN_PASS } else { 'admin' }
$REALM_FILE = if ($env:REALM_FILE) { $env:REALM_FILE } else { Join-Path $PSScriptRoot 'keycloak-realm.json' }

$script:Failures = 0

function Write-Step($message, $state, $detail) {
    $map = @{ 'ok' = 'Green'; 'info' = 'Cyan'; 'warn' = 'Yellow'; 'fail' = 'Red' }
    $tag = @{ 'ok' = '[ OK ]'; 'info' = '[INFO]'; 'warn' = '[WARN]'; 'fail' = '[FAIL]' }[$state]
    $color = $map[$state]
    if ($state -eq 'fail') { $script:Failures++ }
    if ($detail) {
        Write-Host "$tag $message -> $detail" -ForegroundColor $color
    } else {
        Write-Host "$tag $message" -ForegroundColor $color
    }
}

function Write-Title($title) {
    Write-Host ''
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

function Invoke-Kc {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [string]$Token,
        $Body,
        [string]$ContentType
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $params = @{
        Method         = $Method
        Uri            = $Uri
        Headers        = $headers
        UseBasicParsing = $true
        ErrorAction    = 'Stop'
    }
    if ($null -ne $Body) {
        if ($ContentType -eq 'form') {
            $params['ContentType'] = 'application/x-www-form-urlencoded'
            $params['Body'] = $Body
        } else {
            $params['ContentType'] = 'application/json'
            $params['Body'] = ($Body | ConvertTo-Json -Depth 12 -Compress)
        }
    }
    try {
        $response = Invoke-WebRequest @params
        $parsed = $null
        if ($response.Content) {
            try { $parsed = $response.Content | ConvertFrom-Json } catch { $parsed = $response.Content }
        }
        return @{ Status = [int]$response.StatusCode; Body = $parsed }
    } catch {
        $status = 0
        $payload = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $raw = $reader.ReadToEnd()
                if ($raw) {
                    try { $payload = $raw | ConvertFrom-Json } catch { $payload = $raw }
                }
            } catch {
                $payload = $null
            }
        }
        if ($null -eq $payload) { $payload = $_.Exception.Message }
        return @{ Status = $status; Body = $payload }
    }
}

function Get-AdminToken {
    foreach ($path in @('/realms/master/protocol/openid-connect/token', '/auth/realms/master/protocol/openid-connect/token')) {
        $res = Invoke-Kc -Method Post -Uri "$KC_URL$path" -Body @{
            grant_type = 'password'
            client_id  = 'admin-cli'
            username   = $ADMIN_USER
            password   = $ADMIN_PASS
        } -ContentType 'form'
        if ($res.Status -eq 200 -and $res.Body.access_token) { return $res.Body.access_token }
    }
    throw "Token admin non ottenuto: $($res.Body.error) $($res.Body.error_description)"
}

function Get-AdminPath([string]$Token) {
    foreach ($path in @('/admin', '/auth/admin')) {
        $res = Invoke-Kc -Method Get -Uri "$KC_URL$path/realms" -Token $Token
        if ($res.Status -eq 200) { return $path }
    }
    throw 'Impossibile individuare il path della Admin REST API (provati: /admin, /auth/admin)'
}

function New-JwtPayload([string]$Token) {
    $parts = $Token.Split('.')
    $p = $parts[1].Replace('-', '+').Replace('_', '/')
    switch ($p.Length % 4) {
        2 { $p += '==' }
        3 { $p += '=' }
    }
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json
}

$cfg = Get-Content -Raw -LiteralPath $REALM_FILE | ConvertFrom-Json
$realm = $cfg.realm
$clientCfg = $cfg.clients[0]
$testUser = $cfg.users[0]

Write-Host 'Keycloak setup WHODAS 2.0' -ForegroundColor Cyan
Write-Host "Server : $KC_URL"
Write-Host "Admin  : $ADMIN_USER"
Write-Host "Config : $REALM_FILE"

$root = Invoke-Kc -Method Get -Uri "$KC_URL/"
if ($root.Status -eq 0 -or $root.Status -ge 500) { throw "Keycloak non raggiungibile su $KC_URL" }
Write-Step 'Keycloak raggiungibile' 'ok' $KC_URL

$token = Get-AdminToken
Write-Step 'Token master realm ottenuto' 'ok'
$admin = Get-AdminPath $token
Write-Step 'Admin REST API individuata' 'ok' "$KC_URL$admin"

if (-not $VerifyOnly) {
    Write-Title 'Realm'
    $current = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm" -Token $token
    $wanted = @{
        realm                 = $realm
        enabled               = ($cfg.enabled -ne $false)
        displayName           = $cfg.displayName
        sslRequired           = $cfg.sslRequired
        accessTokenLifespan   = $cfg.accessTokenLifespan
        ssoSessionIdleTimeout = $cfg.ssoSessionIdleTimeout
        ssoSessionMaxLifespan = $cfg.ssoSessionMaxLifespan
        registrationAllowed   = $cfg.registrationAllowed
        loginWithEmailAllowed = $cfg.loginWithEmailAllowed
        duplicateEmailsAllowed = $cfg.duplicateEmailsAllowed
        resetPasswordAllowed  = $cfg.resetPasswordAllowed
        editUsernameAllowed   = $cfg.editUsernameAllowed
    }
    if ($current.Status -eq 404) {
        $created = Invoke-Kc -Method Post -Uri "$KC_URL$admin/realms" -Token $token -Body $wanted
        if ($created.Status -eq 201) {
            Write-Step "Realm '$realm' creato" 'ok'
        } else {
            Write-Step "Realm '$realm' non creato" 'fail' "HTTP $($created.Status) $($created.Body.error)"
        }
    } else {
        $updated = Invoke-Kc -Method Put -Uri "$KC_URL$admin/realms/$realm" -Token $token -Body $wanted
        if ($updated.Status -eq 204) {
            Write-Step "Realm '$realm' verificato/aggiornato" 'ok'
        } else {
            Write-Step "Realm '$realm' non aggiornabile" 'fail' "HTTP $($updated.Status) $($updated.Body.error)"
        }
    }

    Write-Title 'Ruoli realm'
    $roleNames = @()
    if ($cfg.realmRoles) { $roleNames += $cfg.realmRoles }
    foreach ($u in $cfg.users) { $roleNames += $u.realmRoles }
    foreach ($roleName in ($roleNames | Select-Object -Unique)) {
        $exists = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/roles/$roleName" -Token $token
        if ($exists.Status -eq 200) {
            Write-Step "Ruolo '$roleName' gia' esistente" 'ok'
        } else {
            $made = Invoke-Kc -Method Post -Uri "$KC_URL$admin/realms/$realm/roles" -Token $token -Body @{ name = $roleName; description = "WHODAS 2.0 ruolo $roleName" }
            if ($made.Status -eq 201) { Write-Step "Ruolo '$roleName' creato" 'ok' } else { Write-Step "Ruolo '$roleName' non creato" 'fail' "HTTP $($made.Status)" }
        }
    }
}

Write-Title 'Client'
$clients = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/clients?clientId=$($clientCfg.clientId)" -Token $token
$client = @($clients.Body) | Select-Object -First 1
if (-not $client) {
    $body = @{
        clientId               = $clientCfg.clientId
        name                   = $clientCfg.name
        description            = $clientCfg.description
        enabled                = $true
        protocol               = $clientCfg.protocol
        clientAuthenticatorType = $clientCfg.clientAuthenticatorType
        secret                 = $clientCfg.secret
        publicClient           = ($clientCfg.publicClient -eq $true)
        standardFlowEnabled    = $true
        implicitFlowEnabled    = $false
        directAccessGrantsEnabled = $true
        serviceAccountsEnabled = $true
        rootUrl                = $clientCfg.rootUrl
        baseUrl                = $clientCfg.baseUrl
        redirectUris           = @($clientCfg.redirectUris)
        webOrigins             = @($clientCfg.webOrigins)
        attributes             = $clientCfg.attributes
    }
    $made = Invoke-Kc -Method Post -Uri "$KC_URL$admin/realms/$realm/clients" -Token $token -Body $body
    if ($made.Status -ne 201) { throw "Client non creato: HTTP $($made.Status) $($made.Body.error)" }
    $clients = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/clients?clientId=$($clientCfg.clientId)" -Token $token
    $client = @($clients.Body) | Select-Object -First 1
    Write-Step "Client '$($clientCfg.clientId)' creato" 'ok' "internal id=$($client.id)"
} else {
    Write-Step "Client '$($clientCfg.clientId)' gia' esistente" 'info' "internal id=$($client.id)"
}

if (-not $VerifyOnly) {
    $detail = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/clients/$($client.id)" -Token $token
    $merged = @{}
    $detail.Body.PSObject.Properties | ForEach-Object { $merged[$_.Name] = $_.Value }
    $merged['id'] = $client.id
    $merged['clientId'] = $clientCfg.clientId
    $merged['name'] = $clientCfg.name
    $merged['enabled'] = $true
    $merged['protocol'] = $clientCfg.protocol
    $merged['clientAuthenticatorType'] = $clientCfg.clientAuthenticatorType
    $merged['secret'] = $clientCfg.secret
    $merged['publicClient'] = ($clientCfg.publicClient -eq $true)
    $merged['standardFlowEnabled'] = $true
    $merged['implicitFlowEnabled'] = $false
    $merged['directAccessGrantsEnabled'] = $true
    $merged['serviceAccountsEnabled'] = $true
    $merged['rootUrl'] = $clientCfg.rootUrl
    $merged['baseUrl'] = $clientCfg.baseUrl
    $merged['redirectUris'] = @($clientCfg.redirectUris)
    $merged['webOrigins'] = @($clientCfg.webOrigins)
    $merged['attributes'] = $clientCfg.attributes
    $updated = Invoke-Kc -Method Put -Uri "$KC_URL$admin/realms/$realm/clients/$($client.id)" -Token $token -Body $merged
    if ($updated.Status -eq 204) {
        Write-Step "Client '$($clientCfg.clientId)' sincronizzato" 'ok'
    } else {
        Write-Step "Client '$($clientCfg.clientId)' non sincronizzato" 'fail' "HTTP $($updated.Status) $($updated.Body.error)"
    }

    Write-Title 'Client scope'
    $scopeList = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/client-scopes" -Token $token
    $scopeByName = @{}
    foreach ($s in $scopeList.Body) { $scopeByName[$s.name] = $s.id }
    $wantDefault = @('acr', 'basic', 'email', 'profile', 'roles', 'web-origins') + @($clientCfg.defaultClientScopes)
    $wantOptional = @('offline_access') + @($clientCfg.optionalClientScopes)
    foreach ($scopeName in ($wantDefault | Select-Object -Unique)) {
        if ($scopeName -eq 'openid') { continue }
        if (-not $scopeByName.ContainsKey($scopeName)) {
            Write-Step "Scope '$scopeName' non definito nel realm" 'warn'
            continue
        }
        $res = Invoke-Kc -Method Put -Uri "$KC_URL$admin/realms/$realm/clients/$($client.id)/default-client-scopes/$($scopeByName[$scopeName])" -Token $token
        if ($res.Status -eq 204) { Write-Step "Scope '$scopeName' assegnato (default)" 'ok' }
    }
    foreach ($scopeName in ($wantOptional | Select-Object -Unique)) {
        if (-not $scopeByName.ContainsKey($scopeName)) {
            Write-Step "Scope '$scopeName' non definito nel realm" 'warn'
            continue
        }
        $res = Invoke-Kc -Method Put -Uri "$KC_URL$admin/realms/$realm/clients/$($client.id)/optional-client-scopes/$($scopeByName[$scopeName])" -Token $token
        if ($res.Status -eq 204) { Write-Step "Scope '$scopeName' assegnato (optional)" 'ok' }
    }
}

if (-not $VerifyOnly) {
    Write-Title 'Utenti'
    foreach ($user in $cfg.users) {
        $found = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/users?username=$($user.username)" -Token $token
        $kcUser = @($found.Body) | Select-Object -First 1
        if (-not $kcUser) {
            $newUser = @{
                username      = $user.username
                email         = $user.email
                emailVerified = $true
                enabled       = $true
                firstName     = $user.firstName
                lastName      = $user.lastName
            }
            $created = Invoke-Kc -Method Post -Uri "$KC_URL$admin/realms/$realm/users" -Token $token -Body $newUser
            if ($created.Status -ne 201) { throw "Utente $($user.username) non creato: HTTP $($created.Status)" }
            $found = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/users?username=$($user.username)" -Token $token
            $kcUser = @($found.Body) | Select-Object -First 1
            Write-Step "Utente '$($user.username)' creato" 'ok' "id=$($kcUser.id)"
        } else {
            Write-Step "Utente '$($user.username)' gia' esistente" 'info' "id=$($kcUser.id)"
        }

        $password = @($user.credentials | Where-Object { $_.type -eq 'password' -or -not $_.type })[0]
        if ($password) {
            $reset = Invoke-Kc -Method Put -Uri "$KC_URL$admin/realms/$realm/users/$($kcUser.id)/reset-password" -Token $token -Body @{ type = 'password'; value = $password.value; temporary = $false }
            if ($reset.Status -eq 204) { Write-Step "Password di '$($user.username)' impostata" 'ok' } else { Write-Step "Password di '$($user.username)' non impostata" 'fail' "HTTP $($reset.Status)" }
        }

        $mapped = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/users/$($kcUser.id)/role-mappings/realm" -Token $token
        $have = @($mapped.Body | ForEach-Object { $_.name })
        foreach ($roleName in $user.realmRoles) {
            if ($have -contains $roleName) {
                Write-Step "'$($user.username)' ha gia' il ruolo '$roleName'" 'ok'
                continue
            }
            $role = Invoke-Kc -Method Get -Uri "$KC_URL$admin/realms/$realm/roles/$roleName" -Token $token
            $roleMapping = @([pscustomobject]@{ id = $role.Body.id; name = $role.Body.name })
            $assign = Invoke-Kc -Method Post -Uri "$KC_URL$admin/realms/$realm/users/$($kcUser.id)/role-mappings/realm" -Token $token -Body $roleMapping
            if ($assign.Status -eq 204) { Write-Step "Ruolo '$roleName' assegnato a '$($user.username)'" 'ok' } else { Write-Step "Ruolo '$roleName' non assegnato" 'fail' "HTTP $($assign.Status) $($assign.Body)" }
        }
    }
}

Write-Title 'Verifica funzionale'
$realmPath = "/realms/$realm"
$discovery = Invoke-Kc -Method Get -Uri "$KC_URL$realmPath/.well-known/openid-configuration"
if ($discovery.Status -eq 200) {
    Write-Step 'OIDC discovery' 'ok' "issuer=$($discovery.Body.issuer)"
} else {
    Write-Step 'OIDC discovery' 'fail' "HTTP $($discovery.Status)"
}
$jwks = Invoke-Kc -Method Get -Uri "$KC_URL$realmPath/protocol/openid-connect/certs"
if ($jwks.Status -eq 200 -and $jwks.Body.keys.Count -gt 0) {
    Write-Step 'JWKS disponibile' 'ok' "$($jwks.Body.keys.Count) chiavi"
} else {
    Write-Step 'JWKS disponibile' 'fail' "HTTP $($jwks.Status)"
}

$grant = Invoke-Kc -Method Post -Uri "$KC_URL$realmPath/protocol/openid-connect/token" -Body @{
    grant_type    = 'password'
    client_id     = $clientCfg.clientId
    client_secret = $clientCfg.secret
    username      = $testUser.username
    password      = $testUser.credentials[0].value
    scope         = 'openid profile email'
} -ContentType 'form'
if ($grant.Status -eq 200 -and $grant.Body.access_token) {
    $claims = New-JwtPayload $grant.Body.access_token
    Write-Step 'Password grant (ROPC)' 'ok' "expires_in=$($grant.Body.expires_in) refresh=$([bool]$grant.Body.refresh_token)"
    Write-Step 'Claim access token' 'ok' "sub=$($claims.sub) azp=$($claims.azp) iss=$($claims.iss)"
    Write-Step 'Ruoli nel token' 'ok' ($claims.realm_access.roles -join ', ')
    if ($claims.iss -ne "$KC_URL$realmPath") { Write-Step 'Issuer non coerente' 'fail' $claims.iss }
    if ($claims.azp -ne $clientCfg.clientId) { Write-Step 'azp non coerente' 'fail' $claims.azp }
    if (-not $claims.sub) { Write-Step 'Claim sub assente (scope basic mancante)' 'fail' }

    $userinfo = Invoke-Kc -Method Get -Uri "$KC_URL$realmPath/protocol/openid-connect/userinfo" -Token $grant.Body.access_token
    if ($userinfo.Status -eq 200 -and $userinfo.Body.sub) {
        Write-Step 'Userinfo endpoint' 'ok' "sub=$($userinfo.Body.sub) email=$($userinfo.Body.email)"
    } else {
        Write-Step 'Userinfo endpoint' 'fail' "HTTP $($userinfo.Status)"
    }

    $refresh = Invoke-Kc -Method Post -Uri "$KC_URL$realmPath/protocol/openid-connect/token" -Body @{
        grant_type    = 'refresh_token'
        client_id     = $clientCfg.clientId
        client_secret = $clientCfg.secret
        refresh_token = $grant.Body.refresh_token
    } -ContentType 'form'
    if ($refresh.Status -eq 200 -and $refresh.Body.access_token) { Write-Step 'Refresh token grant' 'ok' 'nuovo access_token emesso' } else { Write-Step 'Refresh token grant' 'fail' "HTTP $($refresh.Status)" }
} else {
    Write-Step 'Password grant (ROPC)' 'fail' "$($grant.Body.error) $($grant.Body.error_description)"
}

$machine = Invoke-Kc -Method Post -Uri "$KC_URL$realmPath/protocol/openid-connect/token" -Body @{
    grant_type    = 'client_credentials'
    client_id     = $clientCfg.clientId
    client_secret = $clientCfg.secret
} -ContentType 'form'
if ($machine.Status -eq 200 -and $machine.Body.access_token) {
    $machineClaims = New-JwtPayload $machine.Body.access_token
    Write-Step 'Client credentials grant' 'ok' "preferred_username=$($machineClaims.preferred_username)"
} else {
    Write-Step 'Client credentials grant' 'fail' "$($machine.Body.error) $($machine.Body.error_description)"
}

Write-Title 'Riepilogo'
Write-Host "Realm         : $realm"
Write-Host "Realm URL     : $KC_URL$realmPath"
Write-Host "Client ID     : $($clientCfg.clientId)"
Write-Host "Client Secret : $($clientCfg.secret)"
Write-Host "Utente test   : $($testUser.username) / $($testUser.credentials[0].value)"
Write-Host ''
if ($script:Failures -gt 0) {
    Write-Host "RISULTATO: $script:Failures fallimenti" -ForegroundColor Red
    exit 1
}
Write-Host 'RISULTATO: OK' -ForegroundColor Green
exit 0
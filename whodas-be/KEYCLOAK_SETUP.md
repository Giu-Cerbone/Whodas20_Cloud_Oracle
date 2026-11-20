# Keycloak SSO Setup for WHODAS 2.0 Platform

This guide will help you configure Keycloak SSO for the whodas-be gateway project.

## Prerequisites

- Keycloak running at http://localhost:8080
- Admin credentials: admin/admin
- Spring Boot application with OAuth2 Resource Server support

## Keycloak Client Configuration

### Manual Setup via Admin Console

1. **Login to Keycloak Admin Console**
   - URL: http://localhost:8080/admin
   - Username: admin
   - Password: admin

2. **Create Realm**
   - Click "Create realm"
   - Realm name: `whodas-realm`
   - Display name: `WHODAS 2.0`
   - Click "Create"

3. **Create Client**
   - Go to "Clients" → "Create client"
   - Client ID: `whodas20-client`
   - Client authentication: ON
   - Standard flow: ON
   - Direct access grants: ON (needed for password grant)
   - Access type: `confidential`
   - Valid redirect URIs: `http://localhost:4200/*`
   - Web origins: `http://localhost:4200`
   - Click "Save"

4. **Get Client Secret**
   - Go to the client's "Credentials" tab
   - Copy the "Secret" value
   - Update `keycloak-realm.json` and `whodas-gateway/src/main/resources/application.yml` with this secret

5. **Create Roles**
   - Go to "Roles" → "Create role"
   - Role name: `user`
   - Description: "Regular user role"
   - Click "Create"
   - Repeat for `admin` role

6. **Create Test User**
   - Go to "Users" → "Create user"
   - Username: `testuser`
   - Email: `testuser@whodas.com`
   - Enabled: ON
   - Click "Create"
   - Go to "Credentials" tab
   - Set password: `test123`
   - Turn OFF "Temporary"
   - Click "Save"
   - Assign "user" role to the user

### Automated Setup

All scripts read `keycloak-realm.json` (single source of truth) and call the Keycloak
Admin REST API directly. They are idempotent: safe to run on a fresh or existing server.

```bash
# Windows
setup-keycloak.bat

# Node (also available inside the .bat)
node setup-keycloak.js
node setup-keycloak.js --verify-only

# PowerShell
powershell -ExecutionPolicy Bypass -File setup-keycloak.ps1
powershell -ExecutionPolicy Bypass -File setup-keycloak.ps1 -VerifyOnly
```

Overridable environment variables: `KEYCLOAK_URL`, `KC_ADMIN_USER`, `KC_ADMIN_PASS`, `REALM_FILE`.

### Native JSON import

`keycloak-realm.json` is a valid `RealmRepresentation`: it can be imported as is with a
single REST call.

```bash
# Deletes the realm if present, then POST /admin/realms with the JSON file verbatim
node setup-keycloak.js --import

# Same, but imports into a temporary realm first to validate the JSON, then removes it
node setup-keycloak.js --import --keep-realm

node setup-keycloak.js --import --json    # machine readable output
```

What the import does:

1. `GET /admin/realms/whodas-realm` (404 if absent)
2. `DELETE /admin/realms/whodas-realm` when it already exists (skipped with `--keep-realm`)
3. `POST /admin/realms` with the JSON body → `201 Created`
4. Ensures the client scopes (`basic` is mandatory, otherwise the access token has no `sub`)
5. Assigns the realm default roles to the imported users
6. Runs the verification suite

What the scripts do:

1. Resolve the admin REST base path (`/admin` for Keycloak 17+, `/auth/admin` for older versions).
2. Create or update the realm `whodas-realm`.
3. Create the realm roles listed in `keycloak-realm.json`.
4. Create or synchronize the client `whodas20-client` (confidential, direct access grants ON).
5. Assign the client scopes, including `basic` (without it the access token has no `sub` claim).
6. Create the users, reset their password and assign realm roles.
7. Verify: OIDC discovery, JWKS, password grant, JWT RS256 signature, userinfo,
   refresh token, client credentials.

Exit code is `0` only when every verification step passes.

> If Keycloak runs with a non-persistent dev database, the realm is lost at every
> restart: re-run the setup script after each restart.

## Spring Boot Configuration

The gateway has been configured with the following:

### 1. Dependencies Added
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-oauth2-client`
- `spring-security-oauth2-jose`

### 2. Security Configuration
`SecurityConfig.java` currently keeps every route public (`permitAll()`) and does not
validate JWT tokens: the `keycloak.*` properties are declared but unused. Enabling
resource-server validation is a separate step.

### 3. Application Configuration
Updated `application.yml` with:
- Keycloak base URL, realm, client id and client secret
- JWT issuer URI: `http://localhost:8080/realms/whodas-realm`

## Testing the Setup

### Get an Access Token

```bash
# Using curl (the client is confidential: client_secret is mandatory)
curl -X POST http://localhost:8080/realms/whodas-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=whodas20-client&client_secret=whodas20-client-key&username=testuser&password=test123&scope=openid%20profile%20email"
```

### Use the Token

```bash
# The gateway listens on 7001
# Authorization e' l'unico header di autenticazione accettato, in schema Basic
# con base64("username:access_token")
curl -X GET http://localhost:7001/api/v1/patients \
  -u "testuser:<access_token>"
```

## Configuration Details

### Keycloak Client Settings
- **Client ID**: `whodas20-client`
- **Client Secret**: must match `keycloak-realm.json` and `application.yml`
- **Auth Type**: `client-secret`
- **Flow**: Standard flow + Direct access grants
- **Access Type**: confidential

### JWT Token Validation
The gateway is expected to validate JWT tokens by:
1. Checking the issuer matches `http://localhost:8080/realms/whodas-realm`
2. Verifying the token signature using Keycloak's public keys
3. Extracting roles from `realm_access.roles` and `resource_access.*.roles`

### Microservice Communication
All microservices are reachable through the gateway (routes declared in `application.yml`):
- Patient Service: `http://localhost:7001/api/v1/patients`
- Questionnaire Service: `http://localhost:7001/api/v1/questionnaire`
- Assessment Service: `http://localhost:7001/api/v1/assessments`
- Certificate Service: `http://localhost:7001/api/v1/certificates`

## Troubleshooting

### Common Issues

1. **"Invalid token" error**
   - Verify the token hasn't expired
   - Check that the issuer URI matches your Keycloak realm
   - Ensure the client secret is correct

2. **"Access denied" error**
   - Check if the user has the required roles
   - Verify the security configuration allows access to the endpoint

3. **"Connection refused" to microservices**
   - Ensure all microservices are running
   - Check that they're registered with the discovery client

### Debugging

Enable debug logging in `application.yml`:
```yaml
logging:
  level:
    it.whodas.gateway: DEBUG
    org.springframework.security: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Next Steps

1. Run `setup-keycloak.bat` (or `node setup-keycloak.js`) to provision realm, client and users
2. Configure the frontend (Angular app) with the client ID `whodas20-client` and issuer
   `http://localhost:8080/realms/whodas-realm`
3. Enable JWT validation in `SecurityConfig` if the gateway must reject anonymous requests
4. Test the authentication flow end-to-end
# Manual Keycloak Setup Instructions for WHODAS 2.0

Follow these steps to set up Keycloak manually:

## Step 1: Create Realm

1. Open Keycloak Admin Console: http://localhost:8080/admin
2. Login with: admin / admin
3. Click "Create realm"
4. Enter:
   - Realm name: `whodas-realm`
   - Display name: `WHODAS 2.0`
5. Click "Create"

## Step 2: Create Client

1. Go to "Clients" in the left menu
2. Click "Create client"
3. Configure:
   - Client ID: `whodas-be`
   - Client authentication: ON
   - Standard flow: ON
   - Access type: `confidential`
4. Click "Save"

## Step 3: Configure Client Settings

After creating the client, go to the client's settings:

1. **Valid redirect URIs**: `http://localhost:4200/*`
2. **Web origins**: `http://localhost:4200`
3. **Access token lifespan**: 3600 (1 hour)
4. **Refresh token lifespan**: 86400 (24 hours)

## Step 4: Get Client Secret

1. Go to the "Credentials" tab
2. Copy the "Secret" value
3. Update the `application.yml` with this secret

## Step 5: Create Roles

1. Go to "Roles" in the left menu
2. Click "Create role"
3. Create two roles:
   - **user**: "Regular user role"
   - **admin**: "Administrator role"

## Step 6: Create Test User

1. Go to "Users" in the left menu
2. Click "Create user"
3. Configure:
   - Username: `testuser`
   - Email: `testuser@whodas.com`
   - Enabled: ON
4. Click "Create"
5. Go to "Credentials" tab
6. Set password: `test123`
7. Turn OFF "Temporary"
8. Click "Save"
9. Go to "Role mapping" tab
10. Assign "user" role

## Step 7: Test Authentication

Get a test token:
```bash
curl -X POST http://localhost:8080/realms/whodas-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser&password=test123&client_id=whodas-be&grant_type=password"
```

Use the token:
```bash
# L'unico header di autenticazione accettato dal gateway e' Authorization: Basic
# con base64("username:token")
curl -X GET http://localhost:7001/api/v1/patients \
  -u "testuser:<access_token>"
```

## Configuration Files Updated

The following files have been configured:
- `whodas-gateway/pom.xml` - Added OAuth2 dependencies
- `whodas-gateway/src/main/java/it/whodas/gateway/config/SecurityConfig.java` - Security configuration
- `whodas-gateway/src/main/java/it/whodas/gateway/config/KeycloakJwtAuthenticationConverter.java` - JWT converter
- `whodas-gateway/src/main/resources/application.yml` - Updated with Keycloak settings

## Application Configuration

Update `application.yml` with your client secret:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/whodas-realm
          jwk-set-uri: http://localhost:8080/realms/whodas-realm/protocol/openid-connect/certs
```
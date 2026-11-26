#!/usr/bin/env node
/**
 * Keycloak Setup - WHODAS 2.0
 * Idempotent provisioning of realm/client/user through the Keycloak Admin REST API.
 * Source of truth: keycloak-realm.json
 *
 *   node setup-keycloak.js                 provisioning granulare (idempotente)
 *   node setup-keycloak.js --import         import nativo del JSON (DELETE realm + POST /admin/realms)
 *   node setup-keycloak.js --import --keep-realm   import su un realm temporaneo senza cancellare
 *   node setup-keycloak.js --verify-only    solo verifica
 *   node setup-keycloak.js --json           output JSON
 *
 * Env overrides: KEYCLOAK_URL, KC_ADMIN_USER, KC_ADMIN_PASS, REALM_FILE
 */

'use strict';

const fs = require('fs');
const http = require('http');
const https = require('https');
const path = require('path');
const crypto = require('crypto');

const KC_URL = (process.env.KEYCLOAK_URL || 'http://localhost:8080').replace(/\/+$/, '');
const ADMIN_USER = process.env.KC_ADMIN_USER || 'admin';
const ADMIN_PASS = process.env.KC_ADMIN_PASS || 'admin';
const REALM_FILE = process.env.REALM_FILE || path.join(__dirname, 'keycloak-realm.json');

const VERIFY_ONLY = process.argv.includes('--verify-only');
const IMPORT_ONLY = process.argv.includes('--import');
const KEEP_REALM = process.argv.includes('--keep-realm');
const AS_JSON = process.argv.includes('--json');

const REQUIRED_DEFAULT_SCOPES = ['acr', 'basic', 'email', 'profile', 'roles', 'web-origins', 'whodas-audience'];
const REQUIRED_OPTIONAL_SCOPES = ['offline_access'];

const results = [];
let exitCode = 0;

function url(target) {
  const u = new URL(target, KC_URL);
  return {
    protocol: u.protocol,
    hostname: u.hostname,
    port: u.port || (u.protocol === 'https:' ? 443 : 80),
    path: u.pathname + u.search,
    href: u.href
  };
}

function request(method, target, { body, raw, headers = {}, form } = {}) {
  return new Promise((resolve, reject) => {
    const opts = url(target);
    const client = opts.protocol === 'https:' ? https : http;
    let payload = null;
    const finalHeaders = Object.assign({ Accept: 'application/json' }, headers);

    if (raw !== undefined) {
      payload = raw;
      finalHeaders['Content-Type'] = 'application/json';
    } else if (form) {
      payload = new URLSearchParams(form).toString();
      finalHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
    } else if (body !== undefined) {
      payload = JSON.stringify(body);
      finalHeaders['Content-Type'] = 'application/json';
    }
    if (payload !== null) finalHeaders['Content-Length'] = Buffer.byteLength(payload);

    const req = client.request(Object.assign({}, opts, { method, headers: finalHeaders }), (res) => {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', (c) => (raw += c));
      res.on('end', () => {
        let parsed = raw;
        if (raw) {
          try {
            parsed = JSON.parse(raw);
          } catch (e) {
            parsed = raw;
          }
        }
        resolve({ status: res.statusCode, body: parsed });
      });
    });
    req.on('error', reject);
    req.setTimeout(20000, () => req.destroy(new Error('timeout ' + target)));
    if (payload !== null) req.write(payload);
    req.end();
  });
}

function decodeJwtPart(part) {
  return JSON.parse(Buffer.from(part.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8'));
}

function decodeJwt(token) {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('JWT malformato');
  return {
    header: decodeJwtPart(parts[0]),
    payload: decodeJwtPart(parts[1]),
    signature: parts[2],
    signingInput: parts[0] + '.' + parts[1]
  };
}

function verifyJwtSignature(token, jwks) {
  const jwt = decodeJwt(token);
  const kid = jwt.header.kid;
  const jwk = (jwks.keys || []).find((k) => k.kid === kid);
  if (!jwk) return { ok: false, reason: 'nessuna chiave JWKS con kid=' + kid };
  const pub = crypto.createPublicKey({ key: { kty: jwk.kty, n: jwk.n, e: jwk.e }, format: 'jwk' });
  const alg = jwt.header.alg === 'RS256' ? 'RSA-SHA256' : null;
  if (!alg) return { ok: false, reason: 'alg non supportato: ' + jwt.header.alg };
  const signature = Buffer.from(jwt.signature.replace(/-/g, '+').replace(/_/g, '/'), 'base64');
  const ok = crypto.createVerify(alg).update(jwt.signingInput).verify(pub, signature);
  return { ok, reason: ok ? 'firma RS256 valida (kid=' + kid + ')' : 'firma non valida' };
}

function log(step, message, state, detail) {
  const resultsStep = { step, message, state, detail: detail || null };
  results.push(resultsStep);
  if (state === 'fail') exitCode = 1;
  if (AS_JSON) return;
  const tag = { ok: '[ OK ]', info: '[INFO]', warn: '[WARN]', fail: '[FAIL]' }[state] || '[INFO]';
  console.log(`${tag} ${message}${detail ? ' -> ' + detail : ''}`);
}

function head(title) {
  if (!AS_JSON) {
    console.log('');
    console.log('=== ' + title + ' ===');
  }
}

async function adminApi(token) {
  const candidates = ['/admin', '/auth/admin'];
  for (const adminPath of candidates) {
    const res = await request('GET', `${KC_URL}${adminPath}/realms`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.status === 200) return adminPath;
  }
  throw new Error('impossibile individuare il path admin REST (provati: ' + candidates.join(', ') + ')');
}

async function getAdminToken() {
  const paths = ['/realms/master/protocol/openid-connect/token', '/auth/realms/master/protocol/openid-connect/token'];
  let lastError = null;
  for (const p of paths) {
    const res = await request('POST', KC_URL + p, {
      form: {
        grant_type: 'password',
        client_id: 'admin-cli',
        username: ADMIN_USER,
        password: ADMIN_PASS
      }
    });
    if (res.status === 200 && res.body && res.body.access_token) return res.body.access_token;
    lastError = res;
  }
  const detail = lastError && lastError.body && lastError.body.error
    ? `${lastError.body.error}: ${lastError.body.error_description || ''}`
    : JSON.stringify(lastError && lastError.body);
  throw new Error('token admin non ottenuto - ' + detail);
}

async function api(method, adminPath, subPath, { body, raw, token, expect } = {}) {
  const res = await request(method, `${KC_URL}${adminPath}${subPath}`, {
    body,
    raw,
    headers: { Authorization: `Bearer ${token}` }
  });
  if (expect && !expect.includes(res.status)) {
    throw new Error(`${method} ${subPath} -> HTTP ${res.status} ${JSON.stringify(res.body)}`);
  }
  return res;
}

const SETUP_ONLY_KEYS = ['whodasSetup'];

async function loadConfig() {
  const raw = fs.readFileSync(REALM_FILE, 'utf8');
  const cfg = JSON.parse(raw);
  if (!cfg.realm) throw new Error('keycloak-realm.json: campo "realm" mancante');
  if (!Array.isArray(cfg.clients) || cfg.clients.length === 0) {
    throw new Error('keycloak-realm.json: campo "clients" mancante o vuoto');
  }
  const importPayload = JSON.parse(raw);
  SETUP_ONLY_KEYS.forEach((key) => delete importPayload[key]);
  Object.keys(importPayload).forEach((key) => {
    if (key.startsWith('_')) delete importPayload[key];
  });
  return { cfg, raw: JSON.stringify(importPayload) };
}

async function importRealm(adminPath, token, cfg, raw) {
  head('Import nativo del realm');
  const existing = await api('GET', adminPath, `/realms/${cfg.realm}`, { token, expect: [200, 404] });

  if (existing.status === 200) {
    if (KEEP_REALM) {
      const tmpRealm = `${cfg.realm}-import-test`;
      const tmpCfg = Object.assign({}, cfg, { realm: tmpRealm });
      const tmpRaw = raw.replace(new RegExp(`"realm"\\s*:\\s*"${cfg.realm}"`), `"realm": "${tmpRealm}"`);
      log('import', `Realm '${cfg.realm}' esistente: import di prova nel realm '${tmpRealm}'`, 'info');
      const probe = await api('GET', adminPath, `/realms/${tmpRealm}`, { token, expect: [200, 404] });
      if (probe.status === 200) {
        await api('DELETE', adminPath, `/realms/${tmpRealm}`, { token, expect: [204] });
      }
      await api('POST', adminPath, '/realms', { token, raw: tmpRaw, expect: [201] });
      log('import', `Import riuscito nel realm '${tmpRealm}'`, 'ok');
      await api('DELETE', adminPath, `/realms/${tmpRealm}`, { token, expect: [204] });
      log('import', `Realm di prova '${tmpRealm}' rimosso`, 'ok');
      return null;
    }
    log('import', `Realm '${cfg.realm}' esistente: eliminazione per import pulito`, 'warn',
      'i dati del realm vengono ricreati dal JSON');
    const deleted = await api('DELETE', adminPath, `/realms/${cfg.realm}`, { token, expect: [204, 404] });
    log('import', `Realm '${cfg.realm}' eliminato`, deleted.status === 204 ? 'ok' : 'info',
      'HTTP ' + deleted.status);
  }

  const created = await api('POST', adminPath, '/realms', { token, raw, expect: [201] });
  log('import', `Import di keycloak-realm.json riuscito (POST ${KC_URL}${adminPath}/realms)`,
    created.status === 201 ? 'ok' : 'fail', 'HTTP ' + created.status);
  return cfg;
}

async function reportImportState(adminPath, token, cfg) {
  head('Contenuto importato');
  const roles = await api('GET', adminPath, `/realms/${cfg.realm}/roles`, { token, expect: [200] });
  const custom = (roles.body || []).map((r) => r.name).filter((n) => !['offline_access', 'uma_authorization'].includes(n) && !n.startsWith('default-roles-'));
  log('import', 'Ruoli realm', custom.length ? 'ok' : 'warn', custom.join(', '));

  const clients = await api('GET', adminPath, `/realms/${cfg.realm}/clients?clientId=${encodeURIComponent(cfg.clients[0].clientId)}`, { token, expect: [200] });
  const client = (clients.body || [])[0];
  if (!client) {
    log('import', 'Client importato', 'fail', cfg.clients[0].clientId + ' non trovato');
    return null;
  }
  const detail = await api('GET', adminPath, `/realms/${cfg.realm}/clients/${client.id}`, { token, expect: [200] });
  log('import', 'Client importato', 'ok',
    `id=${client.id} secret=${detail.body.secret} standardFlow=${detail.body.standardFlowEnabled} ` +
    `directAccessGrants=${detail.body.directAccessGrantsEnabled} serviceAccounts=${detail.body.serviceAccountsEnabled}`);
  log('import', 'Client scope', (detail.body.defaultClientScopes || []).includes('basic') ? 'ok' : 'warn',
    'default=' + (detail.body.defaultClientScopes || []).join(',') +
    ' | optional=' + (detail.body.optionalClientScopes || []).join(','));

  const users = await api('GET', adminPath, `/realms/${cfg.realm}/users`, { token, expect: [200] });
  for (const u of cfg.users || []) {
    const found = (users.body || []).find((x) => x.username === u.username);
    if (!found) {
      log('import', `Utente '${u.username}'`, 'fail', 'non trovato');
      continue;
    }
    const mapped = await api('GET', adminPath, `/realms/${cfg.realm}/users/${found.id}/role-mappings/realm`, { token, expect: [200] });
    const have = (mapped.body || []).map((r) => r.name);
    const wanted = u.realmRoles || [];
    const missing = wanted.filter((r) => !have.includes(r));
    log('import', `Utente '${u.username}'`, missing.length ? 'fail' : 'ok',
      `id=${found.id} email=${found.email} ruoli=${have.join(',')}` + (missing.length ? ' MANCANTI: ' + missing.join(',') : ''));
  }
  return client;
}

async function ensureUserDefaultRoles(adminPath, token, cfg) {
  head('Ruoli predefiniti del realm');
  const realm = await api('GET', adminPath, `/realms/${cfg.realm}`, { token, expect: [200] });
  const defaultRole = realm.body.defaultRole;
  if (!defaultRole) {
    log('user-default-role', 'Ruolo predefinito del realm non dichiarato', 'warn');
    return;
  }
  for (const wanted of cfg.users || []) {
    const found = await api('GET', adminPath, `/realms/${cfg.realm}/users?username=${encodeURIComponent(wanted.username)}`, {
      token,
      expect: [200]
    });
    const user = (found.body || [])[0];
    if (!user) {
      log('user-default-role', `Utente '${wanted.username}' non trovato`, 'fail');
      continue;
    }
    const mapped = await api('GET', adminPath, `/realms/${cfg.realm}/users/${user.id}/role-mappings/realm`, {
      token,
      expect: [200]
    });
    if ((mapped.body || []).some((r) => r.name === defaultRole.name)) {
      log('user-default-role', `'${wanted.username}' ha gia' '${defaultRole.name}'`, 'ok');
      continue;
    }
    const role = await api('GET', adminPath, `/realms/${cfg.realm}/roles/${encodeURIComponent(defaultRole.name)}`, {
      token,
      expect: [200, 404]
    });
    const roleRep = role.status === 200 ? role.body : { id: defaultRole.id, name: defaultRole.name };
    const res = await api('POST', adminPath, `/realms/${cfg.realm}/users/${user.id}/role-mappings/realm`, {
      token,
      body: [roleRep],
      expect: [204]
    });
    log('user-default-role', `'${defaultRole.name}' assegnato a '${wanted.username}'`,
      res.status === 204 ? 'ok' : 'fail', 'HTTP ' + res.status);
  }
}

async function ensureRealm(adminPath, token, cfg) {
  head('Realm');
  const current = await api('GET', adminPath, `/realms/${cfg.realm}`, { token, expect: [200, 404] });
  const wanted = {
    realm: cfg.realm,
    enabled: cfg.enabled !== false,
    displayName: cfg.displayName || cfg.realm,
    sslRequired: cfg.sslRequired || 'external',
    accessTokenLifespan: cfg.accessTokenLifespan || 3600,
    ssoSessionIdleTimeout: cfg.ssoSessionIdleTimeout || 1800,
    ssoSessionMaxLifespan: cfg.ssoSessionMaxLifespan || 36000,
    registrationAllowed: cfg.registrationAllowed !== false,
    loginWithEmailAllowed: cfg.loginWithEmailAllowed !== false,
    duplicateEmailsAllowed: cfg.duplicateEmailsAllowed === true,
    resetPasswordAllowed: cfg.resetPasswordAllowed !== false,
    editUsernameAllowed: cfg.editUsernameAllowed === true
  };

  if (current.status === 404) {
    await api('POST', adminPath, '/realms', { token, body: wanted, expect: [201] });
    log('realm', `Realm '${cfg.realm}' creato`, 'ok');
    return;
  }

  if (current.body.enabled !== wanted.enabled || current.body.displayName !== wanted.displayName) {
    await api('PUT', adminPath, `/realms/${cfg.realm}`, { token, body: wanted, expect: [204] });
    log('realm', `Realm '${cfg.realm}' aggiornato`, 'ok');
  } else {
    log('realm', `Realm '${cfg.realm}' gia' esistente e coerente`, 'ok');
  }

  const scopeNames = REQUIRED_DEFAULT_SCOPES;
  const realmDefaults = await api('GET', adminPath, `/realms/${cfg.realm}/default-default-client-scopes`, { token, expect: [200] });
  const have = new Set((realmDefaults.body || []).map((s) => s.name));
  const missing = scopeNames.filter((n) => !have.has(n));
  if (missing.length === 0) {
    log('realm', 'Default client scopes del realm corretti', 'ok', scopeNames.join(', '));
  } else {
    log('realm', 'Default client scopes del realm incompleti', 'warn', 'mancano: ' + missing.join(', '));
  }
}

async function ensureRoles(adminPath, token, cfg) {
  head('Ruoli realm');
  const wanted = new Set();
  const declared = (cfg.roles && cfg.roles.realm) || [];
  declared.forEach((r) => wanted.add(typeof r === 'string' ? r : r.name));
  (cfg.realmRoles || []).forEach((r) => wanted.add(typeof r === 'string' ? r : r.name));
  (cfg.users || []).forEach((u) => (u.realmRoles || []).forEach((r) => wanted.add(r)));

  for (const name of wanted) {
    const existing = await api('GET', adminPath, `/realms/${cfg.realm}/roles/${encodeURIComponent(name)}`, {
      token,
      expect: [200, 404]
    });
    if (existing.status === 200) {
      log('role', `Ruolo '${name}' gia' esistente`, 'ok');
      continue;
    }
    await api('POST', adminPath, `/realms/${cfg.realm}/roles`, {
      token,
      body: { name, description: `WHODAS 2.0 ruolo ${name}` },
      expect: [201]
    });
    log('role', `Ruolo '${name}' creato`, 'ok');
  }
}

async function ensureClientScopeDefinitions(adminPath, token, cfg) {
  const declared = (cfg.clientScopes || (cfg.whodasSetup && cfg.whodasSetup.clientScopes)) || [];
  if (declared.length === 0) return;
  head('Client scope con mapper');

  const all = await api('GET', adminPath, `/realms/${cfg.realm}/client-scopes`, { token, expect: [200] });
  const byName = new Map((all.body || []).map((s) => [s.name, s.id]));

  for (const wanted of declared) {
    const scopeId = byName.get(wanted.name);

    if (!scopeId) {
      await api('POST', adminPath, `/realms/${cfg.realm}/client-scopes`, {
        token,
        body: {
          name: wanted.name,
          description: wanted.description || '',
          protocol: wanted.protocol || 'openid-connect',
          attributes: wanted.attributes || {},
          protocolMappers: wanted.protocolMappers || []
        },
        expect: [201]
      });
      log('client-scope-def', `Client scope '${wanted.name}' creato con i mapper`, 'ok',
        (wanted.protocolMappers || []).map((m) => m.protocolMapper + ':' + m.name).join(', '));
      continue;
    }

    const detail = await api('GET', adminPath, `/realms/${cfg.realm}/client-scopes/${scopeId}`, {
      token,
      expect: [200]
    });
    const existingMappers = (detail.body.protocolMappers || []).map((m) => m.name);
    const missing = (wanted.protocolMappers || []).filter((m) => !existingMappers.includes(m.name));

    if (missing.length === 0) {
      log('client-scope-def', `Client scope '${wanted.name}' gia' esistente`, 'ok',
        existingMappers.join(', '));
      continue;
    }

    for (const mapper of missing) {
      const res = await api('POST', adminPath, `/realms/${cfg.realm}/client-scopes/${scopeId}/protocol-mappers/models`, {
        token,
        body: mapper,
        expect: [201]
      });
      log('client-scope-def', `Mapper '${mapper.name}' aggiunto a '${wanted.name}'`,
        res.status === 201 ? 'ok' : 'fail', 'HTTP ' + res.status);
    }
  }
}

async function ensureClientScopes(adminPath, token, cfg, clientId) {
  await ensureClientScopeDefinitions(adminPath, token, cfg);

  const wantedDefault = new Set(REQUIRED_DEFAULT_SCOPES);
  (cfg.clients[0].defaultClientScopes || []).forEach((s) => {
    const name = typeof s === 'string' ? s : s.name;
    if (name && name !== 'openid') wantedDefault.add(name);
  });
  const wantedOptional = new Set(REQUIRED_OPTIONAL_SCOPES);
  (cfg.clients[0].optionalClientScopes || []).forEach((s) => wantedOptional.add(s));

  const all = await api('GET', adminPath, `/realms/${cfg.realm}/client-scopes`, { token, expect: [200] });
  const byName = new Map((all.body || []).map((s) => [s.name, s.id]));

  for (const [name, list] of [[wantedDefault, 'default-client-scopes'], [wantedOptional, 'optional-client-scopes']]) {
    for (const scopeName of name) {
      const scopeId = byName.get(scopeName);
      if (!scopeId) {
        log('client-scope', `Client scope '${scopeName}' non definito nel realm`, 'warn');
        continue;
      }
      const res = await api(
        'PUT',
        adminPath,
        `/realms/${cfg.realm}/clients/${clientId}/${list}/${scopeId}`,
        { token, expect: [204, 409] }
      );
      if (res.status === 204) log('client-scope', `Scope '${scopeName}' assegnato come ${list}`, 'ok');
    }
  }

  const detail = await api('GET', adminPath, `/realms/${cfg.realm}/clients/${clientId}`, { token, expect: [200] });
  const currentDefault = new Set(detail.body.defaultClientScopes || []);
  const currentOptional = new Set(detail.body.optionalClientScopes || []);
  const missingDefault = [...wantedDefault].filter((n) => !currentDefault.has(n));
  const missingOptional = [...wantedOptional].filter((n) => !currentOptional.has(n));
  if (missingDefault.length || missingOptional.length) {
    log('client-scope', 'Scope del client incompleti', 'warn',
      [...missingDefault.map((n) => 'default:' + n), ...missingOptional.map((n) => 'optional:' + n)].join(', '));
  } else {
    log('client-scope', 'Scope del client completi', 'ok',
      'default=' + [...currentDefault].join(',') + ' | optional=' + [...currentOptional].join(','));
  }
}

async function ensureClient(adminPath, token, cfg) {
  head('Client');
  const wanted = cfg.clients[0];
  const found = await api('GET', adminPath, `/realms/${cfg.realm}/clients?clientId=${encodeURIComponent(wanted.clientId)}`, {
    token,
    expect: [200]
  });

  let client = (found.body || [])[0];
  if (!client) {
    const body = {
      clientId: wanted.clientId,
      name: wanted.name || wanted.clientId,
      description: wanted.description || '',
      enabled: true,
      protocol: wanted.protocol || 'openid-connect',
      clientAuthenticatorType: wanted.clientAuthenticatorType || 'client-secret',
      secret: wanted.secret,
      publicClient: wanted.publicClient === true,
      bearerOnly: false,
      standardFlowEnabled: wanted.standardFlowEnabled !== false,
      implicitFlowEnabled: wanted.implicitFlowEnabled === true,
      directAccessGrantsEnabled: wanted.directAccessGrantsEnabled === true,
      serviceAccountsEnabled: wanted.serviceAccountsEnabled === true,
      rootUrl: wanted.rootUrl || '',
      baseUrl: wanted.baseUrl || '/',
      redirectUris: wanted.redirectUris || [],
      webOrigins: wanted.webOrigins || [],
      attributes: wanted.attributes || {}
    };
    const created = await api('POST', adminPath, `/realms/${cfg.realm}/clients`, { token, body, expect: [201] });
    client = { id: created.headers ? created.headers['location'] : null };
    const reread = await api('GET', adminPath, `/realms/${cfg.realm}/clients?clientId=${encodeURIComponent(wanted.clientId)}`, {
      token,
      expect: [200]
    });
    client = (reread.body || [])[0];
    log('client', `Client '${wanted.clientId}' creato`, 'ok', 'internal id=' + client.id);
  } else {
    log('client', `Client '${wanted.clientId}' gia' esistente`, 'info', 'internal id=' + client.id);
  }

  if (!VERIFY_ONLY) {
    const upd = await api('GET', adminPath, `/realms/${cfg.realm}/clients/${client.id}`, { token, expect: [200] });
    const merged = Object.assign({}, upd.body, {
      id: client.id,
      clientId: wanted.clientId,
      name: wanted.name || upd.body.name,
      description: wanted.description || upd.body.description,
      enabled: true,
      protocol: wanted.protocol || 'openid-connect',
      clientAuthenticatorType: wanted.clientAuthenticatorType || 'client-secret',
      secret: wanted.secret,
      publicClient: wanted.publicClient === true,
      bearerOnly: false,
      standardFlowEnabled: wanted.standardFlowEnabled !== false,
      implicitFlowEnabled: wanted.implicitFlowEnabled === true,
      directAccessGrantsEnabled: wanted.directAccessGrantsEnabled === true,
      serviceAccountsEnabled: wanted.serviceAccountsEnabled === true,
      rootUrl: wanted.rootUrl || upd.body.rootUrl || '',
      baseUrl: wanted.baseUrl || '/',
      redirectUris: wanted.redirectUris || [],
      webOrigins: wanted.webOrigins || [],
      attributes: Object.assign({}, upd.body.attributes, wanted.attributes || {})
    });
    await api('PUT', adminPath, `/realms/${cfg.realm}/clients/${client.id}`, { token, body: merged, expect: [204] });
    log('client', `Client '${wanted.clientId}' sincronizzato con keycloak-realm.json`, 'ok');
  }

  if (!VERIFY_ONLY) await ensureClientScopes(adminPath, token, cfg, client.id);
  return client;
}

async function ensureUsers(adminPath, token, cfg) {
  head('Utenti');
  for (const wanted of cfg.users || []) {
    const found = await api('GET', adminPath, `/realms/${cfg.realm}/users?username=${encodeURIComponent(wanted.username)}`, {
      token,
      expect: [200, 400]
    });
    let users = [];
    if (found.status === 200) {
      users = found.body || [];
    } else {
      log('user', `Ricerca utente '${wanted.username}' fallita (HTTP 400), procedo come se non esistesse`, 'warn');
    }
    let user = users[0];

    if (!user) {
      const created = await api('POST', adminPath, `/realms/${cfg.realm}/users`, {
        token,
        body: {
          username: wanted.username,
          email: wanted.email || null,
          emailVerified: wanted.emailVerified === true,
          enabled: wanted.enabled !== false,
          firstName: wanted.firstName || null,
          lastName: wanted.lastName || null,
          credentials: (wanted.credentials || []).map((c) => ({
            type: c.type || 'password',
            value: c.value,
            temporary: c.temporary === true
          }))
        },
        expect: [201]
      });
      const reread = await api('GET', adminPath, `/realms/${cfg.realm}/users?username=${encodeURIComponent(wanted.username)}`, {
        token,
        expect: [200]
      });
      user = (reread.body || [])[0];
      log('user', `Utente '${wanted.username}' creato`, 'ok', 'id=' + user.id + ' ' + created.status);
    } else {
      log('user', `Utente '${wanted.username}' gia' esistente`, 'info', 'id=' + user.id);
    }

    if (!VERIFY_ONLY) {
      const password = (wanted.credentials || []).find((c) => !c.type || c.type === 'password');
      if (password) {
        const pwRes = await api('PUT', adminPath, `/realms/${cfg.realm}/users/${user.id}/reset-password`, {
          token,
          body: { type: 'password', value: password.value, temporary: password.temporary === true },
          expect: [204, 400]
        });
        if (pwRes.status === 204) {
          log('user', `Password di '${wanted.username}' impostata`, 'ok');
        } else {
          log('user', `Password di '${wanted.username}' non impostabile (utente read-only da LDAP)`, 'warn',
            'HTTP ' + pwRes.status);
        }
      }

      const mapped = await api('GET', adminPath, `/realms/${cfg.realm}/users/${user.id}/role-mappings/realm`, {
        token,
        expect: [200]
      });
      const have = new Set((mapped.body || []).map((r) => r.name));
      for (const roleName of wanted.realmRoles || []) {
        if (have.has(roleName)) {
          log('user-role', `'${wanted.username}' ha gia' il ruolo '${roleName}'`, 'ok');
          continue;
        }
        const role = await api('GET', adminPath, `/realms/${cfg.realm}/roles/${encodeURIComponent(roleName)}`, {
          token,
          expect: [200]
        });
        await api('POST', adminPath, `/realms/${cfg.realm}/users/${user.id}/role-mappings/realm`, {
          token,
          body: [{ id: role.body.id, name: role.body.name }],
          expect: [204]
        });
        log('user-role', `Ruolo '${roleName}' assegnato a '${wanted.username}'`, 'ok');
      }
    }
  }
}

async function ensureLdapProvider(adminPath, token, cfg) {
  head('LDAP User Federation');
  const ldapConfig = (cfg.ldapSetup && cfg.ldapSetup.provider) || {
    connectionUrl: 'ldap://ldap:389',
    baseDn: 'dc=whodascompany,dc=local',
    bindDn: 'cn=admin,dc=whodascompany,dc=local',
    bindCredential: 'admin',
    usersDn: 'ou=users,dc=whodascompany,dc=local',
    userObjectClasses: [],
    usernameLDAPAttribute: 'uid',
    rdnLDAPAttribute: 'uid',
    uuidLDAPAttribute: 'entryUUID',
    userModelAttribute: 'username',
    searchScope: '1',
    pagination: 'true',
    batchSizeForSync: '1000',
    fullSyncPeriod: '-1',
    changedSyncPeriod: '-1',
    priority: '0',
    importEnabled: 'true',
    editMode: 'READ_ONLY'
  };

  const wanted = {
    name: 'ldap',
    providerId: 'ldap',
    providerType: 'org.keycloak.storage.UserStorageProvider',
    subType: 'user',
    config: {
      connectionUrl: [ldapConfig.connectionUrl],
      baseDn: [ldapConfig.baseDn],
      bindDn: [ldapConfig.bindDn],
      bindCredential: [ldapConfig.bindCredential],
      usersDn: [ldapConfig.usersDn],
      userObjectClasses: Array.isArray(ldapConfig.userObjectClasses) ? ldapConfig.userObjectClasses : [ldapConfig.userObjectClasses].filter(Boolean),
      usernameLDAPAttribute: [ldapConfig.usernameLDAPAttribute],
      rdnLDAPAttribute: [ldapConfig.rdnLDAPAttribute],
      uuidLDAPAttribute: [ldapConfig.uuidLDAPAttribute],
      userModelAttribute: [ldapConfig.userModelAttribute],
      searchScope: [ldapConfig.searchScope],
      pagination: [ldapConfig.pagination],
      batchSizeForSync: [ldapConfig.batchSizeForSync],
      fullSyncPeriod: [ldapConfig.fullSyncPeriod],
      changedSyncPeriod: [ldapConfig.changedSyncPeriod],
      priority: [ldapConfig.priority],
      importEnabled: [ldapConfig.importEnabled],
      editMode: [ldapConfig.editMode]
    }
  };

  const existing = await api('GET', adminPath, `/realms/${cfg.realm}/components?parent=${cfg.realm}&type=org.keycloak.storage.UserStorageProvider`, {
    token,
    expect: [200]
  });
  const current = (existing.body || []).find((c) => c.providerId === 'ldap' && c.name === 'ldap');

  if (!current) {
    const created = await api('POST', adminPath, `/realms/${cfg.realm}/components`, { token, body: wanted, expect: [201] });
    const reread = await api('GET', adminPath, `/realms/${cfg.realm}/components?parent=${cfg.realm}&type=org.keycloak.storage.UserStorageProvider`, {
      token,
      expect: [200]
    });
    const provider = (reread.body || []).find((c) => c.providerId === 'ldap' && c.name === 'ldap');
    log('ldap', `Provider LDAP 'ldap' creato nel realm '${cfg.realm}'`, 'ok', 'id=' + (provider ? provider.id : 'n/d'));
    return provider;
  }

  log('ldap', `Provider LDAP 'ldap' gia' esistente nel realm '${cfg.realm}'`, 'info', 'id=' + current.id);

  if (VERIFY_ONLY) return current;

  const merged = Object.assign({}, current, wanted);
  await api('PUT', adminPath, `/realms/${cfg.realm}/components/${current.id}`, { token, body: merged, expect: [204] });
  log('ldap', `Provider LDAP 'ldap' sincronizzato nel realm '${cfg.realm}'`, 'ok');

  return current;
}

async function ensureLdapMappers(adminPath, token, cfg, provider) {
  head('LDAP attribute mappers');
  if (!provider) return;

  const desired = [
    { name: 'username', ldapAttribute: 'uid', modelAttribute: 'username' },
    { name: 'first name', ldapAttribute: 'cn', modelAttribute: 'firstName' },
    { name: 'last name', ldapAttribute: 'sn', modelAttribute: 'lastName' },
    { name: 'email', ldapAttribute: 'mail', modelAttribute: 'email' }
  ];

  const existing = await api('GET', adminPath, `/realms/${cfg.realm}/components?parent=${provider.id}`, {
    token,
    expect: [200]
  });
  const mappers = existing.body || [];

  for (const wanted of desired) {
    const current = mappers.find((c) => {
      const model = c.config && c.config['user.model.attribute'];
      return c.providerId === 'user-attribute-ldap-mapper' && Array.isArray(model) && model[0] === wanted.modelAttribute;
    });

    if (current) {
      const merged = Object.assign({}, current, {
        config: Object.assign({}, current.config, { 'ldap.attribute': [wanted.ldapAttribute] })
      });
      await api('PUT', adminPath, `/realms/${cfg.realm}/components/${current.id}`, { token, body: merged, expect: [204] });
      log('ldap-mapper', `Mapper '${wanted.modelAttribute}' -> LDAP '${wanted.ldapAttribute}'`, 'ok', 'aggiornato');
      continue;
    }

    const body = {
      name: wanted.name,
      providerId: 'user-attribute-ldap-mapper',
      providerType: 'org.keycloak.storage.UserStorageProviderMapper',
      parentId: provider.id,
      config: {
        'ldap.attribute': [wanted.ldapAttribute],
        'user.model.attribute': [wanted.modelAttribute],
        'read.only': ['true'],
        'always.read.value.from.ldap': ['false'],
        'is.mandatory.in.ldap': ['false']
      }
    };
    await api('POST', adminPath, `/realms/${cfg.realm}/components`, { token, body, expect: [201] });
    log('ldap-mapper', `Mapper '${wanted.modelAttribute}' -> LDAP '${wanted.ldapAttribute}'`, 'ok', 'creato');
  }
}

async function ensureLdapSync(adminPath, token, cfg, provider) {
  if (!provider) return;
  const sync = await api('POST', adminPath, `/realms/${cfg.realm}/user-storage/${provider.id}/sync?direction=fetch&action=triggerFullSync`, {
    token,
    expect: [200]
  });
  const status = sync.body && sync.body.status ? sync.body.status : 'HTTP ' + sync.status;
  log('ldap-sync', `Sincronizzazione utenti da LDAP`, sync.status === 200 ? 'ok' : 'warn', status);
}

async function verify(cfg, client) {
  head('Verifica funzionale');
  const realmPath = `/realms/${cfg.realm}`;
  const wanted = cfg.clients[0];

  const discovery = await request('GET', `${KC_URL}${realmPath}/.well-known/openid-configuration`);
  if (discovery.status === 200) {
    log('verify', 'OIDC discovery raggiungibile', 'ok',
      `issuer=${discovery.body.issuer} token=${discovery.body.token_endpoint}`);
  } else {
    log('verify', 'OIDC discovery non raggiungibile', 'fail', 'HTTP ' + discovery.status);
  }

  const jwksRes = await request('GET', `${KC_URL}${realmPath}/protocol/openid-connect/certs`);
  const jwks = jwksRes.body || { keys: [] };
  if (jwksRes.status === 200 && jwks.keys && jwks.keys.length) {
    log('verify', 'JWKS disponibile', 'ok', jwks.keys.length + ' chiavi: ' + jwks.keys.map((k) => k.kid).join(', '));
  } else {
    log('verify', 'JWKS non disponibile', 'fail', 'HTTP ' + jwksRes.status);
  }

  const user = (cfg.users || [])[0];
  if (!user) return;

  const grant = await request('POST', `${KC_URL}${realmPath}/protocol/openid-connect/token`, {
    form: {
      grant_type: 'password',
      client_id: wanted.clientId,
      client_secret: wanted.secret,
      username: user.username,
      password: (user.credentials || [])[0].value,
      scope: 'openid profile email'
    }
  });

  if (grant.status !== 200 || !grant.body.access_token) {
    log('verify', 'Password grant (ROPC) fallito', 'fail',
      JSON.stringify(grant.body && grant.body.error ? grant.body : grant.body));
    return;
  }

  const claims = decodeJwt(grant.body.access_token).payload;
  log('verify', 'Password grant (ROPC) riuscito', 'ok',
    `expires_in=${grant.body.expires_in} id_token=${Boolean(grant.body.id_token)} refresh_token=${Boolean(grant.body.refresh_token)}`);
  log('verify', 'Claim access token', 'ok',
    `sub=${claims.sub} azp=${claims.azp} scope=${claims.scope} exp-iat=${claims.exp - claims.iat}s`);
  log('verify', 'Ruoli nel token', 'ok',
    ((claims.realm_access && claims.realm_access.roles) || []).join(', '));

  if (claims.iss !== `${KC_URL}${realmPath}`) {
    log('verify', 'Issuer del token non coerente', 'warn', `atteso=${KC_URL}${realmPath} trovato=${claims.iss}`);
  }
  if (claims.azp !== wanted.clientId) {
    log('verify', 'azp del token non coerente', 'fail', claims.azp);
  }
  if (!claims.sub) {
    log('verify', 'Claim sub assente (scope "basic" non assegnato al client)', 'fail');
  }
  for (const roleName of user.realmRoles || []) {
    const roles = (claims.realm_access && claims.realm_access.roles) || [];
    if (!roles.includes(roleName)) log('verify', `Ruolo '${roleName}' assente dal token`, 'fail');
  }

  const sig = verifyJwtSignature(grant.body.access_token, jwks);
  log('verify', 'Verifica firma JWT (RS256 vs JWKS)', sig.ok ? 'ok' : 'fail', sig.reason);

  const userinfo = await request('GET', `${KC_URL}${realmPath}/protocol/openid-connect/userinfo`, {
    headers: { Authorization: `Bearer ${grant.body.access_token}` }
  });
  if (userinfo.status === 200 && userinfo.body && userinfo.body.sub) {
    log('verify', 'Userinfo endpoint', 'ok', 'sub=' + userinfo.body.sub + ' email=' + userinfo.body.email);
  } else {
    log('verify', 'Userinfo endpoint', 'fail', 'HTTP ' + userinfo.status);
  }

  const refreshed = await request('POST', `${KC_URL}${realmPath}/protocol/openid-connect/token`, {
    form: {
      grant_type: 'refresh_token',
      client_id: wanted.clientId,
      client_secret: wanted.secret,
      refresh_token: grant.body.refresh_token
    }
  });
  log('verify', 'Refresh token grant', refreshed.status === 200 ? 'ok' : 'fail',
    refreshed.status === 200 ? 'nuovo access_token emesso' : JSON.stringify(refreshed.body));

  const machine = await request('POST', `${KC_URL}${realmPath}/protocol/openid-connect/token`, {
    form: {
      grant_type: 'client_credentials',
      client_id: wanted.clientId,
      client_secret: wanted.secret
    }
  });
  log('verify', 'Client credentials grant (service account)',
    machine.status === 200 ? 'ok' : 'fail',
    machine.status === 200
      ? 'preferred_username=' + decodeJwt(machine.body.access_token).payload.preferred_username
      : JSON.stringify(machine.body));

  if (machine.status === 200 && claims.sub) {
    log('verify', 'Il service account e\' distinto dall utente', 'ok',
      `service-account vs ${user.username}`);
  }

  const loginPage = await request('GET', `${KC_URL}${realmPath}/protocol/openid-connect/auth?client_id=${encodeURIComponent(wanted.clientId)}&response_type=code&scope=openid&redirect_uri=${encodeURIComponent((wanted.redirectUris || [])[0])}`);
  log('verify', 'Authorization endpoint (standard flow)',
    loginPage.status < 400 ? 'ok' : 'fail',
    loginPage.status === 200 ? 'login page raggiungibile' : 'HTTP ' + loginPage.status);

  head('Riepilogo');
  console.log('Realm        : ' + cfg.realm);
  console.log('Realm URL    : ' + KC_URL + '/realms/' + cfg.realm);
  console.log('Client ID    : ' + wanted.clientId);
  console.log('Client Secret: ' + wanted.secret);
  console.log('Utente test  : ' + user.username + ' / ' + (user.credentials || [])[0].value);
  console.log('Internal id  : ' + (client ? client.id : 'n/d'));
  console.log('');
  console.log(exitCode === 0 ? 'RISULTATO: OK' : 'RISULTATO: CI SONO FALLIMENTI');
}

async function main() {
  const { cfg, raw } = await loadConfig();
  if (!AS_JSON) {
    console.log('Keycloak setup WHODAS 2.0');
    console.log('Server  : ' + KC_URL);
    console.log('Admin   : ' + ADMIN_USER);
    console.log('Config  : ' + REALM_FILE);
    if (IMPORT_ONLY) console.log('Modalita: import nativo del JSON');
    if (VERIFY_ONLY) console.log('Modalita: solo verifica');
  }

  const root = await request('GET', KC_URL + '/');
  if (root.status >= 500 || root.status === 0) throw new Error('Keycloak non raggiungibile su ' + KC_URL);
  log('server', 'Keycloak raggiungibile', 'ok', KC_URL);

  const token = await getAdminToken();
  log('token', 'Token master realm ottenuto', 'ok');
  const adminPath = await adminApi(token);
  log('token', 'Admin REST API', 'ok', KC_URL + adminPath);

  if (IMPORT_ONLY) {
    await importRealm(adminPath, token, cfg, raw);
    const imported = await reportImportState(adminPath, token, cfg);
    if (imported) {
      await ensureClientScopes(adminPath, token, cfg, imported.id);
      await ensureUserDefaultRoles(adminPath, token, cfg);
    }
    await verify(cfg, imported);
    return;
  }

  if (!VERIFY_ONLY) {
    await ensureRealm(adminPath, token, cfg);
    await ensureRoles(adminPath, token, cfg);
  }
  const client = await ensureClient(adminPath, token, cfg);
  const ldapProvider = await ensureLdapProvider(adminPath, token, cfg);
  if (!VERIFY_ONLY) {
    await ensureLdapMappers(adminPath, token, cfg, ldapProvider);
    await ensureLdapSync(adminPath, token, cfg, ldapProvider);
    await ensureUsers(adminPath, token, cfg);
  }
  await verify(cfg, client);
}

main()
  .then(() => {
    if (AS_JSON) console.log(JSON.stringify({ ok: exitCode === 0, results }, null, 2));
    process.exit(exitCode);
  })
  .catch((err) => {
    console.error('[FATAL] ' + (err && err.message ? err.message : err));
    if (AS_JSON) console.log(JSON.stringify({ ok: false, error: String(err && err.message), results }, null, 2));
    process.exit(1);
  });
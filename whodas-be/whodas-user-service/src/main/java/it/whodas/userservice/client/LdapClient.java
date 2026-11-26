package it.whodas.userservice.client;

import java.util.Hashtable;
import java.util.Optional;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.whodas.common.exception.WhodasException;
import it.whodas.userservice.config.LdapProperties;

/**
 * Client LDAP (JNDI, senza dipendenze esterne) per la creazione e la
 * rimozione delle utenze.
 *
 * <p>createUser crea l'entry uid=&lt;username&gt; sotto ou=users con
 * objectClass top, person, organizationalPerson, inetOrgPerson,
 * posixAccount, shadowAccount e attributi uid, cn, sn, givenName, mail,
 * userPassword, uidNumber (max esistente sotto ou=users + 1, sequenza da
 * 10001), gidNumber, homeDirectory, loginShell; se l'username esiste gia'
 * lancia {@link WhodasException} 409 "Username gia' esistente".
 *
 * <p>deleteUser e' best-effort (rollback della registrazione): qualsiasi
 * errore viene solo loggato, mai propagato.
 *
 * <p>Usa bind(dn, null, attributes) per la creazione dell'entry: e'
 * l'equivalente standard di add(dn, attributes) per il provider LDAP
 * (crea una nuova entry e, se gia' esistente, produce
 * NameAlreadyBoundException, tradotta in 409). L'equivalente
 * DirContext.add non e' disponibile nella build JDK di questo host.
 * La rimozione usa unbind(dn), equivalente di remove per LDAP.
 */
@Service
public class LdapClient {

    private static final Logger log = LoggerFactory.getLogger(LdapClient.class);

    private static final int BASE_UID_NUMBER = 10001;
    private static final int GID_NUMBER = 10000;
    private static final String HOME_DIRECTORY_PREFIX = "/home/";
    private static final String LOGIN_SHELL = "/bin/sh";
    private static final int LDAP_TIMEOUT_MILLIS = 5000;
    private static final String[] OBJECT_CLASSES = {
            "top", "person", "organizationalPerson", "inetOrgPerson", "posixAccount", "shadowAccount"
    };

    private final LdapProperties props;

    public LdapClient(LdapProperties props) {
        this.props = props;
    }

    public record LdapUserSpec(String username, String password, String firstName, String lastName, String email) {
    }

    public record LdapUserAttributes(String uid, String firstName, String lastName) {
    }

    /**
     * Crea la entry LDAP della nuova utenza durante la registrazione.
     *
     * <p>Bind su {@code LdapProperties.url} con le credenziali
     * {@code LdapProperties.bindDn} / {@code LdapProperties.bindPassword};
     * entry su {@code LdapProperties.userDn(username)} con objectClass
     * top, person, organizationalPerson, inetOrgPerson, posixAccount,
     * shadowAccount e attributi uid, cn, sn, givenName, mail,
     * userPassword (in chiaro), uidNumber (max esistente sotto ou=users + 1,
     * sequenza che parte da 10001), gidNumber, homeDirectory, loginShell.
     *
     * <p>Se l'entry esiste gia': {@link WhodasException} 409
     * "Username gia' esistente" (code USERNAME_EXISTS). Altri errori LDAP:
     * {@link RuntimeException} (500, con rollback a carico del servizio).
     */
    public void createUser(LdapUserSpec spec) {
        DirContext ctx = createContext();
        try {
            if (usernameExistsSilently(ctx, spec.username())) {
                throw new WhodasException("Username gia' esistente", "USERNAME_EXISTS");
            }
            int uidNumber = nextUidNumber(ctx);
            Attributes attributes = new BasicAttributes(true);
            BasicAttribute objectClass = new BasicAttribute("objectClass");
            for (String value : OBJECT_CLASSES) {
                objectClass.add(value);
            }
            attributes.put(objectClass);
            attributes.put("uid", spec.username());
            attributes.put("cn", spec.firstName() + " " + spec.lastName());
            attributes.put("sn", spec.lastName());
            attributes.put("givenName", spec.firstName());
            attributes.put("mail", spec.email());
            attributes.put("userPassword", spec.password());
            attributes.put("uidNumber", String.valueOf(uidNumber));
            attributes.put("gidNumber", String.valueOf(GID_NUMBER));
            attributes.put("homeDirectory", HOME_DIRECTORY_PREFIX + spec.username());
            attributes.put("loginShell", LOGIN_SHELL);
            ctx.bind(props.userDn(spec.username()), null, attributes);
            log.info("Entry LDAP creata per username '{}' (uidNumber {})", spec.username(), uidNumber);
        } catch (AttributeInUseException e) {
            log.warn("Attributo LDAP gia' in uso per username '{}' (race condition): {}", spec.username(), e.toString());
            throw new WhodasException("Username gia' esistente", "USERNAME_EXISTS");
        } catch (NamingException | NoClassDefFoundError e) {
            if (usernameExistsSilently(ctx, spec.username())) {
                log.warn("Username LDAP gia' occupato per '{}' (race condition): {}", spec.username(), e.toString());
                throw new WhodasException("Username gia' esistente", "USERNAME_EXISTS");
            }
            log.error("Creazione entry LDAP per username '{}' fallita", spec.username(), e);
            throw new RuntimeException("LDAP: creazione utenza '" + spec.username() + "' fallita", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Rimuove la entry LDAP {@code uid=<username>} dell'utenza.
     *
     * <p>Usato nel rollback della registrazione: e' best-effort, quindi
     * qualsiasi errore (inclusa l'entry gia' assente) viene loggato e NON
     * propagato, per non mascherare l'errore originale.
     */
    public void deleteUser(String username) {
        DirContext ctx = createContext();
        try {
            if (!usernameExistsSilently(ctx, username)) {
                log.info("Entry LDAP gia' assente per username '{}', rollback LDAP saltato", username);
                return;
            }
            ctx.unbind(props.userDn(username));
            log.info("Entry LDAP rimossa per username '{}'", username);
        } catch (Exception | NoClassDefFoundError e) {
            log.warn("Rollback LDAP per username '{}' non completato: {}", username, e.toString());
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Aggiorna la password dell'utente LDAP.
     *
     * @param username nome utente
     * @param newPassword nuova password in chiaro
     * @throws WhodasException se l'utente non esiste
     */
    public void updatePassword(String username, String newPassword) {
        DirContext ctx = createContext();
        try {
            if (!usernameExistsSilently(ctx, username)) {
                throw new WhodasException("Utente LDAP non trovato", "USER_NOT_FOUND");
            }
            String userDn = props.userDn(username);
            ModificationItem[] mods = new ModificationItem[] {
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newPassword))
            };
            ctx.modifyAttributes(userDn, mods);
            log.info("Password LDAP aggiornata per username '{}'", username);
        } catch (NamingException e) {
            log.error("Aggiornamento password LDAP per username '{}' fallito", username, e);
            throw new RuntimeException("LDAP: aggiornamento password per '" + username + "' fallito", e);
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Verifica la password attuale dell'utente eseguendo un bind LDAP come
     * {@code uid=<username>,...} con la password fornita.
     *
     * @return true se il bind ha successo (password corretta), false altrimenti
     */
    public boolean verifyPassword(String username, String password) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, props.getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, props.userDn(username));
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put("java.naming.timeout", String.valueOf(LDAP_TIMEOUT_MILLIS));
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            return true;
        } catch (NamingException | NoClassDefFoundError e) {
            log.debug("Verifica password LDAP fallita per username '{}': {}", username, e.toString());
            return false;
        } finally {
            if (ctx != null) {
                closeQuietly(ctx);
            }
        }
    }

    /**
     * Ritorna true se esiste la entry LDAP {@code uid=<username>}.
     */
    public boolean userExists(String username) {
        DirContext ctx = createContext();
        try {
            return existsWithFilter(ctx, "(uid=" + escapeFilterValue(username) + ")");
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Ritorna true se esiste la entry LDAP con attributo {@code mail=<email>}.
     */
    public boolean userExistsByEmail(String email) {
        DirContext ctx = createContext();
        try {
            return existsWithFilter(ctx, "(mail=" + escapeFilterValue(email) + ")");
        } finally {
            closeQuietly(ctx);
        }
    }

    private boolean existsWithFilter(DirContext ctx, String filter) {
        try {
            SearchControls controls = subtreeControls(new String[] { "uid" });
            NamingEnumeration<SearchResult> results =
                    ctx.search(props.getUsersDn(), filter, controls);
            try {
                return results.hasMore();
            } finally {
                results.close();
            }
        } catch (Exception | NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * Recupera nome e cognome dell'utente LDAP cercato per {@code uid}.
     */
    public Optional<LdapUserAttributes> getUserAttributes(String username) {
        DirContext ctx = createContext();
        try {
            return searchAttributes(ctx, "(uid=" + escapeFilterValue(username) + ")");
        } finally {
            closeQuietly(ctx);
        }
    }

    /**
     * Recupera nome e cognome dell'utente LDAP cercato per {@code mail}.
     */
    public Optional<LdapUserAttributes> getUserAttributesByEmail(String email) {
        DirContext ctx = createContext();
        try {
            return searchAttributes(ctx, "(mail=" + escapeFilterValue(email) + ")");
        } finally {
            closeQuietly(ctx);
        }
    }

    private Optional<LdapUserAttributes> searchAttributes(DirContext ctx, String filter) {
        try {
            SearchControls controls = subtreeControls(new String[] { "uid", "givenName", "sn", "mail" });
            NamingEnumeration<SearchResult> results =
                    ctx.search(props.getUsersDn(), filter, controls);
            try {
                if (!results.hasMore()) {
                    return Optional.empty();
                }
                Attributes attrs = results.next().getAttributes();
                Attribute uidAttr = attrs.get("uid");
                Attribute givenName = attrs.get("givenName");
                Attribute sn = attrs.get("sn");
                String uid = uidAttr != null ? (String) uidAttr.get() : null;
                String firstName = givenName != null ? (String) givenName.get() : null;
                String lastName = sn != null ? (String) sn.get() : null;
                return Optional.of(new LdapUserAttributes(uid, firstName, lastName));
            } finally {
                results.close();
            }
        } catch (Exception | NoClassDefFoundError e) {
            return Optional.empty();
        }
    }

    private DirContext createContext() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, props.getUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, props.getBindDn());
        env.put(Context.SECURITY_CREDENTIALS, props.getBindPassword());
        env.put("java.naming.timeout", String.valueOf(LDAP_TIMEOUT_MILLIS));
        try {
            return new InitialDirContext(env);
        } catch (NamingException e) {
            throw new RuntimeException("LDAP: apertura context su " + props.getUrl() + " fallita", e);
        }
    }

    private boolean usernameExistsSilently(DirContext ctx, String username) {
        try {
            SearchControls controls = subtreeControls(new String[] { "uid" });
            NamingEnumeration<SearchResult> results =
                    ctx.search(props.getUsersDn(), "(uid=" + escapeFilterValue(username) + ")", controls);
            try {
                return results.hasMore();
            } finally {
                results.close();
            }
        } catch (Exception | NoClassDefFoundError e) {
            return false;
        }
    }

    private int nextUidNumber(DirContext ctx) throws NamingException {
        SearchControls controls = subtreeControls(new String[] { "uidNumber" });
        int max = 0;
        boolean found = false;
        NamingEnumeration<SearchResult> results = ctx.search(props.getUsersDn(), "(uidNumber=*)", controls);
        try {
            while (results.hasMore()) {
                Attribute uidNumberAttribute = results.next().getAttributes().get("uidNumber");
                if (uidNumberAttribute == null) {
                    continue;
                }
                for (int i = 0; i < uidNumberAttribute.size(); i++) {
                    int value = parseUidNumber(uidNumberAttribute.get(i).toString());
                    if (value > 0) {
                        max = Math.max(max, value);
                        found = true;
                    }
                }
            }
        } finally {
            results.close();
        }
        return found ? max + 1 : BASE_UID_NUMBER;
    }

    private int parseUidNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Valore uidNumber non numerico ignorato nel calcolo del successivo: '{}'", value);
            return 0;
        }
    }

    private SearchControls subtreeControls(String[] returningAttributes) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(returningAttributes);
        return controls;
    }

    private static String escapeFilterValue(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\\' -> sb.append("\\5c");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private void closeQuietly(DirContext ctx) {
        try {
            ctx.close();
        } catch (NamingException e) {
            log.warn("Chiusura context LDAP fallita: {}", e.getMessage());
        }
    }
}

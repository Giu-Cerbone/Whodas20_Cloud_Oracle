package it.whodas.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    private String url = "ldap://localhost:389";
    private String bindDn = "cn=admin,dc=whodascompany,dc=local";
    private String bindPassword = "admin";
    private String baseDn = "dc=whodascompany,dc=local";
    private String usersDn = "ou=users,dc=whodascompany,dc=local";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUsersDn() {
        return usersDn;
    }

    public void setUsersDn(String usersDn) {
        this.usersDn = usersDn;
    }

    public String userDn(String username) {
        return "uid=" + username + "," + usersDn;
    }
}

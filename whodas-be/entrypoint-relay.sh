#!/bin/sh
# Configurazione permanente del relay Brevo per docker-mailserver
cp /tmp/sasl_passwd /etc/postfix/sasl_passwd
postconf -e "relayhost = [smtp-relay.brevo.com]:587"
postconf -e "smtp_sasl_auth_enable = yes"
postconf -e "smtp_sasl_password_maps = hash:/etc/postfix/sasl_passwd"
postconf -e "smtp_sasl_security_options = noanonymous"
postconf -e "smtp_tls_security_level = encrypt"
postconf -e "smtp_tls_CAfile = /etc/ssl/certs/ca-certificates.crt"
postmap hash:/etc/postfix/sasl_passwd
postfix reload 2>/dev/null || true
exec /usr/bin/dumb-init -- supervisord -c /etc/supervisor/supervisord.conf
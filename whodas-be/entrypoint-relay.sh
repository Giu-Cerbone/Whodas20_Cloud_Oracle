#!/bin/sh
# Configurazione permanente del relay Brevo per docker-mailserver
# Il file sasl_passwd è già montato su /etc/postfix/sasl_passwd (volume ro).
# Copiamolo in /tmp (scrivibile), generiamo il database .db con postmap,
# poi riportiamo entrambi i file in /etc/postfix/.
cp /etc/postfix/sasl_passwd /tmp/sasl_passwd
postmap hash:/tmp/sasl_passwd
cp /tmp/sasl_passwd /etc/postfix/sasl_passwd
cp /tmp/sasl_passwd.db /etc/postfix/sasl_passwd.db
postconf -e "relayhost = [smtp-relay.brevo.com]:587"
postconf -e "smtp_sasl_auth_enable = yes"
postconf -e "smtp_sasl_password_maps = hash:/etc/postfix/sasl_passwd"
postconf -e "smtp_sasl_security_options = noanonymous"
postconf -e "smtp_tls_security_level = encrypt"
postconf -e "smtp_tls_CAfile = /etc/ssl/certs/ca-certificates.crt"
postfix reload 2>/dev/null || true
exec /usr/bin/dumb-init -- supervisord -c /etc/supervisor/supervisord.conf
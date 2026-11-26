#!/bin/bash
# Custom startup script for docker-mailserver
# This script runs after the main services start and applies Postfix configuration

# Wait for Postfix to be ready
sleep 10

# Apply Postfix configuration for Brevo relay
postconf -e "relayhost=[smtp-relay.brevo.com]:587"
postconf -e "smtp_sasl_auth_enable=yes"
postconf -e "smtp_sasl_password_maps=hash:/etc/postfix/sasl_passwd"
postconf -e "smtp_sasl_security_options=noanonymous"
postconf -e "smtp_tls_security_level=encrypt"
postconf -e "smtp_tls_CAfile=/etc/ssl/certs/ca-certificates.crt"

# Create sasl_passwd file from environment variable
if [ -n "$SASL_PASSWD" ]; then
  echo "$SASL_PASSWD" > /etc/postfix/sasl_passwd
  chmod 640 /etc/postfix/sasl_passwd
  chown root:postfix /etc/postfix/sasl_passwd
  postmap /etc/postfix/sasl_passwd
fi

# Reload Postfix
postfix reload

echo "Custom Postfix configuration applied"
#!/usr/bin/env bash
svcadm disable rohrlid
SOURCE=/opt/local/etc/letsencrypt/live/rohrli.gamlor.info
TARGET=/etc/rohrli
openssl pkcs12 -inkey $SOURCE/privkey.pem -in $SOURCE/fullchain.pem -export -passout pass: -out $TARGET/cert-key.pkcs12
chown -R rohrlid:rohrlid $TARGET/cert-key.pkcs12
svcadm enable rohrlid

echo "certificate copied and server restarted"
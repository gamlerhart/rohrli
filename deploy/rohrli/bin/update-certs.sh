#!/usr/bin/env bash

SOURCE=/opt/local/etc/letsencrypt/live/rohrli.gamlor.info
TARGET=/etc/rohrli
openssl pkcs12 -inkey $SOURCE/privkey.pem -in $SOURCE/fullchain.pem -export -out $TARGET/cert-key.pkcs12
chown -R rohrlid:rohrlid $TARGET/cert-key.pkcs12
svcadm restart rohrlid

echo "certificate copied and server restarted"
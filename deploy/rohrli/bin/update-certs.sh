#!/usr/bin/env bash
SOURCE=/etc/letsencrypt/live/rohrli.gamlor.info
TARGET=/etc/rohrli
openssl pkcs12 -inkey $SOURCE/privkey.pem -in $SOURCE/fullchain.pem -export -passout pass: -out $TARGET/cert-key.pkcs12
chown -R rohrli:rohrli $TARGET/cert-key.pkcs12
systemctl enable rohrli.service
systemctl start rohrli.service

echo "certificate copied and server restarted"
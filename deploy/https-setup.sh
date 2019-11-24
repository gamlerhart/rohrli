#!/usr/bin/env bash

apt install certbot
# Getting certs
certbot --standalone -d rohrli.gamlor.info certonly

certbot renew --pre-hook "/etc/rohrli/bin/preupdate-certs.sh" --post-hook "/etc/rohrli/bin/update-certs.sh"
# Crontab -e
42 6 * * * certbot renew --pre-hook "/etc/rohrli/bin/preupdate-certs.sh" --post-hook "/etc/rohrli/bin/update-certs.sh"
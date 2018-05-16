#!/usr/bin/env bash
# Manual setup steps right now.
pkgin -y install tmux
pkgin -y install py27-certbot

# Getting certs
certbot --standalone -d rohrli.gamlor.info certonly
#

certbot renew --pre-hook "/etc/rohrli/bin/preupdate-certs.sh" --post-hook "/etc/rohrli/bin/update-certs.sh"
# Crontab -e
42 6 * * * certbot renew --pre-hook "/etc/rohrli/bin/preupdate-certs.sh" --post-hook "/etc/rohrli/bin/update-certs.sh"
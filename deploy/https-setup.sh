#!/usr/bin/env bash

pkgin -y install tmux
pkgin -y install py27-certbot

# Getting certs
certbot --standalone -d rohrli.gamlor.info certonly
#
certbot renew --dry-run

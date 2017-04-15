#!/usr/bin/env bash

pkgin -y install tmux
pkgin -y install py27-certbot

# Getting certs
certbot --standalone -d rohrli.gamlor.info certonly
#
certbot renew --dry-run

#Ngnix
pkgin -y install nginx

cp ngnix.conf /opt/local/etc/nginx/ngnix.conf
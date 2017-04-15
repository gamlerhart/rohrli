#!/usr/bin/env bash

PASSWORD=RohrTest123
#openssl genrsa -aes128 -passout pass:RohrTest123 -out rohrli.key
#openssl req -new -x509 -newkey rsa:2048 -sha256 -key rohrli.key -passin pass:RohrTest123 -out rohrli.crt
openssl req -x509 -newkey rsa:4096 -subj '/CN=localhost' -keyout rohrli.key -passout pass:$PASSWORD -out rohrli.pem -days 365
openssl pkcs12 -inkey rohrli.key -in rohrli.pem -export -passout pass:$PASSWORD -passin pass:$PASSWORD  -out rohrli.pkcs12
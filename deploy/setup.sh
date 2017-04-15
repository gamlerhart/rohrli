#!/usr/bin/env bash
set -e

if ! [[ -d /etc/rohrli ]]; then
    echo "first time installation."
    echo "Installing OpenJDK 8"
    pkgin -y install openjdk8

    echo "Creating service user"
    groupadd rohrlid
    useradd -grohrlid -m rohrlid
else
    echo "Existing installation. Updating"
fi

echo "Copy over new files"
tar -xvf rohrli.tar -C /etc/
chmod -R u+rx,g+rx /etc/rohrli
mkdir -p /etc/rohrli/logs
chmod -R u+rwx,g+rwx /etc/rohrli/logs
chown -R rohrlid:rohrlid /etc/rohrli


echo "Import, enable and restart service"
svccfg import /etc/rohrli/rohrlid-service.xml
svcadm enable rohrlid
svcadm restart rohrlid



#!/usr/bin/env bash
set -e

if ! [[ -d /etc/rohrli ]]; then
    echo "First time installation."
    echo "Installing OpenJDK 11"
    apt update
    apt install -y openjdk-11-jdk-headless

    echo "Creating service user"
    groupadd rohrli
    useradd -grohrli -m rohrli
else
    echo "Existing installation. Updating"
fi

echo "Copy over new files"
tar -xvf rohrli.tar -C /etc/
chmod -R u+rx,g+rx /etc/rohrli
mkdir -p /etc/rohrli/logs
chmod -R u+rwx,g+rwx /etc/rohrli/logs
chown -R rohrli:rohrli /etc/rohrli
[[ -f /etc/systemd/system/rohrli.service ]] || ln -s /etc/rohrli/bin/rohrli.service /etc/systemd/system/rohrli.service

echo "Import, enable and restart service"
systemctl enable rohrli.service
systemctl restart rohrli.service



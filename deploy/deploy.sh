#!/usr/bin/env bash

set -e

if [[ -z "$SERVER" ]]; then
    echo 'Expect $SERVER to be set'
    exit 1
fi

# Build stuff
lein uberjar
rm -r -f /S /Q ./target/rohrlid
cp -r ./deploy/rohrli ./target/rohrli
cp ./target/uberjar/rohrli.jar ./target/rohrli
cp -r ./resources/web ./target/rohrli

pushd ./target
tar czf rohrli.tar rohrli
scp ./rohrli.tar root@$SERVER:~/rohrli.tar
scp ../deploy/setup.sh root@$SERVER:~/setup.sh
ssh root@$SERVER '/usr/bin/env bash ~/setup.sh'

popd
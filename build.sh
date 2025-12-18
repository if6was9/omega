#!/bin/bash

set -e 

#ssh git@github.com

./mvnw -B clean test install

cp $(find target -name '*.jar' -maxdepth 1 | head -1) target/stage/app.jar

if [[ "${CI}" = "true" ]]; then
docker buildx build .  --platform linux/amd64,linux/arm64 --push --tag ghcr.io/if6was9/omega:latest
else 
docker buildx build . --tag ghcr.io/if6was9/omega:latest
fi

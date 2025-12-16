#!/bin/bash

set -e 

./mvnw -B clean test install

cp $(find target -name '*.jar' -maxdepth 1 | head -1) target/stage/app.jar


if [[ "${CI}" = "true" ]]; then
docker buildx build . --no-cache --platform linux/amd64,linux/arm64 --push --tag ghcr.io/if6was9/vfrmap:latest
else 
docker buildx build . --no-cache --platform linux/arm64,linux/amd64 --tag ghcr.io/if6was9/vfrmap:latest
fi

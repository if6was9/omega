#!/bin/bash
# -v ${HOME}/Desktop/ssh:/root/.ssh 
# -v ${HOME}/Desktop/ssh:/root/.ssh
docker run -it  -e GIT_URL -e VERIFY_HOST_KEY=false -v /var/run/docker.sock:/var/run/docker.sock ghcr.io/if6was9/omega:latest

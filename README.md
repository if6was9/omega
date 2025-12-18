
# Omega

Omega allows . The omega container polls a remote
git repo periodically, looks for omega-enabled compose files and runs them if so configured.

There is a special `x-omega` section that needs to be added to your `docker-compose.yaml` file. It is igored by compose itself.
The `id` element matches the `ID` element from `docker info`, which uniquely identifies the host.

```yaml
services:
  web:
    image: nginx

x-omega:
  run:
  - id:  57e8cf92-eaf3-49ff-a9cf-baf6112b76dc
```

If you want to temporarily disbale the service, you can add `enabled: false` to the appropriate `run` section.  Renaming
the `id` value would work too.

## Why Omega?

I have a handful of standalone hosts running containers and I wanted a simple way to 
manage what is running on them in a GitOps manner. 

# Configuration

| Env Var | Description | Default |
|----|----|-----|
|`GIT_URL`|url to clone repo | required | 
|`GIT_USERNAME` | Username for password auth | optional |
|`GIT_PASSWORD` | Password for password auth | optional |
|`VERIFY_HOST_KEY`|set to false to ignore `known_hosts` (Default: `true`) |optional|
|`POLL_SECS`|# of seconds between git pull _(default: 60)_ | optional |

Bind-mounting the SSH dir may be the easiest way to configure auth.  This can be accomplished with
the following docker option `-v ${HOME}/.ssh:/root/.ssh`.

## Future

* Provide a way to match on something other than docker engine ID
* Add support for Kubernetes  
* Re-write this in Go, Python or Rust



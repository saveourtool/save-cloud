# SAVE Cloud deployment configuration
## Components
SAVE Cloud contains the following microservices:
* backend: REST API for DB
* test-preprocessor: clones projects for test and discovers tests
* orchestrator: moderates distributed execution of tests, feeds new batches of tests to a set of agents

## Building
* To build the project and run all tests, execute `./gradlew build`.
* For deployment, all microservices are packaged as docker images with the version based on latest git tag and latest commit hash, if there are commits after tag.
To build release version after you create git tag, make sure to run gradle with `-Preckon.stage=final`.

Deployment is performed on server via docker swarm or locally via docker-compose. See detailed information below.

## Server deployment
* Ensure that docker daemon is running and that docker is in swarm mode.
* Pull new changes to the server and run `./gradlew deployDockerStack`.

## Local deployment
* Ensure that docker daemon is running and docker-compose is installed.
* Run `./gradlew deployLocal` to start only some components.

## Ports allocation
| port | descritpion |
| ---- | ----------- |
| 5000 | save-backend |
| 5100 | save-orchestrator |
| 5200 | save-test-preprocessor |
| 6000 | local docker registry (not used currently) |
| 8081 | h2 console (active with `dev` profile) |
| 9090 | prometheus |
| 9091 | node_exporter |
| 9100 | grafana |
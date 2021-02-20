# SAVE Cloud deployment configuration
SAVE Cloud contains the following microservices:
* backend: REST API for DB
* test-preprocessor: clones projects for test and discovers tests
* orchestrator: moderates distributed execution of tests, feeds new batches of tests to a set of agents

# Local deployment
Run `./gradlew deployLocal`

## Ports allocation
* 5000 - save-backend
* 5100 - save-orchestrator
* 5200 - save-test-preprocessor
* 6000 - local docker registry (not used currently)
* 9090 - prometheus
* 9091 - node_exporter
* 9100 - grafana
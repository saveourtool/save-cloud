# Contributing
1. Fork this repository to your own account
2. Make your changes and verify that tests pass (`./gradlew check`)
3. Run `diktat` static analyzer with `diktatFix` gradle task or `diktatCheck` task depending on what you want.
4. Run `detekt` with `detektAll` gradle task
5. After all tests and analyzers passing commit your work and push to a new branch on your fork
6. Submit a pull request
7. Participate in the code review process by responding to feedback

## Launching save-cloud with command line (usually for local development)
1. Use `gradlew.bat startMysqlDb` on Windows and `./gradlew startMysqlDb` on other platforms for setting up database.
Make sure you have Docker installed and active.
2. Run backend.
It can be run either with `./gradlew save-backend:bootRun` or with Intellij Idea Ultimate plugin from the menu `Services`.
3. Prepare a storage. For local run we have MINIO: `./gradlew startMinio`
4. Run frontend. It can be run with `./gradlew save-frontend:run`.
You can enable hot reload by passing `--continuous` flag.
5. More specific instructions can be found in [save-deploy](save-deploy/README.md)

## Spring Intellij Idea Ultimate plugin
In order to make Spring Intellij Idea Ultimate plugin work properly, you need to set these active profiles in service's configuration:  

|         | SaveApplication  | SaveGateway |   SaveOrchestrator   | SavePreprocessor |         SaveSandbox         |
|:-------:|:----------------:|:-----------:|:--------------------:|:----------------:|:---------------------------:|
|   Mac   | `mac,dev,secure` |  `mac,dev`  | `dev,mac,docker-tcp` |    `dev,mac`     | `dev,mac,docker-tcp,secure` | 
| Windows |   `dev,secure`   |    `dev`    | `dev,win,docker-tcp` |      `dev`       | `dev,win,docker-tcp,secure` |
|  Linux  |   `dev,secure`   |    `dev`    |   `dev,docker-tcp`   |      `dev`       |   `dev,docker-tcp,secure`   |

### Mac M1 contributors
In order to run `save-orchestrator` on Mac with M1 in order to make it run executions, in addition to `save-deploy/README.md` you need to 
1. manually put all the files from `save-agent-*-distribution.jar` into `save-orchestrator/build/resources/main` as well as `save-*-linuxX64.kexe` (temporary workaround) 
2. run `docker-mac-settings.sh` script (from `save-deploy` folder) in order to let docker be available via TCP 
Also check `save-deploy/README.md` for extra information

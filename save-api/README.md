# Save Cloud API

### General information

Current module provides the basic example of using API for execution submission process.

According specified information from config files, it will automatically build and submit\
execution requests and received results, like it could be done manually at [saveourtool.com](https://saveourtool.com/)

For proper usage you should be registered at [saveourtool.com](https://saveourtool.com/)
and create at least one project in your organization.

### Configuration

The general configuration represents the properly filled configuration files:
1) `web-client.properties` 
2) `evaluated-tool.properties`

While first of them should contain only proper URLs of SAVE-cloud server, the
second one is more complicated.

`evaluated-tool.properties` contains three main sections, all fields in them correspondingly
marked as required/optional:

* **General:** in this section you need to provide general information about
  your organization and project, which should be tested. Data should be the same
  with one, that were
  specified, when you registered it at [saveourtool.com](https://saveourtool.com/)
* **Git:** current section should contain corresponding information about your tool,
  if you would like to test it in the `GIT` mode
* **Standard:** this section should contain configuration for `STANDARD` mode
  if you would like to test it in corresponding mode
  
**Note** optional fields should be commented or not provided at all for proper execution,\
if you won't use such configuration. If you use only one mode of `git/standard`,
required fields from another section could have empty values.

### Execution:

**Note:** Module will be included into the project, only if system variable `includeSaveApi` is set

Current API required few cli arguments for execution:
```
  -m execution mode: git or standard
  -u username in SAVE-cloud system
  -t OAuth token for SAVE-cloud system
```

Example:

    ./gradlew save-api:run -DincludeSaveApi='true' --args='-u user -p password -m git'
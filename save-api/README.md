# Save Cloud API Library

Current module provides the library for usage of Save Cloud API.
The library is written in Kotlin using Ktor client and is available as a dependency for JVM projects.

Module contain few main interfaces: Set of http requests in `RequestUtils.kt`, which contain the required
requests for tool evaluation in Save Cloud system, and client, `SaveCloudClient.kt`,
which combined in itself completed interface for execution submission. Basically, the only client
is necessary for usage of Save Cloud API. It requires just a tiny configuration for running the process of execution submission.

The four main characteristics for client are necessary for execution:

* **Configuration for web client** \
  Basically it's just the Save Cloud server address and port. (`WebClientProperties`)
* **Configuration for evaluated tool** \
  Current configuration should contain information about evaluated tool. (`EvaluatedToolProperties`)

* **Authorization data** \
  Credentials for authorization in Save Cloud system (`Authorization.kt`)

* **Execution type** Git or standard


  Detailed description about these configurations could be found in
  [Backend-API.md](../save-backend/Backend-API.md)

The example of cli application, which uses this library could be found in [save-api-cli](../save-api-cli) module,
however there could be used any other convenient implementation. 

With completed configuration, provided to the `SaveCloudClient` by `WebClientProperties`, `EvaluatedToolProperties`
and `Authorization` instances the only `SaveCloudClient.start()` method is enough for automatic initialization of execution process.

## Alternative API

Alternatively, you can use `SaveCloudClientEx`. It allows you to:

 - list organizations;
 - list projects within an organization;
 - list test suites within an organization;
 - list active contests;
 - list uploaded files;
 - upload and delete files;
 - list executions;
 - submit an execution;
 - look up an execution by id.

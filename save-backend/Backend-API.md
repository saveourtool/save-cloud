## SAVE Cloud Backend API
### Overview

The process of execution submission is very simple, all that you have to do
is just to provide your credentials and information about your project, which you
would like to evaluate and do few requests.\
In this document will be represented the examples, which use `cURL`
as instrument for working with SAVE Cloud API.

To make the requests more universal, we will
store major information, which is required by SAVE Cloud in requests, by
into variables in **command prompt**.

### Authorization configuration

Each request, performed to API, require credentials, which could be configured like below, where

* `SAVE_CLOUD_URL` - SAVE Cloud url and port
* `SAVE_CLOUD_AUTH_SOURCE` where the identity is coming from, e.g. "github"
* `SAVE_CLOUD_AUTH` - basic authorization header in format `source@username:token`.\
   `source` is the same like `SAVE_CLOUD_AUTH_SOURCE`, and `token` is unique token,\
    which could be created in personal settings in SAVE Cloud system.

Just open up a command prompt and enter the following commands:

```bash
SAVE_CLOUD_URL=https://saveourtool.com:443

SAVE_CLOUD_AUTH_SOURCE=github

SAVE_CLOUD_AUTH='Basic Z2l0aHViQHVzZXJuYW1lOnRva2Vu'
```

### Configuration of information about evaluated tool

To specify information about project, that you would like to evaluate,
it's necessary to create the fields below in command promt. Only the `organizationName`,
`projectName` and `gitUrl` are required, if your project is public:


```bash
# Required
organizationName=Huawei

# Required
projectName='"save"'

# Required
gitUrl='"https://github.com/analysis-dev/save-cli"'

# Optional
sdkName='"openjdk"'
sdkVersion='"11"'

# Optional
# Git username (provide it, if tests and benchmarks that you plan to use are stored in the private repository on git)
gitUserName=null

# Optional
# Git password. 
# Provide an access token, if tests and benchmarks that you plan to use are stored in the private repository on git
gitPassword=null
```

**Note**: The `organizationName` here is intentionally don't contain quotes, since this value
will be used in requests url, while other in requests bodies.

There is two modes for execution, that could be performed: Git and Standard.
* Git mode: Type of evaluation for testing your tool with your own benchmarks.
* Standard mode: Type of evaluation for testing your tool with 'standard' benchmarks, provided by SAVE Cloud system.

Each of them require own configuration.

#### Configuration for Git Mode
<details>
  <summary>Expand</summary>

Only the `testRootPath`, which represents 
the relative path to the root directory with tests in your repository is required for execution.

```bash
# Required
testRootPath='"examples/kotlin-diktat"'

# Optional
# Specify concrete branch in your git repository
branch='"origin/feature/testing_for_cloud"'

# Optional
# Specify concrete commit in your git repository, the latest one will be used by default
commitHash=null
```

</details>

#### Configuration for Standard Mode

For Standard Mode you need provide at least the list of test suites,
which should be involved in testing

<details>
  <summary>Expand</summary>

```bash
# Required
testSuites='["Directory: Chapter 1", "Directory: Chapter2"]'

# Optional
execCmd=null

# Optional
# Batch size controls how many files will be processed at the same time.
batchSize=null

```
</details>

### API for execution

#### Upload additional files into SAVE Cloud storage

If your tests require additional files for execution, they could be uploaded by
following `post` request:

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/files/upload" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "file=@your-file-name"
```

It will return `json` with metadata about your file, which will be required\
in the execution request later.

The format will have the following form:

```bash
{
  "name": "your-file-name",
  "isExecutable":false
}
```

By default, the `isExecutable` argument, which indicates, whether the provided file\
should be executable, or not, is `false`. \
However, for the execution request from the next section, you can set this argument with value, that you need.


#### Execution submission

For execution request you will need full information about your project, which could be
saved into variable `project` by following request:

```bash
project=$(curl -X GET "${SAVE_CLOUD_URL}/api/v1/projects/get/organization-name?name=save&organizationName=${organizationName}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}")
```
Unifying all above, here the examples of how to submit execution, both in
Git and Standard modes via curl:

<details>
  <summary>Request for execution submission in Git Mode</summary>

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/submitExecutionRequest" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "executionRequest={
    \"project\": ${project},
    \"gitDto\": {
        \"url\": ${gitUrl},
        \"username\": ${gitUserName},
        \"password\": ${gitPassword},
        \"branch\": ${branch},
        \"hash\": ${commitHash}
    },
    \"testRootPath\": ${testRootPath},
    \"sdk\": {
      \"name\": ${sdkName},
      \"version\": ${sdkVersion}
    },
    \"executionId\" : null
};type=application/json" \
-F 'file={
  "name": "ktlint",
  "isExecutable": false
};type=application/json' \
-F 'file={
  "name": "diktat.jar",
  "isExecutable": false
};type=application/json'
```

</details>

<details>
  <summary>Request for execution submission in Standard Mode</summary>

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/executionRequestStandardTests" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "execution={
    \"project\": ${project},
    \"testsSuites\": ${testSuites},
    \"sdk\": {
      \"name\": ${sdkName},
      \"version\": ${sdkVersion}
    },
    \"executionId\" : null
};type=application/json" \
-F 'file={
  "name": "ktlint",
  "isExecutable": false
};type=application/json' \
-F 'file={
  "name": "diktat-analysis.yml",
  "isExecutable":false
};type=application/json' \
-F 'file={
  "name": "diktat.jar",
  "isExecutable": false
};type=application/json'
```
</details>

These requests will also return execution id, which could be used for getting the
execution results or for rerun command.

For example:
```bash
Clone pending, execution id is 42
```

To get execution results, knowing the execution id, use the following request:

```bash
# Taken after submitExecutionRequest request
executionId=42

curl -X GET "${SAVE_CLOUD_URL}/api/v1/executionDto?executionId=${executionId}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```


You are also able to get results of latest execution by `/latestExecution` endpoind,
with the following `get` request: 

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/latestExecution?name=save&organizationName=${organizationName}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

The response format will look like:

```bash
{
  "id": 42, # execution id
  "status": "FINISHED", # execution status, i.e. running, finished and so on
  "type":"GIT", # execution type
  "version": "264e5feb8f4c6410d70536d6fc4bdf090df62287", # commit hash
  "startTime": 1651856549, # start time of execution in Unix format
  "endTime": 1651856797, # end time of execution in Unix format
  "runningTests":0, # number of running tests at this moment
  "passedTests":20, # number of passed tests
  "failedTests":3, # number of failed tests
  "skippedTests":1, # number of skipped tests, i.e., because of configuration 
  "additionalFiles": [ # the list of additional files
    "/file-1",
    "/file-2",
    ]
}
```


If you would like to rerun some of your executions, you can use `/rerunExecution` endpoint:

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/rerunExecution?id=${executionId}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```
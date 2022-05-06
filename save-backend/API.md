## SAVE Cloud Backend API
### Overview

The process of execution submission is very simple, all that you have to do
is just to provide your credentials and information about your project, which you
would like to evaluate and do few requests.\
In this document will be represented the examples, which use `cURL`
as instrument for working with SAVE Cloud API.

To make the requests more universal, we will
use the environment variables for all data, which is required by SAVE Cloud

### Authorization configuration

Each request, performed to API, require credentials, which could be configured like below, where

* `SAVE_CLOUD_URL` - SAVE Cloud url and port
* `SAVE_CLOUD_AUTH_SOURCE` where the identity is coming from, e.g. "github"
* `SAVE_CLOUD_AUTH` - basic authorization header in format `source@username:token`.\
   `source` is the same like `SAVE_CLOUD_AUTH_SOURCE`, and `token` is unique token,\
    which could be created in personal settings in SAVE Cloud system.

```bash
SAVE_CLOUD_URL=https://saveourtool.com:443

SAVE_CLOUD_AUTH_SOURCE=github

SAVE_CLOUD_AUTH='Basic Z2l0aHViQHVzZXJuYW1lOnRva2Vu'
```

### Configuration of data of evaluated tool

To specify information about project, that you would like to evaluate,
it's necessary to fill the fields below. Only the `organizationName`,
`projectName` and `gitUrl` are required, if your project is public:


```bash
# Required
organizationName=Huawei

# Required
projectName='"save"'

# Optional
sdkName='"openjdk"'
sdkVersion='"11"'

# Required
gitUrl='"https://github.com/analysis-dev/save-cli"'

# Optional
# Git username (generally, should be provided, only if tested project is private)
gitUserName=null

# Optional
# Git password (generally, should be provided, only if tested project is private)
gitPassword=null
```

**Note**: The `organizationName` here is intentionally doesn't contain quotes, since this value
will be used in requests url, while other in request body.

There is two modes for execution, that could be performed: Git and Standard.\
Each of them require own information

#### Configuration for Git Mode
<details>
  <summary>Expand</summary>

Only the `testRootPath`, which represents 
the relative path to the root directory with tests in your repository is required for execution.

```bash
# Required
testRootPath='"examples/kotlin-diktat"'

# Optional
branch='"origin/feature/testing_for_cloud"'

# Optional
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

It will return `json` with metadata about your file, which will be used\
in the execution request later.

The format will have the following form:

```bash
{
  "name": "your-file-name",
  "uploadedMillis": 1651662834923,
  "sizeBytes":172,
  "isExecutable":false
}
```

If you already uploaded them into the SAVE Cloud storage, you will need\
to get the corresponding `json`. It could be done by following request,\
which will return the metadata of all files in SAVE Cloud storage:

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/files/list" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

#### Execution submission

```bash
project=$(curl -X GET "${SAVE_CLOUD_URL}/api/v1/projects/get/organization-name?name=save&organizationName=${organizationName}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}")
```

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
  "uploadedMillis": 1637658398621,
  "sizeBytes": 54167132,
  "isExecutable": false
};type=application/json' \
-F 'file={
  "name": "diktat.jar",
  "uploadedMillis": 1637658396121,
  "sizeBytes": 6366668,
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
  "uploadedMillis": 1637658398621,
  "sizeBytes": 54167132,
  "isExecutable": false
};type=application/json' \
-F 'file={
  "name": "diktat-analysis.yml",
  "uploadedMillis": 1637673340431,
  "sizeBytes":3207,
  "isExecutable":false
};type=application/json' \
-F 'file={
  "name": "diktat.jar",
  "uploadedMillis": 1637658396121,
  "sizeBytes": 6366668,
  "isExecutable": false
};type=application/json'
```

</details>

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/latestExecution?name=save&organizationName=${organizationName}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

```bash
# Taken after submitExecutionRequest request
executionId=42

curl -X GET "${SAVE_CLOUD_URL}/api/v1/executionDto?executionId=${executionId}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/rerunExecution?id=${executionId}" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```
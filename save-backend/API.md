## SAVE Cloud Backend API

#### Authorization configuration

```bash
SAVE_CLOUD_URL=https://saveourtool.com:443

SAVE_CLOUD_AUTH_SOURCE=github

SAVE_CLOUD_AUTH='Basic Z2l0aHViQHVzZXJuYW1lOnRva2Vu'
```

#### General information about user and evaluated tool

```bash
# Required
organizationName='"Huawei"'

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


#### Configuration for Git Mode
<details>
  <summary>Expand</summary>

```bash

# Optional
# Specify concrete git branch
branch='"origin/feature/testing_for_cloud"'

# Optional
# Specify concrete commit
commitHash=null

# Required
# Relative path to the root directory with tests in your repository
testRootPath='"examples/kotlin-diktat"'
```

</details>

#### Configuration for Standard Mode

<details>
  <summary>Expand</summary>


```bash
# Required
# Test suite names from standard test suites set, separated by `;`
testSuites='"Directory: Chapter 1;Directory: Chapter2"'

# Optional
# Execution command for testing
execCmd=null

# Optional
# Batch size controls how many files will be processed at the same time.
batchSize=null

```
</details>

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/files/list" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/files/upload" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "file=@main.c"
```

{"name":"main.c","uploadedMillis":1651662834923,"sizeBytes":172,"isExecutable":false}


FixMe:

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/projects/get/organization-name?name=save&organizationName=Huawei" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/submitExecutionRequest" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "executionRequest={
    \"project\": {
        \"name\": ${projectName},
        \"url\": ${gitUrl},
        \"description\": null,
        \"status\": \"CREATED\" ,
        \"public\": true,
        \"userId\": 1,
        \"email\": null,
        \"numberOfContainers\": 3,
        \"organization\": {
            \"name\": ${organizationName},
            \"ownerId\": 1,
            \"dateCreated\": \"2021-01-01T00:00:00\",
            \"avatar\": null,
            \"description\": null,
            \"id\": 1
        },
        \"contestRating\": 0,
        \"id\":5
    },
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

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/latestExecution?name=save&organizationName=Huawei" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

FixMe:
```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/executionDto?executionId=4" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```
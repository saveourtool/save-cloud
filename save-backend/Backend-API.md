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
* `SAVE_CLOUD_AUTH` - basic authorization header in default format `username:token`.\
   `token` is unique token, which could be created in personal settings in SAVE Cloud system.

Just open up a command prompt and enter the following commands:

```bash
SAVE_CLOUD_URL=https://saveourtool.com:443

SAVE_CLOUD_AUTH='Basic Z2l0aHViQHVzZXJuYW1lOnRva2Vu'
```

### Configuration of information about evaluated tool

To specify information about project, that you would like to evaluate,
it's necessary to create the fields below in command prompt.
The `organizationName`, `projectName` are required.
IDs of test suites should be provided for execution. Also need to provide a list of additional files:


```bash
# Required
organizationName=Huawei

# Required
projectName='"save"'

# Optional
sdkName='"openjdk"'
sdkVersion='"11"'

# Required
testSuites='[1, 2]'

# Required
additionalFiles='[fileName1:123;fileName2:321]'
```

**Note**: The `organizationName` here is intentionally don't contain quotes, since this value
will be used in requests url, while other in requests bodies.



#### Optional configuration
<details>
  <summary>Expand</summary>

```bash
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
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-F "file=@your-file-name"
```

It will return `json` with metadata about your file, which will be required\
in the execution request later.

The format will have the following form:

```bash
{
  "name": "your-file-name",
  "uploadedMillis":1658227620000,
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
-H "Authorization: ${SAVE_CLOUD_AUTH}")
```
Unifying all above, here the examples of how to submit execution via curl:

<details>
  <summary>Request for execution submission</summary>

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/run/trigger" \
-H "Authorization: ${SAVE_CLOUD_AUTH}" \
-H "Content-Type: application/json" \
-d "{
    \"projectCoordinates\": {
        \"organizationName\": ${organizationName},
        \"projectName\": ${projectName},
    },
    \"testSuiteIds\": [${testSuiteId1}, ${testSuiteId1}],
    \"files\": [
        {
            \"name\": ${fileKey1.name},
            \"uploadedMillis\": ${fileKey1.uploadedMillis}
        },
        {
            \"name\": ${fileKey2.name},
            \"uploadedMillis\": ${fileKey2.uploadedMillis}
        }
    ],
    \"sdk\": {
      \"name\": ${sdkName},
      \"version\": ${sdkVersion}
    },
    \"execCmd\": ${execCmd},
    \"batchSizeForAnalyzer\": ${batchSizeForAnalyzer}"
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
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```


You are also able to get results of latest execution by `/latestExecution` endpoind,
with the following `get` request: 

```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/latestExecution?name=save&organizationName=${organizationName}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

The response format will look like:

```bash
{
  "id": 42, # execution id
  "status": "FINISHED", # execution status, i.e. running, finished and so on
  "type":"PRIVATE_TESTS", # testing type
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


If you would like to rerun some of your executions, you can use `/run/re-trigger` endpoint:

```bash
curl -X POST "${SAVE_CLOUD_URL}/api/v1/run/re-trigger?executionId=${executionId}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```
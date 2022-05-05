## SAVE Cloud Backend API

```bash
SAVE_CLOUD_URL=https://saveourtool.com:443

SAVE_CLOUD_AUTH_SOURCE=github

SAVE_CLOUD_AUTH='Basic Z2l0aHViQHVzZXJuYW1lOnRva2Vu'
```


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



```bash
curl -X GET "${SAVE_CLOUD_URL}/api/v1/organization/Huawei" \
-H "X-Authorization-Source: ${SAVE_CLOUD_AUTH_SOURCE}" \
-H "Authorization: ${SAVE_CLOUD_AUTH}"
```

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
-F 'executionRequest={
    "project": {
        "name": "save",
        "url": "https://github.com/analysis-dev/save-cli",
        "description": null,
        "status": "CREATED",
        "public": true,
        "userId": 1,
        "email": null,
        "numberOfContainers": 3,
        "organization": {
            "name": "Huawei",
            "ownerId": 1,
            "dateCreated": "2021-01-01T00:00:00",
            "avatar": null,
            "description": null,
            "id": 1
        },
        "contestRating": 0,
        "id":5
    },
    "gitDto": {
        "url": "https://github.com/analysis-dev/save-cli",
        "username": null,
        "password": null,
        "branch": "origin/feature/testing_for_cloud",
        "hash": null
    },
    "testRootPath": "examples/kotlin-diktat",
    "sdk": {
      "name": "openjdk",
      "version" : "11"
    },
    "executionId" : null
};type=application/json' \
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

FixMe:
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
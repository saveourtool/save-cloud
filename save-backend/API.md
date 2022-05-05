## SAVE Cloud Backend API

TODO: extract backend api and password


curl -X GET https://saveourtool.com:443/api/v1/files/list -H "X-Authorization-Source: github" -H "Authorization: Basic pass"


```bash
curl -X GET http://localhost:5800/api/v1/files/list \
-H "X-Authorization-Source: basic" \
-H "Authorization: Basic YWRtaW46IA=="
```

```bash
curl -X POST "http://localhost:5800/api/v1/files/upload" \
-H "X-Authorization-Source: basic" \
-H "Authorization: Basic YWRtaW46IA==" \
-F "file=@main.c"
```

{"name":"main.c","uploadedMillis":1651662834923,"sizeBytes":172,"isExecutable":false}



```bash
curl -X GET 'http://localhost:5800/api/v1/organization/Huawei' \
-H 'X-Authorization-Source: basic' \
-H 'Authorization: Basic YWRtaW46IA=='
```

FixMe:

```bash
curl -X GET 'http://localhost:5800/api/v1/projects/get/organization-id?name=save&organizationId=1' \
-H 'X-Authorization-Source: basic' \
-H 'Authorization: Basic YWRtaW46IA=='
```

```bash
curl -X POST 'http://localhost:5800/api/v1/submitExecutionRequest' \
-H 'X-Authorization-Source: basic' \
-H 'Authorization: Basic YWRtaW46IA==' \
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
curl -X GET 'http://localhost:5800/api/v1/latestExecution?name=save&organizationId=1' \
-H 'X-Authorization-Source: basic' \
-H 'Authorization: Basic YWRtaW46IA=='
```

FixMe:

```bash
curl -X GET 'http://localhost:5800/api/v1/executionDto?executionId=4' \
-H 'X-Authorization-Source: basic' \
-H 'Authorization: Basic YWRtaW46IA=='
```
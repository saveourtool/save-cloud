# Helm chart for save-cloud

## Prerequisites
* `kubectl create secret eneric db-secrets --from_literal=db_url=<...> --from_literal=db_username=<...> --from_literal=db_password=<...>`

## Build and deploy
```bash
$ helm package ./save-cloud
Successfully packaged chart and saved it to: .../save-cloud-0.1.0.tgz
$ helm.exe install save-cloud-0.1.0.tgz --name save-cloud --namespace save-cloud
```

# Helm chart for save-cloud

## Prerequisites
* `kubectl create secret generic db-secrets --from_literal=db_url=<...> --from_literal=db_username=<...> --from_literal=db_password=<...>`
* `kubectl create secret generic oauth-credentials ...` this secret should contain properties recognizable by spring security OAuth

## Build and deploy
```bash
$ helm package ./save-cloud
Successfully packaged chart and saved it to: .../save-cloud-0.1.0.tgz
$ helm install save-cloud save-cloud-0.1.0.tgz --namespace save-cloud
```

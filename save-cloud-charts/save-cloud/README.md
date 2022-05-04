# Helm chart for save-cloud
This chart will install components of save-cloud application: gateway, backend, orchestrator and preprocessor.
It will also create a Service for an external MySQL database.

api-gateway acts as an entrypoint and svc/gateway is actually a LoadBalancer.

## Prerequisites
* `kubectl create secret generic db-secrets --from_literal=db_url=<...> --from_literal=db_username=<...> --from_literal=db_password=<...>`
  For example, for minikube and dev profile run `kubectl --context=minikube --namespace=save-cloud create secret generic db-secrets --from_literal=db_url=jdbc:mysql://mysql-service:3306/save_cloud --from_literal=db_username=root --from_literal=db_password=123`
* `kubectl create secret generic oauth-credentials ...` this secret should contain properties recognizable by spring security OAuth

## Build and deploy
```bash
$ helm package ./save-cloud
Successfully packaged chart and saved it to: .../save-cloud-0.1.0.tgz
$ helm install save-cloud save-cloud-0.1.0.tgz --namespace save-cloud
```

## Local deployment
* Install minikube: https://minikube.sigs.k8s.io/docs/start/
* Install Helm chart using `values-minikube.yaml`: 
  ```bash
  $ helm install save-cloud save-cloud-0.1.0.tgz --namespace save-cloud --values values-minikube.yaml
  ```

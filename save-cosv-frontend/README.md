# COSV Frontend

### Building
* For IR usage see https://github.com/JetBrains/kotlin-wrappers#experimental-ir-backend

To run frontend locally, use `./gradlew :save-cosv-frontend:browserDevelopmentRun --continuous` or `./gradlew :save-cosv-frontend:browserProductionRun --continuous`.

To pack distribution, use `./gradlew :save-cosv-frontend:browserDevelopmentWebpack` and `./gradlew :save-cosv-frontend:browserProductionWebpack`.

save-backend consumes frontend distribution as a dependency. Frontend distribution is copied and included in spring boot resources.

### `nginx` [configuration](../save-frontend-common/README.md)

### `webpack-dev-server` [configuration for no `api-gateway` run](../save-frontend-common/README.md)

### [Using OAuth with a local deployment (`api-gateway` on)](../save-frontend-common/README.md)

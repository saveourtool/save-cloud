# SAVE Cloud Frontend

### Building
* For IR usage see https://github.com/JetBrains/kotlin-wrappers#experimental-ir-backend

To run frontend locally, use `./gradlew :save-frontend:browserDevelopmentRun --continuous` or `./gradlew :save-frontend:browserProductionRun --continuous`.

To pack distribution, use `./gradlew :save-frontend:browserDevelopmentWebpack` and `./gradlew :save-frontend:browserProductionWebpack`.

save-backend consumes frontend distribution as a dependency. Frontend distribution is copied and included in spring boot resources.
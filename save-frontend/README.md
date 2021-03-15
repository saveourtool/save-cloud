# SAVE Cloud Frontend

### Building
* For IR usage see https://github.com/JetBrains/kotlin-wrappers#experimental-ir-backend

To run frontend locally, use `./gradlew :save-fronted:browserDevelopmentRun` or `./gradlew :save-fronted:browserProductionRun`.

To pack distribution, use `./gradlew :save-fronted:browserDevelopmentWebpack` and `./gradlew :save-fronted:browserProductionWebpack`.

save-backend consumes frontend distribution as a dependency. Frontend distribution is copied and included in spring boot resources.
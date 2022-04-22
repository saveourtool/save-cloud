# Contributing
1. Fork this repository to your own account
2. Make your changes and verify that tests pass
3. Commit your work and push to a new branch on your fork
4. Submit a pull request
5. Participate in the code review process by responding to feedback


### Mac M1 contributors
In order to launch the project locally, you need to do these preparations:
1. In file `save-cloud/build.gradle.kts` change languageVersion of `org.liquibase.gradle.LiquibaseTask` from 11 to 17
so there would be something like this:
```
tasks.withType<org.liquibase.gradle.LiquibaseTask>().configureEach {
    this.javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}
```
2. If you use Spring Idea Plugin (built into IDEA Ultimate) on Mac, you should set `Active Profiles` to be `dev, mac, secure` for `SaveApplication`
and `mac` for 

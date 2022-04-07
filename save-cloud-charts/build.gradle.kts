plugins {
    // fixme: this is needed only to configure diktat, because we configure it from `allprojects`
    java
}

tasks.register<Exec>("setupChartRepositories") {
    commandLine("helm", "repo")
    args("add", "grafana", "https://grafana.github.io/helm-charts")
    inputs.file("save-cloud/requirements.yaml")
}

val chartArchive = "$buildDir/save-cloud-$version.tgz"
tasks.register<Exec>("packageChart") {
    inputs.dir("save-cloud")
    commandLine("helm", "package")
    args("--dependency-update", "--destination", buildDir, "save-cloud")
    outputs.file(chartArchive)
}

tasks.register<Exec>("installChart") {
    dependsOn("packageChart")
    inputs.files(chartArchive)
    commandLine("helm", "install", chartArchive)
    args("--name", "save-cloud")
    args("--namespace", "save-cloud")
    args("--set", "dockerTag=$version")
}

tasks.register<Exec>("upgradeChart") {
    dependsOn("packageChart")
    inputs.files(chartArchive)
    commandLine("helm", "upgrade", "save-cloud")
    args(chartArchive)
}

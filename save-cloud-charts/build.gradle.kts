/**
 * These tasks can either be used as a reference to run Helm commands
 * or can be enhanced as needed to actually subsitute them.
 * Either way, kubectl with configured connection to the cluster is required.
 */

plugins {
    // fixme: this is needed only to configure diktat, because we configure it from `allprojects`
    java
}

tasks.register<Exec>("setupChartRepositories") {
    commandLine("helm", "repo")
    args("add", "grafana", "https://grafana.github.io/helm-charts")
    inputs.file("save-cloud/requirements.yaml")
}

val chartVersion = "0.1.0"  // same as in `Chart.yaml`
val chartArchive = "$buildDir/save-cloud-$chartVersion.tgz"
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
    args("--namespace", "save-cloud")
    args("--set", "dockerTag=$version")
}

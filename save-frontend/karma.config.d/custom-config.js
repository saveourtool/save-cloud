// https://stackoverflow.com/questions/62826599/what-code-coverage-tool-to-use-for-the-javascript-code-in-a-kotlin-multiplatform
;(function(config) {
    const path = require("path")
    config.reporters.push("coverage-istanbul")
    config.plugins.push("karma-coverage-istanbul-reporter")
    config.webpack.module.rules.push(
        {
            test: /\.js$/,
            use: {loader: 'istanbul-instrumenter-loader'},
            // fixme: need to exclude Kotlin dependencies
            include: [path.resolve(__dirname, '../save-cloud-save-frontend/kotlin/')]
        }
    )
    config.coverageIstanbulReporter = {
        reports: ["html", "lcov"]
    }
}(config));

config.set({
    client: {
        mocha: {
            // completely disable timeout
            timeout: 0
        }
    }
})
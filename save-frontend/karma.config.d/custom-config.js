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
    },
    proxies: {
        // serving mockServiceWorker.js.js from location relative to base url
        // the file should be included into Karma's `files` to be served by server at all
        '/mockServiceWorker.js': '/base/mockServiceWorker.js',
    },
})

// http://karma-runner.github.io/6.3/config/files.html
// 'All of the relative patterns will get resolved using the basePath first.', where basePath is set by KGP to `node_modules`
config.files.push('./mockServiceWorker.js')

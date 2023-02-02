/**
 * Utils to set up the environment for demo
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.save.demo.agent.DemoConfiguration

/**
 * Download all the required files from save-demo
 *
 * @param demoUrl url to save-demo
 * @param demoConfiguration all the information required for tool download
 */
fun setupEnvironment(demoUrl: String, demoConfiguration: DemoConfiguration) {
    downloadDemoFiles(demoUrl, demoConfiguration)
    executeSetupSh()
}

// todo: implement setup.sh run
private fun executeSetupSh() = Unit

// todo: implement file download from save-demo storage
private fun downloadDemoFiles(demoUrl: String, demoConfiguration: DemoConfiguration) = Unit

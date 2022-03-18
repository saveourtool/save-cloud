package org.cqfn.save.api

import kotlinx.coroutines.runBlocking

fun main() {
    val automaticTestInitializator = AutomaticTestInitializator()
    runBlocking {
        automaticTestInitializator.start()
    }
}

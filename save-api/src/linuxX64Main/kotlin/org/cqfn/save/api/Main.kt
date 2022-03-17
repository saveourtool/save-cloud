package org.cqfn.save.api

suspend fun main() {
    val automaticTestInitializator = AutomaticTestInitializator()
    automaticTestInitializator.start()
}
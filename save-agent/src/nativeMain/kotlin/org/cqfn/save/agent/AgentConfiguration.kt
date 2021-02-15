package org.cqfn.save.agent

data class AgentConfiguration(
    val backendUrl: String = "http://localhost:5000",
    val orchestratorUrl: String = "http://localhost:5100",
    val heartbeatIntervalMillis: Long = 15_000L,
    val requestTimeoutMillis: Long = 1_000L,
    val executionDataRequestRetryMillis: Long = 1_000L,
)

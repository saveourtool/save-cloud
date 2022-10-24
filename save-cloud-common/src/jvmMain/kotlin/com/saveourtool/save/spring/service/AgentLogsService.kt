package com.saveourtool.save.spring.service

interface AgentLogsService {
    fun getLogs(containerName: String): String
}
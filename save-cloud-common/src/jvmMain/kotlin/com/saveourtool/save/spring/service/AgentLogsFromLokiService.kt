package com.saveourtool.save.spring.service

import org.springframework.web.reactive.function.client.WebClient

class AgentLogsFromLokiService(
    lokiServiceUrl: String
) : AgentLogsService {

    private val webClient = WebClient.create(lokiServiceUrl)
        .mutate()
        .codecs {
//            it.defaultCodecs().jackson2JsonEncoder(
//                Jackson2JsonEncoder(objectMapper)
//            )
        }
        .build()

    override fun getLogs(containerName: String): String {
        webClient.get()
            .uri("/query-range?query={}", getQueryByContainerName(containerName))
        TODO("Not yet implemented")
    }

    private fun getQueryByContainerName(containerName: String) = "{container_name=~\"$containerName\"}"

}
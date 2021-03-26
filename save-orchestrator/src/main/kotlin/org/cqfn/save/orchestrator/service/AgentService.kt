package org.cqfn.save.orchestrator.service

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.test.TestDto
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class AgentService(baseUrl: String = "http://backend:5000" ) {
    /**
     * Used to send requests to backend
     */
    private val webClient = WebClient.create(baseUrl)

    /**
     * Sets new tests ids
     */
    fun setNewTestsIds(): Mono<out HeartbeatResponse> {
        return webClient
            .get()
            .uri("/getTestBatches")
            .retrieve()
            .bodyToMono(String::class.java)
            .map {
                val listTest = Json.decodeFromString<List<TestDto>>(it)
                NewJobResponse(listTest)
            }
    }

    fun checkSavedData(): Boolean {
        return true
    }

    fun resendTestsOnError() {

    }
}

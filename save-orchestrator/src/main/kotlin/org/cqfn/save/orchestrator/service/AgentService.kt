package org.cqfn.save.orchestrator.service

import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.test.TestDto

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Service for work with agents and backend
 *
 * @param baseUrl
 */
@Service
// Fixme: delete suppress when resendTestsOnError function has any code
@Suppress("EmptyFunctionBlock")
class AgentService(configProperties: ConfigProperties) {
    /**
     * Used to send requests to backend
     */
    private val webClient = WebClient.create(configProperties.backendUrl)

    /**
     * Sets new tests ids
     *
     * @return Mono<NewJobResponse>
     */
    fun setNewTestsIds(): Mono<out HeartbeatResponse> =
            webClient
                .get()
                .uri("/getTestBatches")
                .retrieve()
                .bodyToMono(String::class.java)
                .map {
                    val listTest: List<TestDto> = Json.decodeFromString(it)
                    NewJobResponse(listTest)
                }

    /**
     * @return nothing for now Fixme
     */
    @Suppress("FunctionOnlyReturningConstant")
    fun checkSavedData() = true

    /**
     * If an error occurs, should try to resend tests
     */
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")  // Fixme
    fun resendTestsOnError() {

    }
}

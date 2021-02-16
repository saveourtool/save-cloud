package org.cqfn.save.orchestrator.service

import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

@Service
class AgentService {
    /**
     * Used to send requests to backend
     */
    private val webClient = WebClient.create("http://localhost:5000")

    /**
     * Sets new tests ids
     */
    fun setNewTestsIds(): List<String> {
        // some cool logic to set needed test ids
        return emptyList() // Fixme
    }

    fun checkSavedData(): Boolean {
        return true
    }

    fun resendTestsOnError() {

    }
}

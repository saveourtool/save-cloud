package org.cqfn.save.orchestrator.service

import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AgentService {
    /**
     * Used to send requests to backend
     */
    private val restTemplate = RestTemplate()

    /**
     * Sets new tests ids
     */
    fun setNewTestsIds(): List<String> {
        // some cool logic to set needed test ids
        return emptyList() // Fixme
    }

}
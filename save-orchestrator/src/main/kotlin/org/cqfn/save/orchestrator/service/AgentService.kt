package org.cqfn.save.orchestrator.service

import org.cqfn.save.test.TestDto
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class AgentService {
    /**
     * Used to send requests to backend
     */
    private val webClient = WebClient.create("http://localhost:5000")

    /**
     * Sets new tests ids
     */
    fun setNewTestsIds(): List<TestDto> {
        val list: MutableList<TestDto> = emptyList<TestDto>().toMutableList()
        val q = webClient
                .get()
                .uri("/getTestBatches")
                .retrieve()
                .bodyToMono(List::class.java)

        q.subscribe {
            it.forEach { elem ->
                val q = elem as LinkedHashMap<*, *>
                println(q.values)
                list.add(TestDto(q.values.toList()[0] as String, q.values.toList()[1] as String, q.values.toList()[2] as Long, q.values.toList()[3] as String))
            }
        }
//        if (list.isEmpty()) {
//            // TODO: to we need to kill agent here?
//        }
        return list
    }

    fun checkSavedData(): Boolean {
        return true
    }

    fun resendTestsOnError() {

    }
}

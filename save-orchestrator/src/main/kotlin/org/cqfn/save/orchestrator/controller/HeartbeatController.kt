package org.cqfn.save.orchestrator.controller

import org.cqfn.save.orchestrator.model.AgentState.BUSY
import org.cqfn.save.orchestrator.model.AgentState.ERROR
import org.cqfn.save.orchestrator.model.AgentState.FINISHED
import org.cqfn.save.orchestrator.model.AgentState.IDLE
import org.cqfn.save.orchestrator.model.EmptyResponse
import org.cqfn.save.orchestrator.model.Heartbeat
import org.cqfn.save.orchestrator.model.HeartbeatResponse
import org.cqfn.save.orchestrator.model.NewJobResponse
import org.cqfn.save.orchestrator.model.TerminatingResponse
import org.cqfn.save.orchestrator.service.AgentService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class HeartbeatController(private val agentService: AgentService) {
    private val logger = LoggerFactory.getLogger(HeartbeatController::class.java)

    /**
     * This controller accepts heartbeat and depending on the state it returns the needed response
     *
     * 1. Response has IDLE state. Then orchestrator should send new jobs.
     * 2. Response has FINISHED state. Then orchestrator should send new jobs and validate that data has actually been saved successfully.
     * 3. Response has BUSY state. Then orchestrator sends an Empty response.
     * 4. Response has ERROR state. Then orchestrator sends Terminating response.
     */
    @PostMapping("/heartbeat")
    fun acceptHeartbeat(@RequestBody heartbeat: Heartbeat): Mono<HeartbeatResponse> {
        logger.info("Got heartbeat state: ${heartbeat.state.name} from ${heartbeat.agentId}")
        return when (heartbeat.state) {
            IDLE -> Mono.just(NewJobResponse(agentService.setNewTestsIds()))
            FINISHED -> {
                agentService.checkSavedData()
                Mono.just(NewJobResponse(agentService.setNewTestsIds()))
            }
            BUSY -> Mono.just(EmptyResponse)
            ERROR -> {
                // Maybe set tests in TerminatingResponse
                agentService.resendTestsOnError()
                Mono.just(TerminatingResponse)
            }
        }
    }
}

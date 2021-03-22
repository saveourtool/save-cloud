package org.cqfn.save.orchestrator.controller

import org.cqfn.save.agent.AgentState.*
import org.cqfn.save.agent.ContinueResponse
import org.cqfn.save.agent.Heartbeat
import org.cqfn.save.agent.HeartbeatResponse
import org.cqfn.save.agent.NewJobResponse
import org.cqfn.save.agent.WaitResponse
import org.cqfn.save.orchestrator.service.AgentService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
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
            IDLE -> {
                val some = agentService.setNewTestsIds()
                Mono.just(NewJobResponse(agentService.setNewTestsIds()))
            }
            FINISHED -> {
                agentService.checkSavedData()
                Mono.just(WaitResponse)
            }
            BUSY -> Mono.just(ContinueResponse)
            BACKEND_FAILURE -> Mono.just(WaitResponse)
            BACKEND_UNREACHABLE -> Mono.just(WaitResponse)
            CLI_FAILED -> Mono.just(WaitResponse)
        }
    }
}

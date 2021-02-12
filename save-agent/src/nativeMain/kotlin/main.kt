import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cqfn.save.agent.SaveAgent

fun main() {
    val saveAgent = SaveAgent()
    GlobalScope.launch {
        saveAgent.runSave(emptyList())
    }
    GlobalScope.launch {
        while (true) {
            delay(15_000)
            saveAgent.sendHeartbeat()
        }
    }
}

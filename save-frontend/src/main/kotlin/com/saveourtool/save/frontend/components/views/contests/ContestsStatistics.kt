import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.utils.*
import csstype.AlignItems
import csstype.ClassName
import csstype.JustifyContent
import csstype.rem
import kotlinx.js.jso
import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.strong
import react.useState

val statistics = statistics()

private fun statistics() = VFC {
    val (activeContests, setActiveContests) = useState<Set<ContestDto>>(emptySet())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/active",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setActiveContests(contests.toSet())
    }

    val (finishedContests, setFinishedContests) = useState<Set<ContestDto>>(emptySet())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/active",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setFinishedContests(contests.toSet())
    }

    div {
        className = ClassName("col-lg-4")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 7.rem
            }
            div {
                className = ClassName("col-lg-6 mt-2")
                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
                    }
                    strong {
                        className = ClassName("d-inline-block mb-2 card-text")
                        +"Active contests:"
                    }

                }
                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
                    }
                    h2 {
                        className = ClassName("text-dark")
                        +activeContests.size.toString()
                    }
                }
            }
            div {
                className = ClassName("col-lg-6 mt-2")
                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
                    }
                    strong {
                        className = ClassName("d-inline-block mb-2 card-text ")
                        +"Finished contests:"
                    }
                }
                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        alignItems = AlignItems.center
                    }
                    h2 {
                        className = ClassName("text-dark")
                        +finishedContests.size.toString()
                    }
                }
            }
        }
    }
}

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.fontawesome.faUser
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import csstype.*
import kotlinx.js.jso
import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p

/**
 * [Props] of the project rating functional interface
 */
external interface ProjectRatingProps : PropsWithChildren {
    /**
     * logged-in user or null
     */
    var userInfo: UserInfo?
}

val myProjectsRatings = myProjectsRatings()

private fun myProjectsRatings() = FC<ProjectRatingProps> { props ->
    val (myProjects, setMyProjects) = useState<Set<Project>>(emptySet())
    useRequest {
        val projects: Response = get(
            url = "$apiUrl/projects/current-user?userId=${props.userInfo?.id}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        console.log(props.userInfo?.id)
        val decoded = projects.decodeFromJsonString<List<Project>>()
        setMyProjects(decoded.toSet())
    }


    div {
        className = ClassName("col-lg-2")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }
            div {
                className = ClassName("col")
                style = jso {
                    minHeight = 7.rem
                }
                title(" Your stats ", icon = faUser)
                myProjects.forEach {
                    div {
                        className = ClassName("row")
                        style = jso {
                            alignItems = AlignItems.center
                            justifyContent = JustifyContent.center
                        }
                        p {
                            +it.name
                        }
                    }
                    div {
                        className = ClassName("row")
                        style = jso {
                            alignItems = AlignItems.center
                            justifyContent = JustifyContent.center
                        }
                        h4 {
                            +"432796"
                        }
                    }
                }
            }
        }
    }
}

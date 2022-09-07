package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.fontawesome.faUser
import com.saveourtool.save.frontend.utils.*
import csstype.ClassName
import csstype.rem
import kotlinx.js.jso
import org.w3c.fetch.Response
import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.useState

val myProjectsRatings = myProjectsRatings()

private fun myProjectsRatings() = VFC {

    val (myProjects, setMyProjects) = useState<Set<Project>>(emptySet())
    useRequest {
        val projects: Response = get(
            url = "$apiUrl/projects/current-user",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        console.log(projects.toString())
        val decoded = projects.decodeFromJsonString<List<Project>>()
        setMyProjects(decoded.toSet())
    }


    div {
        className = ClassName("col-lg-2")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            div {
                className = ClassName("col")

                style = jso {
                    minHeight = 7.rem
                }
                title(" Your stats ", icon = faUser)
                myProjects.forEach {
                    console.log(it)
                }
            }
        }
    }
}

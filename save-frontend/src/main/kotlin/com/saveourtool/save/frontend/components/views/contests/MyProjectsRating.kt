/**
 * Rating of projects linked to current user (where this user is added)
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.frontend.externals.fontawesome.faUser
import com.saveourtool.save.frontend.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p
import web.cssom.*

val myProjectsRating = myProjectsRatings()

/**
 * @return functional component
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun myProjectsRatings() = FC<ContestListViewProps> { props ->
    val (myProjects, setMyProjects) = useState(emptySet<ProjectDto>())
    val getMyProjects = useDeferredRequest {
        setMyProjects(
            get(
                url = "$apiUrl/projects/get-for-current-user",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            ).decodeFromJsonString<Set<ProjectDto>>()
        )
    }

    props.currentUserInfo?.let {
        getMyProjects()
    }

    div {
        className = ClassName("col-lg-2")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 40.rem
            }
            div {
                className = ClassName("col")
                style = jso {
                    minHeight = 7.rem
                }
                title(" Your stats ", icon = faUser)
                if (myProjects.isEmpty()) {
                    div {
                        className = ClassName("row justify-content-center")
                        p {
                            +"You don't have any projects"
                        }
                    }
                }
                myProjects.forEach {
                    div {
                        className = ClassName("row justify-content-center align-items-center")
                        h4 {
                            +it.contestRating.toFixedStr(2)
                        }
                    }
                    div {
                        className = ClassName("row justify-content-center align-items-center")
                        p {
                            +it.name
                        }
                    }
                }
            }
        }
    }
}

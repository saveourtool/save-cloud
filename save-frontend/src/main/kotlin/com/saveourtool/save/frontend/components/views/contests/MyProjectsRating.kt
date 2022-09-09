/**
 * Rating of projects linked to current user (where this user is added)
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.fontawesome.faUser
import com.saveourtool.save.frontend.utils.*

import csstype.*
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p

import kotlinx.js.jso

val myProjectsRating = myProjectsRatings()

/**
 * @return functional component
 */
fun myProjectsRatings() = FC<ContestListViewProps> { props ->
    val (myProjects, setMyProjects) = useState(emptySet<Project>())
    val getMyProjects = useDeferredRequest {
        if (props.currentUserInfo != null && props.currentUserInfo?.id != null) {
            setMyProjects(
                get(
                    url = "$apiUrl/projects/get-by-user?userId=${props.currentUserInfo!!.id}",
                    headers = jsonHeaders,
                    loadingHandler = ::loadingHandler,
                ).decodeFromJsonString<Set<Project>>()
            )
        }
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
                        className = ClassName("row")
                        style = jso {
                            alignItems = AlignItems.center
                            justifyContent = JustifyContent.center
                        }
                        p {
                            +"You don't have any projects"
                        }
                    }
                }
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

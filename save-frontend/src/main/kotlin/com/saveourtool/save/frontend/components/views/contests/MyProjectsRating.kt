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
    val (myProjects, setMyProjects) = useState<Set<Project>>(emptySet())
    val getMyProjects = useDeferredRequest {
        setMyProjects(
            get(
                url = "$apiUrl/projects/current-user?userId=${props.currentUserInfo?.id}",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            ).decodeFromJsonString<List<Project>>()
                .toSet()
        )
    }

    props.currentUserInfo?.let {
        getMyProjects()
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

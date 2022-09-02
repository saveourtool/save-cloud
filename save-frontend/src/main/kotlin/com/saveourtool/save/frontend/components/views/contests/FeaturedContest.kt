/**
 * A card with a FEATURED contest (left top card)
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.ContestNameProps
import com.saveourtool.save.frontend.components.basic.showContestEnrollerModal
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import csstype.rem
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.js.jso

val featuredContest = featuredContest()

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.image() {
    img {
        className = ClassName("card-img-right flex-auto d-none d-md-block")
        src = "img/undraw_certificate_re_yadi.svg"
        style = jso {
            width = 12.rem
        }
    }
}

/**
 * Rendering of featured contest card
 */
@Suppress("MAGIC_NUMBER", "TOO_LONG_FUNCTION", "LongMethod")
private fun featuredContest() = VFC {
    val (featuredContests, setFeaturedContests) = useState<List<ContestDto>>(emptyList())
    useRequest {
        val contests: List<ContestDto> = get(
            url = "$apiUrl/contests/featured/list-active",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setFeaturedContests(contests)
    }

    val (selectedContest, setSelectedContest) = useState(ContestDto.empty)
    val (isConfirmationWindowOpen, setIsConfirmationWindowOpen) = useState(false)
    val (isContestEnrollerModalOpen, setIsContestEnrollerModalOpen) = useState(false)
    val (enrollerResponseMessage, setEnrollerResponseMessage) = useState("")
    showContestEnrollerModal(
        isContestEnrollerModalOpen,
        ContestNameProps(selectedContest.name),
        { setIsContestEnrollerModalOpen(false) }
    ) {
        setIsConfirmationWindowOpen(true)
        setIsContestEnrollerModalOpen(false)
        setEnrollerResponseMessage(it)
    }

    displayModal(
        isConfirmationWindowOpen,
        "Contest enroller",
        enrollerResponseMessage,
        mediumTransparentModalStyle,
        { setIsConfirmationWindowOpen(false) }
    ) {
        buttonBuilder("Close", "secondary") {
            setIsConfirmationWindowOpen(false)
        }
    }
    div {
        className = ClassName("col-lg-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                height = 14.rem
            }
            image()
            val contestToShow = featuredContests.firstOrNull()
            contestToShow?.let {
                div {
                    className = ClassName("card-body d-flex flex-column align-items-start")
                    strong {
                        className = ClassName("d-inline-block mb-2 text-info")
                        +"Featured Contest"
                    }
                    h3 {
                        className = ClassName("mb-0")
                        a {
                            className = ClassName("text-dark")
                            href = "#/contests/${contestToShow.name}"
                            +contestToShow.name
                        }
                    }
                    p {
                        className = ClassName("card-text mb-auto")
                        +(contestToShow.description ?: "No description provided yet.")
                    }
                    div {
                        className = ClassName("row")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-outline-primary mr-1")
                            onClick = {
                                setSelectedContest(contestToShow)
                                setIsContestEnrollerModalOpen(true)
                            }
                            +"Enroll"
                        }

                        a {
                            className = ClassName("btn btn-sm btn-outline-success")
                            href = "#/contests/${contestToShow.name}"
                            +"Description "
                            fontAwesomeIcon(icon = faArrowRight)
                        }
                    }
                }
            } ?: run {
                div {
                    className = ClassName("card-body d-flex flex-column align-items-start")
                    strong {
                        className = ClassName("d-inline-block mb-2 text-info")
                        +"Featured Contest"
                    }
                    h3 {
                        className = ClassName("mb-0")
                        +"Stay turned..."
                    }
                    p {
                        className = ClassName("card-text mb-auto")
                        +("Right now there is no contest that we would recommend you to participate in, but it is going to change soon. " +
                                "Stay turned and soon we will find good contests for you and your tools!")
                    }
                }
            }
        }
    }
}

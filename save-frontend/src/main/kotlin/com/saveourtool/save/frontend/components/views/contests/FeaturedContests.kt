/**
 * Preparation for a participation card
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
import csstype.*
import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div

import kotlinx.js.jso
import react.ChildrenBuilder
import react.StateSetter
import react.dom.aria.AriaRole
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.useState

val yourContests = yourContests()

private fun yourContests() = VFC {
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
        className = ClassName("col-lg-8")
        if (featuredContests.isEmpty()) {
            div {
                className = ClassName("card flex-md-row box-shadow")
                style = jso {
                    height = 15.rem
                }
                stayTuned()
            }
        } else {
            div {
                className = ClassName("carousel slide card flex-md-row box-shadow")
                style = jso {
                    height = 15.rem
                }
                id = "contestCarousel"
                asDynamic()["data-ride"] = "carousel"

                div {
                    className = ClassName("carousel-inner my-auto")
                    featuredContests.forEachIndexed { i, contestDto ->
                        if (i == 0) {
                            slide("active", contestDto, setSelectedContest, setIsContestEnrollerModalOpen)
                        } else {
                            slide("", contestDto, setSelectedContest, setIsContestEnrollerModalOpen)
                        }
                    }
                }
            }

            carouselArrows()
        }
    }
}


private fun ChildrenBuilder.carouselArrows() {
    a {
        style = jso {
            filter = invert(100);
        }
        className = ClassName("carousel-control-prev ")
        href = "#contestCarousel"
        role = AriaRole.button
        asDynamic()["data-slide"] = "prev"
        span {
            className = ClassName("carousel-control-prev-icon")
        }
    }
    a {
        style = jso {
            filter = invert(100);
        }
        className = ClassName("carousel-control-next")
        href = "#contestCarousel"
        role = AriaRole.button
        asDynamic()["data-slide"] = "next"
        span {
            className = ClassName("carousel-control-next-icon")
        }
    }
}

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.stayTunedImage() {
    img {
        className = ClassName("card-img-right flex-auto d-none d-md-block")
        src = "img/undraw_notify_re_65on.svg"
        style = jso {
            width = 24.rem
        }
    }
}

private fun ChildrenBuilder.stayTuned() {
    div {
        className = ClassName("card-body d-flex flex-column align-items-start")
        ReactHTML.strong {
            className = ClassName("d-inline-block mb-2 text-primary")
            +"Featured Contest"
        }
        ReactHTML.h3 {
            className = ClassName("mb-0 text-dark")
            +"Stay turned..."
        }
        p {
            className = ClassName("card-text mb-auto")
            +("Right now there is no contest that we would recommend you to participate in, but it is going to change soon. " +
                    "Stay turned and soon we will find good contests for you and your tools!")
        }
    }
    stayTunedImage()
}

private fun ChildrenBuilder.slide(
    classes: String,
    contestToShow: ContestDto,
    setSelectedContest: StateSetter<ContestDto>,
    setIsContestEnrollerModalOpen: StateSetter<Boolean>
) {
    div {
        className = ClassName("carousel-item $classes")
        div {
            className = ClassName("row")

            div {
                className = ClassName("col-3 ml-auto")

                img {
                    style = jso {
                        width = 12.rem
                    }
                    // FixMe: we need to have information about the programming language in contest
                    src = "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/devicon/devicon-original.svg"
                }
            }

            div {
                className = ClassName("col-3 mr-auto")
                contestDescription(contestToShow, setSelectedContest, setIsContestEnrollerModalOpen)
            }
        }
    }
}

private fun ChildrenBuilder.contestDescription(
    contestToShow: ContestDto,
    setSelectedContest: StateSetter<ContestDto>,
    setIsContestEnrollerModalOpen: StateSetter<Boolean>
) {
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
        p {
            className = ClassName("card-text mb-auto")
            +("Created by: ${contestToShow.organizationName}")
        }
        div {
            className = ClassName("row ml-auto mt-1")
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
}
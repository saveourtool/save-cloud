package org.cqfn.save.frontend.components.views

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.onChange
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import react.*
import react.dom.*


external interface NewProjectProps : RProps {
    /**
     * Currently logged in user or null
     */
    var userName: String?

    /**
     * Current path received from router
     */
    var pathname: String
}

/**
 * A state of top bar component
 */
external interface NewProjectState : RState {
    /**
     * Whether logout window is opened
     */
    var projectName: String
    var projectOwner: String
    var projectDescription: String
    var gitUsername: String
    var gitPassword: String
    var gitUrl: String
}


class NewProject: RComponent<RProps, NewProjectState>() {

    init {
        state.projectName = ""
        state.projectOwner = ""
        state.projectDescription = ""
        state.gitUsername = ""
        state.gitPassword = ""
        state.gitUrl = ""
    }

    override fun RBuilder.render() {
        div("bg-gradient-primary") {
            div("container card o-hidden border-0 shadow-lg my-5 card-body p-0 col-lg-7 p-5") {
                div("text-center") {
                    h1("h4 text-gray-900 mb-4") {
                        +"Add new project"
                    }
                }
                label("hehe") {
                    + "General Information"
                }
                form {
                    div ("form-group row") {
                        div("col-sm-6 mb-3 mb-sm-0") {
                            input(classes = "form-control form-control-user") {
                                attrs.placeholder = "Project name"
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLInputElement
                                    setState { projectName = target.value }
                                }
                            }
                        }
                        div("col-sm-6") {
                            input(classes = "form-control form-control-user") {
                                attrs.placeholder = "Project owner"
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLInputElement
                                    setState { projectOwner = target.value }
                                }
                            }
                        }
                    }
                    div("form-group") {
                        textarea(classes = "form-control form-control-user qwe") {
                            attrs.placeholder = "Project description"
                            attrs.onChangeFunction = {
                                val target = it.target as HTMLTextAreaElement
                                setState { projectDescription = target.value }
                            }
                        }
                    }
                }
                label("hehe") {
                    + "GitHub Information"
                }
                form("user") {
                    div ("form-group row") {
                        div("col-sm-6 mb-3 mb-sm-0") {
                            input(classes = "form-control form-control-user") {
                                attrs.placeholder = "Username"
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLInputElement
                                    setState { gitUsername = target.value }
                                }
                            }
                        }
                        div("col-sm-6") {
                            input(type = InputType.password, classes = "form-control form-control-user") {
                                attrs.placeholder = "Password"
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLInputElement
                                    setState { gitPassword = target.value }
                                }
                            }
                        }
                    }
                    div("form-group") {
                        input(classes = "form-control form-control-user") {
                            attrs.placeholder = "Git url"
                            attrs.onChangeFunction = {
                                val target = it.target as HTMLInputElement
                                setState { gitUrl = target.value }
                            }
                        }
                    }

                }
                a(classes = "btn btn-primary btn-user btn-block") {
                    + "Add new project"
                }
            }
        }
    }
}
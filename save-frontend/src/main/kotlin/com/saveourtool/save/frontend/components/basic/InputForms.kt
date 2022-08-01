@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import react.ChildrenBuilder
import react.dom.*
import react.dom.aria.ariaDescribedBy
import react.dom.events.ChangeEvent
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.span

/**
 * @property str
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class InputTypes(val str: String) {
    // ==== new project view
    DESCRIPTION("project description"),
    GIT_BRANCH("git branch"),
    GIT_TOKEN("git token"),
    GIT_URL("git url"),
    GIT_USER("git username"),
    PROJECT_EMAIL("project email"),

    // ==== signIn view
    LOGIN("login"),
    PASSWORD("password"),
    PROJECT_NAME("project name"),
    PROJECT_URL("project Url"),

    // ==== create organization view
    ORGANIZATION_NAME("organization name"),

    // ==== user setting view
    USER_EMAIL("user email"),
    COMPANY("company"),
    LOCATION("location"),
    GIT_HUB("git hub"),
    LINKEDIN("linkedin"),
    TWITTER("twitter"),

    // ==== contest creation component
    CONTEST_NAME("contest name"),
    CONTEST_START_TIME("contest starting time"),
    CONTEST_END_TIME("contest ending time"),
    CONTEST_DESCRIPTION("contest description"),
    CONTEST_SUPER_ORGANIZATION_NAME("contest's super organization's name"),
    ;
}

/**
 * @param form
 * @param validInput
 * @param classes
 * @param text
 * @param isProjectOrOrganizationName
 * @param onChangeFun
 * @return div with an input form
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
internal fun ChildrenBuilder.inputTextFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
    isProjectOrOrganizationName: Boolean = false,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) =
        div {
            className = ClassName("$classes mt-1")
            label {
                className = ClassName("form-label")
                htmlFor = form.name
                +text
            }

            div {
                className = ClassName("input-group has-validation")
                span {
                    className = ClassName("input-group-text")
                    id = "${form.name}Span"
                    +"*"
                }

                val inputType = if (form == InputTypes.PASSWORD) InputType.password else InputType.text
                input {
                    type = inputType
                    onChange = onChangeFun
                    id = form.name
                    required = true
                    if (validInput) {
                        className = ClassName("form-control")
                    } else {
                        className = ClassName("form-control is-invalid")
                    }
                }

                if (!validInput) {
                    if (isProjectOrOrganizationName) {
                        div {
                            className = ClassName("invalid-feedback d-block")
                            +"Please input a valid ${form.str}. The name can be no longer than 64 characters and can't contain any spaces."
                        }
                    } else {
                        div {
                            className = ClassName("invalid-feedback d-block")
                            +"Please input a valid ${form.str}"
                        }
                    }
                }
            }
        }

/**
 * @param form
 * @param classes
 * @param text
 * @param validInput
 * @param onChangeFun
 * @return div with an input form
 */
internal fun ChildrenBuilder.inputTextFormOptional(
    form: InputTypes,
    classes: String,
    text: String,
    validInput: Boolean = true,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) = div {
    className = ClassName("$classes pl-2 pr-2")
    label {
        className = ClassName("form-label")
        htmlFor = form.name
        +text
    }
    input {
        type = InputType.text
        onChange = onChangeFun
        ariaDescribedBy = "${form.name}Span"
        id = form.name
        required = false
        if (validInput) {
            className = ClassName("form-control")
        } else {
            className = ClassName("form-control is-invalid")
        }
    }
}

/**
 * @param form
 * @param classes
 * @param name
 * @param inputText
 * @return div with a disabled input form
 */
internal fun ChildrenBuilder.inputTextDisabled(
    form: InputTypes,
    classes: String,
    name: String,
    inputText: String,
) = div {
    className = ClassName("$classes pl-2 pr-2")
    label {
        className = ClassName("form-label")
        htmlFor = form.name
        +name
    }
    input {
        type = InputType.text
        ariaDescribedBy = "${form.name}Span"
        id = form.name
        required = false
        className = ClassName("form-control")
        disabled = true
        value = inputText
    }
}

/**
 * @param form
 * @param classes
 * @param text
 * @param onChangeFun
 * @return a [div] with optional input form with datepicker
 */
internal fun ChildrenBuilder.inputDateFormOptional(
    form: InputTypes,
    classes: String,
    text: String,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) = div {
    className = ClassName("$classes pl-2 pr-2")
    label {
        className = ClassName("form-label")
        htmlFor = form.name
        +text
    }
    input {
        type = InputType.date
        onChange = onChangeFun
        ariaDescribedBy = "${form.name}Span"
        id = form.name
        required = false
        className = ClassName("form-control")
    }
}

/**
 * @param form
 * @param validInput
 * @param classes
 * @param text
 * @param onChangeFun
 * @return a [div] with required input form with datepicker
 */
internal fun ChildrenBuilder.inputDateFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) = div {
    className = ClassName("$classes mt-1")
    label {
        className = ClassName("form-label")
        htmlFor = form.name
        +text
    }
    div {
        className = ClassName("input-group has-validation")
        span {
            className = ClassName("input-group-text")
            id = "${form.name}Span"
            +"*"
        }
        input {
            type = InputType.date
            onChange = onChangeFun
            id = form.name
            required = true
            if (validInput) {
                className = ClassName("form-control")
            } else {
                className = ClassName("form-control is-invalid")
            }
        }
        if (!validInput) {
            div {
                className = ClassName("invalid-feedback d-block")
                +"Please input a valid ${form.str}"
            }
        }
    }
}

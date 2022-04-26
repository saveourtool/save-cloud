package org.cqfn.save.frontend.components.views.usersettingsview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.cqfn.save.frontend.components.basic.cardComponent

import react.FC
import react.dom.*
import react.fc
import react.setState

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.domain.Role
import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.http.getUser
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.useRequest
import org.cqfn.save.info.UserInfo
import org.w3c.fetch.Headers

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsTokenMenuView : UserSettingsView() {
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            val headers = Headers().also {
                it.set("Accept", "application/json")
                it.set("Content-Type", "application/json")
            }
            val newUserInfo = UserInfo(
                name = "",
                password ="",
                source = "",
                projects = mapOf("" to Role.ADMIN),
                email = fieldsMap[InputTypes.USER_EMAIL]?.trim(),
                company = fieldsMap[InputTypes.COMPANY]?.trim(),
                location = fieldsMap[InputTypes.LOCATION]?.trim(),
                linkedin = fieldsMap[InputTypes.LINKEDIN]?.trim(),
                gitHub = fieldsMap[InputTypes.GIT_HUB]?.trim(),
                twitter = fieldsMap[InputTypes.TWITTER]?.trim(),
                avatar = "",
            )
//            scope.launch {
//                println("SENT REQUEST TO UPDATE USER INFO11111")
//            }

            div("d-sm-flex align-items-center justify-content-center mb-4") {
                h1("h3 mb-0 mt-2 text-gray-800") {
                    +"Personal access tokens"
                }
            }

            div("row justify-content-center") {
                button(type = ButtonType.button, classes = "btn btn-primary mb-2 mr-2") {
                    +"Generate new token"
                    attrs.onClickFunction = {
                        generateToken()
//                        useRequest {
//                            println("SENT REQUEST TO UPDATE USER INFO2222")
//                            post("$apiUrl/users/save", headers, Json.encodeToString(newUserInfo)).text().await()
//                            //scope.launch {
//                            //    println("SENT REQUEST TO UPDATE USER INFO2222")
//                            //    post("$apiUrl/users/save", headers, Json.encodeToString(newUserInfo)).text().await()
//                            //}
//                        }

                        println("End updateUser()")
                    }
                }
            }

            state.token?.let {
                div("col-md-12 mt-3") {
                    input {
                        attrs.value = state.token ?: ""
                        attrs["required"] = true
                        attrs["class"] = "form-control"
                    }
                    div("invalid-feedback d-block") {
                        +"This is your unique token. It will be shown to you only once. Please remember it."
                    }
                }
            }
        })
    }

    override fun componentDidMount() {
        super.componentDidMount()
        println("UserSettingsTokenMenuView: props.userName=${props.userName}")
        scope.launch {
            val user = props.userName?.let { getUser(it) }
            setState {
                userInfo = user
                println("After USERNAME ${userInfo?.name}")
            }
        }
    }

    @Suppress("MAGIC_NUMBER")
    private fun generateToken() {
        scope.launch {
            println("SENT REQUEST TO UPDATE USER INFO111")
        }

        var token = "ghp_"
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        while (token.length < 40) {
            token += charPool.random()
        }

        setState {
            state.token = token
//            println("\n\nupdateUser start")
//            println("${state.userInfo?.name} ${state.token}")
//            updateUser()
//            println("\n\nupdateUser finish")
        }
        println("\n\n===============================updateUser start")
        println("${state.userInfo?.name} ${state.token}")
        updateUser()
        //println("\n\nupdateUser finish")
    }
}

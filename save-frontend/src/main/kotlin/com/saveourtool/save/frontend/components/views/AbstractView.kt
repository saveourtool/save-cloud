package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.utils.ComponentWithScope

import react.Props
import react.State

import kotlinx.browser.document

/**
 * Abstract view class that should be used in all functional views
 */
abstract class AbstractView<P : Props, S : State>(private val hasBg: Boolean = true) : ComponentWithScope<P, S>() {
    // A small hack to avoid duplication of main content-wrapper from App.kt
    // We will have custom background only for sign-up and sign-in views
    override fun componentDidMount() {
        val style = if (hasBg) {
            Style(
                "-webkit-linear-gradient(270deg, rgb(84, 83, 97), rgb(25, 34, 99), rgb(102 103 171))",
                "",
                "transparent"
            )
        } else {
            Style(
                "bg-light",
                "bg-dark",
                "bg-dark"
            )
        }

        document.getElementById("content-wrapper")?.setAttribute(
            "style",
            "background: ${style.globalBackground}"
        )

        val topBar = document.getElementById("navigation-top-bar")
        topBar?.setAttribute(
            "class",
            "navbar navbar-expand ${style.topBarBgColor} navbar-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded"
        )

        topBar?.setAttribute(
            "style",
            "background: ${style.topBarTransparency}"
        )
    }

    /**
     * @property globalBackground
     * @property topBarBgColor
     * @property topBarTransparency
     */
    private data class Style(
        val globalBackground: String,
        val topBarBgColor: String,
        val topBarTransparency: String,
    )
}

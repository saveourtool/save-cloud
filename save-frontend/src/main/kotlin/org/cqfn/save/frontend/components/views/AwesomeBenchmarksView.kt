/**
 * A view with project creation details
 */

@file:Suppress("MAGIC_NUMBER", "WildcardImport", "FILE_WILDCARD_IMPORTS")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.benchmarks.BenchmarkCategoryEnum
import org.cqfn.save.frontend.externals.fontawesome.*
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.utils.AwesomeBenchmarks

import csstype.Height
import csstype.Width
import csstype.rem
import org.w3c.fetch.Headers
import react.*
import react.dom.*

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType

/**
 * [RState] of project creation view component
 *
 */
external interface AwesomeBenchmarksState : State {
    /**
     * list of benchmarks from DB
     */
    var benchmarks: List<AwesomeBenchmarks>

    /**
     * list of unique languages from benchmarks
     */
    var languages: List<String>
}

/**
 * A functional RComponent for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class AwesomeBenchmarksView : AbstractView<PropsWithChildren, AwesomeBenchmarksState>(true) {
    init {
        state.benchmarks = emptyList()
        getBenchmarks()
    }

    @Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR", "LongMethod")
    override fun RBuilder.render() {
        main("main-content mt-0 ps") {
            div("page-header align-items-start min-vh-100") {
                div("row justify-content-center") {
                    div("col-lg-6") {
                        div("row mb-2") {
                            div("col-md-6") {
                                div("card flex-md-row mb-1 box-shadow") {
                                    attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                        height = 14.rem
                                    }.unsafeCast<Height>()

                                    div("card-body d-flex flex-column align-items-start") {
                                        strong("d-inline-block mb-2 text-info") { +"Total Benchmarks:" }
                                        h1("mb-0") {
                                            a(classes = "text-dark", href = "#") {
                                                +state.benchmarks.count().toString()
                                            }
                                        }
                                        p("card-text mb-auto") { +"Checkout updates and new benchmarks." }
                                        a(href = "https://github.com/analysis-dev/awesome-benchmarks/pulls?q=is%3Apr+is%3Aclosed") {
                                            +"Check the GitHub"
                                        }
                                    }
                                    img(classes = "card-img-right flex-auto d-none d-md-block") {
                                        attrs["data-src"] = "holder.js/200x250?theme=thumb"
                                        attrs["src"] = "img/undraw_result_re_uj08.svg"
                                        attrs["data-holder-rendered"] = "true"
                                        attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                            width = 12.rem
                                        }.unsafeCast<Width>()
                                    }
                                }
                            }
                            div("col-md-6") {
                                div("card flex-md-row mb-1 box-shadow") {
                                    attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                        height = 14.rem
                                    }.unsafeCast<Height>()

                                    div("card-body d-flex flex-column align-items-start") {
                                        strong("d-inline-block mb-2 text-success") { +"""News""" }
                                        h3("mb-0") {
                                            a(classes = "text-dark", href = "#") {
                                                +"SAVE"
                                            }
                                        }
                                        p("card-text mb-auto") { +"Checkout latest news about SAVE project." }
                                        a(href = "https://github.com/analysis-dev/save-cloud") {
                                            +"SAVE-cloud "
                                        }
                                        a(href = "https://github.com/analysis-dev/save") {
                                            +" SAVE-cli"
                                        }
                                    }
                                    img(classes = "card-img-right flex-auto d-none d-md-block") {
                                        attrs["data-src"] = "holder.js/200x250?theme=thumb"
                                        attrs["src"] = "img/undraw_happy_news_re_tsbd.svg"
                                        attrs["data-holder-rendered"] = "true"
                                        attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                            width = 12.rem
                                        }.unsafeCast<Width>()
                                    }
                                }
                            }
                        }
                        span("mask opacity-6") {
                            form(classes = "d-none d-inline-block form-inline w-100 navbar-search") {
                                div("input-group") {
                                    input(classes = "form-control bg-light border-0 small") {
                                        attrs["type"] = "text"
                                        attrs["placeholder"] = "Search for the benchmark..."
                                        attrs["aria-label"] = "Search"
                                        attrs["aria-describedby"] = "basic-addon2"
                                    }
                                    div("input-group-append") {
                                        button(classes = "btn btn-primary", type = ButtonType.button) {
                                            fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                                        }
                                    }
                                }
                            }
                        }

                        div("container card o-hidden border-0 shadow-lg my-2 card-body p-0") {
                            div("p-5 text-center") {
                                h1("h3 text-gray-900 mb-5") {
                                    +"Awesome Benchmarks Archive"
                                }

                                div("row") {
                                    nav("nav nav-tabs mb-4") {
                                        BenchmarkCategoryEnum.values().forEachIndexed { i, value ->
                                            li("nav-item") {
                                                val classVal = if (i == 0) " active font-weight-bold" else ""
                                                p("nav-link $classVal text-gray-800") { +value.name }
                                            }
                                        }
                                    }
                                }

                                div("row mt-3") {
                                    div("col-lg-8") {
                                        // https://devicon.dev
                                        state.benchmarks.forEachIndexed { i, benchmark ->
                                            div("media text-muted ${if (i != 0) "pt-3" else ""}") {
                                                img(classes = "rounded mt-1") {
                                                    attrs["data-src"] =
                                                            "holder.js/32x32?theme=thumb&amp;bg=007bff&amp;fg=007bff&amp;size=1"
                                                    attrs["src"] = "img/undraw_code_inspection_bdl7.svg"
                                                    attrs["data-holder-rendered"] = "true"
                                                    attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                                        width = 4.2.rem
                                                    }.unsafeCast<Width>()
                                                }

                                                p("media-body pb-3 mb-0 small lh-125 border-bottom border-gray text-left") {
                                                    strong("d-block text-gray-dark") { +benchmark.name }
                                                    +benchmark.description
                                                    div("navbar-landing mt-2") {
                                                        // FixMe: links should be limited with the length of the div
                                                        benchmark.tags.split(",").map { " #$it " }.forEach {
                                                            // FixMe: support proper logic here
                                                            a("/#/awesome-benchmarks") {
                                                                +it
                                                            }
                                                        }
                                                    }
                                                    div("navbar-landing mt-3") {
                                                        a(
                                                            classes = "btn-sm btn-primary mr-2",
                                                            href = benchmark.documentation
                                                        ) {
                                                            +"""Docs"""
                                                        }
                                                        a(classes = "btn-sm btn-info mr-2", href = benchmark.sources) {
                                                            +"""Sources"""
                                                        }
                                                        a(
                                                            classes = "btn-sm btn-success ml-auto",
                                                            href = benchmark.homepage
                                                        ) {
                                                            +"""More """
                                                            fontAwesomeIcon(icon = faArrowRight)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    div("col-lg-4") {
                                        ul("list-group") {
                                            val languages = state.benchmarks.map { it.language }
                                            // FixMe: optimize this code (create maps with numbers only once). May be even store this data in DB?
                                            languages.distinct().forEach { language ->
                                                li("list-group-item d-flex justify-content-between align-items-center") {
                                                    +language.replace("language independent", "lang independent")
                                                    span("badge badge-primary badge-pill") {
                                                        +state.benchmarks.count { it.language == language }.toString()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div("col-lg-4 mb-4") {
                        div("card shadow mb-4") {
                            div("card-header py-3") {
                                h6("m-0 font-weight-bold text-primary") { +"Purpose of this list" }
                            }
                            div("card-body") {
                                p {
                                    +"""As a group of enthusiasts who create """

                                    a("https://github.com/analysis-dev/") {
                                        +"""dev-tools"""
                                    }

                                    +""" (including static analysis tools),
                                            we have seen a lack of materials related to testing scenarios or benchmarks that can be used to evaluate and test our applications.
                                            
                                            So we decided to create this """

                                    a("https://github.com/analysis-dev/awesome-benchmarks") {
                                        +"""curated list of standards, tests and benchmarks"""
                                    }

                                    +""" that can be used for testing and evaluating dev tools.
                                            Our focus is mainly on the code analysis, but is not limited by this category,
                                             in this list we are trying to collect all benchmarks that could be useful 
                                             for creators of dev-tools."""
                                }

                                div("text-center") {
                                    img(classes = "img-fluid px-3 px-sm-4 mt-3 mb-4") {
                                        attrs["style"] = kotlinext.js.jso<CSSProperties> {
                                            width = 20.rem
                                        }.unsafeCast<Width>()
                                        attrs["src"] = "img/undraw_programming_re_kg9v.svg"
                                        attrs["alt"] = "..."
                                    }
                                }
                            }
                        }
                        div("card shadow mb-4") {
                            div("card-header py-3") {
                                h6("m-0 font-weight-bold text-primary") { +"Easy contribution steps" }
                            }
                            div("card-body") {
                                ol {
                                    li {
                                        fontAwesomeIcon(icon = faGithub)
                                        +""" Go to the"""
                                        a("https://github.com/analysis-dev/awesome-benchmarks") {
                                            +""" awesome-benchmarks """
                                        }
                                        +"""repository"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faCopy)
                                        +""" Create a fork to your account"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faPlus)
                                        +""" Create the description in a proper format"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faFolderOpen)
                                        +""" Add your benchmark to"""
                                        a("https://github.com/analysis-dev/awesome-benchmarks/tree/main/benchmarks") {
                                            +""" benchmarks """
                                        }
                                        +"""dir"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faCheckCircle)
                                        +""" Validate the format with """
                                        a("https://docs.gradle.org/current/userguide/command_line_interface.html") {
                                            +"""`./gradlew build`"""
                                        }
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faArrowRight)
                                        +""" Create the PR to the main repo"""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getBenchmarks() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }

        GlobalScope.launch {
            val response: List<AwesomeBenchmarks> = get("$apiUrl/awesome-benchmarks", headers).decodeFromJsonString()

            setState {
                benchmarks = response
            }
        }
    }
}

package com.saveourtool.save.frontend.components.views.vuln

import com.saveourtool.save.frontend.components.modal.displayModalWithPreTag
import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.modal.loaderModalStyle
import com.saveourtool.save.frontend.utils.useWindowOpenness
import react.VFC
import react.useState
import react.dom.html.ReactHTML.div
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.views.vuln.utils.cosvFieldsDescriptionMap

import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.useBackground
import react.dom.html.ReactHTML
import web.cssom.ClassName
import kotlinx.serialization.json.Json


val jsonSchema = """
{
  "schema_version": "string",
  "id": "string",
  "modified": "string",
  "published": "string",
  "withdrawn": "string",
  "aliases": [ "string" ],
  "cwe_ids": [ "string" ],
  "cwe_names": [ "string" ],
  "timeline": [ {
      "timeline.type": "string",
      "timeline.value": "string"
    }
  ],
  "related": [ "string" ],
  "summary": "string",
  "details": "string",
  "severity": [ {
      "severity.type": "string",
      "severity.score": "string",
      "severity.level": "string",
      "severity.score_num": "string"
    }
  ],
  "affected": [ {
      "package": {
        "ecosystem": "string",
        "package.name": "string",
        "purl": "string",
        "language": "string",
        "repository": "string",
        "introduced_commits": [ "string" ],
        "fixed_commits": [ "string" ],
        "home_page": "string",
        "edition": "string"
      },
      "severity": [ {
          "severity.type": "string",
          "severity.score": "string",
          "severity.level": "string",
          "severity.score_num": "string"
        }
      ],
      "ranges": [ {
          "ranges.type": "string",
          "ranges.repo": "string",
          "events": [ {
              "introduced": "string",
              "fixed": "string",
              "last_affected": "string",
              "limit": "string"
            }
          ],
          "ranges.database_specific": { "see description": "" }
        }
      ],
      "versions": [ "string" ],
      "affected.ecosystem_specific": { "see description": "" },
      "affected.database_specific": { "see description": "" }
    }
  ],
  "patches_detail": [ {
      "patch_url": "string",
      "issue_url": "string",
      "main_language": "string",
      "author": "string",
      "commiter": "string",
      "branches": [ "string" ],
      "tags": [ "string" ]
    }
  ],
  "contributors": [ {
      "org": "string",
      "contributors.name": "string",
      "email": "string",
      "contributions": "string"
    }
  ],
  "confirm_type": "string",
  "references": [ {
      "references.type": "string",
      "url": "string"
    }
  ],
  "credits": [ {
      "credits.name": "string",
      "contact": [ "string" ],
      "credits.type": "string"
    }
  ],
  "database_specific": { "see description": "" }
}
"""

val cosvSchemaView = VFC {
    useBackground(Style.VULN_DARK)
    val windowOpenness = useWindowOpenness()
    val (textInModal, setTextInModal) = useState<Pair<String, String>>()

    if (textInModal != null) {
        displayModalWithPreTag(
                windowOpenness.isOpen(),
                textInModal.first,
                textInModal.second,
                loaderModalStyle,
                windowOpenness.closeWindowAction()
        ) {
            buttonBuilder("Close", "secondary") {
                windowOpenness.closeWindow()
            }
        }
    }

    div {
        className = ClassName("card")
        JSON.stringify(jsonSchema, null, 2).drop(1).dropLast(1).split("\\n").forEach {
            val str = it.replace("\\", "")
            val key = str.takeWhile { it != ':' }.replace("\"", "").trim()

            div {
                //class = ClassName("row") // если будет работать
                console.log("TEXT KEY [$key]")

                val cosvKey = cosvFieldsDescriptionMap.firstOrNull { (k, v) ->
                    k == key
                }

                console.log("NULL ${cosvKey == null}")
                console.log("STR ${str}")

                ReactHTML.pre {
                    cosvKey?.let { (key, value) ->
                        // hold the tabulations
                        +"${str.takeWhile { it != '\"' }}\""
                        // make from key the button
                        buttonBuilder(key, classes = "btn-sm") {
                            setTextInModal(key to value)
                            windowOpenness.openWindow()
                        }
                        +"\":"
                        // print the type, i.e. value
                        +str.dropWhile { it != ':' }.drop(1)
                    } ?: run {
                        // just print string
                        +str
                    }
                }
            }


        }
    }
}




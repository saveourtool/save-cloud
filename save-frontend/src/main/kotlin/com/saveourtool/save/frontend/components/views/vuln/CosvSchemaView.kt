package com.saveourtool.save.frontend.components.views.vuln

import com.saveourtool.osv4k.OsvSchema
import com.saveourtool.osv4k.RawOsvSchema
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.utils.useWindowOpenness
import react.VFC
import react.useState
import react.dom.html.ReactHTML.div
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.views.vuln.utils.cosvFieldsDescriptionMap

import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.useBackground
import kotlinext.js.js
import react.dom.html.ReactHTML
import web.cssom.ClassName
import kotlinx.serialization.json.Json


private val json = Json { prettyPrint = true }

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
      "type": "string",
      "value": "string"
    }
  ],
  "related": [ "string" ],
  "summary": "string",
  "details": "string",
  "severity": [ {
      "type": "string",
      "score": "string",
      "level": "string",
      "score_num": "string"
    }
  ],
  "affected": [ {
      "package": {
        "ecosystem": "string",
        "name": "string",
        "purl": "string",
        "language": "string",
        "repository": "string",
        "introduced_commits": [ "string" ],
        "fixed_commits": [ "string" ],
        "home_page": "string",
        "edition": "string"
      },
      "severity": [ {
          "type": "string",
          "score": "string",
          "level": "string",
          "score_num": "string"
        }
      ],
      "ranges": [ {
          "type": "string",
          "repo": "string",
          "events": [ {
              "introduced": "string",
              "fixed": "string",
              "last_affected": "string",
              "limit": "string"
            }
          ],
          "database_specific": { "see description": "" }
        }
      ],
      "versions": [ "string" ],
      "ecosystem_specific": { "see description": "" },
      "database_specific": { "see description": "" }
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
      "name": "string",
      "email": "string",
      "contributions": "string"
    }
  ],
  "confirm_type": "string",
  "references": [ {
      "type": "string",
      "url": "string"
    }
  ],
  "credits": [ {
      "name": "string",
      "contact": [ "string" ],
      "type": "string"
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
        displayModal(
                windowOpenness.isOpen(),
                textInModal.first,
                textInModal.second,
                mediumTransparentModalStyle,
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




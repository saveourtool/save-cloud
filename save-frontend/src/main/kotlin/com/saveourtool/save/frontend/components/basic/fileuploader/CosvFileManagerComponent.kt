@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.frontend.components.inputform.dragAndDropForm
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.utils.FILE_PART_NAME
import js.core.asList
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.useState
import web.cssom.ClassName
import web.file.File
import web.http.FormData

val cosvFileManagerComponent: FC<Props> = FC { props ->
    useTooltip()

    val (cosvAvailableFiles, setCosvAvailableFiles) = useState<List<String>>(emptyList())

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())

    val uploadCosvFiles = useDeferredRequest {
        val uploadedFiles: List<String> = post(
            url = "$apiUrl/cosv/batch-upload",
            Headers(),
            FormData().apply { filesForUploading.forEach { append(FILE_PART_NAME, it) } },
            loadingHandler = ::noopLoadingHandler,
        ).decodeFromJsonString()
        setCosvAvailableFiles { uploadedFiles }
    }

    div {
        ul {
            className = ClassName("list-group shadow")
            cosvAvailableFiles.map { file ->
                li {
                    +file
                }
            }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isMultipleFilesSupported = true
                    tooltipMessage = "Regular files/Executable files/ZIP Archives"
                    onChangeEventHandler = { files ->
                        setFilesForUploading(files!!.asList())
                        uploadCosvFiles()
                    }
                }
            }
        }
    }
}

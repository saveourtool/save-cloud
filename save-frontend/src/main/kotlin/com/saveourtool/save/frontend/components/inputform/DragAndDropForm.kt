/**
 * File containing input form for file uploading.
 *
 * Supports both drag 'n' drop and file browsing (with clicking on it)
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.inputform

import js.core.asList
import react.*
import react.dom.events.DragEventHandler
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.file.FileList
import web.html.HTMLElement
import web.html.InputType

val dragAndDropForm: FC<DragAndDropFormProps> = FC { props ->
    val (isDragActive, setIsDragActive) = useState(false)
    val inputRef: MutableRefObject<HTMLElement> = useRef(null)

    val dragHandler: DragEventHandler<*> = {
        it.preventDefault()
        it.stopPropagation()
        if (it.type.unsafeCast<String>() == "dragenter" || it.type.unsafeCast<String>() == "dragover") {
            setIsDragActive(true)
        } else if (it.type.unsafeCast<String>() == "dragleave") {
            setIsDragActive(false)
        }
    }

    val dropHandler: DragEventHandler<*> = {
        it.preventDefault()
        it.stopPropagation()
        setIsDragActive(false)
        if (it.dataTransfer.files.asList()
            .isNotEmpty()) {
            props.onChangeEventHandler(it.dataTransfer.files)
        }
    }

    val onButtonClick = { inputRef.current?.click() }

    form {
        className = ClassName("btn m-0 flex-fill p-0")
        div {
            val dragActive = if (isDragActive) "drag-active" else ""
            className = ClassName("p-3 $dragActive")
            id = "drag-file-element"
            onDragEnter = dragHandler
            onDragLeave = dragHandler
            onDragOver = dragHandler
            onDrop = dropHandler
            input {
                ref = inputRef
                type = InputType.file
                id = "input-file-upload"
                multiple = true
                hidden = true
                onChange = { props.onChangeEventHandler(it.target.files) }
            }
            strong { +" Click or drag'n'drop a file " }
            onClick = { onButtonClick() }
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "bottom"
            title = "Regular files/Executable files/ZIP Archives"
        }
    }
}

/**
 * [Props] for [dragAndDropForm]
 */
external interface DragAndDropFormProps : Props {
    /**
     * Callback that defines file uploading process
     */
    var onChangeEventHandler: (FileList?) -> Unit
}

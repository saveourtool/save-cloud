@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("react-ace")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.reactace

import react.*

/**
 * External declaration of [reactAce] react [FC]
 */
@JsName("default")
external val reactAce: FC<AceEditorProps>

/**
 * [Props] for [reactAce]
 */
@Suppress("COMMENTED_OUT_CODE")
@JsName("IAceEditorProps")
external interface AceEditorProps : Props {
    var name: String
    var style: CSSProperties
    var mode: String
    var theme: String
    var height: String
    var width: String
    var className: String
    var fontSize: String
    var showGutter: Boolean
    var showPrintMargin: Boolean
    var highlightActiveLine: Boolean
    var focus: Boolean
    var cursorStart: Int
    var wrapEnabled: Boolean
    var readOnly: Boolean
    var minLines: Int
    var maxLines: Int
    var navigateToFileEnd: Boolean
    var debounceChangePeriod: Int?
    var enableBasicAutocompletion: Boolean
    var enableLiveAutocompletion: Boolean
    var tabSize: Int
    var value: String
    var placeholder: String?
    var defaultValue: String
    var enableSnippets: Boolean
    var setOptions: dynamic
    var markers: Array<AceMarker>

    @Suppress("TYPE_ALIAS")
    var onChange: (value: String, event: dynamic) -> Unit
    // var onSelectionChange: (value: String, event: Event) -> Unit
    // onCursorChange?: (value: any, event?: any) => void;
    // onInput?: (event?: any) => void;
    // onLoad?: (editor: Ace.Editor) => void;
    // onValidate?: (annotations: Ace.Annotation[]) => void;
    // onBeforeLoad?: (ace: typeof AceBuilds) => void;
    // onSelection?: (selectedText: string, event?: any) => void;
    // onCopy?: (value: string) => void;
    // onPaste?: (value: string) => void;
    // onFocus?: (event: any, editor?: Ace.Editor) => void;
    // onBlur?: (event: any, editor?: Ace.Editor) => void;
    // onScroll?: (editor: IEditorProps) => void;
    // editorProps?: IEditorProps;
    // keyboardHandler?: string;
    // commands?: ICommand[];
    // annotations?: Ace.Annotation[];
}

/**
 * Line markers for [reactAce]
 */
@JsName("IMarker")
external interface AceMarker {
    var startRow: Int
    var startCol: Int
    var endRow: Int
    var endCol: Int
    var className: String
    var type: dynamic
    var inFront: Boolean
}

@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JsModule("rehype-highlight")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.markdown.rehype

/**
 * Plugin for syntax highlighting in code blocks
 *
 * @param options
 * @return dynamic
 */
@JsName("default")
external fun rehypeHighlightPlugin(options: dynamic = definedExternally): dynamic

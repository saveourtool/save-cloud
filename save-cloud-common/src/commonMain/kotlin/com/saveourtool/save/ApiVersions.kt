/**
 * File, which contain the list of versions of backend API
 */

@file:Suppress("VARIABLE_NAME_INCORRECT", "CONSTANT_UPPERCASE", "TopLevelPropertyNaming")
@file:OptIn(ExperimentalJsExport::class)

package com.saveourtool.save

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@JsExport
const val v1: String = "v1"
@JsExport
const val latestVersion: String = v1

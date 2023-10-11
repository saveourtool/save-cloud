package com.saveourtool.save.utils.http

/**
 * A custom implementation for [HttpHeader]
 */
data class CustomHttpHeader(override val name: String, override val value: String): HttpHeader {
    companion object {
        /**
         * @param value
         * @return [CustomHttpHeader] with name as [this] and [value]
         */
        fun String.asHttpHeader(value: String): HttpHeader = CustomHttpHeader(this, value)
    }
}

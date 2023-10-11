package com.saveourtool.save.utils.http

/**
 * A base interface for http header in backend
 */
interface HttpHeader {
    val name: String
    val value: String
}
package com.saveourtool.save.exceptions

import java.lang.Exception

/**
 * Exception for backend
 */
class BackendInternalException(message: String) : Exception(message) {
    constructor() : this("Backend failure")
}

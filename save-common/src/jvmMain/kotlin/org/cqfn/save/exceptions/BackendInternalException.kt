package org.cqfn.save.exceptions

import java.lang.Exception

class BackendInternalException(s: String) : Exception(s) {
    constructor(): this("Backend failure")
}
package org.cqfn.save.api

data class Authorization(
    val userName: String,
    val password: String? = null
)
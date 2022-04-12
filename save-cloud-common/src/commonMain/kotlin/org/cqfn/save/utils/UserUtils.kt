package org.cqfn.save.utils

fun extractUserNameAndSource(userInformation: String) = userInformation.split(":").map { it.trim() }.let {
    require(it.size == 2)
    it.last() to it.first()
}
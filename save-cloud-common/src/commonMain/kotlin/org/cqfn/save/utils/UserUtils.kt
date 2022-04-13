package org.cqfn.save.utils


fun extractUserNameAndSource(userInformation: String): Pair<String, String> {
    if (!userInformation.contains("@")) {
        return userInformation to "basic"
    }
    userInformation.split("@").map { it.trim() }.let {
        require(it.size == 2) {
            "User information should contain source and username, separated by `@` but found after extraction: $it"
        }
        return it.last() to "${it.first()}-basic"
    }
}
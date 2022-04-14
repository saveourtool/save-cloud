/**
 * Utilities of User entity
 */

package org.cqfn.save.utils

/**
 * @param userInformation
 * @return pair of username and source (where the user identity is coming from)
 */
fun extractUserNameAndSource(userInformation: String): Pair<String, String> {
    // for users, which are not linked with any source (convenient in local deployment)
    if (!userInformation.contains("@")) {
        return userInformation to "basic"
    }
    userInformation.split("@").map { it.trim() }.let {
        require(it.size == 2) {
            "User information $userInformation should contain source and username, separated by `@` but found after extraction: $it"
        }
        return it.last() to it.first()
    }
}

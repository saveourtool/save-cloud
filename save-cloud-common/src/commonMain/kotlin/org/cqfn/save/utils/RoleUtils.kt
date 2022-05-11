/**
 * Utilities of Role entity
 */

package org.cqfn.save.utils

import org.cqfn.save.domain.Role

/**
 * @param oneRole
 * @param anotherRole
 * @return [Role] that has higher priority
 */
fun getHighestRole(oneRole: Role?, anotherRole: Role?): Role =
        listOf(oneRole ?: Role.NONE, anotherRole ?: Role.NONE).maxByOrNull { it.priority }!!

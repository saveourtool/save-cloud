package org.cqfn.save.entities

import org.cqfn.save.info.UserInfo
import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property source where the user identity is coming from, e.g. "github"
 * @property email email of user
 * @property avatar avatar of user
 */
@Entity
class User(
    var name: String?,
    var password: String?,
    var role: String?,
    var source: String,
    var email: String?,
    var avatar: String? = null,
) : BaseEntity() {
    /**
     * @return [UserInfo] object
     */
    fun toUserInfo() = UserInfo(
        userName = name ?: "Unknown",
        email = email,
        avatar = avatar,
    )
}

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.info.UserInfo
import react.Props
import react.StateSetter

/**
 * Property to propagate user info from App
 */
external interface UserInfoAwareProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}


/**
 * Property to propagate user info from App with ability to update it
 */
external interface UserInfoAwareMutableProps : UserInfoAwareProps {
    /**
     * Setter of user info (it can be updated in settings on several views)
     */
    var userInfoSetter: StateSetter<UserInfo?>
}

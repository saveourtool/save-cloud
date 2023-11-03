package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import react.State

/**
 * State with role of current user
 */
external interface StateWithRole : State {
    /**
     * Role of a user that is seeing this view
     */
    var selfRole: Role
}

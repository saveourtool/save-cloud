package org.cqfn.save.entities

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property containerId id of the container, inside which the agent is running
 * @property execution id of the execution, which the agent is serving
 * @property version
 */
@Entity
class Agent(
    var containerId: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    var version: String? = null,
) : BaseEntity()

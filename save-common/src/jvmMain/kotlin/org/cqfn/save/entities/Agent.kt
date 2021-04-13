package org.cqfn.save.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property containerId id of the container, inside which the agent is running
 * @property execution id of the execution, which the agent is serving
 */
@Entity
@Table(name = "agent")
class Agent(

    @Column(name = "container_id")
    var containerId: String,

    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

) : BaseEntity()

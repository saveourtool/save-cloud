package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity

import com.fasterxml.jackson.annotation.JsonBackReference

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property user
 * @property name
 * @property source
 */
@Entity
class OriginalLogin(
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    var user: User,
    var name: String,
    var source: String,
) : BaseEntity()

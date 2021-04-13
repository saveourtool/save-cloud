/**
 * This file contains actual annotation from Spring data jpa
 */

package org.cqfn.save.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

actual typealias Entity = Entity
actual typealias Id = Id
actual typealias GeneratedValue = GeneratedValue
actual typealias JoinColumn = JoinColumn
actual typealias ManyToOne = ManyToOne
actual typealias OneToMany = OneToMany

package org.cqfn.save.entities

@Entity
class Result(
    @Id val id: Int,
    val status: String,
    val date: String // fixme should be date
)
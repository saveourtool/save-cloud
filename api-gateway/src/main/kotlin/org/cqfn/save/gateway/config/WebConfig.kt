package org.cqfn.save.gateway.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = Long::class),
)
internal class IdentitySourceAwareUserDetailsMixin

package com.saveourtool.save.utils

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@Suppress("MISSING_KDOC_TOP_LEVEL")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = Long::class),
)
class IdentitySourceAwareUserDetailsMixin

package com.saveourtool.save.backend.security

import java.util.Base64

private fun String.base64Encode() = Base64.getEncoder().encodeToString(toByteArray())

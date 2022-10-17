/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.sandbox.config

import org.springframework.context.annotation.ComponentScan


@ComponentScan("com.saveourtool.save.authservice.config.WebSecurityConfig")
class SandboxWebSecurityConfig

@ComponentScan("com.saveourtool.save.authservice.config.NoopWebSecurityConfig")
class SandboxNoopWebSecurityConfig


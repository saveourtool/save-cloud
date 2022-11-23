package com.saveourtool.save.orchestrator.config

import com.saveourtool.save.authservice.utils.KubernetesAuthenticationUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

/**
 * Configuration class to set up Spring Security
 */
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Import(KubernetesAuthenticationUtils::class)
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class KubernetesServiceAccountWebSecurityConfig

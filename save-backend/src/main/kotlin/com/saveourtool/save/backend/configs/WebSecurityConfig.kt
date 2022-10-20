/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.backend.configs

import com.saveourtool.save.backend.utils.ConvertingAuthenticationManager
import com.saveourtool.save.backend.utils.CustomAuthenticationBasicConverter
import com.saveourtool.save.domain.Role
import com.saveourtool.save.spring.security.KubernetesAuthenticationUtils
import com.saveourtool.save.spring.security.ServiceAccountAuthenticatingManager
import com.saveourtool.save.spring.security.ServiceAccountTokenExtractorConverter
import com.saveourtool.save.spring.security.serviceAccountTokenAuthentication
import com.saveourtool.save.v1
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.access.hierarchicalroles.RoleHierarchyUtils
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import javax.annotation.PostConstruct

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("secure")
@Import(KubernetesAuthenticationUtils::class)
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig(
    private val authenticationManager: ConvertingAuthenticationManager,
    @Autowired private var defaultMethodSecurityExpressionHandler: DefaultMethodSecurityExpressionHandler
) {
    @Bean
    @Order(1)
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        securityMatcher(
            AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.anyExchange(),
                NegatedServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/actuator", "/actuator/**", "/internal/**")
                )
            )
        )
    }
        .run {
            authorizeExchange()
                .pathMatchers(*publicEndpoints.toTypedArray())
                .permitAll()
        }
        .and()
        .run {
            authorizeExchange()
                .pathMatchers("/api/**")
                .authenticated()
        }
        .and()
        .run {
            // FixMe: Properly support CSRF protection https://github.com/saveourtool/save-cloud/issues/34
            csrf().disable()
        }
        .addFilterBefore(
            AuthenticationWebFilter(authenticationManager).apply {
                setServerAuthenticationConverter(CustomAuthenticationBasicConverter())
            },
            SecurityWebFiltersOrder.HTTP_BASIC,
        )
        .exceptionHandling {
            it.authenticationEntryPoint(
                HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
            )
        }
        .logout()
        .disable()
        .formLogin()
        .disable()
        .build()

    fun roleHierarchy(): RoleHierarchy = mapOf(
        Role.SUPER_ADMIN to listOf(Role.ADMIN, Role.OWNER, Role.VIEWER),
        Role.ADMIN to listOf(Role.OWNER, Role.VIEWER),
        Role.OWNER to listOf(Role.VIEWER),
    )
        .mapKeys { it.key.asSpringSecurityRole() }
        .mapValues { (_, roles) -> roles.map { it.asSpringSecurityRole() } }
        .let(RoleHierarchyUtils::roleHierarchyFromMap)
        .let {
            RoleHierarchyImpl().apply { setHierarchy(it) }
        }

    @PostConstruct
    fun postConstruct() {
        defaultMethodSecurityExpressionHandler.setRoleHierarchy(roleHierarchy())
    }

    companion object {
        /**
         * These endpoints will have `permitAll` enabled on them. We can't selectively put `@PreAuthorize("permitAll")` in the code,
         * because it won't allow us to configure authenticated access to all other endpoints by default.
         * Or we can use custom AccessDecisionManager later.
         */
        internal val publicEndpoints = listOf(
            "/error",
            // `CollectionView` is a public page
            "/api/$v1/projects/not-deleted",
            "/api/$v1/awesome-benchmarks",
            "/api/$v1/check-git-connectivity-adaptor",
            // `OrganizationView` is a public page
            // fixme: when we will want to make organizations accessible for everyone, wi will need to add more endpoints here
            "/api/$v1/organizations/**",
            "/api/$v1/projects/get/projects-by-organization",
            // `ContestListView` and `ContestView` are public pages
            "/api/$v1/contests/*",
            "/api/$v1/contests/active",
            "/api/$v1/contests/finished",
            "/api/$v1/contests/*/public-test",
            "/api/$v1/contests/*/scores",
            "/api/$v1/contests/*/*/best",
        )
    }

    @Profile("kubernetes")
    @Bean
    @Order(2)
    fun internalSecuredSecurityChain(
        http: ServerHttpSecurity,
        serviceAccountAuthenticatingManager: ServiceAccountAuthenticatingManager,
        serviceAccountTokenExtractorConverter: ServiceAccountTokenExtractorConverter,
    ): SecurityWebFilterChain = http.run {
        authorizeExchange().pathMatchers("/actuator/**")
            // all requests to `/actuator` should be sent only from inside the cluster
            // access to this port should be controlled by a NetworkPolicy
            .permitAll()
            .and()
            .authorizeExchange()
            .pathMatchers("/internal/**")
            .authenticated()
            .and()
            .serviceAccountTokenAuthentication(serviceAccountTokenExtractorConverter, serviceAccountAuthenticatingManager)
            .csrf()
            .disable()
            .logout()
            .disable()
            .formLogin()
            .disable()
            .build()
    }

    @Profile("!kubernetes")
    @Bean
    @Order(2)
    fun internalInsecureSecurityChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        // All `/internal/**` and `/actuator/**` requests should be sent only from internal network,
        // they are not proxied from gateway.
        authorizeExchange().pathMatchers("/internal/**", "/actuator/**")
            .permitAll()
            .and()
            .build()
    }
}

@EnableWebFluxSecurity
@Profile("!secure")
@Order(1)
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class NoopWebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.authorizeExchange()
        .anyExchange()
        .permitAll()
        .and()
        .csrf()
        .disable()
        .build()
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

package org.cqfn.save.gateway.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.cqfn.save.utils.IdentitySourceAwareUserDetails
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.jackson2.CoreJackson2Module

@Configuration
class WebConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer { jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder ->
        jacksonObjectMapperBuilder
        .modules(CoreJackson2Module())
        .mixIn(IdentitySourceAwareUserDetails::class.java, IdentitySourceAwareUserDetailsMixin::class.java)
        //.modulesToInstall(CoreJackson2Module())
        //.mixIn(TestStatus::class.java, TestStatusMixin::class.java)
        //.mixIn(User::class.java, UserMixin::class.java)
    }
}

//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = String::class),
    JsonSubTypes.Type(value = Long::class),
)
internal class IdentitySourceAwareUserDetailsMixin
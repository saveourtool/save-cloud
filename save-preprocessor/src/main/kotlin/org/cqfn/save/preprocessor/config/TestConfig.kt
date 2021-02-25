package org.cqfn.save.preprocessor.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["org.cqfn.save.entities.repository", "org.cqfn.save.entities.service"])
open class TestConfig {
}

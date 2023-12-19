package com.saveourtool.save.gateway.config

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component
import reactor.netty.http.server.HttpServer

//@Component
class NettyConfiguration : WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {
    override fun customize(factory: NettyReactiveWebServerFactory) {
        val dnsOverriding = NettyServerCustomizer { httpServer ->
            httpServer
        }
        factory.addServerCustomizers(dnsOverriding)
    }
}
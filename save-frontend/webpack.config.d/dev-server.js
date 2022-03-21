config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/api/**"],
          target: 'http://localhost:5000',
          logLevel: 'debug',
          onProxyReq: function (proxyReq, req, res) {
            // username `admin:`
            proxyReq.setHeader("Authorization", "Basic YWRtaW46");
            proxyReq.setHeader("X-Authorization-Source", "basic");
          }
        }
      ]
    }
)

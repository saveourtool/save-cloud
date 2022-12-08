config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/api/**"],
          target: 'http://localhost:5800',
          logLevel: 'debug',
          onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("Authorization", "Basic YWRtaW46");
            proxyReq.setHeader("X-Authorization-Source", "basic");
          }
        },
        {
          context: ["/sandbox/api/**"],
          target: 'http://localhost:5400',
          logLevel: 'debug',
          onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("Authorization", "Basic YWRtaW46");
            proxyReq.setHeader("X-Authorization-Source", "basic");
          }
        },
        {
          context: ["/demo/api/**"],
          target: 'http://localhost:5421',
          logLevel: 'debug',
        },
        {
          context: ["/cpg/api/**"],
          target: 'http://localhost:5500',
          logLevel: 'debug',
        },
        {
          bypass: (req, res) => {
            if (req.url.endsWith("/sec/user")) {
              return res.send(
                // mocked userName
                "admin"
              );
            } else if (req.url.endsWith("/sec/oauth-providers")) {
              return res.send([])
            }
          }
        }
      ]
    }
)

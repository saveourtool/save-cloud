config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/api/**", "**.ico", "**.png"],
          target: 'http://localhost:5800',
          logLevel: 'debug',
          onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("Authorization", "Basic YWRtaW46");
            proxyReq.setHeader("X-Authorization-Source", "basic");
          }
        },
        {
          bypass: (req, res) => {
            if (req.url.endsWith("/sec/user")) {
              return res.send({
                // mocked UserInfo object
                name: "admin"
              });
            } else if (req.url.endsWith("/sec/oauth-providers")) {
              return res.send([])
            }
          }
        }
      ]
    }
)

config.devServer = Object.assign(
  {},
  config.devServer || {},
  {
    setupMiddlewares: (middlewares, devServer) => {
      devServer.app.get("/sec/user", (req, res) => { return res.send("admin"); });
      devServer.app.get("/sec/oauth-providers", (req, res) => { return res.send([]); });
      return middlewares;
    },
    proxy: [
      {
        context: ["/api/sandbox/**"],
        target: 'http://localhost:5400',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
          proxyReq.setHeader("X-Authorization-Id", "1");
          proxyReq.setHeader("X-Authorization-Name", "admin");
          proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
        }
      },
      {
        context: ["/api/demo/**"],
        target: 'http://localhost:5421',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
          proxyReq.setHeader("X-Authorization-Id", "1");
          proxyReq.setHeader("X-Authorization-Name", "admin");
          proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
        }
      },
      {
        context: ["/api/cpg/**"],
        target: 'http://localhost:5500',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
          proxyReq.setHeader("X-Authorization-Id", "1");
          proxyReq.setHeader("X-Authorization-Name", "admin");
          proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
        }
      },
      {
        context: ["/api/**"],
        target: 'http://localhost:5800',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
          proxyReq.setHeader("X-Authorization-Id", "1");
          proxyReq.setHeader("X-Authorization-Name", "admin");
          proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
        }
      }
    ],
    historyApiFallback: true
  }
);
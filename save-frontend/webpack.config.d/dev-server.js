config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/api/**"],
          target: 'http://localhost:5000',
            logProvider: function logProvider(provider) {
                // replace the default console log provider.
                return require('winston');
            },
            logLevel: 'debug',
            onProxyReq: function (proxyReq, req, res) {
                const winston = require('winston');
                const logger = winston.createLogger();
              logger.debug(JSON.stringify(req.headers));
              logger.debug(JSON.stringify(proxyReq.headers));
              proxyReq.setHeader("Authorization", "Basic YWRtaW46");
            }
        }
      ]
    }
)

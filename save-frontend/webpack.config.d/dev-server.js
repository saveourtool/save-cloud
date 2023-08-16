config.devServer = Object.assign(
      {},
      config.devServer || {},
      {
        proxy: [
          {
            context: ["/api/**", "/sec/**", "/oauth2/**", "/logout/**", "/login/oauth2/**"],
            target: 'http://localhost:5300',
            logLevel: 'debug',
          }
        ],
        historyApiFallback: true
      }
  );

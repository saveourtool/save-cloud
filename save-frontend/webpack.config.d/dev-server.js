config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/api/**"],
          pathRewrite: { '^/api': '' },
          target: 'http://localhost:5000',
        }
      ]
    }
)

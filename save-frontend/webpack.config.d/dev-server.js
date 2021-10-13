config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
      proxy: [
        {
          context: ["/**", "!/"],
          target: 'http://localhost:5000',
        }
      ]
    }
)
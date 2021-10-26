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

// WA similar to https://youtrack.jetbrains.com/issue/KT-46082
config.resolve.alias = {
    "os": false,
}
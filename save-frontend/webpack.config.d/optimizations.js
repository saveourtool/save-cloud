const HtmlWebpackPlugin = require('html-webpack-plugin');

config.plugins.push(
    new HtmlWebpackPlugin({
        template: 'index.html'
    })
);

/*config.output.filename = () => {
    "save-frontend-[name].[contenthash].js"
}*/

if (config.mode === "production") {
    config.optimization = {
        // todo: use https://webpack.js.org/guides/output-management/ instead of manually adding js files into html
        splitChunks: {
            cacheGroups: {
                commons: {
                    test: /[\\/]node_modules[\\/]/,
                    name: 'vendors',
                    chunks: 'all'
                }
            }
        }
    };
}

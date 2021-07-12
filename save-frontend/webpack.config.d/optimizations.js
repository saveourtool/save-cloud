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

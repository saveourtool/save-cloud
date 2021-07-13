config.module.rules.push(
    {
        test: /\.scss$/,
        use: [
            'style-loader', // creates style nodes from JS strings
            'css-loader', // translates CSS into CommonJS
            {
                loader: 'postcss-loader', // Run postcss actions
                options: {
                    // postcss plugins, can be exported to postcss.config.js
                    plugins: function () {
                        return [
                            require('autoprefixer')
                        ];
                    }
                }
            },
            'sass-loader', // compiles Sass to CSS, using Node Sass by default
        ]
    },
    {
        // loader for images
        test: /\.(jpg|png|svg)$/,
        use: {
            loader: 'url-loader',
        }
    },
    {
        // loader for fonts
        test: /\.(eot|ttf|woff|woff2)$/,
        use: {
            loader: 'file-loader',
        }
    }
);

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const My_Definitions = require('./webpack.definitions');
const instance = require('./../config/instance.json');

/**
 * Distribution mode:
 * - none: Opts out of any default optimization options
 * - development: Sets process.env.NODE_ENV on DefinePlugin to value development. Enables NamedChunksPlugin and NamedModulesPlugin.
 * for more information see https://webpack.js.org/concepts/mode/
 */
var MODE_DISTRIBUTION = 'none'; // 'development'

/**
 * Variables
 */
var APP_NAME = 'portal';
var DIST_DIR = path.resolve(__dirname, "destTEST");
var CLIENT_DIR = path.resolve(__dirname, "src");
var PUBLIC_PATH = '/' + APP_NAME + '/dist/';

/**
 * Prepare the plugins
 */
var My_Plugins = [
    new CleanWebpackPlugin([DIST_DIR]),
    new MiniCssExtractPlugin({
        filename: "[name].css",
        chunkFilename: "[id].css"
    })
];

for (let i = 0; i < My_Definitions.html_files_to_add.length; i++) {
    const html_file_name = My_Definitions.html_files_to_add[i];
    My_Plugins.push(
        new HtmlWebpackPlugin({
            template: path.resolve(CLIENT_DIR, html_file_name),
            output: DIST_DIR,
            inject: 'head',
            filename: html_file_name,
            BACKEND: instance.portal.backend,
            NAME: instance.portal.backend.substring(8).split(/[\.\:]/)[0]
        }))
}

module.exports = {
    /**
     * MODE
     */
    mode: MODE_DISTRIBUTION,

    /**
     * ENTRY
     */
    entry: My_Definitions.entry,

    /**
     * OUTPUT
     */
    output: {
        filename: '[name].bundle.js',
        path: DIST_DIR
    },

    /**
     * CONFIGURATION TO DEPLOY
     */
    devServer: {
        contentBase: DIST_DIR,
        port: 9004,
        https: true,
        publicPath: PUBLIC_PATH,
        openPage: APP_NAME + '/dist/index.html'
    },

    /**
     * MODULES
     */
    module: {
        rules: [
            {
                test: /\.(le|sa|sc|c)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    "css-loader",
                    //'postcss-loader',
                    'sass-loader',
                    'less-loader'
                ]
            },
            {
                test: /\.(png|svg|jpg|gif)$/,
                use: [
                    'file-loader'
                ]
            },
            {
                test: /\.(woff|woff2|eot|ttf|otf)$/,
                use: [
                    'file-loader'
                ]
            }
        ]
    },

    /**
     * PLUGINS
     */
    plugins: My_Plugins
};
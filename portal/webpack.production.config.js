const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const My_Definitions = require('./webpack.definitions');

/**
 * Distribution mode:
 * - production: Sets process.env.NODE_ENV on DefinePlugin to value production. Enables FlagDependencyUsagePlugin, FlagIncludedChunksPlugin, ModuleConcatenationPlugin, NoEmitOnErrorsPlugin, OccurrenceOrderPlugin, SideEffectsFlagPlugin and UglifyJsPlugin
 * for more information see https://webpack.js.org/concepts/mode/
 */
var MODE_DISTRIBUTION = 'production';

/**
 * Variables
 */
var DIST_DIR = path.resolve(__dirname, "dist");
var CLIENT_DIR = path.resolve(__dirname, "src");

/**
 * Prepare the plugins
 */
var My_Plugins = [
    new CleanWebpackPlugin([DIST_DIR]),
    new MiniCssExtractPlugin({
        filename: "[name].[contenthash].css",
        chunkFilename: "[id].[contenthash].css"
    })
];

for (let i = 0; i < My_Definitions.html_files_to_add.length; i++) {
    const html_file_name = My_Definitions.html_files_to_add[i];
    My_Plugins.push(
        new HtmlWebpackPlugin({
            template: path.resolve(CLIENT_DIR, html_file_name),
            output: DIST_DIR,
            inject: 'head',
            filename: html_file_name
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
        filename: '[name].[contenthash].bundle.js',
        path: DIST_DIR
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
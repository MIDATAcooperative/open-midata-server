/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CopyWebpackPlugin = require('copy-webpack-plugin');
const My_Definitions = require('./webpack.definitions');
const instance = require('./../config/instance.json');
const autoprefixer = require('autoprefixer');

/**
 * Distribution mode:
 * - production: Sets process.env.NODE_ENV on DefinePlugin to value production. Enables FlagDependencyUsagePlugin, FlagIncludedChunksPlugin, ModuleConcatenationPlugin, NoEmitOnErrorsPlugin, OccurrenceOrderPlugin, SideEffectsFlagPlugin and UglifyJsPlugin
 * for more information see https://webpack.js.org/concepts/mode/
 */
var MODE_DISTRIBUTION = 'production';

/**
 * Variables
 */
var DIST_DIR = path.resolve(__dirname, "dest");
var DIST_IMAGES = path.resolve(DIST_DIR, "images");
var DIST_IMG = path.resolve(DIST_DIR, "img");
var CLIENT_DIR = path.resolve(__dirname, "src");
var CLIENT_IMAGES = path.resolve(CLIENT_DIR, "assets", "images");
var CLIENT_IMG = path.resolve(CLIENT_DIR, "assets", "img");

/**
 * Prepare the plugins
 */
var My_Plugins = [
    new CleanWebpackPlugin(),
    new CopyWebpackPlugin([
        { from: path.resolve(CLIENT_DIR, '**/*.html'), to: DIST_DIR, ignore: [ 'src/index.html', 'src/oauth.html' ], context: 'src/', transform : My_Definitions.jsonReplacer },
        { from: path.resolve(CLIENT_DIR, 'auth.js'), to: path.resolve(DIST_DIR, 'auth.js') },
        { from: CLIENT_IMAGES, to: DIST_IMAGES },
        { from: CLIENT_IMG, to: DIST_IMG },
        { from: path.resolve(CLIENT_DIR, 'assets', 'fonts'), to:  path.resolve(DIST_DIR, 'fonts')},
        { from: path.resolve(CLIENT_DIR, 'i18n', '*.json'), to:  path.resolve(DIST_DIR, 'i18n'), context: 'src/i18n/',        	
          transform : My_Definitions.jsonReplacer}
    ]),
    new MiniCssExtractPlugin({
        filename: "[name].[contenthash].css",
        chunkFilename: "[id].[contenthash].css"
    })
];

for (let i = 0; i < My_Definitions.html_files_to_add.length; i++) {
    const _definition = My_Definitions.html_files_to_add[i];
    My_Plugins.push(
        new HtmlWebpackPlugin({
            template: path.resolve(CLIENT_DIR, _definition.page),
            output: DIST_DIR,
            inject: 'head',
            filename: _definition.page,
            excludeChunks: _definition.exclude,
            BACKEND: instance.portal.backend,
            HOMEPAGE: instance.homepage,
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
        filename: '[name].[contenthash].bundle.js',
        path: DIST_DIR
    },

    /**
     * MODULES
     */
    module: {
        rules: [
            {
                test: /\.(le|c)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    "css-loader",
                    {
                        loader: 'postcss-loader',
                        options: {
                          plugins: () => [autoprefixer()]
                        }
                    },
                    //'sass-loader',
                    'less-loader'
                ]
            },
            {
                test: /\.(sa|sc)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    "css-loader",
                    {
                        loader: 'postcss-loader',
                        options: {
                          plugins: () => [autoprefixer()]
                        }
                    },
                    'sass-loader'
                    //,
                    //'less-loader'
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
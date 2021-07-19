/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CopyWebpackPlugin = require('copy-webpack-plugin');
const My_Definitions = require('./webpack.definitions');
const instance = require('./../config/instance.json');
const autoprefixer = require('autoprefixer');
const { VueLoaderPlugin } = require('vue-loader')
const webpack = require('webpack');
const ESLintPlugin = require('eslint-webpack-plugin')

/**
 * Distribution mode:
 * - none: Opts out of any default optimization options
 * - development: Sets process.env.NODE_ENV on DefinePlugin to value development. Enables NamedChunksPlugin and NamedModulesPlugin.
 * for more information see https://webpack.js.org/concepts/mode/
 */
var MODE_DISTRIBUTION = 'development'

/**
 * Variables
 */
var APP_NAME = 'portal';
var DIST_DIR = path.resolve(__dirname, "dest");
var DIST_IMAGES = path.resolve(DIST_DIR, "images");
var DIST_IMG = path.resolve(DIST_DIR, "img");
var CLIENT_DIR = path.resolve(__dirname, "src");
var CLIENT_IMAGES = path.resolve(CLIENT_DIR, "assets", "images");
var CLIENT_IMG = path.resolve(CLIENT_DIR, "assets", "img");
var PUBLIC_PATH = '/';// + APP_NAME + '/dist/';

/**
 * Prepare the plugins
 */
var My_Plugins = [    
    new CleanWebpackPlugin(),   
    new CopyWebpackPlugin({ patterns : [
        { from: path.resolve(CLIENT_DIR, '**/*.html'), to: DIST_DIR, globOptions: {
            ignore: [ 'src/index_old.html', 'src/oauth_old.html' ] } , context: 'src/', transform : My_Definitions.jsonReplacer },
        { from: path.resolve(CLIENT_DIR, 'auth.js'), to: path.resolve(DIST_DIR, 'auth.js') },       
        { from: CLIENT_IMAGES, to: DIST_IMAGES },      
        { from: CLIENT_IMG, to: DIST_IMG },
        { from: path.resolve(CLIENT_DIR, 'assets', 'fonts'), to:  path.resolve(DIST_DIR, 'fonts')},               
        { from: path.resolve(CLIENT_DIR, 'i18n', '*.json'), to:  path.resolve(DIST_DIR, 'i18n'), context: 'src/i18n/',
        	transform : My_Definitions.jsonReplacer
        }
    ]}),
    new CopyWebpackPlugin({ patterns :[        
        { from: path.resolve(CLIENT_DIR, "override", "images"), to: DIST_IMAGES, force : true }, 
        { from: path.resolve(CLIENT_DIR, "override", "img"), to: DIST_IMG, force : true },    
        { from: path.resolve(CLIENT_DIR, 'override', '*.json'), to:  path.resolve(DIST_DIR, 'i18n'), context: 'src/override/',
        	transform : My_Definitions.jsonReplacer, force : true
        }
    ]}),
    new MiniCssExtractPlugin({
        filename: "[name].css",
        chunkFilename: "[id].css"
    }),
    new webpack.DefinePlugin({
       __VUE_OPTIONS_API__ : true,
       __VUE_PROD_DEVTOOLS__ : false
    }),
    /*new ESLintPlugin({
      extensions : ['js','vue']
    }),*/
    new VueLoaderPlugin()
];

for (let i = 0; i < My_Definitions.html_files_to_add.length; i++) {
    const _definition = My_Definitions.html_files_to_add[i];
    My_Plugins.push(
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, _definition.page),
            output: DIST_DIR,
            inject: 'head',
            filename: _definition.page,
            excludeChunks: _definition.exclude,
            BACKEND: instance.portal.backend,
            HOMEPAGE: instance.homepage,
            PLATFORM: instance.platform,
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
        static: DIST_DIR,
        port: 9002,
        https: true        
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
            },
            {
                test: /\.vue$/,
                loader: 'vue-loader'
            }/*,
            {
                test: /\.js$/,
                use: 'babel-loader'
            }*/
        ]
    },

    /**
     * PLUGINS
     */
    plugins: My_Plugins,

    resolve: {
        modules: [
          'node_modules',
          path.resolve(__dirname + '/vue'),
          path.resolve(__dirname + '/node_modules'),
          path.resolve(__dirname + '/src')          
        ],
        fallback: { 
	       "crypto": require.resolve("crypto-browserify"),
           "buffer": require.resolve("buffer/"),
           "stream": require.resolve("stream-browserify") 
        }
    }
};
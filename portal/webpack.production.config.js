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
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CopyWebpackPlugin = require('copy-webpack-plugin');
const My_Definitions = require('./webpack.definitions');
const autoprefixer = require('autoprefixer');
const { VueLoaderPlugin } = require('vue-loader');
const webpack = require('webpack');

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
    new CopyWebpackPlugin({ patterns : [
        { from: path.resolve(CLIENT_DIR, '**/*.html'), to: DIST_DIR, globOptions: {
            ignore: [ 'src/index_old.html', 'src/oauth_old.html' ] }, context: 'src/' },
        { from: path.resolve(CLIENT_DIR, 'auth.js'), to: path.resolve(DIST_DIR, 'auth.js') },
        { from: CLIENT_IMAGES, to: DIST_IMAGES },       
        { from: CLIENT_IMG, to: DIST_IMG },
        { from: path.resolve(CLIENT_DIR, 'assets', 'fonts'), to:  path.resolve(DIST_DIR, 'fonts')},
        { from: path.resolve(CLIENT_DIR, 'i18n', '*.json'), to:  path.resolve(DIST_DIR, 'i18n'), context: 'src/i18n/'        	
         
        }
    ]}),
    new CopyWebpackPlugin({ patterns : [        
        { from: path.resolve(CLIENT_DIR, "override", "images"), to: DIST_IMAGES, force : true },
        { from: path.resolve(CLIENT_DIR, "override", "img"), to: DIST_IMG, force : true },    
        { from: path.resolve(CLIENT_DIR, 'override', '*.json'), to:  path.resolve(DIST_DIR, 'i18n'), context: 'src/override/',
        	force : true
        }
    ]}),
    new MiniCssExtractPlugin({
        filename: "[name].[contenthash].css",
        chunkFilename: "[id].[contenthash].css"
    }),
    new webpack.DefinePlugin({
       __VUE_OPTIONS_API__ : true,
       __VUE_PROD_DEVTOOLS__ : false
    }),
    new VueLoaderPlugin(),
	
	new webpack.ProvidePlugin({
	   process: 'process/browser.js',
	   Buffer: ['buffer', 'Buffer']
	})
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
            BACKEND: ""
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
        path: DIST_DIR,
		clean: true
    },

    /**
     * MODULES
     */
    module: {
        rules: [
			{
			   mimetype: 'image/svg+xml',			 
			   type: 'asset/resource',
			   generator: {
			     filename: 'icons/[hash].svg'
			   }
			},
            {
                test: /\.(le|c)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    "css-loader",
                    {
                        loader: 'postcss-loader',
                        options: {
	                      postcssOptions : {
                            plugins: () => [autoprefixer()]
                          }
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
	                      postcssOptions : {
                            plugins: () => [autoprefixer()]
                          }
                        }
                    },
                    'sass-loader'
                    //,
                    //'less-loader'
                ]
            },
			{
			  test: /\.(ico|png|jpg|jpeg|gif|svg|webp|tiff)$/i,
			  type: "asset/resource",
			  generator: {
			    filename: "images/[name].[hash][ext]",
			  },
			},
			{
			  test: /\.(woff|woff2|eot|ttf|otf)$/i,
			  type: "asset/resource",
			  generator: {
			     filename: "fonts/[name].[hash][ext]",
			  },
			},
            {
                test: /\.vue$/,
                loader: 'vue-loader'
            }/*,
            {
               test: /\.js$/,
               use: 'babel-loader'
            } */
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
        fallback : { 
	       "crypto": require.resolve("crypto-browserify"),
           "buffer": require.resolve("buffer/"),
           "stream": require.resolve("stream-browserify"),
           "querystring": require.resolve("querystring-es3"),
		   'process/browser': require.resolve('process/browser'),		   
		   'process': require.resolve('process/browser')   
        }
    },

    externals: {
      config: "config"
    }
};
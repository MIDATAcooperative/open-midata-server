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

const glob = require('glob');
const instance = require('./../config/instance.json');

var my_exports = {};

my_exports.jsonReplacer = function(buffer) {
   var str = buffer.toString();	
   
        str = str.replace(/@PLATFORM/ig,instance.platform);
        str = str.replace(/@SUPPORT/ig,instance.support);
        str = str.replace(/@OPERATOR/ig,instance.operator);
        str = str.replace(/@HOMEPAGE/ig,instance.homepage);		
		return Buffer.from(str);	
}; 

my_exports.entry = {
    miniportal: './src/main-miniportal.js',
    vueminiportal: './vue/main.js',
    mainportal: './src/main-portal.js',
    vueportal: './vue/main.js',
       
    minicss: [
    ],
    maincss: [ 
    ]
};

my_exports.entry.minicss.push('./src/assets/scss/main.scss');

my_exports.entry.maincss = my_exports.entry.maincss.concat(glob.sync('./src/assets/css/*', { ignore: ['./src/assets/css/main.css']}));
my_exports.entry.maincss = my_exports.entry.maincss.concat(glob.sync('./src/views/**/*.less'));



my_exports.html_files_to_add = [
    {
        page:'index_old.html',
        exclude: ['miniportal', 'vueportal', 'vueminiportal', 'minicss']
    },
    {
        page:'oauth_old.html',
        exclude: ['mainportal', 'vueportal', 'vueminiportal', 'maincss']
    },
    {
        page:'oauth.html',
        exclude: ['mainportal', 'miniportal', 'vueportal', 'minicss']
    },{
        page:'index.html',
        exclude: ['mainportal', 'miniportal', 'vueminiportal', 'minicss']
    }];

module.exports = my_exports;
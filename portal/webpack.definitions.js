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

var my_exports = {};


my_exports.entry = {
    
    vueminiportal: './vue/main.js',
    
    vueportal: './vue/main.js',
       
   
    maincss: ['./src/assets/css/application.scss','./src/assets/css/typeahead.css'] 
    
};

my_exports.html_files_to_add = [    
    {
        page:'oauth.html',
        exclude: ['vueportal']
    },{
        page:'index.html',
        exclude: ['vueminiportal']
    }];

module.exports = my_exports;
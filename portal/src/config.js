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

var instance = require('./../../config/instance.json');

angular.module('config', [])
.constant('ENV', {
    name: process.env.NODE_ENV==='production'?'production':'development',
    apiurl: instance.portal.backend,
    beta : instance.portal.beta,
    instance : instance.portal.backend.substring(8).split(/[\.\:]/)[0],
    instanceType : instance.instanceType,
    languages : instance.portal.languages,
    countries : instance.portal.countries,
    build : require('../package.json').version
});
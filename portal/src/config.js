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
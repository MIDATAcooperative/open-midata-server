const glob = require('glob');

var my_exports = {};

my_exports.entry = {
    miniportal: './src/main-miniportal.js',
    mainportal: './src/main-portal.js',

    //bootstrap: './node_modules/bootstrap/less/bootstrap.less',
    //midatacss: './node_modules/angular-midatajs/css/app.css'
    minicss: [
    ],
    maincss: [
    ]
};

my_exports.entry.minicss.push('./src/assets/scss/main.scss');

my_exports.entry.maincss = my_exports.entry.maincss.concat(glob.sync('./src/assets/css/*'));
my_exports.entry.maincss = my_exports.entry.maincss.concat(glob.sync('./src/views/**/*.less'));

my_exports.html_files_to_add = [
    {
        page:'index.html',
        exclude: ['miniportal']
    },{
        page:'oauth.html',
        exclude: ['mainportal']
    }];

module.exports = my_exports;
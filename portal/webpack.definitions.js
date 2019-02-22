const glob = require('glob');

var my_exports = {};

my_exports.entry = {
    miniportal: './src/main-miniportal.js',
    mainportal: './src/main-portal.js',
       
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
        page:'index.html',
        exclude: ['miniportal','minicss']
    },{
        page:'oauth.html',
        exclude: ['mainportal','maincss']
    }];

module.exports = my_exports;
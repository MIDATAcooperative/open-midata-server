var my_exports = {};

my_exports.entry = {
    vendor: './src/vendor.js',
    app: './src/main.js',

    //bootstrap: './node_modules/bootstrap/less/bootstrap.less',
    //midatacss: './node_modules/angular-midatajs/css/app.css'
    //config: './src/config.js'
};

my_exports.html_files_to_add = ['index.html', 'oauth.html'];

module.exports = my_exports;
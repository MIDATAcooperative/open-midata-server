var my_exports = {};

my_exports.entry = {
    miniportal: './src/main-miniportal.js',
    mainportal: './src/main-portal.js',

    //bootstrap: './node_modules/bootstrap/less/bootstrap.less',
    //midatacss: './node_modules/angular-midatajs/css/app.css'
    //config: './src/config.js'
    minicss: [ // Gruntjs: app.scss ln. 243
        './src/assets/scss/_reset.scss',
        './src/assets/scss/_variables.scss',
        './src/assets/scss/_mixins.scss',
        './src/assets/scss/_colors.scss',
        './src/assets/scss/_placeholder_classes.scss',
        './src/assets/scss/_html.scss',
        './src/assets/scss/_animations.scss',
        './src/assets/scss/_typography.scss',
        './src/assets/scss/components/**/*.scss',
        //C:\Repos\i4mi\01_MIDATA\platform-private\portal\src\assets\scss\components\organisms\02-Desktop-Responsive\02-responsive-menu
        //responsive-menu.scss
        
        './src/views/**/*.scss',
        //'./src/views/shared/public/terms/terms.scss',
        './src/assets/scss/_olddesign.scss',
        './src/assets/scss/_midataextra.scss'
    ],
    maincss: [ // Gruntjs: app.less ln. 232
        'src/assets/css/*' , 'src/views/**/*.less', '!src/assets/css/main.css'
    ]
};

my_exports.html_files_to_add = [
    {
        page:'index.html',
        exclude: ['miniportal']
    },{
        page:'oauth.html',
        exclude: ['mainportal']
    }];

module.exports = my_exports;
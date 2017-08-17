module.exports = function(grunt) {
  var instance = grunt.file.readJSON('../config/instance.json');
  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    /*
    uglify: {
      options: {
       
      },
      build: {
        src: 'dest/app.js',
        dest: 'dest/app.min.js'
      }
    },
    
    cssmin: {
    	  options: {
    	    shorthandCompacting: false,
    	    roundingPrecision: -1
    	  },
    	  target: {
    	    files: {
    	      'dest/app.min.css': ['dest/app.css']
    	    }
    	  }
    },*/
    
    env : {

        options : {
        },

        dev: {
            NODE_ENV : 'DEVELOPMENT',
            BACKEND : instance.portal.backend,
            NAME : instance.portal.backend.substring(8).split(/[\.\:]/)[0],
            INSTANCETYPE : instance.instanceType
        },

        prod : {
            NODE_ENV : 'PRODUCTION',
            BACKEND : instance.portal.backend,
            NAME : instance.portal.backend.substring(8).split(/[\.\:]/)[0],
            INSTANCETYPE : instance.instanceType
        }
    },
    
    preprocess : {
        all : {
          files : { 
        	  'dest/index.html' : 'src/index.html',
        	  'dest/oauth.html' : 'src/oauth.html',
          }            
        }
    },
    
    useminPrepare: {
      html: ['dest/index.html', 'dest/oauth.html' ],
      options : { 
    	  dest : 'dest'
      }
    },
    
    usemin: {
      html: ['dest/index.html', 'dest/oauth.html' ]      
    },
    
    // Which files to watch
    watch: {
        gruntfile: {
          files: 'Gruntfile.js',
          tasks: ['default'],
        },
        src: {
          files: ['src/**/*.{html,png,jpg,jpeg,gif,webp,svg,json}' ],
          tasks: ['copy','preprocess'],
          options: { livereload: true }
        },
        js : {
          files : 'src/**/*.js',
          tasks : ['concat:js'],
          options: { livereload: true }
        },
        css: {
          files : 'src/**/*.less',
          tasks : ['concat:css', 'less']
        }
    },
    
    // What files to clean
    clean: {
       build: ["dest/*", "!dest/components/**" ]    	  
    },
    
    // Check syntax
    jshint: {
    	options: {    	    
    	   eqeqeq: false,
    	   eqnull: true
    	},
        all: ['Gruntfile.js', 'src/**/*.js' ]
    },
    
    // Copy files
    copy: {
      main: {    
    	files : [
    	 { expand : true, cwd: 'src/', src: '**/*.html', dest: 'dest/' },    	 
    	 { expand : true, cwd: 'src/', src: 'auth.js', dest: 'dest/' },
    	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dest/images' },
    	 { expand : true, flatten:true, cwd: 'dest/components/', src: ['**/*.ttf','**/*.woff','**/*.woff2'], dest: 'dest/fonts' }
        ]
      }
    },
    
    jsonmin : {
    	main: {    
        	files : [        	
        	   { expand : true, flatten:true , cwd: 'src/', src: '**/*.json', dest: 'dest/i18n/' },        	
            ]
        }	
    },
    
    // Concat javascript files
    concat: {
        
        js: {
          src: ['src/app.js', 'tmp/scripts/config.js', 'src/assets/**/*.js', 'src/views/**/*.js' ],
          dest: 'dest/app.js'
        },
        oauthjs: {
            src: ['src/oauthapp.js', 'tmp/scripts/config.js', 'src/assets/**/*.js', 'src/views/shared/public/oauth2/*.js', 'src/views/shared/public/postregister/*.js', 'src/views/members/public/registration/*.js', 'src/views/shared/public/terms/*.js' ],
            dest: 'dest/oauthapp.js'
        },
        css : {
          src : ['src/assets/css/*' , 'src/views/**/*.less'],
          dest: 'dest/app.less'
        }
     },
     
     // ng-annotate
     ngAnnotate: {
         options: {
             singleQuotes: true,
         },
         app: {
             files: {
                 'dest/app.js': ['dest/app.js'],
                 'dest/oauthapp.js' : [ 'dest/oauthapp.js' ]
             }
         }
     },
     
     // Test Server
     connect: {
        server: {
          options: {
            port: 9002,
            base: 'dest',
            protocol : 'https'
          }
          
        }
     },
     
     /*reload: {
         port: 35729,
         liveReload: {},
         proxy: {
           host: "localhost",
           port: 9002
         }
     },*/
     
     less: {
       development: {
    	 options: {
    	      paths: []
    	 },
    	 files: {
    	      "dest/app.css": "dest/app.less"
    	 }
      }
     },
     
     ngconstant: {    	  
    	  options: {
    	    space: '  ',
    	    wrap: '"use strict";\n\n {%= __ngModule %}',
    	    name: 'config',
    	  },
    	  // Environment targets
    	  development: {
    	    options: {
    	      dest: 'tmp/scripts/config.js'
    	    },
    	    constants: {
    	      ENV: {
    	        name: 'development',
    	        apiurl: instance.portal.backend,
    	        beta : instance.portal.beta,
    	        instance : instance.portal.backend.substring(8).split(/[\.\:]/)[0],
    	        instanceType : instance.instanceType,
    	        languages : instance.portal.languages,
    	        countries : instance.portal.countries
    	      }
    	    }
    	  },
    	  production: {
    	    options: {
    	      dest: 'tmp/scripts/config.js'
    	    },
    	    constants: {
    	      ENV: {
    	        name: 'production',
    	        apiurl: instance.portal.backend,
    	        beta : instance.portal.beta,
    	        instance : instance.portal.backend.substring(8).split(/[\.\:]/)[0],
    	        instanceType : instance.instanceType,
    	        languages : instance.portal.languages,
    	        countries : instance.portal.countries
    	      }
    	    }
    	  }
    	}
  });

  // Load grunt tasks
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-ng-annotate');
  grunt.loadNpmTasks('grunt-ng-constant');
  grunt.loadNpmTasks('grunt-usemin');
  grunt.loadNpmTasks('grunt-jsonmin');
  grunt.loadNpmTasks('grunt-env');
  grunt.loadNpmTasks('grunt-preprocess');
  //grunt.loadNpmTasks('connect-livereload');

  // Default task(s).
  grunt.registerTask('default', ['deploy']);
  grunt.registerTask('webserver', ['connect', 'watch']);
  grunt.registerTask('bundle', [ 'copy', 'jsonmin', 'preprocess', 'jshint', 'concat', 'less' ]);
  grunt.registerTask('build', [
                               'ngAnnotate',
                               'useminPrepare',
                               'concat:generated',
                               'cssmin:generated',
                               'uglify:generated',
                               'usemin'
  ]);
  grunt.registerTask('server'           , ['clean', 'ngconstant:development', 'env:dev', 'bundle','webserver']);
  grunt.registerTask('server-remote'    , ['clean', 'ngconstant:production', 'env:prod', 'bundle','webserver']);
  grunt.registerTask('server-local-dist', ['clean', 'ngconstant:development', 'env:dev', 'bundle', 'build', 'webserver']);
  grunt.registerTask('server-remote-dist', ['clean', 'ngconstant:production', 'env:prod', 'bundle', 'build', 'webserver']);
  grunt.registerTask('deploy', ['clean', 'ngconstant:production', 'env:prod', 'bundle', 'build']);
  
};

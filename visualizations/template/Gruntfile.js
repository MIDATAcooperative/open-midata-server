module.exports = function(grunt) {
  
	
  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    
    /*
    env : {

        options : {
        },

        dev: {
            NODE_ENV : 'DEVELOPMENT',
            BACKEND : instance.portal.backend
        },

        prod : {
            NODE_ENV : 'PRODUCTION',
            BACKEND : instance.portal.backend
        }
    },*/
    
    preprocess : {
        all : {
        	files : {
               'src/index.html' : 'dist/index.html',
               'src/preview.html' : 'dist/preview.html'
        	}
        }
    },
    
    useminPrepare: {
      html: 'dist/*.html',
      options : { 
    	  dest : 'dist'
      }
    },
    
    usemin: {
      html: 'dist/*.html'      
    },
    
    // Which files to watch
    watch: {
        gruntfile: {
          files: 'Gruntfile.js',
          tasks: ['default'],
        },
        src: {
          files: ['src/**/*.{html,png,jpg,jpeg,gif,webp,svg}' ],
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
       build: ["dist/*", "!dist/components/**" ]    	  
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
    	 { expand : true, cwd: 'src/', src: '**/*.html', dest: 'dist/' },
    	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dist/images' },
    	 { expand : true, flatten:true, cwd: 'dist/components/', src: ['**/*.ttf','**/*.woff','**/*.woff2'], dest: 'dist/fonts' }
        ]
      }
    },
    
    // Concat javascript and css files
    concat: {
        
        js: {
          src: ['src/app.js', 'src/**/*.js'],
          dest: 'dist/app.js'
        },
        css : {
          src : ['src/**/*.css' , 'src/**/*.less'],
          dest: 'dist/app.less'
        }
     },
     
     // ng-annotate
     ngAnnotate: {
         options: {
             singleQuotes: true,
         },
         app: {
             files: {
                 'dist/app.js': ['dist/app.js']                
             }
         }
     },
     
     // Test Server
     connect: {
        server: {
          options: {
            port: 9004,
            base: 'dist',
            protocol : 'https'
          }
          
        }
     },
         
     less: {
       development: {
    	 options: {
    	      paths: []
    	 },
    	 files: {
    	      "dist/app.css": "dist/app.less"
    	 }
      }
     }
     /*
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
    	        beta : instance.portal.beta
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
    	        beta : instance.portal.beta
    	      }
    	    }
    	  }
    	}*/
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
  grunt.loadNpmTasks('grunt-env');
  grunt.loadNpmTasks('grunt-preprocess');
  //grunt.loadNpmTasks('connect-livereload');

  // Default task(s).
  grunt.registerTask('default', ['deploy']);
  grunt.registerTask('webserver', ['connect', 'watch']);
  grunt.registerTask('bundle', [ 'copy', 'preprocess', 'jshint', 'concat', 'less' ]);
  grunt.registerTask('build', [
                               'ngAnnotate',
                               'useminPrepare',
                               'concat:generated',
                               'cssmin:generated',
                               'uglify:generated',
                               'usemin'
  ]);
  grunt.registerTask('server'           , ['clean', 'bundle','webserver']); 
  grunt.registerTask('deploy', ['clean', 'bundle', 'build']);
  
};

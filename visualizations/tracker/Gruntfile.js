module.exports = function(grunt) {
  
	var midataName = "tracker";
	
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
               'tmp/index.html' : 'src/index.html',
               'tmp/preview.html' : 'src/preview.html'
        	}
        }
    },
    
    bower : {
    	
       install : {
    	   
       },
       options : {
    	   targetDir : 'tmp/components',
    	   layout : 'byComponent'
       }
    },
    
    useminPrepare: {
      html: 'tmp/*.html',
      options : {     	 
    	  dest : 'tmp'
      }
    },
    
    usemin: {
      html: 'tmp/*.html'
    },
    
    // Which files to watch
    watch: {
        gruntfile: {
          files: 'Gruntfile.js',
          tasks: ['default'],
        },
        src: {
          files: ['src/**/*.{html,png,jpg,jpeg,gif,webp,svg}' ],
          tasks: ['preprocess', 'copy:dev'],
          options: { livereload: true }
        },
        js : {
          files : 'src/**/*.js',
          tasks : ['jshint', 'concat', 'copy:dev'],
          options: { livereload: true }
        },
        css: {
          files : 'src/**/*.less',
          tasks : ['concat', 'less', 'copy:dev'],
          options: { livereload: true }
        }
    },
    
    // What files to clean
    clean: {
       build: ["dist/*", "dev/*", "tmp/*" ]    	  
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
      dist: {    
    	files : [
    	 { expand : true, cwd: 'src/', src: '**/*.html', dest: 'dist/' },
    	 { expand : true, cwd: 'tmp/', src: ['**/*.html','**/*.min.js','**/*.min.css'], dest: 'dist' },
    	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dist/images' },
    	 { expand : true, flatten:true, cwd: 'bower_components/', src: ['**/*.ttf','**/*.woff','**/*.woff2'], dest: 'dist/fonts' }
        ]
      },
      dev: {    
      	files : [
      	 { expand : true, cwd: 'src/', src: ['**/*.html','**/*.js'], dest: 'dev/'+midataName+'/dist' },
      	 { expand : true, cwd: 'tmp/', src: ['**/*.html','**/*.min.js','**/*.css'], dest: 'dev/'+midataName+'/dist' },
      	 { expand : true, cwd: 'tmp/', src: 'components/**', dest: 'dev/'+midataName+'/dist' },
      	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dev/'+midataName+'/dist' },
      	 { expand : true, flatten:true, cwd: 'bower_components/', src: ['**/*.ttf','**/*.woff','**/*.woff2'], dest: 'dev/'+midataName+'/dist/fonts' }
        ]
      },
      devdist: {    
        	files : [
        	 { expand : true, cwd: 'src/', src: '**/*.html', dest: 'dev/'+midataName+'/dist' },
        	 { expand : true, cwd: 'tmp/', src: ['**/*.html','**/*.min.js','**/*.css'], dest: 'dev/'+midataName+'/dist' },        	 
        	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dev/'+midataName+'/dist' },
        	 { expand : true, flatten:true, cwd: 'bower_components/', src: ['**/*.ttf','**/*.woff','**/*.woff2'], dest: 'dev/'+midataName+'/dist/fonts' }
          ]
     }
    },
    
    // Concat javascript and css files
    concat: {
                
        all: {
        	files : {
        		'tmp/app.js' : ['src/app.js', 'src/**/*.js'],
        		'tmp/app.less' : ['src/**/*.css' , 'src/**/*.less']
        	}          
        }
     },
     
     // ng-annotate
     ngAnnotate: {
         options: {
             singleQuotes: true,
         },
         app: {
             files: {
                 'tmp/app.js': ['tmp/app.js']                
             }
         }
     },
     
     // Test Server
     connect: {
        server: {
          options: {
            port: 9004,
            base: 'dev',
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
    	      "tmp/app.css": "tmp/app.less"
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
  grunt.loadNpmTasks('grunt-bower-task');
  //grunt.loadNpmTasks('connect-livereload');

  // Default task(s).
  grunt.registerTask('default', ['build']);
  grunt.registerTask('webserver', ['connect', 'watch']);
  grunt.registerTask('min', [
                               'ngAnnotate',
                               'useminPrepare',
                               'concat:generated',
                               'cssmin:generated',
                               'uglify:generated',
                               'usemin'
  ]);
  grunt.registerTask('server-dist', ['clean', 'jshint', 'bower', 'preprocess', 'concat', 'less', 'min', 'copy:devdist', 'webserver']); 
  grunt.registerTask('server', ['clean', 'jshint', 'bower', 'preprocess', 'concat', 'less', 'copy:dev', 'webserver']); 
  grunt.registerTask('build', ['clean', 'jshint', 'bower', 'preprocess', 'concat', 'less', 'min', 'copy:dist']);
  
};

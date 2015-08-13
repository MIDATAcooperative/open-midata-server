module.exports = function(grunt) {

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
    
    useminPrepare: {
      html: 'dest/index.html'
    },
    
    
    
    // Which files to watch
    watch: {
        gruntfile: {
          files: 'Gruntfile.js',
          tasks: ['default'],
        },
        src: {
          files: ['src/**/*.{html,png,jpg,jpeg,gif,webp,svg}' ],
          tasks: ['copy'],
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
    	 { expand : true, cwd: 'src/assets/images/', src : '**/*', dest : 'dest/images' }    	 
        ]
      }
    },
    
    // Concat javascript files
    concat: {
        
        js: {
          src: ['src/portals/app.js', 'src/assets/**/*.js', 'src/views/**/*.js' ],
          dest: 'dest/app.js'
        },
        css : {
          src : ['src/assets/css/*' , 'src/views/**/*.less'],
          dest: 'dest/app.less'
        }
     },
     
     // Test Server
     connect: {
        server: {
          options: {
            port: 9002,
            base: 'dest'
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
  grunt.loadNpmTasks('grunt-contrib-usemin');
  //grunt.loadNpmTasks('connect-livereload');

  // Default task(s).
  grunt.registerTask('default', ['clean','copy']);
  grunt.registerTask('build', [
                               'useminPrepare',
                               'concat:generated',
                               'cssmin:generated',
                               'uglify:generated',
                               'usemin'
  ]);
  grunt.registerTask('server', ['clean', 'copy', 'jshint', 'concat', 'less','connect', 'watch']);
  grunt.registerTask('deploy', ['clean', 'copy', 'jshint', 'concat', 'less','build']);
  
};

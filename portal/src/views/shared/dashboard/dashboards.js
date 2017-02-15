angular.module('portal')
.constant("tiles", {	
    'jawboneupimport' : { 
	   id : "5360f7c1e4b0e85c6385cd0c", 
	   title : "Jawbone Up Import",
	   teaser : "Import data from Jawbone UP."
    },
	
   'clock' : { 
	 id : "543d2c14e4b0f34f43670afa", 
	 title : "Jawbone Activity Clock", 
	 teaser : "Visualize Jawbone move and sleep events in a clean and friendly interface."
   },
   'meal' : { 
	   id :  "543fd403e4b0722020c54b10", 
	   title : "Jawbone Meals", 
	   teaser : "See your nutritional habits in a nutshell."
   },  
   'credentials' : { 
	   id :  "54450c48e4b0a1a96b5ff54c", 
	   title : "Credentials Manager",
	   teaser : "Manage your credentials for other sites."
   },
   'energy-meter' : { 
	   id :  "54467fe0e4b066dcca1a3dc5", 
	   title : "Jawbone Energy Meter", 
	   teaser : "A geovisualization of Jawbone workouts, with weekly timespan and summaries of intensity, calories and step."
   },
   'fitbit' : { 
	   id :  "545258f4e4b0a9447b348c17", 
	   title : "Fitbit", 
	   teaser : "Import data collected with Fitbit devices, such as the Fitbit Flex armband."
   },
   'water-meter' : { 
	   id : "54621b44e4b0a29eb4b0e72d", 
	   title : "Fitbit Water Meter", 
	   teaser : "See your Fitbit water consumption glass by glass."
   },
   'weight-watcher' : { 
	   id :  "5469e31de4b065acd080126a", 
	   title : "Fitbit Weight Watcher", 
	   teaser : "Displays a scale with your latest BMI and weight and some advice, based on your historical weight data."
   },
   'fileupload' : { 
	   id :  "5475f8b3e4b0c5d11a59a999", 
	   title : "File Uploader", 
	   teaser : "Simply upload files of any kind up to a size of 2GB."
   },
   
   'trainingeditor' : { 
	   id :  "554c867de4b08413ff37d6f5", 
	   title : "Training Editor", 
	   teaser : "Create and edit training plans for yourself or others."
   },
   'trainingdiary' : { 
	   id :  "5550872fe4b042b13ab4c95c", 
	   title : "Training Diary", 
	   teaser : "Fill out a training diary based upon a training plan created by the training plan visualization."
   },
   'cdaimport' : { 
	   id :  "55682c26e4b08b543c12b834", 
	   title : "CDA Import", 
	   teaser : "This is a CDA import"
   },
   'cdaviewer' : { 
	   id :  "556c007fe4b06e7275af0f19", 
	   title : "CDA Viewer", 
	   teaser : "Displays CDA Files in browser"
   },
   'charts' : { 
	   id :  "5571700fe4b0c309cbc949ce", 
	   title : "Charts", 
	   teaser : "Tries to show a useful diagram for assigned records."
   },
   
   'entry' : { 
	   id :  "557936b7e4b0cf0d3bcafd6c", 
	   title : "Data Entry", 
	   teaser : "Enter your weight, steps or bloodpressure directly into the portal."
   },
   'surveys' : { 
	   id :  "55af6055e4b044f0420c9bf2", 
	   title : "Surveys", 
	   teaser : "Allows participation in surveys."
   },
   'calendar' : {
	   id :  "56a8897ee4b0d31bf38cacdf", 
	   title : "Calendar", 
	   teaser : "Display your data in a calendar."
   }
})
.constant("dashboards",
	{
	  "me" : 
		  [
		   {
			    id: "help_welcome",			
			    template : "/views/members/dashboardTiles/help/help_welcome.html",
			    active : true,
			    position : "full"			    
		    },  
		    {
		    	   id : "tasks",
		    	   template : "/views/shared/dashboardTiles/tasks/tasks.html",
		    	 
		    	   active : true,
		    	   position : "small"
		    },
		    /*
		    {
		    	   id : "createrecord",
		    	   template : "/views/members/dashboardTiles/createrecord/createrecord.html",
		    	   title : "Create a new record",
		    	   active : true,
		    	   position : "small",		    	   		    	   
		    	   setup : { allowSelection : true }
		    },
		    */
			{
			    id: "myviews",
			
			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
			    active : true,
			    position : "small",
			    setup : {	
			    	 context : "me",		        	 
			    	 always : true,
			    	 visualizations : []
			    }
			}
		  ],
	  "overview" :
		  [
			{
			    id: "mydata",
			
			    template : "/views/shared/dashboardTiles/summary/summary.html",
			    active : true,
			    position : "small",
			    setup : {
			   	 text : "dashboard.mydata_text",
			   	 link : "member.dashboard({ dashId : 'mydata'})",
			   	 icon : "/images/icons/health.png",			   	 
			   	 button : "dashboard.view_btn"
			    }
			},
		    {
		     id: "social",
		  
		     template : "/views/shared/dashboardTiles/summary/summary.html",
	         active : true,
	         position : "small",
	         setup : {
	        	 text : "dashboard.social_text",
	        	 icon : "/images/icons/social.png",
	        	 link : "member.dashboard({ dashId : 'social'})",	        	 
	        	 button : "dashboard.view_btn"
	         }
		    },
		    {
			     id: "research",
			
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "dashboard.research_text",
		        	 link : "member.dashboard({ dashId : 'studies'})",
		        	 icon : "/images/icons/research.png",
		        	 button : "dashboard.view_btn"
		         }
			},
			
			{
			     id: "health",
			   
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "dashboard.health_text",
		        	 link : "member.dashboard({ dashId : 'providers'})",		        	 
		        	 icon : "/images/icons/empower.png",
		        	 button : "dashboard.view_btn"
		         }
			},
			{
			     id: "help",
			  
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "dashboard.help_text",
		        	 link : "member.dashboard({ dashId : 'help'})",
		        	 icon : "/images/icons/help.png",
		        	 button : "dashboard.view_btn"
		         }
			}/*,
			{
			     id: "training",
			   
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Manage your personal training!",
		        	 link : "member.dashboard({ dashId : 'training'})",
		        	 icon : "/images/icons/training.png",
		        	 button : "dashboard.view_btn"
		         }
			}*/
		  ],
      "help" : 
    	  [
				{
				    id: "help_welcome",
				   
				    template : "/views/members/dashboardTiles/help/help_welcome.html",
				    active : true,
				    position : "full"			    
				}
    	  ],
	  "config" : 
			  [
			   {
				    id: "help_welcome",
				   
				    template : "/views/members/dashboardTiles/help/help_welcome.html",
				    active : true,
				    position : "full"			    
			    },
			    {
				    id: "myviews",
				  
				    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
				    active : true,
				    position : "small",
				    setup : {	
				    	 context : "config",		        	 
			        	 always : false
			        }
				}
			  ],  
      "mydata" : [
			 
		    {
		    	id : "newrecords",
		    	template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
		    	
		    	active : true,
		    	position : "small",
		    	actions : { remove : "config" },
		    	teaser : "dashboard.newrecords_teaser",
		    	setup : { properties : { "max-age" : 86400 * 31, "limit" : 7 } , fields : [ "ownerName", "created", "id", "name" ], allowBrowse : true}
		    },
		    /*
		    {
		    	   id : "createrecord",
		    	   template : "/views/members/dashboardTiles/createrecord/createrecord.html",
		    	   title : "Create a new record",
		    	   active : true,
		    	   position : "small",		    	   
		    	   teaser : { link : "^.market", button : "Visit Market", text : "To get started we recommend to install some applications from our market place!" },
		    	   setup : { allowSelection : true }
		     },*/
		     {
				    id: "myaccount",
				   
				    template : "/views/members/dashboardTiles/accountdata/accountdata.html",
				    active : true,
				    position : "small"			    
			},   
			{
			    id: "myviews",
			  
			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
			    active : true,
			    position : "small",
			    setup : {	
			    	 context : "mydata",		        	 
		        	 always : true
		        }
			}
          ],
      "social" : [
            {
            	id : "newsharedrecords",
		    	template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
		    
		    	active : true,
		    	position : "small",
		    	teaser : "dashboard.newsharedrecords_teaser",
		    	actions : { remove : "config" },
		    	setup : { properties : { "max-age" : 86400 * 31, "owner" : "members" } , fields : [ "ownerName", "owner", "created", "id", "name" ], allowBrowse : true }
		
		    },		    
			 {
			     id: "circles",
			  
			     template : "/views/members/dashboardTiles/othercircles/othercircles.html",
		         active : true,
		         position : "small",
		         teaser : "dashboard.circles_teaser",
		         setup : {
		        	 properties : { member : true },
		        	 instances : true
		         }
			 },
			 {
			     id: "circles2",
			   
			     template : "/views/members/dashboardTiles/circles/circles.html",
		         active : true,
		         position : "small",
		         teaser : "dashboard.circles2_teaser",
		         setup : {
		        	 properties : { owner : true }
		         }
			 },
			{
			     id: "messages",
			     title: "Messages",
			     template : "/views/shared/dashboardTiles/messages/messages.html",
		         active : true,
		         position : "small",
		         teaser : "Use our email system to safely communicate with other MIDATA members."
			},
			{
			     id: "gaming",
			     title: "Challenges",
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Compete with others for fun. Neither specified nor implemented.",		        	 
		        	 icon : "/images/icons/gaming.png"
		        	     
		         }
			},
			{
			     id: "barometer",
			     title: "Sharing Barometer",
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Neither specified nor implemented."		    
		         }
			},
			{
			    id: "myviews",
			  
			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
			    active : true,
			    position : "small",
			    setup : {	
			    	 context : "social",		        	 
		        	 always : true
		        }
			}
          ],
	  "providers" :
		  [
		   {
	    	 id : "providers",
	    	            
	         template : "/views/members/dashboardTiles/providers/providers.html",
	         active : true,
	         position : "small",
	         teaser : "dashboard.providers_teaser",
	         links : { "click-provider" : "records" }
	       },
	       {
			     id: "providersearch",
			 
			     template : "/views/shared/dashboardTiles/providersearch/providersearch.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "dashboard.providersearch_text",
		        	 icon : "/images/icons/search.png"
		         }
			},
	       {
	    	 id : "hcrecords",
	    	
	    	 template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
	    	 active : false,
	    	 position : "small",
	    	 links : { "record" : "details" }
	       },
	       {                        
	         id : "records",
	         template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
	       
	         position : "modal",
	         active : false,
	         links : { "shareFrom" : "share", "record" : "details" }
	       },
	       {
			    id: "myviews",
			 
			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
			    active : true,
			    position : "small",
			    setup : {	
			    	 context : "providers",		        	 
		        	 always : true
		        }
			},
	       {                        
		      id : "share",
		      template : "/views/members/dashboardTiles/addrecords/search.html",
		    
		      position : "modal",
		      active : false,
		      links : { "record" : "details" },
		      dependend : [ "records" ]
		   },
		   {                        
			  id : "details",
			  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
			 
			  active : false,
			  position : "modal"			 
		   }
	     ],
	  "studies" : [
	       {
	    	   id : "yourstudies",
	    	   template : "/views/members/dashboardTiles/yourstudies/yourstudies.html",
	    	  
	    	   active : true,
	    	   teaser : "dashboard.studies_teaser",
	    	   position : "small"
	       },
	       {
	    	   id : "newstudies",
	    	   template : "/views/shared/dashboardTiles/flexiblestudies/flexiblestudies.html",
	    	 
	    	   active : true,
	    	   position : "small",
	    	   setup : { properties : { }, fields : ["name"] }
	       },
	       {
			     id: "researchnews",
			   
			     template : "/views/shared/dashboardTiles/summary/summary.html",
		         active : true,
		         position : "small",
		         setup : {
			         text : "dashboard.researchnews_text",
		        	 icon : "/images/icons/info.png",
		        	 link : "http://scholar.google.de/scholar?q=Health+study+publications",	        	 
		        	 button : "dashboard.researchnews_btn"
		         }
			},
	       {
			     id: "studysettings",
			   
			     template : "/views/members/dashboardTiles/researchsettings/researchsettings.html",
		         active : true,
		         position : "small"		         
			},
	       {
	    	   id :"entercode",
	    	   template : "/views/members/dashboardTiles/entercode/entercode.html",
	    	 
	    	   active : true,
	    	   position : "small"
	       }
	     ],
	     "studiessmall" : [
				  {
					   id : "newsconfig",
					   template : "/views/shared/dashboardTiles/newsconfig/newsconfig.html",					 
					   active : true,
					   teaser : "dashboard.newsconfig_teaser",
					   position : "full",
					   setup : {}
				   },
	       	       {
	       	    	   id : "yourstudies",
	       	    	   template : "/views/members/dashboardTiles/yourstudies/yourstudies.html",
	       	    	 
	       	    	   active : true,
	       	    	   teaser : "dashboard.yourstudies_teaser",
	       	    	   position : "full"
	       	       },
	       	       {
	       	    	   id : "newstudies",
	       	    	   template : "/views/shared/dashboardTiles/flexiblestudies/flexiblestudies.html",
	       	    	 
	       	    	   active : true,
	       	    	   position : "full",
	       	    	   setup : { properties : { }, fields : ["name"] }
	       	       }
	     ],
	 "circles" : [
			{
				   id : "records_shared",
				   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
				
				   active : false,
				   position : "full",
				   links : { "record" : "details" }
				   
			},			
		    {                        
			      id : "share",
			      template : "/views/members/dashboardTiles/addrecords/search.html",
			  
			      position : "modal",
			      active : false,
			      links : { "record" : "details" },
			      dependend : [ "records_shared" ]
			},
			{                        
				  id : "details",
				  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
				 
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "addusers",
				  template : "/views/shared/dashboardTiles/addusers/addusers.html",
				 
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "providersearch",
				  template : "/views/shared/providersearch/providersearch.html",
				
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "usergroupsearch",
				  template : "/views/shared/usergroupsearch/usergroupsearch.html",				
				  active : false,
				  position : "modal"			 
			}
			
	 ],
	 "usergroup" : [			
			{                        
				  id : "addusers",
				  template : "/views/shared/dashboardTiles/addusers/addusers.html",
				 
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "providersearch",
				  template : "/views/shared/providersearch/providersearch.html",
				
				  active : false,
				  position : "modal"			 
			}
			
	 ],
	 "studydetails" : [
	        {
				   id : "shared_with_study",
				   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
				 
				   active : false,
				   position : "full",
				   links : { "shareFrom" : "share", "record" : "details" }
				   
			},			
		    {                        
			      id : "share",
			      template : "/views/members/dashboardTiles/addrecords/search.html",
			    
			      position : "modal",
			      active : false,
			      links : { "record" : "details" },
			      dependend : [ "shared_with_study" ]
			},
			{                        
				  id : "details",
				  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
				 
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "providersearch",
				  template : "/views/shared/providersearch/providersearch.html",
				
				  active : false,
				  position : "modal"			 
			}
			
	 ],
	 "memberdetails" : [
			{
				   id : "patient_records",
				   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
				 
				   active : false,
				   position : "full",
				   links : { "shareFrom" : "share" }				   
				   
			},
			 {                        
			      id : "share",
			      template : "/views/members/dashboardTiles/addrecords/search.html",
			    
			      position : "modal",
			      active : false,
			      links : { "record" : "details" },
			      dependend : [ "patient_records" ]
			},
			{                        
				  id : "details",
				  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
				
				  active : false,
				  position : "modal"			 
			},
			{                        
				  id : "addtask",
				  template : "/views/shared/dashboardTiles/addtask/addtask.html",
				
				  active : false,
				  position : "modal"			 
			}              
	 ],
	 "studyactions" : [
	        			{
	        				   id : "group_records",
	        				   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
	        				  
	        				   active : false,
	        				   position : "full",
	        				   links : { "shareFrom" : "share", "record" : "details" }				   
	        				   
	        			},
	        			 {                        
	        			      id : "share",
	        			      template : "/views/members/dashboardTiles/addrecords/search.html",
	        			  
	        			      position : "modal",
	        			      active : false,
	        			      links : { "record" : "details" },
	        			      dependend : [ "1" ]
	        			},
	        			{                        
	        				  id : "details",
	        				  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
	        				
	        				  active : false,
	        				  position : "modal"			 
	        			},
	        			{                        
	        				  id : "addtask",
	        				  template : "/views/shared/dashboardTiles/addtask/addtask.html",
	        				 
	        				  active : false,
	        				  position : "modal"			 
	        			}              
	        	 ],
	 "studyparticipant" : [
			{
				   id : "study_records",
				   template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
				  
				   active : false,
				   position : "full",
				   links : { "record" : "details" }
				   
			},					
			{                        
				  id : "details",
				  template : "/views/shared/dashboardTiles/recorddetail/recorddetail.html",
				
				  active : false,
				  position : "modal"			 
			}   
	 ],
	 "training" : [				 	 		   	 		    
	 			{
	 			    id: "editor",
	 			    title: "Plan Editor",
	 			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
	 			    active : true,
	 			    position : "small",
	 			    setup : {
	 			    	 context : "training",
	 			    	 always : true,
	 			    	 visualizations : [
	 			    	   { 
	 			    		 id : "554c867de4b08413ff37d6f5", 
	 			    		 title : "Training Editor", 
	 			    		 teaser : "Plan your training using our training app.", 
	 			    		 spaces : [ {
	 			    			name : "Training Editor",
	 			    			context : "training",
	 			    			query : { 
	 			    				content : ["calendar/trainingplan"]
	 			    			}
	 			    		 } ] 
	 			    	   },
	 			    	   { 
	 			    		 id : "5550872fe4b042b13ab4c95c", 
	 			    	     title : "Training Diary", 
	 			    	     teaser : "Control your training using the training diary!",
	 			    	     spaces : [ {
	 			    	    	 name : "Training Diary",
	 			    	    	 context : "training",
	 			    	    	 query : {
	 			    	    		 content : [ "calendar/trainingplan", "activities/minutes-active", "body/weight", "sleep/time-in-bed", "activities/heartrate" ]
	 			    	    	 }	 			    	    	 
	 			    	     } ]
	 			    	   },
	 			    	   {
	 			    		   id : "5571700fe4b0c309cbc949ce",
	 			    		   title : "Performance Charts",
	 			    		   teaser : "View your training performance and compare results with others!",
	 			    		   spaces : [
	 			    		      {
	 			    		    	  name : "Your Performance",
	 			    		    	  context : "training",
	 			    		    	  query : {
	 			    		    		  content : [ "activities/minutes-active", "body/weight", "sleep/time-in-bed", "activities/heartrate" ],
	 			    		    		  owner : "self"
	 			    		    	  }
	 			    		      },
	 			    		      {
	 			    		    	  name : "Comparison",
	 			    		    	  context : "training",
	 			    		    	  query : {
	 			    		    		 format : [ "activities/minutes-active", "body/weight", "sleep/time-in-bed", "activities/heartrate" ]	 			    		    		 
	 			    		    	  }
	 			    		      }
	 			    		   ]
	 			    	   }
	 			    	 ]	 		        	 
	 		        }
	 			}
	           ],	          
	      	 "qself" : [				 	 		   	 		    
	      	 			{
	      	 			    id: "editor",
	      	 			    title: "Quantified Self",
	      	 			    template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
	      	 			    active : true,
	      	 			    position : "small",
	      	 			    setup : {
	      	 			    	 context : "qself",
	      	 			    	 always : true
	      	 		        }
	      	 			}
	      	           ],
	      	           
	      	   // Providers
  	 	  "research" : [
  	 	       
  	 	       {
  	 	    	   id : "newstudies",
  	 	    	   template : "/views/shared/dashboardTiles/flexiblestudies/flexiblestudies.html",
  	 	    	
  	 	    	   active : true,
  	 	    	   position : "small",
  	 	    	   setup : { properties : { }, fields : ["name"] }
  	 	       },
  	 	       {
  	 			     id: "researchnews",
  	 			  
  	 			     template : "/views/shared/dashboardTiles/summary/summary.html",
  	 		         active : true,
  	 		         position : "small",
  	 		         setup : {
  	 			         text : "dashboard.researchnews_text",
  	 		        	 icon : "/images/icons/info.png",
  	 		        	 link : "http://scholar.google.de/scholar?q=Health+study+publications",	        	 
  	 		        	 button : "dashboard.researchnews_btn"
  	 		         }
  	 			}  	 	      
  	 	       ],
  	 	       "sandbox" : [

                {
                	id : "newrecords",
                	template : "/views/shared/dashboardTiles/flexiblerecords/flexiblerecords.html",
                	
                	active : true,
                	position : "small",  	 			    	
                	teaser : "dashboard.newrecords_teaser",
                	setup : { properties : { "max-age" : 86400 * 31, "limit" : 7 } , fields : [ "ownerName", "created", "id", "name" ], allowBrowse : true}
                },
                /*
                {
                	id : "createrecord",
                	template : "/views/members/dashboardTiles/createrecord/createrecord.html",
                	title : "Create a new record",
                	active : true,
                	position : "small",		    	   
                	teaser : { link : "^.yourapps", button : "Your Apps", text : "Install Plugins you developed." },
                	setup : { allowSelection : true }
                },  
                */	 			       
                {
                	id: "myviews",
                
                	template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
                	active : true,
                	position : "small",
                	setup : {	
                		context : "sandbox",		        	 
                		always : true
                	}
                }
                ],
                "workspace" : [
             /*
              {
            	  id : "createrecord",
            	  template : "/views/members/dashboardTiles/createrecord/createrecord.html",
            	  title : "Create a new record",
            	  active : true,
            	  position : "small",		    	   
            	  teaser : { link : "^.market({ context : 'workspace' })", button : "Install", text : "Install Plugins" },
            	  setup : { allowSelection : true }
              },*/  	 			       
              {
            	  id: "myviews",
            	
            	  template : "/views/shared/dashboardTiles/viewconfig/viewconfig.html",
            	  active : true,
            	  position : "small",
            	  setup : {	
            		  context : "workspace",		        	 
            		  always : true
            	  }
              }
              ]
	}			
)
.controller('DashboardCtrl', ['$scope', '$timeout', '$state', 'views', 'dashboards', 'tiles', 'spaces', 'portal', 'session', function($scope, $timeout, $state, views, dashboards, tiles, spaces, portal, session) {  
	   
	  $scope.isReady = false;
	  views.reset();
	  views.isreset = false;
	   views.layout = $scope.layout = {
	     full: [], small:[], modal:[], line:[], page:[]
	   };
	   var dashid = $state.params.dashId || $state.current.dashId;
	   views.context = dashid;
	   
	   console.log(dashid);
	   /*
		 * _.each(dashboards[dashid], function(view) {
		 * $scope.layout[view.position].push(views.def(view)); });
		 */
	   
	   portal.getConfig()
	   .then(function(res) {
		  var conf = { add : [], remove : [] };
		  if (res.data && res.data.dashboards && res.data.dashboards[dashid]) {
			  conf = res.data.dashboards[dashid];
		  }
			 
		  _.each(dashboards[dashid], function(view) {
			  console.log("VIEW:"+view.id);			  
			  if (conf.remove.indexOf(view.id) < 0) {
				  console.log("ADDED");
				  $scope.layout[view.position].push(views.def(view));
			  }
		  });
		  
		  var vc = views.getView("myviews");
		  if (vc != null) {			
			 if (!vc.setup) { vc.setup = {}; }
			 vc.setup.visualizations = [];
		  }
		  
		  _.each(conf.add, function(add) {
			 var tile = tiles[add];
			 tile.name = add;
			 if (tile.template != null) {
				console.log("ADD VIEW:"+tile);
			   $scope.layout[tile.position].push(views.def(tile));
			 } else {				
				 if (vc != null) {
					 console.log("ADD TILE");
					 console.log(tile);
					 vc.setup.visualizations.push(tile);
				 }
			 }
		  });
			
		  $timeout(function () { $scope.isReady = true; }, 500);
	   });
	   
	   $scope.makeBig = function(view) {
		 /*if (view.makeBig != null) {
			 view.makeBig();
		 } else {*/		   
			 $scope.layout.small.splice($scope.layout.small.indexOf(view), 1);
			 $scope.layout.full.push(view);
			 view.position = "full";
		 //}
	   };
	   
	   $scope.makeSmall = function(view) {
			 $scope.layout.full.splice($scope.layout.full.indexOf(view), 1);
			 $scope.layout.small.push(view);
			 view.position = "small";
	   };
	   
	   $scope.remove = function(view) {
		   if (view.position == "small") {
			   $scope.layout.small.splice($scope.layout.small.indexOf(view), 1);
		   } else if (view.position == "full") {
			   $scope.layout.full.splice($scope.layout.full.indexOf(view), 1);
		   }
		   
		   if (view.actions.remove && view.actions.remove.space) {
			   spaces.deleteSpace(view.actions.remove.space);
		   } 
		   
		   if (view.actions.remove === "config") {
			   portal.remove(views.context, view.name);
		   }
	   };
	   
	   $scope.filterEvenStartFrom = function (index) {
		    return function (item) {
		        return index++ % 2 == 1;
		    };
	   };
	   
	   $scope.$on('$messageIncoming', function (event, data){	
		 if (data && data.viewHeight && data.viewHeight !== "0px" && data.name) {			 
			 if (views.getView(data.name).pos == "small") return;
  			 console.log("adjust height for "+data.name+" to:"+data.viewHeight);
  		   	 document.getElementById(data.name).style.height = data.viewHeight;
  		 }
		 else if (data && data.type==="link") {
			 
			if (data.app) {		
				session.currentUser.then(function(userId) {
				spaces.openAppLink($state, userId, data);
				});
				return;
			}
			 
  			var vc = views.getView(data.name);
  			 
  			console.log("link");
  			console.log(data);
  		   
  			var pos = data.pos;  	    	 
  	    	var spacedef =
  		     {
  		    	   id : "gen"+(new Date()).getTime(),
  		    	   template : "/views/shared/dashboardTiles/spacesummary/spacesummary.html",
  		    	   title : vc.title,
  		    	   active : true,
  		    	   position : pos,
  		    	   actions : {  },
  		    	   setup : { allowSelection : false, info : vc.attach, url : data.url, params : data.params, showview:false, showadd:false }
  		     };
  		     views.layout[pos].push(views.def(spacedef));   			
  			
  		 } else if (data && data.type==="set") {
  			var vset = views.getView(data.name);
  			var func = data.func;
  			console.log("SET");
  			console.log(data);
  			vset.actions[func] = data;  
  			if (data.func === "view" && data.pos === "hide") vset.setup.showview = false;
  		 }
		 else if (data && data.type==="close") {
			views.disableView(data.name);
		 } else if (data && data.type=="update") {
			 console.log("UPDATE");
			angular.forEach(views.layout.small, function(v) {
				if (v.reload) v.reload();
				//console.log(v.id);
				//views.update(v.id);
			});
			angular.forEach(views.layout.full, function(v) {
				if (v.reload) v.reload();
				//views.update(v.id);
			});
		 }
  	  });
}]);
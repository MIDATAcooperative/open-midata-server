var dashboards = angular.module('dashboards', [ 'services', 'views' ]);
dashboards.constant("dashboards",
	{
	  "overview" :
		  [
			{
			    id: "mydata",
			    title: "Your personal data",
			    template : "/assets/views/members/info/summary.html",
			    active : true,
			    position : "small",
			    setup : {
			   	 text : "Manage your health data. Enter new data and visualize existing data.",
			   	 link : "/members/dashboard/mydata",
			   	 icon : "/assets/images/icons/empower.png",
			   	 button : "Your Data Dashboard"
			    }
			},
		    {
		     id: "social",
		     title: "Social",
		     template : "/assets/views/members/info/summary.html",
	         active : true,
	         position : "small",
	         setup : {
	        	 text : "Share your data with friends. Communicate with people sharing your health situation. Motivate yourself by competition with others.",
	        	 icon : "/assets/images/icons/social.png",
	        	 link : "/members/dashboard/social",	        	 
	        	 button : "Social Dashboard"
	         }
		    },
		    {
			     id: "research",
			     title: "Research",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Participate in studies. Inform yourself about ongoing research.",
		        	 link : "/members/dashboard/studies",
		        	 icon : "/assets/images/icons/research.png",
		        	 button : "Research Dashboard"
		         }
			},
			
			{
			     id: "health",
			     title: "Health",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Share data with your doctor. Search for health professionals.",
		        	 link : "/members/dashboard/providers",
		        	 icon : "/assets/images/icons/health.png",
		        	 button : "Health Professional Dashboard"
		         }
			},
			{
			     id: "training",
			     title: "Training",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Manage your personal training!",
		        	 link : "/members/dashboard/training",
		        	 icon : "/assets/images/icons/health.png",
		        	 button : "Training Dashboard"
		         }
			}
		  ],
      "mydata" : [
			 
		    {
		    	id : "newrecords",
		    	template : "/assets/views/members/flexiblerecords.html",
		    	title : "Newest records added this month:",
		    	active : true,
		    	position : "small",
		    	actions : { big : "/members/records" },
		    	teaser : "There are no new records. Visit the records page to browser all of your records.",
		    	setup : { properties : { "max-age" : 86400 * 31, "limit" : 7 } , fields : [ "ownerName", "created", "id", "name" ], allowBrowse : true}
		    },
		    {
		    	   id : "createrecord",
		    	   template : "/assets/views/members/createrecord.html",
		    	   title : "Create a new record",
		    	   active : true,
		    	   position : "small",		    	   
		    	   teaser : { link : "/members/market", button : "Visit Market", text : "To get started we recommend to install some applications from our market place!" },
		    	   setup : { allowSelection : true }
		     },
		     {
				    id: "myaccount",
				    title: "My Account Data",
				    template : "/assets/views/members/accountdata.html",
				    active : true,
				    position : "small"			    
			},   
			{
			    id: "myviews",
			    title: "My Views",
			    template : "/assets/views/members/viewconfig.html",
			    active : true,
			    position : "small",
			    setup : {
		        	 text : "Add Visualizations to improve your experience!",
		        	 link : "/members/market",
		        	 icon : "/assets/images/icons/eye.png",
		        	 button : "Market Place"
		        }
			}
          ],
      "social" : [
            {
            	id : "newsharedrecords",
		    	template : "/assets/views/members/flexiblerecords.html",
		    	title : "New records shared with you",
		    	active : true,
		    	position : "small",
		    	teaser : "Other MIDATA members may share their data with you.",
		    	actions : { big : "/members/records" },
		    	setup : { properties : { "max-age" : 86400 * 31, "set" : "circles" } , fields : [ "ownerName", "owner", "created", "id", "name" ], allowBrowse : true }
		
		    },		    
			 {
			     id: "circles",
			     title: "Member of Circles",
			     template : "/assets/views/members/othercircles.html",
		         active : true,
		         position : "small",
		         teaser : "Ask others to add you to their circles.",
		         setup : {
		        	 properties : { member : true },
		        	 instances : true
		         }
			 },
			 {
			     id: "circles2",
			     title: "Your Circles",
			     template : "/assets/views/members/circles.html",
		         active : true,
		         position : "small",
		         teaser : "Create Circles to share your data with others. Share with friends, family or others.",
		         setup : {
		        	 properties : { owner : true }
		         }
			 },
			{
			     id: "messages",
			     title: "Messages",
			     template : "/assets/views/members/messages.html",
		         active : true,
		         position : "small",
		         teaser : "Use our email system to safely communicate with other MIDATA members."
			},
			{
			     id: "gaming",
			     title: "Challenges",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Compete with others for fun. Neither specified nor implemented.",		        	 
		        	 icon : "/assets/images/icons/gaming.png"
		        	     
		         }
			},
			{
			     id: "barometer",
			     title: "Sharing Barometer",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Neither specified nor implemented."		    
		         }
			}
          ],
	  "providers" :
		  [
		   {
	    	 id : "providers",
	    	 title:"Your Health Providers",                 
	         template : "/assets/views/members/providers.html",
	         active : true,
	         position : "small",
	         teaser : "Search for healthcare providers and add them to your list in order to share data with them.",
	         links : { "click-provider" : "records" }
	       },
	       {
			     id: "providersearch",
			     title: "Search for Health Providers",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
		        	 text : "Search function for healthcare providers not yet implemented.",
		        	 icon : "/assets/images/icons/search.png"
		         }
			},
	       {
	    	 id : "hcrecords",
	    	 title : "Newest Records from your healthcare providers",
	    	 template : "/assets/views/members/flexiblerecords.html",
	    	 active : false,
	    	 position : "small",
	    	 links : { "record" : "details" }
	       },
	       {                        
	         id : "records",
	         template : "/assets/views/members/flexiblerecords.html",
	         title : "Records",
	         position : "modal",
	         active : false,
	         links : { "shareFrom" : "share", "record" : "details" }
	       },
	       {                        
		      id : "share",
		      template : "/assets/views/members/search.html",
		      title : "Share",
		      position : "modal",
		      active : false,
		      links : { "record" : "details" },
		      dependend : [ "records" ]
		   },
		   {                        
			  id : "details",
			  template : "/assets/views/members/record.html",
			  title : "Details",
			  active : false,
			  position : "modal"			 
		   }
	     ],
	  "studies" : [
	       {
	    	   id : "yourstudies",
	    	   template : "/assets/views/members/research/yourstudies.html",
	    	   title : "Studies you are involved:",
	    	   active : true,
	    	   teaser : "If you want to participate in research configure your research settings. You will be contacted in this tile if you match a studies requirements.",
	    	   position : "small"
	       },
	       {
	    	   id : "newstudies",
	    	   template : "/assets/views/members/research/flexiblestudies.html",
	    	   title : "New Studies",
	    	   active : true,
	    	   position : "small",
	    	   setup : { properties : { }, fields : ["name"] }
	       },
	       {
			     id: "researchnews",
			     title: "Research News",
			     template : "/assets/views/members/info/summary.html",
		         active : true,
		         position : "small",
		         setup : {
			         text : "Search for new health research publications.",
		        	 icon : "/assets/images/icons/info.png",
		        	 link : "http://scholar.google.de/scholar?q=Health+study+publications",	        	 
		        	 button : "Search for News"
		         }
			},
	       {
			     id: "studysettings",
			     title: "My Research Settings",
			     template : "/assets/views/members/research/researchsettings.html",
		         active : true,
		         position : "small"		         
			},
	       {
	    	   id :"entercode",
	    	   template : "/templates/entercode",
	    	   title : "Join by participation code",
	    	   active : true,
	    	   position : "small"
	       }
	     ],
	
	 "circles" : [
			{
				   id : "1",
				   template : "/assets/views/members/flexiblerecords.html",
				   title : "Records shared with this circle:",
				   active : false,
				   position : "full",
				   links : { "shareFrom" : "share", "record" : "details" }
				   
			},			
		    {                        
			      id : "share",
			      template : "/assets/views/members/search.html",
			      title : "Share",
			      position : "modal",
			      active : false,
			      links : { "record" : "details" },
			      dependend : [ "1" ]
			},
			{                        
				  id : "details",
				  template : "/assets/views/members/record.html",
				  title : "Details",
				  active : false,
				  position : "modal"			 
			}
	 ],
	 "studydetails" : [
	        {
				   id : "1",
				   template : "/assets/views/members/flexiblerecords.html",
				   title : "Shared Records:",
				   active : false,
				   position : "full",
				   links : { "shareFrom" : "share", "record" : "details" }
				   
			},			
		    {                        
			      id : "share",
			      template : "/assets/views/members/search.html",
			      title : "Share",
			      position : "modal",
			      active : false,
			      links : { "record" : "details" },
			      dependend : [ "1" ]
			},
			{                        
				  id : "details",
				  template : "/assets/views/members/record.html",
				  title : "Details",
				  active : false,
				  position : "modal"			 
			}
	 ],
	 "memberdetails" : [
			{
				   id : "1",
				   template : "/assets/views/members/flexiblerecords.html",
				   title : "Records of this patient:",
				   active : true,
				   position : "full",
				   links : { "record" : "details" }
				   
			},					
			{                        
				  id : "details",
				  template : "/assets/views/members/record.html",
				  title : "Details",
				  active : false,
				  position : "modal"			 
			}            
	 ],
	 "studyparticipant" : [
			{
				   id : "1",
				   template : "/assets/views/members/flexiblerecords.html",
				   title : "Shared records:",
				   active : true,
				   position : "full",
				   links : { "record" : "details" }
				   
			},					
			{                        
				  id : "details",
				  template : "/assets/views/members/record.html",
				  title : "Details",
				  active : false,
				  position : "modal"			 
			}   
	 ],
	 "training" : [				 	 		   	 		    
	 			{
	 			    id: "editor",
	 			    title: "Plan Editor",
	 			    template : "/assets/views/members/viewconfig.html",
	 			    active : true,
	 			    position : "small",
	 			    setup : {
	 			    	 context : "training",
	 			    	 visualizations : [
	 			    	   { 
	 			    		 id : "554c867de4b08413ff37d6f5", 
	 			    		 title : "Training Editor", 
	 			    		 teaser : "Plan your training using our training app.", 
	 			    		 spaces : [ {
	 			    			name : "Training Editor",
	 			    			context : "training",
	 			    			query : { 
	 			    				format : "trainingplan"
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
	 			    	    		 format : [ "trainingplan" ]
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
	 			    		    		  format : [],
	 			    		    		  set : "user"
	 			    		    	  }
	 			    		      },
	 			    		      {
	 			    		    	  name : "Comparison",
	 			    		    	  context : "training",
	 			    		    	  query : {
	 			    		    		 format : [],
	 			    		    		 set : ["user", "circles"]
	 			    		    	  }
	 			    		      }
	 			    		   ]
	 			    	   }
	 			    	 ]	 		        	 
	 		        }
	 			}
	           ]
	}			
);
dashboards.controller('DashboardCtrl', ['$scope', '$attrs', 'views', 'dashboards', function($scope, $attrs, views, dashboards) {  
	   
	   views.layout = $scope.layout = {
	     full: [], small:[], modal:[]
	   };
	   _.each(dashboards[$attrs.dashid], function(view) {
		  $scope.layout[view.position].push(views.def(view)); 
	   });
	   
	   $scope.makeBig = function(view) {
		 $scope.layout.small.splice($scope.layout.small.indexOf(view), 1);
		 $scope.layout.full.push(view);
		 view.position = "full";
	   };
	   
	   $scope.makeSmall = function(view) {
			 $scope.layout.full.splice($scope.layout.full.indexOf(view), 1);
			 $scope.layout.small.push(view);
			 view.position = "small";
	   };
	   
	   $scope.filterEvenStartFrom = function (index) {
		    return function (item) {
		        return index++ % 2 == 1;
		    };
	   };
}]);
var dashboards = angular.module('dashboards', [ 'services', 'views' ]);
dashboards.constant("dashboards",
	{
	  "providers" :
		  [
		   {
	    	 id : "providers",
	    	 title:"Your Health Providers",                 
	         template : "/assets/views/members/providers.html",
	         active : true,
	         position : "small"
	       },
	       {
	    	 id : "hcrecords",
	    	 title : "Records added this month by your healthcare providers",
	    	 template : "/assets/views/members/flexiblerecords.html",
	    	 active : false,
	    	 position : "small",
	    	 links : { "record" : "details" }
	       },
	       {                        
	         id : "records",
	         template : "/assets/views/members/flexiblerecords.html",
	         title : "Records",
	         position : "full",
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
	    	   id :"entercode",
	    	   template : "/templates/entercode",
	    	   title : "Join by participation code",
	    	   active : true,
	    	   position : "small"
	       }
	     ],
	 "dashboard" : [
	     {
	    	  id : "appteaser",
	    	  template : "/assets/views/members/info/appteaser.html",
	    	  title : "Welcome to MIDATA!",
	    	  active : true,
	    	  position : "small",
	    	  setup : { type : "noapps" }
	     },
	     {
	    	  id : "visualizationteaser",
	    	  template : "/assets/views/members/info/visualizationteaser.html",
	    	  title : "Add Visualizations!",
	    	  active : true,
	    	  position : "small",
	    	  setup : { type : "novisualizations" }
	     },
	     {
	    	  id : "whatsnew",
	    	  template : "/assets/views/members/info/whatsnew.html",
	    	  title : "What's new?",
	    	  active : true,
	    	  position : "small",
	    	  setup : { type : "news" }
	     },
	     {
	    	  id : "newstudies",
	    	  template : "/assets/views/members/research/flexiblestudies.html",
	    	  title : "New Studies:",
	    	  active : true,
	    	  position : "small",
	    	  setup : { properties : { }, fields : ["name"] }
	     },
	     {
	    	   id : "newrecords",
	    	   template : "/assets/views/members/flexiblerecords.html",
	    	   title : "Records added this month:",
	    	   active : true,
	    	   position : "small",
	    	   setup : { properties : { "max-age" : 86400 * 31 } , fields : [ "ownerName", "created", "id", "name" ]}
	     },
	     {
	    	   id : "showspace",
	    	   template : "/assets/views/members/showspace.html",
	    	   title : "View a space",
	    	   active : true,
	    	   position : "small",
	    	   setup : { allowSelection : true }
	     },
	     {
	    	   id : "createrecord",
	    	   template : "/assets/views/members/createrecord.html",
	    	   title : "Create a new record",
	    	   active : true,
	    	   position : "small",
	    	   setup : { xappId : "529f095fe4b035c062eb7ed4", allowSelection : true }
	     }
	     	     	     
	 ],
	 "circles" : [
			{
				   id : "1",
				   template : "/assets/views/members/flexiblerecords.html",
				   title : "Records shared with this circle:",
				   active : false,
				   position : "small",
				   links : { "shareFrom" : "share", "record" : "details" }
				   
			},			
		    {                        
			      id : "share",
			      template : "/assets/views/members/search.html",
			      title : "Share",
			      position : "small",
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
	 ]
	}			
);
dashboards.controller('DashboardCtrl', ['$scope', '$attrs', 'views', 'dashboards', function($scope, $attrs, views, dashboards) {  
	   
	   $scope.layout = {
	     full: [], small:[], modal:[]
	   };
	   _.each(dashboards[$attrs.dashid], function(view) {
		  $scope.layout[view.position].push(views.def(view)); 
	   });
	   
	   $scope.makeBig = function(view) {
		 $scope.layout.small.splice($scope.layout.small.indexOf(view), 1);
		 $scope.layout.full.push(view);
	   };
	   
	   $scope.makeSmall = function(view) {
			 $scope.layout.full.splice($scope.layout.full.indexOf(view), 1);
			 $scope.layout.small.push(view);
	   };
	   
	   $scope.filterEvenStartFrom = function (index) {
		    return function (item) {
		        return index++ % 2 == 1;
		    };
	   };
}]);
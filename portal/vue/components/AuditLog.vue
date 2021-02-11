<template>
<div v-if="log.filtered">
    <pagination v-model="log"></pagination>
			

	<div v-for="entry in log.filtered" :key="entry._id">
		<div class="row">
		    <div class="col-sm-6 col-md-2" style="color:#707070">{{ $filters.dateTime(entry.recorded)  }}</div>
		    <div class="col-sm-6 col-md-3">
   			    
			    <span class="fas" :class="{ 'fa-wrench text-danger' : entry.outcome == 12, 'fa-exclamation-triangle text-danger' : entry.outcome == 8, 'fa-times text-warning' : entry.outcome == 4, 'fa-check text-success' : entry.outcome == 0 }"></span> <b><span>{{ $t('enum.eventtype.'+entry.subtype[0].code) }}</span></b>			    
			    <div v-if="entry.extension && entry.extension.length && entry.extension[0].valueString">{{ $t(entry.extension[0].valueString) }}</div>	      			    			   
                <div v-else>{{ entry.outcomeDesc }}</div>		
			</div>
			<div class="col-sm-6 col-md-4">
			    <div>
                    <span v-if="entry.agent[0].name!='?'">{{ entry.agent[0].name }}</span>
                    <span v-else v-t="'auditlog.anonymous'"></span> 
                    <span v-if="entry.agent[0].role[0].coding[0].code != 'MEMBER'" class="badge badge-info">{{ $t('enum.userrole.'+entry.agent[0].role[0].coding[0].code) }}</span>
                </div>
			    <div class="text-primary">{{ entry.agent[0].altId }}</div>
			    <div v-if="entry.agent.length>1"><i><span v-t="'auditlog.via'"></span> {{ entry.agent[1].name }}</i></div>  
			</div>			  
			  
			<div class="col-sm-6 col-md-3">
			    <div v-for="entity in entry.entity" :key="entity._id">
			        <div v-if="entity.name">
			            <b v-if="entity.type.code"><span>{{ $t('auditlog.'+entity.type.code) }}</span>:</b>
			            <div>{{ entity.name }}</div>
			            <div class="text-primary">{{ entity.what.display }}</div>
			       </div>
			    </div>
			</div>
		</div>			  			  
		<div style="border-bottom: 1px solid #e0e0e0; margin-top:10px; margin-bottom:5px"></div>
	</div>
            
    <p v-if="log.filtered.length==0" v-t="'auditlog.empty'"></p>
</div>		
		   
</template>
<script>
/*
angular.module('portal')
.directive('auditlog', ['views', function (views) {
    return {
      templateUrl: 'assets/directives/auditlog.html',
      restrict: 'E',
      transclude: false, 
      scope : {
    	"patient" : "@",
    	"entity" : "@",
    	"altentity" : "@",
    	"all" : "@",
    	"from" : "=",
    	"to" : "=",
    	"api" : "="
      },
      controller : ['$scope', 'status', 'fhir', 'paginationService', function($scope, status, fhir, paginationService) {

    		$scope.status = new status(true);        	
    		
    		$scope.page = { nr : 1 };
    		var crit = {};
    	    
    		$scope.reload = function() {
    			
    			if ($scope.patient) crit.patient = $scope.patient;
    			if ($scope.entity && $scope.altentity) {
    				crit.entity = [$scope.entity, $scope.altentity];
    			} else if ($scope.entity) crit.entity = $scope.entity;
    			if ($scope.from && $scope.to) crit.date = ["sa"+$scope.from.toISOString(), "eb"+$scope.to.toISOString()];
    			
    			
    			if (!$scope.all && !$scope.patient && !$scope.entity && !$scope.from && !$scope.to) return;
    			crit._count=1001;
    			console.log(crit);    		    
    			$scope.status.doBusy(fhir.search("AuditEvent", crit))
    			.then(function(log) {
    				//if (!comeback) paginationService.setCurrentPage("membertable", 1); // Reset view to first page
    				$scope.log = log;
    				console.log($scope.log);
    			});
    			
    			
    		};    
    		
    		$scope.pageChanged = function(pn) {
    			if (pn == 101) {
    				crit._page = $scope.log[$scope.log.length-1].id;
    				console.log(crit);
    				$scope.status.doBusy(fhir.search("AuditEvent", crit))
        			.then(function(log) {
        				//if (!comeback) paginationService.setCurrentPage("membertable", 1); // Reset view to first page
        				$scope.log = log;
        				paginationService.setCurrentPage("audit", 1);        				
        				console.log($scope.log);
        			});
    			}
    		};
    		
    		var api = $scope.api || {};
    		api.reload = $scope.reload;
    		
    		$scope.$watchGroup(['patient','entity','altentity', 'from','to'], function() { $scope.reload(); });    		
    		if ($scope.all) $scope.reload();

    	}]
    };
}]);

*/

import rl from 'mixins/resultlist.js'
import fhir from "services/fhir";

export default {
    props: ['patient', 'entity', 'altentity', 'from', 'to'],

    data : ()=>({      
        title : "",
        log : {}
    }),

    components : { },

    mixins : [ rl ],

     computed: {
        range() {
          return `${this.from}|${this.to}`;
        },
      },

    methods : {
        reload() {
            const { $data }	= this, me = this;
            let crit = {};
    		if (me.patient) crit.patient = me.patient;
    		if (me.entity && me.altentity) {
    			crit.entity = [me.entity, me.altentity];
    		} else if (me.entity) crit.entity = me.entity;
    		if (me.from && me.to) crit.date = ["sa"+new Date(me.from).toISOString(), "eb"+new Date(me.to).toISOString()];
    			    			    		
    		crit._count=1001;
    		
    		fhir.search("AuditEvent", crit)
    		.then(function(log) {
    			$data.log = me.process(log);    				
    		});
    			
    			
    	}
    },

    watch : {
        range() { console.log("RE!");this.reload(); }
    },

    mounted() {
        this.reload();
    }
}
</script>
<template>
    <panel :title="$t('repository.title')" :busy="isBusy">
				
		<form name="myform" ref="myform" novalidate role="form" class="form-horizontal" :class="{ 'mark-danger' : app._id }" @submit.prevent="submit()">
            <error-box :error="error"></error-box>
		    <form-group name="filename" label="manageapp.filename" :path="errors.filename">
		        <input type="text" id="filename" name="filename" class="form-control"  v-validate v-model="app.filename" readonly>		    
		    </form-group>
		  
		    <form-group name="repository" label="repository.repository_url" :path="errors.repository">
		        <input type="text" id="repository" name="repository" class="form-control" v-validate v-model="app.repositoryUrl" required>		    
		    </form-group>
	  
	  	    <form-group name="repository_token" label="repository.repository_token" :path="errors.repository_token">
		        <input type="text" id="repository_token" name="repository_token" class="form-control" v-validate v-model="app.repositoryToken">		    
		    </form-group>

		    <form-group label="repository.repository_date">
		        <p v-if="app.repositoryDate" class="form-control-plaintext">{{ $filters.dateTime(app.repositoryDate) }}</p>		    		   
		        <p v-else class="form-control-plaintext" v-t="'repository.never'"></p>
		    </form-group>
	  
	        <form-group label="repository.status">
	            <ul class="list-group">
                    <li class="list-group-item" v-t="'repository.phase_provided'" :class="{ 'list-group-item-success' : report }"></li>
                    <li class="list-group-item" v-t="'repository.phase_sceduled'" :class="style('SCEDULED','COORDINATE','CHECKOUT')"></li>
                    <li class="list-group-item" v-t="'repository.phase_checkout'" :class="style('COORDINATE','CHECKOUT','INSTALL')"></li>
                    <li class="list-group-item" v-t="'repository.phase_install'" :class="style('CHECKOUT','INSTALL','AUDIT')"></li>
                    <li class="list-group-item" v-t="'repository.phase_audit'" :class="style('INSTALL','AUDIT','COMPILE')"></li>
                    <li class="list-group-item" v-t="'repository.phase_build'" :class="style('AUDIT','COMPILE','FINISHED')"></li>
                    <li class="list-group-item" v-t="'repository.phase_complete'" :class="style('COMPILE','FINISHED','XXX')"></li>
                </ul>
	        </form-group>
	    
	  
		    <form-group myid="x" label="common.empty">
		        <router-link :to="{ path : './manageapp', query :  {appId:app._id} }" class="btn btn-default mr-1" v-t="'common.back_btn'"></router-link>
		        <button type="button" class="btn btn-danger mr-1" @click="doDelete()" :disabled="!report" v-t="'common.delete_btn'"></button>		    
		        <button type="submit" :disabled="action!=null" class="btn btn-primary mr-1" v-t="'repository.submit_btn'">Submit</button>		    		    
		    </form-group>
		  
		  		  
	    </form>	  
	
    </panel>
		  
	<panel :title="getTitle()" :busy="isBusy">
				  		  
		<b v-t="'repository.server'"></b>:
		<select class="form-control" v-model="crit.sel">
            <option v-for="node in report.clusterNodes" :key="node" :value="dot(node)">{{ node }}</option>
        </select>
		<hr>
        <div v-if="report && report.checkoutReport">
            <div v-if="report.checkoutReport[crit.sel]">
                <b v-t="'repository.checkout'"></b>
                <pre>{{ report.checkoutReport[crit.sel] }}</pre>
            </div>
            
            <div v-if="report.installReport[crit.sel]">
                <b v-t="'repository.install'"></b>
                <pre>{{ report.installReport[crit.sel] }}</pre>
            </div>
            
            <div v-if="report.auditReport[crit.sel]">
                <b v-t="'repository.audit'"></b>
                <pre>{{ report.auditReport[crit.sel] }}</pre>
            </div>
            
            <div v-if="report.buildReport[crit.sel]">
                <b v-t="'repository.build'"></b>
                <pre>{{ report.buildReport[crit.sel] }}</pre>
            </div>
		  		  		 		
        </div>
	</panel>
</template>
<script>


import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import { status, ErrorBox, CheckBox, FormGroup } from 'basic-vue3-components'

let pull = null;

export default {

    data: () => ({	
        report : { clusterNodes : [] },
        appId : null,
        app : null,
        crit : { sel : null }
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $t, $filters, $data } = this;
            if (!$data.report) return $t('repository.log');
            return $t('repository.log')+" - "+$filters.dateTime($data.report.sceduled);		  	
        },

        loadApp(appId) {
            const { $data } = this, me = this;
            $data.appId=appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "repositoryUrl", "repositoryDate" ])
            .then(function(data) { 
                $data.app = data.data[0];			
            }));
	    },
	
	    loadReport(appId) {
            const { $data } = this, me = this;
            server.get(jsRoutes.controllers.Market.updateFromRepository($data.appId).url).then(function(result) {
                if (result.data && result.data.clusterNodes) {
                    $data.report = result.data;	
                    if (!$data.crit.sel && $data.report && $data.report.clusterNodes && $data.report.clusterNodes.length) {			   
                        $data.crit.sel = me.dot($data.report.clusterNodes[0]); 
                    }
                }
            });
				
	    },
	
	    dot(x) {
		    return x!=null ? x.replaceAll(".","[dot]") : "";
	    },
	
	    style(p,c,n) {
            const { $data } = this, me = this;
            if (!$data.report || !$data.report.status) return "list-group-item-light";
            if ($data.report.status.indexOf(c)>=0) {
                if ($data.report.status.indexOf("FAILED")>=0 && $data.report.status.indexOf(n)<0) {
                return "list-group-item-danger";
                } else {
                return "list-group-item-success";
                }
            } else if ($data.report.status.indexOf(p)>=0 && $data.report.status.indexOf("FAILED")<0) {
            return "active"; 
            } else {
            return "list-group-item-light";
            }
	    },
	
	    submit() {
		    const { $data } = this, me = this;
            $data.app.doDelete = undefined;
            me.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($data.app._id).url, $data.app)
            .then(function(result) {
                $data.report = result.data; 
            }));

	    },
	
        doDelete() {
		    const { $data } = this, me = this;
            $data.app.doDelete = true; 							
            me.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($data.app._id).url, $data.app))
            .then(function(result) {
                $data.report = result.data; 
            });

	    }
    },

    created() {
        const { $route, $data } = this, me = this;		
        $data.appId = $route.query.appId;
	    me.loadApp($data.appId);	
	
	    pull = window.setInterval(function() { me.loadReport($data.appId); }, 5000); // pull after 5 seconds
        	
	    me.loadReport($data.appId);
	
    },

    beforeUnmount() {
        if (pull) window.clearInterval(pull);
        pull = null;
    }
}
</script>
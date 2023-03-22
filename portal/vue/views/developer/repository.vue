<template>
    <panel :title="getTitle1()" :busy="isBusy">
				
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
		    
		    <form-group name="repository_dir" label="repository.repository_dir" :path="errors.repository_dir">
		        <input type="text" id="repository_dir" name="repository_dir" class="form-control" v-validate v-model="app.repositoryDirectory" required>		    
		    </form-group>
		    
		    <form-group name="has_scripts" label="repository.has_scripts" :path="errors.hasScripts">
		       <check-box name="has_scripts" v-model="app.hasScripts">
                   <span v-t="'repository.has_scripts2'"></span>
               </check-box>				        		   
		    </form-group>

		    <form-group label="repository.repository_date">
		        <p v-if="app.repositoryDate" class="form-control-plaintext">{{ $filters.dateTime(app.repositoryDate) }}</p>		    		   
		        <p v-else class="form-control-plaintext" v-t="'repository.never'"></p>
		    </form-group>
		    
		    <form-group label="repository.repository_audit_date">
		        <p v-if="app.repositoryAuditDate" class="form-control-plaintext">{{ $filters.dateTime(app.repositoryAuditDate) }}</p>		    		   
		        <p v-else class="form-control-plaintext" v-t="'repository.never'"></p>
		    </form-group>
		    
		    <form-group label="repository.repository_risks">
		        <input type="text" id="repositoryRisks" name="repositoryRisks" class="form-control"  v-validate v-model="app.repositoryRisks" readonly>
		    </form-group>
	  
	        <form-group label="repository.status">
	            <ul class="list-group">
                    <li class="list-group-item" v-t="'repository.phase_provided'" :class="{ 'list-group-item-success' : report }"></li>
                    <li class="list-group-item" v-t="'repository.phase_sceduled'" :class="style('SCEDULED')"></li>
                    <li class="list-group-item" v-t="'repository.phase_checkout'" :class="style('CHECKOUT')"></li>
                    <li class="list-group-item" v-t="'repository.phase_install'" :class="style('INSTALL')"></li>
                    <li class="list-group-item" v-t="'repository.phase_audit'" :class="style('AUDIT')"></li>
                    <li class="list-group-item" v-t="'repository.phase_build'" :class="style('COMPILE')"></li>
                    <li class="list-group-item" v-t="'repository.phase_import_cdn'" :class="style('EXPORT_TO_CDN')"></li>
                    <li class="list-group-item" v-t="'repository.phase_import_scripts'" :class="style('EXPORT_SCRIPTS')"></li>
                    <li class="list-group-item" v-t="'repository.phase_wipe_cdn'" :class="style('WIPE_CDN')"></li>
                    <li class="list-group-item" v-t="'repository.phase_wipe_scripts'" :class="style('WIPE_SCRIPT')"></li>
                    <li class="list-group-item" v-t="'repository.phase_delete'" :class="style('DELETE')"></li>
                    <li class="list-group-item" v-t="'repository.phase_complete'" :class="style('FINISHED')"></li>
                </ul>
	        </form-group>
	    
	  
		    <form-group myid="x" label="common.empty">
		        <button type="button" class="btn btn-default mr-1" v-t="'common.back_btn'" @click="$router.back()"></button>
		        <button type="button" class="btn btn-danger mr-1" @click="repoAction('wipe')" :disabled="!report" v-t="'common.delete_btn'"></button>		    
		        <button type="submit" :disabled="action!=null" class="btn btn-primary mr-1" v-t="'repository.submit_btn'">Submit</button>
		        <button type="button" @click="repoAction('audit')" :disabled="action!=null" class="btn btn-default mr-1" v-t="'repository.audit_btn'">Audit</button>
		        <button type="button" @click="repoAction('auditfix')" :disabled="action!=null" class="btn btn-default mr-1" v-t="'repository.audit_fix_btn'">Audit Fix</button>
		        <button type="button" @click="repoAction('redeploy')" :disabled="action!=null" class="btn btn-default mr-1" v-t="'repository.redeploy_btn'">Redeploy</button>		    		    
		    </form-group>
		  
		  		  
	    </form>	  
	
    </panel>
		  
	<panel :title="getTitle()" :busy="isBusy">
				  		  
		<b v-t="'repository.server'"></b>:
		<select class="form-control" v-model="crit.sel">
            <option v-for="node in report.clusterNodes" :key="node" :value="node">{{ node }}</option>
        </select>
		<hr>
		
        <div v-if="report && report.checkoutReport">
         
            <div v-if="test(report.checkoutReport, crit.sel)">
                <b v-t="'repository.checkout'"></b>
                <pre>{{ test(report.checkoutReport, crit.sel) }}</pre>
            </div>
            
            <div v-if="test(report.installReport, crit.sel)">
                <b v-t="'repository.install'"></b>
                <pre>{{ test(report.installReport, crit.sel) }}</pre>
            </div>
            
            <div v-if="test(report.auditReport, crit.sel)">
                <b v-t="'repository.audit'"></b>
                <pre>{{ test(report.auditReport, crit.sel) }}</pre>
            </div>
            
            <div v-if="test(report.buildReport, crit.sel)">
                <b v-t="'repository.build'"></b>
                <pre>{{ test(report.buildReport, crit.sel) }}</pre>
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
        report : { clusterNodes : [], buildReport : {} },
        appId : null,
        app : null,
        crit : { sel : null }
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox },

    mixins : [ status ],

    methods : {
    
        getTitle1() {
            const { $route, $t, $data } = this;
            let p = this.$data.app ? this.$data.app.name+" - " : "";
            return p+$t("manageapp.repository_btn");                       
        },
    
        getTitle() {
            const { $t, $filters, $data } = this;
            if (!$data.report) return $t('repository.log');
            return $t('repository.log')+" - "+$filters.dateTime($data.report.sceduled);		  	
        },

        loadApp(appId) {
            const { $data } = this, me = this;
            $data.appId=appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["creator", "filename", "name", "description", "repositoryUrl", "repositoryDirectory", "repositoryDate", "repositoryAuditDate", "repositoryRisks", "hasScripts" ])
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
                        $data.crit.sel = $data.report.clusterNodes[0]; 
                    }
                }
            });
				
	    },
	
	    dot(x) {
		    return x!=null ? x.replaceAll(".","[dot]") : "";
	    },
	    
	    test(x,y) {
	       if (x[y]) return x[y];
	       if (x[this.dot(y)]) return x[this.dot(y)];
	       return undefined;
	    },
	
	    style(p) {
            const { $data } = this, me = this;
            if (!$data.report || !$data.report.done) return "list-group-item-light";
            if ($data.report.done.indexOf(p)>=0) return "list-group-item-success";
            if ($data.report.failed.indexOf(p)>=0) return "list-group-item-danger";
            if ($data.report.planned.indexOf(p)<0) return "d-none";          
            return "list-group-item-light";          
	    },
	
	    submit() {
		    const { $data } = this, me = this;
            $data.app.action = undefined;
            me.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($data.app._id).url, $data.app)
            .then(function(result) {
                $data.report = result.data; 
            }));

	    },
	
        repoAction(action) {
		    const { $data } = this, me = this;
            $data.app.action = action; 							
            me.doAction("submit", server.post(jsRoutes.controllers.Market.updateFromRepository($data.app._id).url, $data.app)
            .then(function(result) {
                $data.report = result.data; 
            }));

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
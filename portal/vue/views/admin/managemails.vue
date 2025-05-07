<!--
 This file is part of the Open MIDATA Server.
 
 The Open MIDATA Server is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 any later version.
 
 The Open MIDATA Server is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
-->
<template>
    <panel :title="getTitle()" :busy="isBusy">
       		
		<form name="myform" ref="myform" novalidate role="form" class="form-horizontal" @submit.prevent="updateMail()">
            <error-box :error="error"></error-box>
		
		    <form-group name="name" label="admin_managemails.name" :path="errors.name">
		        <input type="text" name="name" class="form-control" @change="change()" :readonly="!editable" v-validate v-model="mailItem.name" autofocus required>
		    </form-group>
		  
		    <form-group label="admin_managemails.status">
		        <p class="form-control-plaintext">{{ $t('admin_mails.stati.'+mailItem.status) }}</p>
		    </form-group>
		  
		    <form-group name="type" label="admin_managemails.type" :path="errors.type">
		        <select class="form-control" id="type" name="type" :readonly="!editable" v-validate v-model="mailItem.type" required>
                    <option v-for="type in types" :key="type" :value="type">{{ $t('enum.bulkmailtype.'+type) }}</option>
                </select>
		    </form-group>
		  
		    <form-group name="country" label="admin_managemails.country" v-if="mailItem.type=='MARKETING'" :path="errors.country">
		        <select class="form-control" id="country" name="country" :readonly="!editable" v-validate v-model="mailItem.country">
                    <option v-for="country in countries" :key="country" :value="country">{{ $t('enum.country.'+country) }}</option>
                </select>
		    </form-group>
		  
		    <form-group name="studyId" label="admin_managemails.studyId" v-if="mailItem.type!='APP'" :path="errors.studyId">
	            <div class="row">
	                <div class="col-sm-3">
	                    <typeahead class="form-control" name="studyId" :readonly="!editable" @selection="studyselection(mailItem.studyCode);" v-model="mailItem.studyCode"  :required="mailItem.type=='PROJECT'" :suggestions="studies" field="code" />
	                </div>
	                <div class="col-sm-9">
	                    <p class="form-control-plaintext">{{ mailItem.studyName }}</p>	                 
	                </div>
	            </div>	          
	            <div class="form-text text-muted" v-if="mailItem.type=='MARKETING'"><span v-t="'admin_managemails.studyExclude'"></span></div>
	        </form-group> 
	      
	        <form-group name="appId" label="admin_managemails.appId" v-if="mailItem.type=='APP'" :path="errors.appId">
	            <div class="row">
	                <div class="col-sm-3">
	                    <typeahead id="appId" name="appId" class="form-control" :readonly="!editable" @selection="appselection(mailItem.appName);" v-model="mailItem.appName" :required="mailItem.type=='APP'" :suggestions="apps" field="name" />
	                </div>
	                <div class="col-sm-9">
	                    <p class="form-control-plaintext" v-if="app">{{ app.orgName }} {{ app.type }} {{ app.targetUserRole }}</p>	                 
	                </div>
	            </div>	          
	        </form-group> 
		   	
		    <form-group name="lang" label="admin_managemails.lang" :path="errors.lang">
		        <select id="lang" name="lang" class="form-control" v-validate v-model="sel.lang">
                    <option v-for="lang in languages" :key="lang" :value="lang">{{ lang}}</option>
                </select>		    
		    </form-group>
		  		  
		    <form-group name="title" label="admin_managemails.title" :path="errors.title">		    
		        <input type="text" id="title" name="title" class="form-control" :readonly="!editable" @change="change()" v-validate v-model="mailItem.title[sel.lang]">
		    </form-group>
		  
		    <form-group name="content" label="admin_managemails.content" :path="errors.content">
		        <textarea rows="5" id="text" name="text" class="form-control" :readonly="!editable" @change="change()" v-validate v-model="mailItem.content[sel.lang]"></textarea>
   	            <div class="form-text text-muted">
		            <span v-t="'appmessages.available_tags'"></span>:
		            <code>&lt;unsubscribe&gt;</code>
		        </div>		  
		    </form-group>
            
            <form-group name="htmlFrame" label="appmessages.htmlFrame" :path="errors.htmlFrame">
              <textarea rows="5" id="htmlFrame" name="htmlFrame" class="form-control" :readonly="!editable" @change="change()" v-validate v-model="mailItem.htmlFrame"></textarea>
              <div class="form-text text-muted">
                <span v-t="'appmessages.htmlFrame2'"></span>:
              </div>
            </form-group>
		  		  		  
		    <div>		  				  
                <form-group label="admin_managemails.created" v-if="mailItem.created != null">
                    <p class="form-control-plaintext">{{ $filters.dateTime(mailItem.created) }}</p>
                </form-group>
		    </div>	  		
		  
		    <form-group label="admin_managemails.started" v-if="mailItem.started">
		        <p class="form-control-plaintext">{{ $filters.dateTime(mailItem.started)  }}</p>
		    </form-group>
		  
		    <form-group label="admin_managemails.finished" v-if="mailItem.finished">
		        <p class="form-control-plaintext">{{ $filters.dateTime(mailItem.finished) }}</p>
		    </form-group>
		  
		    <form-group label="admin_managemails.progressCount" v-if="mailItem.progressCount">
		        <p class="form-control-plaintext">{{ mailItem.progressCount }} ( - {{ mailItem.progressFailed }})</p>
		    </form-group>
	
		    <form-group label="common.empty">
                <button type="submit" v-submit :disabled="action!=null" v-if="editable" class="btn btn-primary me-1" v-t="'admin_managemails.save_btn'"></button>		    
                <button type="button" class="btn btn-primary me-1" v-if="allowSend" @click="test()" :disabled="action!=null" v-t="'admin_managemails.test_btn'"></button>
                <button type="button" class="btn btn-primary me-1" v-if="allowSend" @click="send()" :disabled="action!=null" v-t="'admin_managemails.send_btn'"></button>
                <button type="button" class="btn btn-danger me-1" v-if="allowDelete" @click="doDelete()" :disabled="action!=null" v-t="'common.delete_btn'"></button>
                <router-link :to="{ path : './mails' }" class="btn btn-default" v-t="'common.back_btn'"></router-link>
		    </form-group>
	    </form>	  		
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import languages from "services/languages.js"
import studies from "services/studies.js"
import apps from "services/apps.js"

import { status, ErrorBox, FormGroup, Typeahead } from 'basic-vue3-components'

export default {
    data: () => ({	
        mailItem : { status : "DRAFT", studyName : "", title:{}, content:{} },
        allowDelete : false,
	    allowSend : false,
	    editable : true,
	    languages : null,
	    sel : { lang: "int" },
	    types : ["MARKETING", "PROJECT","APP"],
	    countries : languages.countries,
        studies : null,
        apps : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Typeahead },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data, $t } = this, me = this;
            if ($data.mailItem && $data.mailItem._id) return $t('admin_managemails.title2');
            return $t('admin_managemails.title1');            
        },

        loadMail(mailId) {
            const { $data, $route, $router } = this, me = this;
            me.doBusy(server.post(jsRoutes.controllers.BulkMails.get().url, { properties : { "_id" : mailId }, fields:["creator", "creatorName", "created", "started", "finished", "name", "status", "title", "content", "studyId", "studyName", "studyCode", "studyGroup", "progressId", "progressCount", "progressFailed"]})
            .then(function(data) { 
                $data.mailItem = data.data[0];	
                if ($data.mailItem.status == "DRAFT" || $data.mailItem.status == "PAUSED") {
                    $data.allowSend = true;	
                }
                if ($data.mailItem.status != "DRAFT") {
                    if ($data.mailItem.status != "FINSIHED" || $data.mailItem.progressCount > 0) $data.allowDelete = false;
                    $data.editable = false;
                }
            }));
	    },
	
	    change() {
            const { $data, $route, $router } = this, me = this;
		    $data.allowSend = false;
	    },
		
	    updateMail() {
            const { $data, $route, $router } = this, me = this;
            if ($data.mailItem._id == null) {
                me.doAction('submit', server.post(jsRoutes.controllers.BulkMails.add().url, $data.mailItem)
                .then(function(result) { me.loadMail(result.data._id); }));
            } else {			
                me.doAction('submit', server.post(jsRoutes.controllers.BulkMails.update().url, $data.mailItem)
                .then(function() { me.loadMail($data.mailItem._id); }));
            }
	    },
		
	    doDelete() {
            const { $data, $route, $router } = this, me = this;
            me.doAction('delete', server.post(jsRoutes.controllers.BulkMails.delete($data.mailItem._id).url))
            .then(function() { 
                $router.push({ path : './mails' }); 
            });
	    },
	
	
	
	    studyselection(study) {
            const { $data, $route, $router } = this, me = this;
		    me.doSilent(studies.search({ code : study }, ["_id", "code", "name" ])
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $data.mailItem.studyId = data.data[0]._id;
				  $data.mailItem.studyCode = data.data[0].code;
				  $data.mailItem.studyName = data.data[0].name;
				}
			}));
	    },
	
	    appselection(app) {		 
            const { $data, $route, $router } = this, me = this;
		    me.doSilent(apps.getApps({ name : app }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"])
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $data.mailItem.appId = data.data[0]._id;
				  $data.mailItem.appName = data.data[0].name;
				  $data.app = data.data[0];	
				}
			}));
	    },

        send() {
            const { $data, $route, $router } = this, me = this;
            me.doAction('send', server.post(jsRoutes.controllers.BulkMails.send($data.mailItem._id).url))
            .then(function() { 
                $router.push({ path : './mails' }); 
            });
	    },
	
	    test() {		
            const { $data, $route, $router } = this, me = this;
		    me.doAction('send', server.post(jsRoutes.controllers.BulkMails.test($data.mailItem._id).url));		
	    }
		
    },

    created() {
        const { $data, $route, $router } = this, me = this;
        $data.allowDelete = $route.meta.allowDelete;
        let langs = [];
        for (let i=0;i<languages.array.length;i++) langs.push(languages.array[i]);
        langs.push("int");
        $data.languages = langs;

        if ($route.query.mailId != null) { me.loadMail($route.query.mailId); }
	    
        me.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ])
        .then(function(data) {
            $data.studies = data.data;
        }));
	
	    me.doBusy(apps.getApps({  }, ["creator", "developerTeam", "filename", "name", "description", "type", "targetUserRole" ])
        .then(function(data) { 
            $data.apps = data.data;			
        }));	
    }
    
}
</script>
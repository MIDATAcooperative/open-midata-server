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
    <panel :title="$t('appsubscriptions.title')" :busy="isBusy">		  	
        <error-box :error="error"></error-box>
			
		<form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()">		    
		    <form-group name="name" label="appicon.name">
		        <p class="form-control-plaintext">{{ app.name }}</p>
		    </form-group>
		    <form-group name="filename" label="appicon.internalname">
		        <p class="form-control-plaintext">{{ app.filename }}</p>
		    </form-group>
		  
		    <p v-if="!app.defaultSubscriptions || !app.defaultSubscriptions.length" v-t="'appsubscriptions.empty'"></p>
		    <table class="table table-striped" v-if="app.defaultSubscriptions && app.defaultSubscriptions.length">
		      <tr>
		        <th v-t="'appsubscriptions.trigger'"></th>
		        <th v-t="'appsubscriptions.triggerDetail'"></th>
		        <th v-t="'appsubscriptions.action'"></th>
		        <th v-t="'appsubscriptions.params'"></th>
		        <th>&nbsp;</th>
		      </tr>
		      <tr v-for="(subscription,idx) in app.defaultSubscriptions" :key="idx">
		        <td><select class="form-control" v-validate v-model="subscription.trigger">
                      <option v-for="trigger in triggers" :key="trigger" :value="trigger">{{ $t('appsubscriptions.triggers.'+trigger) }}</option>
                    </select>
                </td>
		        <td>
		          <input class="form-control" type="text" v-if="subscription.trigger=='fhir_Resource'" placeholder="Observation?code=http://loinc.org|12345" v-validate v-model="subscription.criteria">
		          <input class="form-control" type="text" v-if="subscription.trigger=='fhir_MessageHeader'" placeholder="event[:application]" v-validate v-model="subscription.criteria">
		        </td>
		        <td><select class="form-control" v-validate v-model="subscription.action">
                    <option v-for="action in actions" :key="action" :value="action">{{ $t('appsubscriptions.actions.'+action) }}</option>
                    </select>
                </td>
		        <td><input class="form-control" type="text" v-if="subscription.action!='email'" v-validate v-model="subscription.parameter"></td>
		        <td><button class="btn btn-sm btn-default" @click="delete1(subscription)" v-t="'common.delete_btn'"></button></td>
		      </tr>
		    </table>
		    <form-group name="x" label="common.empty">
		      <router-link class="btn btn-default mr-1" :to="{ path : './manageapp', query : { appId : appId }}" v-t="'common.back_btn'"></router-link>
		
		      <button class="btn btn-default mr-1" type="button" @click="add()" v-t="'common.add_btn'"></button>
		      <button class="btn btn-primary mr-1" type="submit" v-submit v-t="'common.submit_btn'"></button>
		    </form-group>
		</form>
	</panel>

    <panel :title="$t('appsubscriptions.debug.title')" :busy="isBusy">
		  <p v-t="'appsubscriptions.debug.description'"></p>
		  <p v-t="'appsubscriptions.debug.description2'"></p>
		  <p v-t="'appsubscriptions.debug.description3'"></p>		  
		  
		  <button class="btn btn-default" @click="startDebug()" v-if="!app.debugHandle" v-t="'appsubscriptions.debug.start_btn'"></button>
		  <button class="btn btn-default" @click="stopDebug()" v-if="app.debugHandle" v-t="'appsubscriptions.debug.stop_btn'"></button>
		  <div class="extraspace"></div>
		  <p v-if="app.debugHandle" v-t="'appsubscriptions.debug.cmd_to_run'"></p>
		  <pre v-if="app.debugHandle">npx midata-tester {{ ENV.apiurl }} {{ app.debugHandle }}</pre>
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"
import apps from "services/apps.js"
import { status, ErrorBox, CheckBox, FormGroup } from 'basic-vue3-components'
import ENV from 'config';

export default {

    data: () => ({	
        triggers : ["fhir_Consent", "fhir_MessageHeader", "fhir_Resource", "time", "init","time/30m"],
	    actions : ["rest-hook", "email", "sms", "nodejs", "app"],
        ENV : ENV,
        appId : null,
        app : null
    }),

    components: {  Panel, ErrorBox, FormGroup, CheckBox },

    mixins : [ status ],

    methods : {
        loadApp(appId) {
            const { $data } = this, me = this;
            $data.appId=appId;
            me.doBusy(apps.getApps({ "_id" : appId }, ["version", "creator", "filename", "name", "description", "defaultSubscriptions", "debugHandle" ])
            .then(function(data) { 
                let app = data.data[0];
                var subscriptions = app.defaultSubscriptions;
                
                if (subscriptions) {
                    for (let subscription of subscriptions) {
                        if (subscription.format === "fhir/MessageHeader") {
                            subscription.trigger = "fhir_MessageHeader";
                            var p = subscription.fhirSubscription.criteria.indexOf("?event=");
                            var criteria = p > 0 ? subscription.fhirSubscription.criteria.substr(p+7) : subscription.fhirSubscription.criteria;
                            subscription.criteria = criteria;
                        } else if (subscription.format === "fhir/Consent") {
                            subscription.trigger = "fhir_Consent";
                        } else if (subscription.format === "time") {
                            subscription.trigger = "time";
                        } else if (subscription.format === "time/30m") {
                            subscription.trigger = "time/30m";
                        } else if (subscription.format === "init") {
                            subscription.trigger = "init";					
                        } else {
                            subscription.trigger = "fhir_Resource";
                            subscription.criteria = subscription.fhirSubscription.criteria;
                        }
                        
                        if (subscription.fhirSubscription.channel.type === "email") {
                            subscription.action = "email";
                        } else if (subscription.fhirSubscription.channel.type === "rest-hook") {
                            subscription.action = "rest-hook";
                            subscription.parameter = subscription.fhirSubscription.channel.endpoint;
                        } else if (subscription.fhirSubscription.channel.type === "message") {
                            var endpoint = subscription.fhirSubscription.channel.endpoint;
                            if (endpoint && endpoint.startsWith("node://")) {
                                subscription.action = "nodejs";
                                subscription.parameter = endpoint.substr("node://".length);
                            }
                            else if (endpoint && endpoint.startsWith("app://")) {
                                subscription.action = "app";
                                subscription.parameter = endpoint.substr("app://".length);
                            }
                        }
                    }
                }
                $data.app = app;
                
            }));
	    },
	
        add() {
            const { $data } = this, me = this;
            if (!$data.app.defaultSubscriptions) $data.app.defaultSubscriptions = [];
            $data.app.defaultSubscriptions.push({});
        },
    
        delete1(elem) {
            const { $data } = this, me = this;
    	    $data.app.defaultSubscriptions.splice($data.app.defaultSubscriptions.indexOf(elem), 1);
        },
    
        submit() {
            const { $data, $router } = this, me = this;
            for (let subscription of $data.app.defaultSubscriptions) {
                var criteria;
                switch (subscription.trigger) {
                case "fhir_Consent":
                    criteria = "Consent";    			
                    break;
                case "fhir_MessageHeader":
                    if (subscription.criteria) {
                        criteria = "MessageHeader?event="+subscription.criteria;
                    } else {
                        criteria = "MessageHeader";
                    }
                    break;
                case "fhir_Resource":
                    criteria = subscription.criteria;
                    break;
                case "time":
                    criteria = "time";
                    break;
                case "time/30m":
                    criteria = "time/30m";
                    break;
                case "init":
                    criteria = "init";
                    break;
                }
                subscription.fhirSubscription = {
                    "resourceType" : "Subscription",
                    "status" : "active",
                    "criteria" : criteria,
                    "channel" : {}
                };
                switch (subscription.action) {
                case "rest-hook":
                    subscription.fhirSubscription.channel.type = "rest-hook";
                    subscription.fhirSubscription.channel.endpoint = subscription.parameter;
                    break;
                case "email":
                    subscription.fhirSubscription.channel.type = "email";    			
                    break;
                case "nodejs":
                    subscription.fhirSubscription.channel.type = "message";
                    subscription.fhirSubscription.channel.endpoint = "node://"+subscription.parameter;
                    break;
                case "app":
                    subscription.fhirSubscription.channel.type = "message";
                    subscription.fhirSubscription.channel.endpoint = "app://"+subscription.parameter;
                    break;
                }
            }
            
            me.doAction("submit", server.post(jsRoutes.controllers.Market.updateDefaultSubscriptions($data.app._id).url, $data.app))
            .then(function() {
                $router.push({ path : './manageapp', query : { appId : $data.app._id } }); 
            });
        },
    
        startDebug() {
            const { $data } = this, me = this;
            var data = { plugin : $data.app._id, action : "start" };
            me.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, data))
            .then(function(result) {
                $data.app.debugHandle = result.data.debugHandle;
            });
        },
    
        stopDebug() {
            const { $data } = this, me = this;
            var data = { plugin : $data.app._id, action : "stop" };
            me.doAction("debug", server.post(jsRoutes.controllers.Market.setSubscriptionDebug().url, data))
            .then(function(result) {
                $data.app.debugHandle = result.data.debugHandle;
            });
        }
    },

    created() {
        const { $route } = this, me = this;		
        me.loadApp($route.query.appId);	
	
    }
}
</script>
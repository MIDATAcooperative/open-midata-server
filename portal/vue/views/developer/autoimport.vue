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
<div>
    <panel :title="'Auto Import Test'" :busy="isBusy">		  
	
        <error-box :error="error"></error-box>
    
		<p>In order to test the automatic import feature you need to do the following steps:</p>
		<ul>
		  <li>Your plugin needs the 'server.js' file provided by MIDATA.</li>
		  <li>Install your plugin from localhost into the sandbox and run your development version on your PC.</li>
		  <li>Using the sandbox to authorize your plugin to access the OAuth endpoint you want to import data from.</li>
		  <li>Have your plugin save a configuration with autoimport enabled.</li>
		  <li>Using a shell 'cd' into the directory where you develop your plugin and execute the command shown below.</li>
		</ul>
		<div class="alert alert-warning" v-if="calls.length==0">Plugin is not yet installed in your sandbox!</div>		
		<div v-for="(call,idx) in calls" :key="idx">
		  <hr>			
		  <p>Installed Plugin: <b>{{ call.name }}</b></p>
		  <p>Autoimport enabled in config: <b>{{ call.autoImport }}</b></p>
		  <p>Existing OAuth Session: <b>{{ call.loggedIn }}</b></p>
		  <div class="alert alert-warning" v-if="!call.loggedIn">Please establish an OAuth session before testing.</div>
		  <div class="alert alert-warning" v-if="!call.autoImport">Please configure your plugin to use autoImport before testing.</div>		
		  <p>When ready execute this command to test autoimport (paste as one line):</p>
		  <div class="card">
		  <div class="card-body" style="word-wrap:break-word">
           node server.js "{{ call.authToken }}" en {{baseurl}} {{userId}}
		  </div>		 	
		  </div>	  	  
	    </div>	
    </panel>			
</div>

</template>
<script>

import Panel from "components/Panel.vue"

import spaces from "services/spaces.js"
import session from "services/session.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import ENV from "config";


export default {
    data: () => ({	       
        baseurl : ENV.baseurl,
        calls : [],
        app : null,
        userId : null
    }),

    components: {  Panel, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {
        loadSpace(space) {
            const { $data } = this, me = this;
            me.doBusy(spaces.getUrl(space._id)
            .then(function(spaceurl) {	    			
                var call = { name : space.name, autoImport : space.autoImport, authToken : spaceurl.data.token, loggedIn : !(spaceurl.data.authorizationUrl) };
                $data.calls.push(call);	    			
            }));
        },

        init(userId, appId) {
            const { $data } = this, me = this;
            $data.userId = userId;
            var properties = {"owner": userId, "visualization" : appId, "context" : "sandbox" };
            var fields = ["name", "type", "order", "autoImport", "context", "visualization"];
            
            me.doBusy(spaces.get(properties, fields)
            .then(function(results) {	    	
                for (let space of results.data) {
                    me.loadSpace(space);
                }                
            }));
                    
	    }			
	   
    },

    created() {
        const { $route } = this, me = this;
        session.currentUser.then(function(userId) { me.init(userId, $route.query.appId); });        
    }
}
</script>
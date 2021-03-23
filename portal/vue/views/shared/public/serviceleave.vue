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
<panel :title="$t('serviceleave.title')" :busy="isBusy" style="max-width:330px; padding-top:30px; margin:0 auto;">
    <p v-t="'serviceleave.thankyou'"></p>
		   
	<div v-if="!callback">
		<p v-t="'serviceleave.continue'"></p>
		<button type="button" class="btn btn-primary" v-t="'serviceleave.logout_btn'" @click="logout();"></button>
	</div>
	<div v-if="callback=='close'">
		<button class="btn btn-primary" v-t="'serviceleave.goodbye_btn'" @click="close();"></button>
	</div>
		   
	<div v-if="callback && callback!='close'">
		<p v-t="'serviceleave.logoutreturn'"></p>
		<button class="btn btn-primary" v-t="'serviceleave.return_btn'" @click="leave();"></button>
	</div>
		   
</panel>
</template>
<script>

import server from "services/server.js";
import session from "services/session.js";
import actions from "services/actions.js";
import { status, ErrorBox } from 'basic-vue3-components';
import Panel from 'components/Panel.vue'

export default {
    data: () => ({
        callback : null  
    }),

    components : {
        Panel, ErrorBox
    },

    mixins : [ status ],

    methods: {
        init() {		
            const { $data, $route } = this;
		    actions.logout();
		    if ($route.query.callback) {
			    $data.callback = $route.query.callback;			
		    }
	    },
	
	    close() {
		    this.doAction(server.post('/api/logout')
		    .then(function() { 
			    session.logout();
                window.close();            
		    }));
	    },
	
	    leave() {
            const { $data } = this;
		    this.doAction(server.post('/api/logout')
		    .then(function() { 
			    session.logout();
                document.location.href = $data.callback;            
		    }));
	    },
	
	    logout() {		
            const { $route } = this;
		    this.doAction(server.post('/api/logout')
		    .then(function() { 
			    session.logout();
			    if ($route.meta.role=="provider") document.location.href="/#/provider/login";
			    else if ($route.meta.role=="research") document.location.href="/#/research/login";
			    else if ($route.meta.role=="admin" || $route.meta.role == "developer") document.location.href="/#/developer/login";
                else document.location.href="/#/public/login"; 
            }));
	    }
    },

    created() {
        this.init();
    }
    
}
</script>
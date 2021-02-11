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
import status from "mixins/status.js";
import Panel from 'components/Panel.vue'
import ErrorBox from 'components/ErrorBox.vue'

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
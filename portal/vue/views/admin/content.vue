<template>
     <panel :title="'Content Types'" :busy="isBusy">
         To be implemented. Use old angularJS portal until available.
     </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server.js"

import { status, ErrorBox, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({	
        
    }),

    components: {  Panel, ErrorBox, FormGroup },

    mixins : [ status ],

    methods : {
        
    },

    created() {
        const me = this;
        me.ready();        
    }
    
}
</script>
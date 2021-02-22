<template>
    <panel :title="$t('changelog.title')" :busy="isBusy">
        <pagination v-model="changelog" search="search"></pagination>
        <div v-for="entry in changelog.filtered" :key="entry._id" class="row">
		    <div class="col-2"><br>{{ $filters.dateTime(entry.published) }}</div>
		    <div class="col-10">
		        <div class="text-muted">{{ $t('changelog.'+entry.type) }}</div>
                <b>{{ entry.title }}</b>
                <p>{{ entry.description }}</p>        
		    </div>
		</div>
    </panel>
</template>
<script>

import Panel from "components/Panel.vue"
import server from "services/server"
import rl from "mixins/resultlist"
import status from "mixins/status"

export default {
	
    data : ()=>({      
		changelog : []
    }),
	
	components: { Panel },

	mixins : [ status, rl ],
	
	created() {
        const { $data, $filters } = this, me = this;
        me.doBusy(server.get(jsRoutes.controllers.Market.getSoftwareChangeLog().url)
	    .then(function(data) { 
            for (let l of data.data) l.search = l.title+" "+l.description+$filters.dateTime(l.published)
		    $data.changelog = me.process(data.data, { filter : { search : "" }});				
	    }));
	}
    
}
</script>
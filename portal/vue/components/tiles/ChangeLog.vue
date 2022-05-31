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
import { rl, status } from 'basic-vue3-components'

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
		    $data.changelog = me.process(data.data, { filter : { search : "" }, ignoreCase : true });				
	    }));
	}
    
}
</script>
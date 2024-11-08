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
<div class="ignore autosize" v-if="!isBusy">
    <div v-if="recs && recs.all && recs.all.length" class="body"><span class="dashnumber">{{ recs.all.length }}</span> <span v-t="'flexiblerecords.records_available'"></span></div>
    <div class="body text-center" v-else v-t="'flexiblerecords.empty'"></div>
    
    <error-box :error="error"></error-box>
    <pagination v-model="recs" search="search"></pagination>

    <ul class="list-group" v-if="recs && recs.filtered && recs.filtered.length">
		<li class="list-group-item rotate truncate" v-for="record in recs.filtered" :key="record._id">
		    <div class="float-end" v-if="setup.allowRemove">
		        <button type="button" class="btn btn-sm btn-danger" @click="removeRecord(record)" :disabled="action!=null">
				    <span class="fas fa-times"></span>
			    </button>
			</div>	
		    <input type="checkbox" v-model="record.marked" v-if="setup.allowShare"/>
			<span class="badge text-bg-info">{{ $filters.date(record.created) }}</span>&nbsp;
			<span v-if="record.owner != userId" class="badge text-bg-info">{{ record.ownerName }}</span>&nbsp;
			<span v-if="record.creator != userId && record.creatorName" class="label label-info">by {{ record.creatorName }}</span>&nbsp;
			<a href="javascript:;" @click="showDetails(record)">{{record.name}}</a>												
		</li>
	</ul>
	
	<div class="footer" v-if="setup.allowBrowse || setup.allowAdd || setup.allowShare">
	    <router-link :to="{ path : './records' }" v-if="setup.allowBrowse" class="btn btn-default" v-t="'flexiblerecords.browse_btn'"></router-link>
	    <button @click="addRecords()" v-if="setup.allowAdd" class="btn btn-primary" v-t="'flexiblerecords.add_btn'"></button>
	    <button @click="shareRecords()" v-if="setup.allowShare" class="btn btn-primary" v-t="'flexiblerecords.share_btn'"></button>
	</div>	
</div>
</template>
<script>

import { status, rl, ErrorBox } from 'basic-vue3-components'
import records from "services/records";

export default {
    props: [ "setup" ],
	emits : [ "close" ],

    data : ()=>({      
        recs : null,
        all : null
    }),
	
	components: { ErrorBox },

	mixins : [ status, rl ],

    watch : {
        setup() { this.reload(); }
    },

	methods : {
        reload() {		
            console.log("RELOAD");
            const { $data, $filters } = this, me = this;
            me.doBusy(records.getRecords(me.setup.aps, me.setup.properties, me.setup.fields).
            then(function (result) { 
                for (let record of result.data) record.search=record.name+" "+record.ownerName+" "+record.name+" "+$filters.date(record.created);
                $data.all = result.data;
                $data.recs = me.process(result.data, { filter : { search : ""}});
            }));
	    },
	
	    showDetails(record) {
            this.$router.push({ path : "./recorddetail", query : { recordId : record._id+'.'+this.setup.aps } });
	    },
	
	    removeRecord(record) {
            const { $data } = this, me = this;
            me.doSilent(records.unshare(me.setup.aps, record._id, me.setup.type));
            $data.all.splice($data.all.indexOf(record), 1);
	    },
	
	    shareRecords() {
            const me = this;
            var selection = _.filter($data.all, function(rec) { return rec.marked; });
            selection = _.chain(selection).pluck('_id').value();
            me.doAction("share", records.share(me.setup.targetAps, selection, me.setup.type))
            .then(function () {
                me.$emit("close");
            });
	    },
	
	    addRecords() {
            const me = this;
            this.$router.push({ path : "./records", query : { selectedType : "circles", selected : me.setup.aps } });		
	    }
    },

    created() {
        this.reload();
    }
}
</script>
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
    <div class="float-right sharebox" v-if="selectedAps!=null">
		<button class="btn btn-sm btn-primary" :disabled="action!=null" @click="unshare(data);" v-show="isSharedGroup(data)">
			<span class="fas fa-picture"></span><span v-t="'records.unshare'"></span>
		</button>
		<button class="btn btn-sm btn-primary" :disabled="action!=null" v-show="!isSharedGroup(data)" @click="share(data);">
			<span class="fas fa-share"></span><span v-t="'records.share'"></span>
		</button>
	</div>	
    <h1>
        <span v-if="data.children && data.children.length">
            <span @click="setOpen({ group : data, open : true });" v-show="!data.open" class="treehandle fas fa-plus"></span>
            <span v-show="data.open && !data.type" @click="setOpen({ group : data, open : false });" class="treehandle fas fa-minus"></span>
        </span>
        <span v-else class="treehandle" style="padding:1px;padding-left:9px;">&nbsp;</span>
        <a href="javascript:" @click="$emit('show',data)" :class="{ 'text-success' : isSharedGroup(data), 'format': data.type == 'content', 'nodata' : data.count == 0 }">{{data.fullLabel.fullLabel}}</a> <small>(<span v-if="selectedAps">{{ data.countShared }} / </span>{{ data.count }})</small><button class="btn btn-sm btn-danger" v-show="allowDelete" @dblclick="deleteGroup(data)" v-t="'records.delete_all_btn'"></button>
    </h1>
    <div v-if="data.open">          
        <div class="subtree">
            <record-tree-node v-for="data in data.children" :action="action" :key="data._id" :selectedAps="selectedAps" :data="data" :sharing="sharing" @show="show" @share="share" @unshare="unshare" @deleteGroup="deleteGroup" @open="setOpen"></record-tree-node>
        </div>
    </div>    
</template>
<script>
export default {
    props : ["data", "selectedAps","sharing" ,"action"],
    emits : ["show", "share","unshare", "deleteGroup", "open" ],
    name : "record-tree-node",
    methods : {
        isShared(record) {
            const { $data, $route } = this, me = this;
            if (record == null) return;
            if (!me.sharing) return;
            return me.sharing.ids[record._id];
	    },
	
	    isSharedGroup(group) {
            const { $data, $route } = this, me = this;
            
            if ($data.treeMode === "plugin") return false;
            if (group.parent != null && !group._parent) return false;
            
            group.parentShared = (group.parent != null && group._parent.shared);
            group.parentExcluded = (group.parent != null && group._parent.excluded);
            var excluded = me.sharing && 
                me.sharing.query &&
                me.sharing.query["group-exclude"] && 	       
                me.sharing.query["group-exclude"].indexOf(group.name) >= 0;
            group.excluded = group.parentExcluded || excluded;
            
            if (!me.sharing || !me.sharing.query || !me.sharing.query.group) {
                group.shared = group.parentShared && !excluded;
                return group.shared;
            }
            
            var r = group.shared = (me.sharing.query.group.indexOf(group.name) >= 0 || group.parentShared) && !excluded; 
            return r;
        },
        
        setOpen(p) {
            p.group.open = p.open;
            this.$emit("open", p);
        },
        
        show(data) {
            this.$emit("show", data);
        },

        share(data) {
            this.$emit("share", data);
        },

        unshare(data) {
            this.$emit("unshare", data);
        },

        deleteGroup(data) {
            this.$emit("deleteGroup", data);
        }
    }
}
</script>
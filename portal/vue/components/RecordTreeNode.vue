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
            <span @click="setOpen(data,true);" v-show="!data.open" class="treehandle fas fa-plus"></span>
            <span v-show="data.open && !data.type" @click="setOpen(data,false);" class="treehandle fas fa-minus"></span>
        </span>
        <span v-else class="treehandle" style="padding:1px;padding-left:9px;">&nbsp;</span>
        <a href="javascript:" @click="$emit('show',data)" :class="{ 'text-success' : isSharedGroup(data), 'format': data.type == 'content', 'nodata' : data.count == 0 }">{{data.fullLabel.fullLabel}}</a> <small>(<span v-if="selectedAps">{{ data.countShared }} / </span>{{ data.count }})</small><button class="btn btn-sm btn-danger" v-show="allowDelete" @dblclick="deleteGroup(data)" v-t="'records.delete_all_btn'"></button>
    </h1>
    <div v-if="data.open">          
        <div class="subtree">
            <record-tree-node v-for="data in data.children" :action="action" :key="data._id" :selectedAps="selectedAps" :data="data" :sharing="sharing" @show="show" @share="share" @unshare="unshare" @deleteGroup="deleteGroup"></record-tree-node>
        </div>
    </div>    
</template>
<script>
export default {
    props : ["data", "selectedAps","sharing" ,"action"],
    emits : ["show", "share","unshare", "deleteGroup" ],

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
        
        setOpen(group, open) {
            group.open = open;
            //$data.open[group.id] = open;		
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
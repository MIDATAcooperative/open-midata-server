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
    <study-nav page="study.info" :study="study"></study-nav>
    <tab-panel :busy="isBusy">
	
        <error-box :error="error"></error-box>

        <p v-t="'studyinfo.introduction'"></p>
        
        <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="submit()" role="form">
            <form-group name="visibility" label="studyinfo.visibility" :path="errors.visibility">
                <select v-validate v-model="selection.visibility" @change="changevisible" class="form-control">
                    <option v-for="vis in visibilities" :key="vis" :value="vis">{{ $t('studyinfo.visibilities.'+vis) }}</option>
                </select>
            </form-group>
            
            <form-group name="languages" label="studyinfo.showlanguages" :path="errors.languages">
                <div class="checkbox col-sm-12">
                    <label>
                        <input type="checkbox" disabled :checked="selection.langs.indexOf('int')>=0">
                        <span class="ml-1" v-t="'enum.language.INT'"></span>
                    </label>
                </div>
                <div v-for="language in languages" :key="language.value" class="checkbox col-sm-3">
                    <label>
                        <input type="checkbox" :checked="selection.langs.indexOf(language.value)>=0" @click="toggle(selection.langs, language.value);">
                        <span class="ml-1" v-t="language.name"></span>
                    </label>
                </div>
            </form-group>
                
            <p v-t="'studyinfo.introduction2'"></p>	  	  	
        
            <div v-for="section in infosFiltered" :key="section.id">
            
                <p><b>{{ $t('enum.infos.'+section.type) }}</b></p>
        
                <div class="extraspace" v-for="lang in selection.langs" :key="lang">
                    <div class="text-muted"><span>{{ $t('enum.language.'+lang.toUpperCase()) }}</span>:</div>
                    <textarea class="form-control" :disabled="studyLocked()" v-validate v-model="section.value[lang]">
                    </textarea>	        
                </div>
        
                <hr>
            </div>
        
            <div class="extraspace"></div>
            <button type="submit" class="btn btn-primary" v-submit :disabled="studyLocked() || action!=null" v-t="'common.change_btn'"></button>
            <success :finished="finished" action="change" msg="common.save_ok"></success>        
        
        </form>
        
        
    </tab-panel>    
</div>
   
</template>
<script>

import Panel from "components/Panel.vue"
import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import server from "services/server.js"
import languages from "services/languages.js"
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'
import _ from "lodash";

export default {
    data: () => ({	
        studyid : null,
        study : null,
        languages : languages.all,
        sections : ["SUMMARY", "ONBOARDING", "DESCRIPTION", "HOMEPAGE", "CONTACT", "INSTRUCTIONS", "PURPOSE", "AUDIENCE", "LOCATION", "PHASE", "SPONSOR", "SITE", "DEVICES", "COMMENT"],
        allsections : {
		    "ALL" : ["SUMMARY", "ONBOARDING", "DESCRIPTION", "HOMEPAGE", "CONTACT", "INSTRUCTIONS", "PURPOSE", "AUDIENCE", "LOCATION", "PHASE", "SPONSOR", "SITE", "DEVICES", "COMMENT"],
		    "PARTICIPANTS" : ["CONTACT", "INSTRUCTIONS", "DEVICES", "COMMENT"],
		    "INTERNAL" : ["PURPOSE", "PHASE", "COMMENT"]
        },
        visibilities : ["ALL", "PARTICIPANTS", "INTERNAL"],
        selection : { langs : ["int"], visibility : "ALL" },
        infos : [],
        infosFiltered : []
    }),

    components: {  TabPanel, Panel, ErrorBox, FormGroup, StudyNav, Success },

    mixins : [ status ],

    methods : {
        reload() {
            const { $data } = this, me = this;
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
	        .then(function(data) { 				
			    $data.study = data.data;
			    $data.study.recordQuery = undefined;
				me.generate();
		    }));
        },
   
        generate() {
            const { $data } = this;
	        let result = [];
	        let byKey = {};
	        if ($data.study.infos) {
                for (let info of $data.study.infos) {
                info.visibility = "ALL";
                byKey[info.type+"_"+info.visibility] = info;
                }
	        }
	        if ($data.study.infosPart) {
		        for (let info of $data.study.infosPart) {
			        info.visibility = "PARTICIPANTS";
			        byKey[info.type+"_"+info.visibility] = info;
		        }
            }
	        if ($data.study.infosInternal) {
		        for (let info of $data.study.infosInternal) {
			        info.visibility = "INTERNAL";
			        byKey[info.type+"_"+info.visibility] = info;
		        }
	        }
	        for (let v of $data.visibilities) {
		        for (let s of $data.allsections[v]) {
			        let inf = byKey[s+"_"+v] || { type : s, visibility : v, value : {} };
                    inf.id = s+"_"+v;
			        result.push(inf);
		        }
	        }
	        $data.infos = result;
            $data.infosFiltered = _.filter(result, (x) => x.visibility == $data.selection.visibility );
        },
   
  
        changevisible() {
            const { $data } = this;
	        //$data.sections = $data.allsections[$data.selection.visibility];  
            $data.infosFiltered = _.filter($data.infos, (x) => x.visibility == $data.selection.visibility );
	      
        },
   
        submit() {
	    	const { $data } = this, me = this;
            let result = { ALL : [], PARTICIPANTS : [], INTERNAL : [] };
		
		    for (let inf of $data.infos) {
		        let used = false;
		        if (inf.value && inf.value.int && inf.value.int.trim().length > 0) used = true;
		        else for (let l of languages.all) {
			        if (inf.value && inf.value[l.value] && inf.value[l.value].trim().length > 0) used = true; 
		        }
		        if (used) {
			        result[inf.visibility].push(inf);
		        }		
		    }
	        let data = { infos : result.ALL, infosPart : result.PARTICIPANTS, infosInternal : result.INTERNAL };
	        me.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($data.studyid).url, data))
	        .then(function(data) { 				
		        //me.reload();		    
	        }); 
        },

        studyLocked() {
            const { $data } = this;
		    return (!$data.study) ||  !$data.study.myRole.setup;
        },
   
        toggle(array,itm) {		
		    let pos = array.indexOf(itm);
		    if (pos < 0) array.push(itm); else array.splice(pos, 1);
        },
    },

    created() {
        const { $data, $route } = this;
        $data.studyid = $route.query.studyId;
        this.reload();
    }
}
</script>
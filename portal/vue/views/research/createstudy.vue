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
			
	<panel :title="getTitle()" :busy="isBusy">
		<error-box :error="error"></error-box>
	    <div class="alert alert-warning" v-if="role!='research'">
	      <strong>{{ $t('createstudy.developer_nostart') }}</strong>
	      <div>{{ $t('createstudy.developer_nostart2') }}</div>
	    </div>	    
	    <form name="myform" ref="myform" novalidate class="css-form form-horizontal" @submit.prevent="createstudy()" role="form">
	        <form-group name="name" label="createstudy.study_name" :path="errors.name"> 
		        <input type="text" class="form-control" id="name" name="name" v-validate v-model="study.name" required>		
            </form-group>
            <form-group name="type" label="createstudy.study_type" :path="errors.type"> 
		        <select class="form-control" id="type" name="type" v-validate v-model="study.type" required="true">
                    <option v-for="type in studytypes" :key="type" :value="type">{{ $t('enum.studytype.'+type) }}</option>
                </select>
            </form-group>
            <form-group name="description" label="createstudy.study_description" :path="errors.description">
                <textarea class="form-control" id="description" name="description" rows="5" v-validate v-model="study.description" required></textarea>
            </form-group>
            <form-group name="identifiers" label="createstudy.study_identifiers" :path="errors.identifiers">
                <textarea class="form-control" id="identifiers" name="identifiers" rows="5" v-validate v-model="study.identifiersStr"></textarea>
                <p class="form-text text-muted" v-t="'createstudy.study_identifiers_info'"></p>
            </form-group>
            <form-group name="categories" label="createstudy.study_categories" :path="errors.categories">
                <textarea class="form-control" id="categories" name="categories" rows="5" v-validate v-model="study.categoriesStr"></textarea>
                <p class="form-text text-muted" v-t="'createstudy.study_categories_info'"></p>
            </form-group>
   
            <form-group label="common.empty">
                <button type="submit" v-submit class="btn btn-primary" v-t="'common.submit_btn'"></button>
            </form-group>
        </form>	    		
	</panel>

</template>
<script>
import server from "services/server.js";
import studies from "services/studies.js";
import { status, FormGroup, ErrorBox } from 'basic-vue3-components';
import Panel from 'components/Panel.vue';

export default {
  data: () => ({
      studytypes : studies.studytypes,
      study : { identifiers : [], categories:[] },
      role : null
  }),

  components : {
     FormGroup, ErrorBox, Panel
  },

  mixins : [ status ],
    
  methods : {
    getTitle() {
        const { $route, $t } = this;
	    if ($route.query.studyId) return $t('createstudy.title2');
        return $t('createstudy.title');        
    },

    reload() {
        const { $data, $route } = this, me = this;
		if ($route.query.studyId) {
			me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($route.query.studyId).url)
		    .then(function(data) { 				
				let study = data.data;	
				if (study.identifiers) study.identifiersStr = study.identifiers.join("\n");
                if (study.categories) study.categoriesStr = study.categories.join("\n");
				$data.study = study;											
			}));			
		} else me.ready();
		
	},
	
	createstudy() {
		const { $data, $route, $router } = this, me = this;
	    let data;
	    if ($data.study.identifiersStr) {
		  $data.study.identifiers = $data.study.identifiersStr.split(/\s*\n\s*/);
		} else {
		  $data.study.identifiers = [];
		}
        if ($data.study.categoriesStr) {
          $data.study.categories = $data.study.categoriesStr.split(/\s*\n\s*/);
        } else {
          $data.study.categories = [];
        }
		if ($route.query.studyId) {
		
			data = { name : $data.study.name, description : $data.study.description, type : $data.study.type, identifiers : $data.study.identifiers, categories: $data.study.categories };
			me.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($route.query.studyId).url, data))
			.then(function(result) { 
                $router.push({ path : './study.overview', query : { studyId : $route.query.studyId }}); 
            });
		
		} else {
			data = $data.study;		
			me.doAction("submit", server.post(jsRoutes.controllers.research.Studies.create().url, data))
			.then(function(result) { 
                $router.push({ path : './study.overview', query : { studyId : result.data._id }});                 
            });
			
		}
	}
	
  },

  created() {    
      this.$data.role = this.$route.meta.role;
      this.reload();   
  }
}
</script>

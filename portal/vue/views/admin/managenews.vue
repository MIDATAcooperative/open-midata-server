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
      		
		<form class="form-horizontal" name="myform" ref="myform" @submit.prevent="updateNews()">
            
		    <form-group name="title" label="admin_managenews.title" :path="errors.title">
		        <input type="text" name="title" class="form-control" v-validate v-model="newsItem.title" autofocus>
		    </form-group>
		  
		    <form-group name="date" label="admin_managenews.date" :path="errors.date">		      
				<input type="date" class="form-control" name="date" v-validate v-date="newsItem.date" v-model="newsItem.date">			
		    </form-group>
		
		    <form-group name="language" label="admin_managenews.language" :path="errors.language">
		        <select id="language" class="form-control" v-validate v-model="newsItem.language">
                    <option v-for="lang in languages" :key="lang" :value="lang">{{ lang }}</option>
                </select>		    
		    </form-group>
		
		    <form-group name="content" label="admin_managenews.content" :path="errors.content">
		        <textarea rows="5" id="content" class="form-control" v-validate v-model="newsItem.content"></textarea>
		    </form-group>
		  
		    <form-group name="url" label="admin_managenews.url" :path="errors.url">
		        <input type="text" id="url" class="form-control" v-validate v-model="newsItem.url">
		    </form-group>
		  
		    <hr>
		  
		    <form-group name="expires" label="admin_managenews.expires" :path="errors.expires">		      
				<input type="date" name="expires" class="form-control" v-validate v-date="newsItem.expires" v-model="newsItem.expires">				  
		    </form-group>
		
		    <form-group name="layout" label="admin_managenews.layout" :path="errors.laqyout">
		        <select id="layout" class="form-control" v-validate v-model="newsItem.layout">
                    <option v-for="layout in layouts" :key="layout" :value="layout">{{ $t('admin_managenews.layouts.'+layout) }}</option>
                </select>		    
		    </form-group>
		
		    <form-group name="study" label="admin_managenews.studyId" :path="errors.study">
	            <div class="row">
	                <div class="col-sm-3">
                        <typeahead class="form-control" :suggestions="studies" field="code" @selection="studyselection(selection.study, 'studyId');" v-model="selection.study.code"></typeahead>	                   
	                </div>
	                <div class="col-sm-9">
	                    <p class="form-control-plaintext" v-if="selection && selection.study">{{ selection.study.name }}</p>
	                </div>
	            </div>
	        </form-group> 	  
		
		    <form-group name="appId" label="admin_managenews.appId" :path="errors.appId">
	            <div class="row">
	                <div class="col-sm-3">
	                    <typeahead class="form-control" @selection="appselection(selection.app, 'appId');" v-model="selection.app.filename" :suggestions="apps" field="filename" />
	                </div>
	                <div class="col-sm-9">
	                    <p class="form-control-plaintext" v-if="selection && selection.app">{{ selection.app.name }} {{ selection.app.orgName }}</p>
	                </div>
	            </div>
	        </form-group> 	
		

		    <form-group label="common.empty">
		        <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary me-1" v-t="'common.submit_btn'"></button>		    
		        <button type="button" class="btn btn-danger" v-if="allowDelete" @click="doDelete()" :disabled="action!=null" v-t="'common.delete_btn'"></button>
		    </form-group>

            
	    </form>	  
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import news from "services/news.js"
import languages from "services/languages.js"
import studies from "services/studies.js"
import apps from "services/apps.js"

import { status, ErrorBox, CheckBox, FormGroup, Typeahead } from 'basic-vue3-components'

export default {

    data: () => ({	
        newsItem : {  },
        allowDelete : false,
	    languages : null,
        selection : { study : {}, app : {}, onlyStudy : {}, onlyApp : {} },
		layouts : ["large", "wide", "high", "small"],
        studies : null,
        apps : null
    }),

    components: {  Panel, ErrorBox, CheckBox, FormGroup, Typeahead },

    mixins : [ status ],

    methods : {
        getTitle() {
            const { $data, $t } = this;
            if ($data.newsItem && $data.newsItem._id) return $t('admin_managenews.title2');
            return $t('admin_managenews.title1');            
        },

        loadNews(newsId) {
            const { $data, $filters, $router } = this, me = this;
            me.doBusy(news.get({ "_id" : newsId }, ["content", "created", "date", "creator", "expires", "language", "studyId", "appId",  "title", "url", "layout"])
            .then(function(data) { 
                $data.newsItem = data.data[0];
                $data.newsItem.date = $filters.usDate($data.newsItem.date);
                $data.newsItem.expires = $filters.usDate($data.newsItem.expires);
                
                if ($data.newsItem.studyId) {
                    me.doBusy(studies.search({ _id : $data.newsItem.studyId }, ["_id", "code", "name" ])
                    .then(function(data) {
                        if (data.data && data.data.length == 1) {						 
                        $data.selection.study.code = data.data[0].code;
                        $data.selection.study.name = data.data[0].name;
                        }
                    }));
                }
                
                if ($data.newsItem.appId) {
                    me.doBusy(apps.getApps({ _id : $data.newsItem.appId }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"])
                    .then(function(data) {
                        if (data.data && data.data.length == 1) {					  
                            $data.selection.app.filename = data.data[0].filename;
                            $data.selection.app.name = data.data[0].name;
                            $data.selection.app.orgName = data.data[0].orgName;
                        }
                    }));
                }
            }));
	    },
	
        updateNews() {
			const { $data, $route, $router } = this, me = this;
            if ($data.newsItem._id == null) {
                me.doAction('submit', news.add($data.newsItem))
                .then(function(data) { 
                    $router.push({ path : "./news" }); 
                });
            } else {			
                me.doAction('submit', news.update($data.newsItem))
                .then(function() { 
                    $router.push({ path : "./news" }); 
                });
            }
	    },
	
        doDelete() {
            const { $data, $route, $router } = this, me = this;
            me.doAction('delete', news.delete($data.newsItem._id))
            .then(function(data) { 
                $router.push({ path : "./news" }); 
            });
	    },

        studyselection(study, field) {            
            const { $data, $route, $router } = this, me = this;
		    me.doSilent(studies.search({ code : study.code }, ["_id", "code", "name" ])
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $data.newsItem[field] = data.data[0]._id;
				  study.code = data.data[0].code;
				  study.name = data.data[0].name;
				}
			}));
	    },
	
	    appselection(app, field) {
            const { $data, $route, $router } = this, me = this;
		    me.doSilent(apps.getApps({ filename : app.filename }, ["_id", "filename", "name", "orgName", "type", "targetUserRole"])
			.then(function(data) {
				if (data.data && data.data.length == 1) {
				  $data.newsItem[field] = data.data[0]._id;
				  app.filename = data.data[0].filename;
				  app.name = data.data[0].name;
				  app.orgName = data.data[0].orgName;
				}
			}));
	    }
	
	

         
    },
    
    created() {
        const { $data, $route, $router } = this, me = this;
        $data.allowDelete = $route.meta.allowDelete;
        let langs = [];
        for (let i=0;i<languages.array.length;i++) langs.push(languages.array[i]);
        langs.push("int");
        $data.languages = langs;

        if ($route.query.newsId != null) { me.loadNews($route.query.newsId); }
	    
        me.doBusy(studies.search({ validationStatus : "VALIDATED" }, ["_id", "code", "name" ])
        .then(function(data) {
            $data.studies = data.data;
        }));
	
	    me.doBusy(apps.getApps({  }, ["creator", "developerTeam", "filename", "name", "description", "type", "targetUserRole" ])
        .then(function(data) { 
            $data.apps = data.data;			
        }));	
        console.log("FIN CREATED");
    }
}
</script>
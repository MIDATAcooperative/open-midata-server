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
    <study-nav page="study.messages" :study="study"></study-nav>
    <tab-panel :busy="isBusy">   
        
        <div v-if="!selmsg">
            
            <error-box :error="error"></error-box>    
            <table class="table table-striped" v-if="messages.length">
				<thead>
                <tr>
                    <th v-t="'appmessages.reason'"></th>
                    <th v-t="'studyfields.group_name'"></th>
                    <th v-t="'appmessages.languages'"></th>
                    <th>&nbsp;</th>
                </tr>
				</thead>
				<tbody>
                <tr v-for="msg in messages" :key="JSON.stringify(msg)">
                    <td><a @click="showMessage(msg)" href="javascript:">{{ $t('appmessages.reasons.' + msg.reason) }}</a></td>
                    <td>{{ msg.code }}</td>
                    <td>
                        <span v-for="l in languages" :key="l" @click="showMessage(msg,l)">
                            <span v-if="msg.text[l]"><span class="fas fa-check" aria-hidden="true"></span>{{l}}&nbsp;</span>              
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-default" @click="showMessage(msg)" v-t="'common.view_btn'"></button>
                    </td>
                </tr>
				</tbody>
            </table>
                            
            <p v-if="!messages.length" v-t="'appmessages.empty'"></p>
            
            <router-link class="btn btn-default me-1" :to="{ path : './manageapp', query : { studyId : study._id } }" v-t="'common.back_btn'"></router-link>        
            <button class="btn btn-default" @click="addMessage()" v-t="'common.add_btn'"></button>
            
        </div>
        <div v-else>        
                        
            <form name="myform" ref="myform" novalidate role="form" class="form-horizontal" @submit.prevent="updateProject()">
                <error-box :error="error"></error-box>                                        
                <form-group name="reason" label="appmessages.reason" :path="errors.reason">
                    <select id="reason" name="reason" class="form-control" v-validate v-model="selmsg.reason">
                        <option v-for="reason in reasons" :key="reason" :value="reason">{{ $t('appmessages.reasons.' + reason) }}</option>
                    </select>           
                </form-group>
            
                <form-group v-if="selmsg && selmsg.reason && selmsg.reason != 'PROJECT_PARTICIPATION_REQUEST'" name="code" label="studyfields.group_name" :path="errors.code">
                   <select id="code" name="code" v-validate v-model="selmsg.code" class="form-control">
                     <option value="">{{ $t("studymessages.all") }}</option>
                     <option v-for="group in study.groups" :key="group.name">{{ group.name }}</option>
                   </select>                             
                </form-group>
                
                <hr>    
                        
                <form-group label="Multi Language Support">
                    <div class="form-text text-muted">
                        <span v-for="l in languages" :key="l" @click="showMessage(selmsg,l)">
                            <span v-if="selmsg.text[l]"><span class="fas fa-check" aria-hidden="true"></span>{{l}} </span>            
                        </span>
                    </div>
                </form-group>
                        
                                                                    
                <form-group name="lang" label="appmessages.lang" :path="errors.lang">
                    <select id="lang" name="lang" class="form-control" v-validate v-model="sel.lang">
                        <option v-for="lang in languages" :key="lang" :value="lang">{{ lang }}</option>
                    </select>           
                </form-group>
            
            
                <form-group name="title" label="appmessages.msgtitle" :path="errors.title">
                    <input type="text" id="title" name="title" class="form-control" v-validate v-model="selmsg.title[sel.lang]">
                </form-group>
                <form-group name="text" label="appmessages.text" :path="errors.text">
                    <textarea rows="5" id="text" name="text" class="form-control" v-validate v-model="selmsg.text[sel.lang]"></textarea>
                    <div class="form-text text-muted">
                        <span v-t="'appmessages.available_tags'"></span>:
                        <code v-for="tag in tags[selmsg.reason]" :key="tag">&lt;{{ tag }}&gt; </code>
                    </div>
                </form-group>
                    
                <form-group label="common.empty">
                    <button type="submit" v-submit :disabled="action!=null" class="btn btn-primary me-1">Submit</button>    
                    <button type="button" class="btn btn-danger" v-t="'common.delete_btn'" @click="deleteMessage(msg)"></button>        
                </form-group>
            </form>   
        </div>
            
    </tab-panel>
</div>      

</template>
<script>

import TabPanel from "components/TabPanel.vue"
import StudyNav from "components/tiles/StudyNav.vue"
import languages from "services/languages.js"
import server from "services/server.js"
import { addBundle } from "services/lang.js";
import { status, ErrorBox, Success, FormGroup } from 'basic-vue3-components'

export default {
    data: () => ({  
       
        studyid : null,
        study : null,

        languages : languages.array,
        reasons : ['PROJECT_PARTICIPATION_REQUEST', 'PROJECT_PARTICIPATION_APPROVED', 'PROJECT_PARTICIPATION_REJECTED', 'PROJECT_PARTICIPATION_GROUP_ASSIGNED', 'PROJECT_PARTICIPATION_RETREAT'],
        sel : { lang : 'en' },
        selmsg : null,
        messages : [],
        tags : {
            'PROJECT_PARTICIPATION_REQUEST': ["site", "pseudonym", "participation-id", "project-group", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'PROJECT_PARTICIPATION_APPROVED': ["site", "pseudonym", "participation-id", "project-group", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'PROJECT_PARTICIPATION_REJECTED' : ["site", "pseudonym", "participation-id", "project-group", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'PROJECT_PARTICIPATION_GROUP_ASSIGNED' : ["site", "pseudonym", "participation-id", "project-group", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
            'PROJECT_PARTICIPATION_RETREAT' : ["site", "pseudonym", "participation-id", "project-group", "firstname", "lastname", "email", "plugin-name", "midata-portal-url"],
        }
    }),

    components: {  TabPanel, StudyNav, ErrorBox, FormGroup, Success },

    mixins : [ status ],

    methods : {

        reload() {
            const { $data } = this, me = this;
            
            me.doBusy(server.get(jsRoutes.controllers.research.Studies.get($data.studyid).url)
            .then(function(data) {              
             
               let study = data.data;
               if (!study.predefinedMessages) study.predefinedMessages = {};
               $data.study = study;
               $data.messages = [];  
               $data.selmsg = null;              
               for (let msg in study.predefinedMessages) {
                 if (!study.predefinedMessages[msg].code) study.predefinedMessages[msg].code = ""; 
                 $data.messages.push(study.predefinedMessages[msg]); 
               }
             }));
           
        },
    
    
        updateProject() {                   
            const { $data, $route } = this, me = this;
            let predefinedMessages = {};
            for (let msg of $data.messages) {
            
                for (let k in msg.text) if (msg.text[k]==="") { delete msg.text[k]; }
                for (let k in msg.title) if (msg.title[k]==="") { delete msg.title[k]; }
                if (msg.reason == 'PROJECT_PARTICIPATION_REQUEST') msg.code = "";
                                
                predefinedMessages[msg.reason+(msg.code ? ("_"+msg.code) : "")] = msg;
            }
            $data.study.predefinedMessages = predefinedMessages;
            let data = { _id : $data.studyid, predefinedMessages : predefinedMessages };           
            me.doAction("change", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($data.studyid).url, data)
            .then(function() {              
                me.reload();            
             }));      
        },
    
        addMessage() {
            const { $data } = this, me = this;
            $data.messages.push($data.selmsg = { reason:'PROJECT_PARTICIPATION_REQUEST' , code: "", title : {}, text: {} });
        },
    
        showMessage(msg, lang) {
            const { $data } = this, me = this;
            $data.selmsg = msg;
            $data.sel.lang = lang || $data.sel.lang;
        },
    
        deleteMessage() {
            const { $data } = this, me = this;
            $data.messages.splice($data.messages.indexOf($data.selmsg), 1);
            me.updateProject();
            //$scope.selmsg = null;
        }
    },

    created() {
        const { $route } = this, me = this;
        addBundle("developers");
        this.$data.studyid = $route.query.studyId;
        me.reload();
        
    }
}
</script>
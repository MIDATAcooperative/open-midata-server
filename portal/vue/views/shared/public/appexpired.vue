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
  <div class="container">
    <div class="row">

        <div class="col-sm-12">
            <panel :title="$t('appexpired.title')" style="max-width: 600px; padding-top: 20px; margin: 0 auto;"> 
                <div v-if="message" class="terms" v-html="message"></div>               
                <hr>
                <p v-t="'appexpired.intro'"></p>
               
                <button @click="showLogin()" type="button" class="btn btn-default btn-block" v-t="'appexpired.loginpage_btn'"></button>
                <div class="extraspace"></div>                        
                
            </panel>            
        </div>
    </div>
  </div>
</template>
<script>
import ENV from "config";
import sanitizeHtml from 'sanitize-html';
import server from "services/server.js";
import { status } from 'basic-vue3-components';
import Panel from 'components/Panel.vue';
import { getLocale, setLocale } from "services/lang.js";

function getAppInfo(name, type) {
    var data = { "name": name };
    if (type) data.type = type;
    return server.post(jsRoutes.controllers.Plugins.getInfo().url, data);
};


export default {
    data: () => ({
        ENV : {},
        app : null,
        message : "",
        lang : "en"
    }),
    
    components : {
      Panel
    },
    
    mixins : [ status ],

    methods : {
        showLogin() {       
            const { $router, $route } = this;
            $router.push({ path : "./login" });
        }
    },

    created() {
        const { $data, $route } = this;
        $data.ENV = ENV;
        $data.lang = $route.query.lang || getLocale();
        if ($data.lang != getLocale()) setLocale($data.lang);
        this.doBusy(getAppInfo($route.query.client_id)         
        .then(function(results) {
            $data.app = results.data;      
            if (!$data.app || !$data.app.targetUserRole) $data.error ="error.unknown.app";
            let msg = ($data.app.i18n[$data.lang] && $data.app.i18n[$data.lang].postLoginMessage) || ($data.app.i18n.en && $data.app.i18n.en.postLoginMessage) || "--";
            $data.message = sanitizeHtml(msg);
        }));    
        
    }
}
</script>
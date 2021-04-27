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
    <panel :title="$t('admin_news.title')" :busy="isBusy">
    
        <error-box :error="error"></error-box>
        <pagination v-model="news"></pagination>
        <table class="table" v-if="news.filtered.length">
            <thead>
                <tr>
                    <Sorter v-model="news" sortby="created" v-t="'admin_news.created'"></Sorter>
                    <Sorter v-model="news" sortby="language" v-t="'admin_news.language'"></Sorter>
                    <Sorter v-model="news" sortby="title" v-t="'admin_news.title'"></Sorter>
                    <Sorter v-model="news" sortby="expires" v-t="'admin_news.expires'"></Sorter>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="item in news.filtered" :key="item._id">
                    <td>{{ $filters.date(item.created) }}</td>
                    <td>{{ item.language }}</td>
                    <td><router-link :to="{ path : './managenews', query :  { newsId : item._id } }">{{ item.title }}</router-link></td>
                    <td>{{ $filters.date(item.expires) }}</td>
                    <td><button class="btn btn-danger btn-sm" v-t="'admin_news.delete_btn'" @click="deleteNews(item)"></button></td>  
                </tr>
            </tbody>
      </table>
      <p v-else v-t="'admin_news.empty'"></p>
      
      <router-link class="btn btn-primary" v-t="'common.add_btn'" :to="{ path : './newnews' }"></router-link> 
            
    </panel>
  	   
</template>
<script>

import Panel from "components/Panel.vue"
import news from "services/news.js"

import { status, rl, ErrorBox } from 'basic-vue3-components'

export default {

    data: () => ({	
        news : null
    }),

    components: {  Panel, ErrorBox },

    mixins : [ status, rl ],

    methods : {
        deleteNews(newsItem) {
            const me = this;
            me.doAction('delete', news.delete(newsItem._id))
            .then(function() { me.init(); });
        },

        init() {
            const { $data } = this, me = this;
            me.doBusy(news.get({ }, ["content", "created", "title", "studyId", "url", "expires", "language"])
            .then(function(results) {
                $data.news = me.process(results.data);
            }));
        }
    },
    
    created() {
        this.init();       
    }
}
</script>
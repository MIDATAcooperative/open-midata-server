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
<ul class="borderless nav nav-tabs">
  <li class="nav-item"><a :class="{ 'active' : page=='study.overview' }" href="javascript:" class="nav-link" @click="go('study.overview')" v-t="'studynav.overview'">Overview</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.info' }" href="javascript:" class="nav-link" @click="go('study.info')" v-t="'studynav.info'">Info</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.team' }" href="javascript:" class="nav-link" @click="go('study.team')" v-t="'studynav.team'">Team</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.fields' }" href="javascript:" class="nav-link" @click="go('study.fields')" v-t="'studynav.details'">Details</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.rules' }" href="javascript:" class="nav-link" @click="go('study.rules')" v-t="'studynav.rules'">Rules</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.actions' }" href="javascript:" class="nav-link" @click="go('study.actions')" v-t="'studynav.actions'">Actions</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.codes' }" href="javascript:" class="nav-link" @click="go('study.codes')" v-t="'studynav.codes'">Codes</a></li>
  <li v-if="fullMenu" class="nav-item"><a :class="{ 'active' : page=='study.participants' }" href="javascript:" class="nav-link" @click="go('study.participants')" v-t="'studynav.participants'">Participants</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.sharing' }" href="javascript:" class="nav-link" @click="go('study.sharing')" v-t="'studynav.sharing'">Sharing</a></li>
  <li v-if="fullMenu" class="nav-item"><a :class="{ 'active' : page=='study.records' }" href="javascript:" class="nav-link" @click="go('study.records')" v-t="'studynav.records'">Records</a></li> 
</ul>
</template>
<script>
export default {    
    props : ['page'],

    data : ()=>({      
        fullMenu : true
    }),

    methods : {
        go(page) {
            this.$router.push({ path : './'+page, query : { studyId : this.$route.query.studyId} });
        }
    },

    created() {        
        if (this.$route.meta.role != "research") this.$data.fullMenu = false;
    }
}
</script>
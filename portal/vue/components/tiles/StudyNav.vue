<template>
<ul class="borderless nav nav-tabs">
  <li class="nav-item"><a :class="{ 'active' : page=='study.overview' }" href="javascript:" class="nav-link" @click="go('study.overview')" v-t="'studynav.overview'">Overview</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.info' }" href="javascript:" class="nav-link" @click="go('study.info')" v-t="'studynav.info'">Info</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.team' }" href="javascript:" class="nav-link" @click="go('study.team')" v-t="'studynav.team'">Team</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.fields' }" href="javascript:" class="nav-link" @click="go('study.fields')" v-t="'studynav.details'">Details</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.rules' }" href="javascript:" class="nav-link" @click="go('study.rules')" v-t="'studynav.rules'">Rules</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.actions' }" href="javascript:" class="nav-link" @click="go('study.actions')" v-t="'studynav.actions'">Actions</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.codes' }" href="javascript:" class="nav-link" @click="go('study.codes')" v-t="'studynav.codes'">Codes</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.participants' }" href="javascript:" class="nav-link" @click="go('study.participants')" v-t="'studynav.participants'">Participants</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.sharing' }" href="javascript:" class="nav-link" @click="go('study.sharing')" v-t="'studynav.sharing'">Sharing</a></li>
  <li class="nav-item"><a :class="{ 'active' : page=='study.records' }" href="javascript:" class="nav-link" @click="go('study.records')" v-t="'studynav.records'">Records</a></li> 
</ul>
</template>
<script>
export default {    
    props : ['page'],
    methods : {
        go(page) {
            this.$router.push({ path : './'+page, query : { studyId : this.$route.query.studyId} });
        }
    }
}
</script>

<template>         
    <span class="text-success" v-if="finished==action">
        &nbsp;<span class="fas fa-check"></span> <span v-t="msg"></span>
    </span>    
</template>
<script>
export default {    
  props: ['action','msg', 'finished']
}
</script>


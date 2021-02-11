<template>
    <div class="modal" style="display:block">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h4 class="modal-title">{{ title }}</h4>
                    <button type="button" class="close" @click="$emit('close')" aria-hidden="true">&times;</button>
                </div>
            <div class="modal-body"><slot></slot></div>
            </div>
        </div>        
    </div>
</template>
<script>
export default {
    props : [ "title" ],
    emits : [ "close" ]
}
</script>
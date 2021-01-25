import _ from "lodash";
import { computed, reactive } from "vue";

export default {

    methods : {
        process(result, options) {
            options = options || {};
            let input = reactive({ 
                all:result, 
                filtered:[], 
                sort:options.sort, 
                filter:options.filter,
                total:0,
                pagesize:options.pagesize || 5,
                pages:[],
                current:1,
                filtered:computed(() => this.calc(input))
            });                     
            return input;
        },

        calc(input) {
            let process = input.all;

            if (input.filter) {
                for (let key in input.filter) {                    
                    let v = input.filter[key];
                    if (v && v!="") {
                        let filterMethod = (t) => (t[key] && t[key].indexOf(v)>=0);
                        process = _.filter(process, filterMethod);
                    }
                }                
            }

            if (input.sort) {
                let field = input.sort;
                let direction = "asc";        
                if (field.startsWith("-")) { field = field.substr(1); direction = "desc"; }
                process = _.orderBy(process, [ field ], [ direction ]);  
            }

            input.total = process.length;

            if (input.pagesize && input.total > input.pagesize) {
                let pages = [];
                let pageCount = Math.floor(input.total / input.pagesize)+1;
                for (let i=1;i<=pageCount;i++) pages.push(i);
                input.pages = pages;
                if (input.current > pageCount) input.current = 1;
                let end = input.current * input.pagesize;
                if (end > input.total) end = input.total+1;
                process = _.slice(process, (input.current-1) * input.pagesize, end);
            } else {
                input.pages = 0;
                input.current = 1;                
            }

            //input.filtered = process;
            console.log(process);
            return process;
        }
    }
}
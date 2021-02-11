/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

export default {
    data : ()=>({
        error : null,
        errors : { _custom : [] },
        loading : 0,
		isBusy : true,
		action : null,  
		finished : null      
    }),

    methods : {
        loadStart() { 
            this.$data.loading++; this.$data.isBusy = true; if (this.$data.loading==1) this.$data.error = null; 
        },
		loadEnd() { 
			this.$data.loading--; 
			if (this.$data.loading<=0) { 
				this.$data.isBusy = false; 
				this.$data.finished = this.$data.action;
				this.$data.action=null; 
			} 
		},
		ready() {
			this.loadEnd();
		},
		loadStartAction(action) { 
            this.$data.loading++; this.$data.action = action; if (this.$data.loading==1) {
				this.$data.error = null; 
				this.$data.finished = null;
			}
        },
		loadFailure(msg, noerror) { 
			   console.log(msg);
			   if (msg.response) msg = msg.response;
			   this.$data.loading--; 
			   if (!noerror) {
				   if (msg && msg.data) this.$data.error = msg.data.code || msg.data; else this.$data.error = msg;
			   }
			   if (this.$data.loading<=0) { this.$data.isBusy = false;this.$data.action=null; }
			   if (msg.status == 403 || msg.status == 401) {
				   // Now handeled by http interceptor
				   //alert("Please relogin. Your session has expired.");
				   //$state.go("public.login");
			   } else if (msg.status == 503 || msg.status == -1) {
				   console.log("RELOAD");
				   document.location.reload();
			   } else {
			     //if (this.$data.showerrors && !noerror) alert("An error "+msg.status+" occured:"+(msg.data.message || msg.data));
			   }
		},
		doBusy(call) {
			var me = this;
		   	me.loadStart();		 
		   	return call.then(function(result) { me.loadEnd();return result; }, function(err) { me.loadFailure(err);return Promise.reject(err); });		     
		},
		doAction(action, call) {
            var me = this;
            if (this.$refs.myform) this.$refs.myform.classList.remove("was-validated");
		   	me.loadStartAction(action);
		   	return call.then(function(result) { me.loadEnd();return result; }, function(err) { 		 
                let response = err.response || err;
                
		   		if (response.data && response.data.field && response.data.type && me.$refs.myform) {		   			
					me.setError(response.data.field, response.data.code)                       
                    me.loadFailure(response, true);		   			
		   		} else {                       
                    me.loadFailure(response);
                }
		   		
		   		return Promise.reject(err); 
		    });		     
		},
		doSilent(call) {			
			const { $data } = this, me = this;	
			$data.finished = null;
			return call.then(function(result) { return result; }, function(err) { me.loadFailure(err);return Promise.reject(err); });
		},
		setError(field, msg) {
			const { $data } = this;	
			$data.finished = null;
			let myform = this.$refs.myform;
			if (myform) {
				myform.classList.remove("was-validated");
			
				let elem = myform[field];		
				if (elem) {
					elem.classList.add("is-invalid");
					$data.errors[field] = msg;
					$data.errors._custom.push(field);
				} else $data.error = msg;
			} else $data.error = msg;
			
		}
    }

};
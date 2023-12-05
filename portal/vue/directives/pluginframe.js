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

	mounted: function( elem, binding ) {
		
		var html = '<iframe name="'+elem.id+'" src="'+binding.value+'" height="'+elem.style.minHeight+'" width="'+elem.style.width+'"></iframe>';    			
		elem.innerHTML = html;

		const vm = binding.instance;
		elem.myListener = function(e) {
			let data = e.data;
			if (data && data.name == elem.id && data.viewHeight && data.viewHeight !== "0px") {
				  //console.log("adjust height for "+elem.id+" to:"+data.viewHeight);
				  elem.firstElementChild.height = data.viewHeight;
			}  else if (data && data.name == elem.id && data.type==="link") {    	  		 
				 //console.log(data);
				 if (vm.openAppLink) vm.openAppLink(data);
			}
		}

		window.addEventListener('message', elem.myListener);
	},
	
	unmounted: function(elem) {		
		if (elem.myListener) window.removeEventListener('message', elem.myListener);
	}
    
}
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

package models.stats;

import models.MidataId;
import models.Model;
import models.Plugin;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class MonitoringEvent extends Model {

	protected @NotMaterialized static final String collection = "monitoringevent";
	
	public MidataId plugin;
	
	public String pluginName;
		
	public String action;
			
	public int timeslot;
	
	public MonitoringType type;
	
	public int count;
	
	public int avg;
	
	public int var;
	
	public MonitoringEvent() {}
	
	public MonitoringEvent(MonitoringStats stats) throws InternalServerException {
		this._id = new MidataId();
		this.plugin = stats.plugin;
		Plugin pl = Plugin.getById(plugin);
		if (pl != null) this.pluginName = pl.filename; else this.pluginName = "";
		this.action = stats.action;
		this.timeslot = stats.timeslot;		
	}
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
	private String getTs() {
		if (timeslot < 0) return "";
		if (timeslot >= 24) {
			return " at weekend "+(timeslot-24)+":00-"+(timeslot-24)+":59";
		}
		return " at "+timeslot+":00-"+timeslot+":59";
	}
	
	public String toString() {
		String m = "requests with errors ";
		switch(type) {
		case HIGH_USE: m = "too many requests ";break;
		case LOW_USE: m = "not enough requests ";break;		 
		}
		int min = avg-var;
		int max = avg+var * 2;
		if (min < 0) min = 0;
		return ("".equals(pluginName) ? "Platform total" : pluginName)+getTs()+(action!=null?" with action "+action:"")+": "+m+count+", expected "+min+" to "+max;
				  
	}
}

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

import java.util.Set;

import models.GroupContent;
import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class MonitoringStats extends Model {

	protected @NotMaterialized static final String collection = "monitorstats";
	
	public @NotMaterialized static final Set<String> ALL = Sets.create("version", "plugin", "action", "timeslot", "requestsCurrent", "requestsAvg", "requestsVar", "errorsAvg", "errorsVar", "errorsCurrent", "generation", "reported", "changed", "path");
		
			
	public String path;
	
    public MidataId plugin;
		
	public String action;
		
	public int timeslot;
	
	public int requestsCurrent;
	
	public double requestsAvg;
	
	public double requestsVar;
	
	public double errorsAvg;
	
	public double errorsVar;
	
	public int errorsCurrent;
	
	public int generation;
	
	public boolean reported;
	
	public boolean changed;
	
	public MonitoringStats() {}
		
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
	public void upsert() throws InternalServerException {
		Model.upsert(collection, this);
	}
	
	public void delete() throws InternalServerException {
		Model.delete(MonitoringStats.class, collection, CMaps.map("_id", _id));
	}
	
	public static Set<MonitoringStats> getAllByTimeslot(int timeslot) throws InternalServerException {
		return Model.getAll(MonitoringStats.class, collection, CMaps.map("timeslot", timeslot), ALL);
	}
	
	public static Set<MonitoringStats> getAllUsedByTimeslot(int timeslot) throws InternalServerException {
		return Model.getAll(MonitoringStats.class, collection, CMaps.map("timeslot", timeslot).map("changed", true), ALL);
	}
	
	public static MonitoringStats getById(MidataId id) throws InternalServerException {
		return Model.get(MonitoringStats.class, collection, CMaps.map("_id", id), ALL);
	}
	
	public static MonitoringStats getByPath(String path) throws InternalServerException {
		return Model.get(MonitoringStats.class, collection, CMaps.map("path", path), ALL);
	}
	
	public String getPath() {
		if (plugin == null) return "t="+Integer.toString(timeslot);
		if (action != null) return plugin.toString()+";a="+action;
		return plugin.toString()+";t="+Integer.toString(timeslot);
	}
	
	public void calc() {
		int WINDOW_SIZE = timeslot==-1 ? 240 : 10;
		
		double reqAvg = (requestsAvg * (WINDOW_SIZE - 1) + requestsCurrent) / WINDOW_SIZE;
		double diff = Math.abs(requestsCurrent - requestsAvg);
		double reqVar = diff > requestsVar ? diff : (requestsVar * (WINDOW_SIZE - 1) + diff) / WINDOW_SIZE;
		
		if (requestsCurrent > 0) {
			double errRate = errorsCurrent;// * 100 / requestsCurrent;		
			double errAvg = (errorsAvg * (WINDOW_SIZE - 1) + errRate) / WINDOW_SIZE;
			double ediff = Math.abs(errRate - errorsAvg);
			double errVar = ediff > errorsVar ? ediff : (errorsVar * (WINDOW_SIZE - 1) + ediff) / WINDOW_SIZE;
			
			errorsAvg = errAvg;
			errorsVar = errVar;
			errorsCurrent = 0;			
		}
		
		requestsAvg = reqAvg;
		requestsVar = reqVar;
		requestsCurrent = 0;	
		
		generation++;
		changed = false;
		reported = false;		
		
	}
	
	public boolean isUnused() {
		return timeslot==-1 ? (requestsAvg < 0.004 && requestsVar < 1) : (requestsAvg < 0.1 && requestsVar < 1);
	}
}

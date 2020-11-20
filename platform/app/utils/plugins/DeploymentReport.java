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

package utils.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.MidataId;
import models.Model;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.AppException;

public class DeploymentReport extends Model {
	
	@NotMaterialized
	private static final String collection = "deployreport";
	
	@NotMaterialized
	public static Set<String> ALL = Collections.unmodifiableSet(Sets.create("_id", "status", "clusterNodes", "checkoutReport", "installReport", "buildReport", "auditReport", "sceduled", "started", "finished"));
	
	public Set<DeployPhase> status;
	
	public Set<String> clusterNodes;
	
	public Map<String, String> checkoutReport;
	
	public Map<String, String> installReport;
	
	public Map<String, String> buildReport;
	
	public Map<String, String> auditReport;
	
	public long sceduled;
	
	public long started;
	
	public long finsihed;
	
	public void init() {
		clusterNodes = new HashSet<String>();	
		status = new HashSet<DeployPhase>();
		checkoutReport = new HashMap<String, String>();		
		installReport = new HashMap<String, String>();		
		buildReport = new HashMap<String, String>();		
		auditReport = new HashMap<String, String>();
	}
	
	public void add() throws AppException {
		Model.upsert(collection, this);
	}
	
	public static DeploymentReport getById(MidataId id) throws AppException {
		return Model.get(DeploymentReport.class, collection, CMaps.map("_id", id), ALL);
	}
}

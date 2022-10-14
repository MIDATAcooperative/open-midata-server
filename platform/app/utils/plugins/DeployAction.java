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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.stream.SourceRef;
import models.MidataId;

public class DeployAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1795201835146962857L;

	public final MidataId pluginId;
	
	public final MidataId userId;
			
	public final DeployPhase status;
	
	public final String clusterNode;
	
	public final Map<String, String> report;
	
	public final boolean success;
	
	public final ActorRef replyTo;
	
	public final SourceRef exportedData;
			
	public DeployAction(DeployAction msg, String clusterNode, DeployPhase status, Map<String, String> report, boolean success, ActorRef replyTo, SourceRef exportedData) {
		this.pluginId = msg.pluginId;
		this.userId = msg.userId;
		this.clusterNode = clusterNode;
		this.status = status;
		if (report != null) {
			this.report = new HashMap<String, String>();
			this.report.putAll(report);
		} else this.report = null;
		this.success = success;
		this.replyTo = replyTo;
		this.exportedData = exportedData;
	}
	
	public DeployAction newPhase(DeployPhase status, ActorRef replyTo) {
		return new DeployAction(this, null, status, null, true, replyTo, exportedData);
	}
	
	public DeployAction forward(String clusterNode) {
		return new DeployAction(this, clusterNode, status, report, success, replyTo, exportedData);
	}
	
	public DeployAction response(DeployPhase status, boolean success, String report) {
		Map<String, String> mreport = new HashMap<String, String>();
		if (clusterNode==null) mreport.put("all",  report); else mreport.put(clusterNode, report);
		return new DeployAction(this, clusterNode, status, mreport, success, replyTo, null);	
	}
	
	public DeployAction response(boolean success, Map<String, String> reports) {
		return new DeployAction(this, clusterNode, status, reports, success, replyTo, null);
	}
	
	public DeployAction response(DeployPhase status, SourceRef exportedData) {
		return new DeployAction(this, clusterNode, status, report, true, replyTo, exportedData);
	}
		
	public DeployAction(MidataId pluginId, MidataId userId, DeployPhase status) {
		this.pluginId = pluginId;
		this.userId = userId;
		this.status = status;
		this.report = null;
		this.success = true;
		this.clusterNode = null;
		this.replyTo = null;
		this.exportedData = null;
	}
}


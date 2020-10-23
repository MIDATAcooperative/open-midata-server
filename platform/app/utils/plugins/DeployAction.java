/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.plugins;

import java.io.Serializable;

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
	
	public final String report;
	
	public final boolean success;
			
	public DeployAction(DeployAction msg, String clusterNode, DeployPhase status, String report, boolean success) {
		this.pluginId = msg.pluginId;
		this.userId = msg.userId;
		this.clusterNode = clusterNode;
		this.status = status;
		this.report = report;
		this.success = success;
	}
	
	public DeployAction(DeployAction msg, DeployPhase status) {
		this.pluginId = msg.pluginId;
		this.userId = msg.userId;
		this.status = status;
		this.report = null;
		this.success = true;
		this.clusterNode = null;
	}
	
	public DeployAction(MidataId pluginId, MidataId userId, DeployPhase status) {
		this.pluginId = pluginId;
		this.userId = userId;
		this.status = status;
		this.report = null;
		this.success = true;
		this.clusterNode = null;
	}
}


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


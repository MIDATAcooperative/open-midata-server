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

import akka.actor.AbstractActor;
import akka.actor.AbstractActor.Receive;
import utils.ErrorReporter;
import utils.ServerTools;
import utils.exceptions.AppException;
import utils.stats.ActionRecorder;

public abstract class AbstractCDNContainer extends AbstractContainer {


	void deploy(DeployAction msg) {
		String path = "PluginDeployment/cdn";
		long st = ActionRecorder.start(path);
		try {
			defaultMessages(msg);
		} catch (AppException e) {			
		   ErrorReporter.report("CDNContainer", null, e);
		} finally {
		  ServerTools.endRequest();
		  ActionRecorder.end(path, st);
		}
	
	}
	
	abstract void doAction(DeployAction msg) throws AppException;
	
	abstract void publishAction(DeployAction msg) throws AppException;
	
}

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

package utils.messaging;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import controllers.Circles;
import controllers.Plugins;
import models.APSNotExistingException;
import models.BackgroundAction;
import models.Consent;
import models.MidataId;
import models.MobileAppInstance;
import models.Plugin;
import models.SubscriptionData;
import models.TestPluginCall;
import models.User;
import models.enums.AuditEventType;
import models.enums.ConsentStatus;
import models.enums.MessageChannel;
import models.enums.MessageReason;
import models.enums.PluginStatus;
import models.enums.UserRole;
import models.enums.UserStatus;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;
import utils.access.RecordManager;
import utils.audit.AuditEventBuilder;
import utils.audit.AuditManager;
import utils.auth.KeyManager;
import utils.auth.SpaceToken;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.context.ContextManager;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.SubscriptionResourceProvider;
import utils.stats.ActionRecorder;
import utils.stats.Stats;
import utils.sync.Instances;

public class DelayedActionsProcessor extends AbstractActor {
	
	private Map<MidataId, BackgroundAction> running;
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		running = new HashMap<MidataId, BackgroundAction>();
		
		Set<BackgroundAction> all = BackgroundAction.getAll();
		for (BackgroundAction ba : all) {
			getSelf().tell(ba, ActorRef.noSender());
		}
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			   .match(BackgroundAction.class, this::processBackgroundAction)	
			   .match(SubscriptionsDoneMessage.class, this::subscriptionsDoneMessage)
			   .build();
	}
	
	void processBackgroundAction(BackgroundAction ba) {
		String path = "DelayedActionsProcessor/processBackgroundAction";
		long st = ActionRecorder.start(path);
		try {		
			AccessLog.logStart("jobs", ba.toString());
						
			Consent consent = Consent.getByIdUncheckedAlsoDeleted(ba.targetId, Consent.FHIR);
			if (consent != null && consent.reportedStatus != null) {
				ResourceChange change = new ResourceChange("fhir/Consent", consent, false, ba.resource, consent.owner, ba._id);
				running.put(ba._id, ba);
				SubscriptionManager.resourceChange(change, getSelf());
			} else {
				ba.delete();
			}
		} catch (Exception e) {			
			ErrorReporter.report("DelayedActionsProcessor", null, e);
				
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	void subscriptionsDoneMessage(SubscriptionsDoneMessage doneMsg) {
		String path = "DelayedActionsProcessor/processBackgroundAction";
		long st = ActionRecorder.start(path);
		try {		
			AccessLog.logStart("jobs", doneMsg.toString());
			BackgroundAction ba = running.get(doneMsg.getTransactionId());
			String handle = ServiceHandler.decrypt(ba.session);
			KeyManager.instance.continueSession(handle, ba.owner);
			AccessContext context = ContextManager.instance.createSessionForDownloadStream(ba.owner, UserRole.ANY);
			Consent consent = Consent.getByIdUncheckedAlsoDeleted(ba.targetId, Consent.FHIR);
			if (consent != null) Circles.consentStatusChange(context, consent, consent.reportedStatus);
			ba.delete();			
		} catch (Exception e) {			
			ErrorReporter.report("DelayedActionsProcessor", null, e);
				
		} finally {
			ServerTools.endRequest();
			ActionRecorder.end(path, st);
		}
	}
	
	
	
	
	
	

}


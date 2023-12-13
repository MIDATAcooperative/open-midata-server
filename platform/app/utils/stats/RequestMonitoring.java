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

package utils.stats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import models.MidataId;
import models.stats.MonitoringEvent;
import models.stats.MonitoringStats;
import models.stats.MonitoringType;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.RuntimeConstants;
import utils.ServerTools;
import utils.exceptions.InternalServerException;
import utils.messaging.MailSenderType;
import utils.messaging.MailUtils;
import utils.sync.Instances;

public class RequestMonitoring {

	private static ActorRef monitoringCollector;
	private static ActorRef monitoringReporter;
	
	public static void init(ActorSystem system1) {
	   monitoringCollector = system1.actorOf(Props.create(MonitoringCollectorActor.class).withDispatcher("quick-work-dispatcher"), "monitoringCollectorActor");
	   
	   final ClusterSingletonManagerSettings settings =
				  ClusterSingletonManagerSettings.create(system1);
		
		ActorRef managerSingleton = system1.actorOf(ClusterSingletonManager.props(Props.create(MonitoringReporterActor.class).withDispatcher("medium-work-dispatcher"), PoisonPill.getInstance(), settings), "monitoringactor");
		
		final ClusterSingletonProxySettings proxySettings =
			    ClusterSingletonProxySettings.create(Instances.system());

		
		monitoringReporter = system1.actorOf(ClusterSingletonProxy.props("user/monitoringactor", proxySettings).withDispatcher("medium-work-dispatcher"), "monitoringactorProxy");		   	 
	}
	
	public static void report(MidataId plugin, String path, boolean isError) {
		if (plugin == null) plugin = RuntimeConstants.instance.portalPlugin;
		monitoringCollector.tell(new MonitoringMessage(plugin, path, System.currentTimeMillis(), isError), ActorRef.noSender());
	}
	
		
	public static void update(MonitoringBulkMessage msg, ActorRef sender) {
		monitoringReporter.tell(msg, sender);
	}
	
	public static void flush() {
		monitoringCollector.tell(new FlushMessage(), ActorRef.noSender());
		monitoringReporter.tell(new FlushMessage(), ActorRef.noSender());
	}
}

class MonitoringMessage {
	
	public final MidataId plugin;
	
	public final String action;
		
	public final long time;
	
	final boolean isError;
	
	MonitoringMessage(MidataId plugin, String action, long time, boolean isError) {
		this.plugin = plugin;
		this.action = action;
		this.time = time;
		this.isError = isError;
	}
}

class MonitoringBulkMessage {
	
	public final MidataId plugin;
	
	public final String action;
		
	public final int timeslot;
	
	public final int requests;
	
	public final int errors;
	
	MonitoringBulkMessage(MonitoringStats stats) {
		this.plugin = stats.plugin;
		this.action = stats.action;
		this.timeslot = stats.timeslot;
		this.requests = stats.requestsCurrent;
		this.errors = stats.errorsCurrent;
	}
	
	public String getPath() {
		if (plugin == null) return "t="+Integer.toString(timeslot);
		if (action != null) return plugin.toString()+";a="+action;
		return plugin.toString()+";t="+Integer.toString(timeslot);
	}
	
	public MonitoringStats createStats() {
		MonitoringStats result = new MonitoringStats();
		result._id = new MidataId();
		result.path = getPath();
		result.plugin = plugin;
		result.action = action;
		result.timeslot = timeslot;
		result.requestsCurrent = requests;
		result.errorsCurrent = errors;
		result.changed = true;		
		return result;
		
		
	}
}

class MonitoringCollectorActor extends AbstractActor {
	
	private int currentTimeslot;
	private int flushCount;
	private Cancellable timer;
	
	private Map<String, MonitoringStats> workingSet;
		
	
	@Override
	public void preStart() throws Exception {
		workingSet = new HashMap<String, MonitoringStats>();
		flushCount = 0;
		
		timer = getContext().system().scheduler().scheduleWithFixedDelay(Duration.ofMinutes(5), Duration.ofMinutes(5), getSelf(), new FlushMessage(),
				getContext().system().dispatcher(), null);
	}
	
	@Override
	public void postStop() throws Exception {
		flush();
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		workingSet = null;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		  .match(MonitoringMessage.class, this::updateStats)	
		  .match(FlushMessage.class, this::flush)
		  .build();
	}
	
	public void updateStats(MonitoringMessage msg) throws Exception {
		
			currentTimeslot = getCurrentTimeslot();
			
			String tk = getTimeslotKey();
			MonitoringStats tsStats = workingSet.get(tk);
			if (tsStats == null) {
				tsStats = new MonitoringStats();
				tsStats.timeslot = currentTimeslot;
				tsStats.path = tsStats.getPath();
				workingSet.put(tsStats.path, tsStats);				
			}
			
			String pk = getPluginKey(msg);
			MonitoringStats pkStats = workingSet.get(pk);
			if (pkStats == null) {
				pkStats = new MonitoringStats();
				pkStats.timeslot = currentTimeslot;
				pkStats.plugin = msg.plugin;
				pkStats.path = pkStats.getPath();
				workingSet.put(pkStats.path, pkStats);				
			}
			String action = getAction(msg.action);
			String ac = getActionKey(msg.plugin, action);
			MonitoringStats acStats = workingSet.get(ac);
			if (acStats == null) {
				acStats = new MonitoringStats();
				acStats.timeslot = -1;
				acStats.plugin = msg.plugin;
				acStats.action = action;
				acStats.path = acStats.getPath();
				workingSet.put(acStats.path, acStats);				
			}
						
			tsStats.requestsCurrent++;
			pkStats.requestsCurrent++;
			acStats.requestsCurrent++;
			
			if (msg.isError) {
				tsStats.errorsCurrent++;
				pkStats.errorsCurrent++;
				acStats.errorsCurrent++;
			}								
	}	
	
	public void flush(FlushMessage msg) {
		flush();
	}

	public void flush() {		
		Collection<MonitoringStats> toSend = workingSet.values();
		for (MonitoringStats stat : toSend) {
			RequestMonitoring.update(new MonitoringBulkMessage(stat), getSender());
		}
		workingSet.clear();
	}
	
	public int getCurrentTimeslot() {
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int day = cal.get(Calendar.DAY_OF_WEEK);
		boolean isWeekEnd = day == Calendar.SATURDAY || day == Calendar.SUNDAY;
		return hour + (isWeekEnd ? 24 : 0); 
	}
	
	public String getTimeslotKey() {
		return "t="+Integer.toString(currentTimeslot);		
	}
	
	public String getPluginKey(MonitoringMessage msg) {
		if (msg.plugin == null) return null;
		return msg.plugin.toString()+";t="+Integer.toString(currentTimeslot);		
	}
	
	public String getAction(String requestPath) {
		String[] path = requestPath.split("/");
		for (int i=0;i<path.length;i++) {
			  if (MidataId.isValid(path[i]) || path[i].matches("^-?\\d+$")) path[i] = "<id>";
		}
		return String.join("/", path);
	}
	
	public String getActionKey(MidataId plugin, String action) {
		if (plugin == null || action==null) return null;				
		return plugin.toString()+";a="+action;		
	}
				
}

class MonitoringReporterActor extends AbstractActor {
	
	private int currentTimeslot;
	private Cancellable timer;
			
	public MonitoringReporterActor() {
	
	}
	
	@Override
	public void preStart() throws Exception {
		
		DateTime next = new DateTime()				
				.withMinuteOfHour(6)
				.withSecondOfMinute(0)
				.withMillisOfSecond(0);

		next = next.isBeforeNow() ? next.plusHours(1) : next;
		
		int secondsUntilStart = Seconds.secondsBetween(new DateTime(), next).getSeconds();
		
		timer = getContext().system().scheduler().scheduleAtFixedRate(
                Duration.ofSeconds(secondsUntilStart),
                Duration.ofHours(1),
                getSelf(), new FlushMessage(),
                Instances.system().dispatcher(), null);	
		
		currentTimeslot = getCurrentTimeslot();
	}
	
	@Override
	public void postStop() throws Exception {		
		if (timer != null) {
			timer.cancel();
			timer = null;
		}		
	}
			
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		  .match(MonitoringBulkMessage.class, this::updateStats)
		  .match(FlushMessage.class, this::report)
		  .build();
	}
	
	public void updateStats(MonitoringBulkMessage msg) {
		String path = "MonitoringReporterActor/updateStats";
		long st = ActionRecorder.start(path);
		try {			
			MonitoringStats stats = MonitoringStats.getByPath(msg.getPath());
			if (stats == null) {
				stats = msg.createStats();
				stats.add();
			} else {
				stats.requestsCurrent += msg.requests;
				stats.errorsCurrent += msg.errors;
				stats.changed = true;
				stats.upsert();
			}	
		} catch (Exception e) {
			AccessLog.logException("MonitoringReporterActor", e);
			ErrorReporter.report("MonitoringReporterActor", null, e);	
		} finally {
			ServerTools.endRequest();	
			ActionRecorder.end(path, st);
		}
	}
	
	public void report(FlushMessage msg) {
		String path = "MonitoringReporterActor/report";
		long st = ActionRecorder.start(path);
		try {			
		   closeTimeslot(currentTimeslot);		 
		   currentTimeslot = getCurrentTimeslot();
		} catch (Exception e) {
			AccessLog.logException("MonitoringReporterActor", e);
			ErrorReporter.report("MonitoringReporterActor", null, e);				
		} finally {
			ServerTools.endRequest();	
			ActionRecorder.end(path, st);
		}
	}
	
	public int getCurrentTimeslot() {
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int day = cal.get(Calendar.DAY_OF_WEEK);
		boolean isWeekEnd = day == Calendar.SATURDAY || day == Calendar.SUNDAY;
		return hour + (isWeekEnd ? 24 : 0); 
	}
	
	
	public void closeTimeslot(int timeslot) throws InternalServerException {
		List<MonitoringEvent> events = new ArrayList<MonitoringEvent>();
		Set<MonitoringStats> allUsed = MonitoringStats.getAllByTimeslot(timeslot);
		
		allUsed.addAll(MonitoringStats.getAllUsedByTimeslot(-1));
		
		for (MonitoringStats stats : allUsed) {
			MonitoringEvent evt = checkWarnings(stats);
			stats.calc();
			if (evt != null) {
				events.add(evt);
			    evt.add();	
			}
			if (stats.isUnused()) {
				stats.delete();
			} else {
			    stats.upsert();
			}
		}
		
		if (!events.isEmpty()) {
			StringBuffer mail = new StringBuffer();
			mail.append("Dear admin,\n\nplatform monitoring has detected these "+events.size()+" deviations:\n\n");
			for (MonitoringEvent evt : events) {
				mail.append(evt.toString());
				mail.append("\n");
			}
						
			String email = InstanceConfig.getInstance().getConfig().getString("errorreports.targetemail");
			String fullname = InstanceConfig.getInstance().getConfig().getString("errorreports.targetname");
			String server = InstanceConfig.getInstance().getPlatformServer();
		
			MailUtils.sendTextMail(MailSenderType.STATUS, email, fullname, "Monitoring "+server+" ("+events.size()+")", mail.toString());
		}
	}
	
	public MonitoringEvent checkWarnings(MonitoringStats stats) throws InternalServerException {	  
	    if ((stats.timeslot >= 0 || stats.action==null) && stats.generation < 2) return null;
		if (stats.requestsCurrent > 0) {
			int errorRate = stats.errorsCurrent;// * 100 / stats.requestsCurrent;
			if (errorRate > stats.errorsAvg + stats.errorsVar) {
				MonitoringEvent result = new MonitoringEvent(stats);
				result.type = MonitoringType.HIGH_ERROR_RATE;
				result.count = stats.errorsCurrent;
				result.avg = (int) stats.errorsAvg;
				result.var = (int) stats.errorsVar;
				stats.reported = true;
				return result;
			}
		}
		
		
		if (stats.requestsCurrent > stats.requestsAvg + stats.requestsVar) {
			MonitoringEvent result = new MonitoringEvent(stats);
			result.type = MonitoringType.HIGH_USE;
			result.count = stats.requestsCurrent;
			result.avg = (int) stats.requestsAvg;
			result.var = (int) stats.requestsVar;
			stats.reported = true;
			return result;			
		} else if (stats.requestsCurrent < stats.requestsAvg - stats.requestsVar) {
			MonitoringEvent result = new MonitoringEvent(stats);
			result.type = MonitoringType.LOW_USE;
			result.count = stats.requestsCurrent;
			result.avg = (int) stats.requestsAvg;
			result.var = (int) stats.requestsVar;
			stats.reported = true;
			return result;
		} 
		
		
		
		return null;
		
	}
	
}

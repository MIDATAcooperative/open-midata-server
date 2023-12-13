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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import models.MidataId;
import models.Plugin;
import models.UserGroup;
import models.enums.EntityType;
import models.enums.UsageAction;
import models.stats.UsageStats;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.collections.Sets;
import utils.context.AccessContext;
import utils.exceptions.InternalServerException;

public class UsageStatsRecorder {

	private static ActorRef statsRecorder;

	private static ActorSystem system;

	public static void init(ActorSystem system1) {
		system = system1;
		statsRecorder = system.actorOf(Props.create(UsageStatsActor.class).withDispatcher("medium-work-dispatcher"), "usageStatsActor");
	}

	public static void protokoll(MidataId object, MidataId detail, String objectName, UsageAction action) {
		if (object==null) return;
		statsRecorder.tell(new UsageStatsMessage(object, objectName, detail, action), ActorRef.noSender());
	}
	
	public static void protokoll(MidataId object, String objectName, UsageAction action) {
		if (object==null) return;
		statsRecorder.tell(new UsageStatsMessage(object, objectName, null, action), ActorRef.noSender());
	}
	
	public static void protokoll(MidataId object, int httpStatus) {
		if (httpStatus >= 400 && httpStatus < 500) protokoll(object, UsageAction.ERROR);
		else if (httpStatus >= 500) protokoll(object, UsageAction.FAILURE);
	}

	public static void protokoll(MidataId object, UsageAction action) {
		if (object==null) return;
		statsRecorder.tell(new UsageStatsMessage(object, null, null, action), ActorRef.noSender());
	}
	
	public static void protokoll(AccessContext context, UsageAction action) throws InternalServerException {
		MidataId plugin = context.getUsedPlugin();
		MidataId detail = null;
		if (context.getAccessorEntityType() == EntityType.USERGROUP) {
			 detail = context.getAccessor();
		}
		statsRecorder.tell(new UsageStatsMessage(plugin, null, detail, action), ActorRef.noSender());
	}
	
	public static void flush() {
		statsRecorder.tell(new FlushMessage(), ActorRef.noSender());
	}
}

class UsageStatsActor extends AbstractActor {

	private Map<String, UsageStats> cache;
	private int modcount = 0;
	private String today;
	private Cancellable dayChange;

	public UsageStatsActor() {
	}

	private String today() {
		Date now = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(now);
	}

	@Override
	public void preStart() throws Exception {
		cache = new HashMap<String, UsageStats>();
		modcount = 0;
		today = today();

		dayChange = getContext().system().scheduler().scheduleWithFixedDelay(Duration.ofMinutes(5), Duration.ofMinutes(5), getSelf(), new FlushMessage(),
				getContext().system().dispatcher(), null);

	}

	@Override
	public void postStop() throws Exception {
		flush();
		if (dayChange != null) {
			dayChange.cancel();
			dayChange = null;
		}
		cache = null;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(UsageStatsMessage.class, this::updateStats).match(FlushMessage.class, this::flush).build();
	}

	public void updateStats(UsageStatsMessage msg) throws Exception {
		String path = "UsageStatsRecorder/updateStats";
		long st = ActionRecorder.start(path);
		String key = msg.getObject() + "/" + msg.getDetail() + "/" + msg.getAction().toString();
		UsageStats stats = cache.get(key);
		if (stats == null) {
			stats = new UsageStats();
			stats.object = MidataId.from(msg.getObject());
			stats.detail = MidataId.from(msg.getDetail());
			stats.objectName = msg.getObjectName();
			if (stats.objectName == null) {
				Plugin p = Plugin.getById(stats.object);
				if (p != null) {
					stats.objectName = p.filename;
					if (stats.detail != null) {
						UserGroup grp = UserGroup.getById(stats.detail, Sets.create("name"));
						if (grp != null) stats.objectName += " / "+grp.name;
					}
				} 				
			}
			stats.action = msg.getAction();
			cache.put(key, stats);
		}
		stats.count++;
		modcount++;
		if (modcount > 5000) {
			modcount = 0;
			flush();
		}
		ActionRecorder.end(path, st);
	}

	public void flush(FlushMessage msg) {
		flush();
	}

	public void flush() {		
		Collection<UsageStats> toWrite = cache.values();
		if (!toWrite.isEmpty()) AccessLog.logStart("jobs", "flush statistics");
		cache = new HashMap<String, UsageStats>();
		modcount = 0;
		for (UsageStats stat : toWrite) {
			try {
				stat.date = today;
				stat.add();
			} catch (InternalServerException e) {
				ErrorReporter.report("usage stats writer", null, e);
			}
		}
		today = today();
		AccessLog.newRequest();
	}

}

class UsageStatsMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2705265087423275414L;

	public final String object;
	public final String objectName;
	public final String detail;
	public final UsageAction action;

	public UsageStatsMessage(MidataId object, String objectName, MidataId detail, UsageAction action) {
		this.object = object.toString();
		this.objectName = objectName;
		this.detail = detail != null ? detail.toString() : null;
		this.action = action;
	}

	public String getObject() {
		return object;
	}

	public String getObjectName() {
		return objectName;
	}
	
	public String getDetail() {
		return detail;
	}

	public UsageAction getAction() {
		return action;
	}

}

class FlushMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4563742382381551939L;

}
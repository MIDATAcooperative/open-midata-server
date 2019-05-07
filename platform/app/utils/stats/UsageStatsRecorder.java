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
import controllers.AutoRun;
import models.MidataId;
import models.Plugin;
import models.enums.UsageAction;
import models.stats.UsageStats;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.InternalServerException;

public class UsageStatsRecorder {

	private static ActorRef statsRecorder;

	private static ActorSystem system;

	public static void init(ActorSystem system1) {
		system = system1;
		statsRecorder = system.actorOf(Props.create(UsageStatsActor.class), "usageStatsActor");
	}

	public static void protokoll(MidataId object, String objectName, UsageAction action) {
		statsRecorder.tell(new UsageStatsMessage(object, objectName, action), ActorRef.noSender());
	}

	public static void protokoll(MidataId object, UsageAction action) {
		statsRecorder.tell(new UsageStatsMessage(object, null, action), ActorRef.noSender());
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

		dayChange = getContext().system().scheduler().schedule(Duration.ofMinutes(5), Duration.ofMinutes(5), getSelf(), new FlushMessage(),
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
		String key = msg.getObject() + "/" + msg.getAction().toString();
		UsageStats stats = cache.get(key);
		if (stats == null) {
			stats = new UsageStats();
			stats.object = MidataId.from(msg.getObject());
			stats.objectName = msg.getObjectName();
			if (stats.objectName == null) {
				Plugin p = Plugin.getById(stats.object);
				if (p != null)
					stats.objectName = p.filename;
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
	}

	public void flush(FlushMessage msg) {
		flush();
	}

	public void flush() {
		Collection<UsageStats> toWrite = cache.values();
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
	public final UsageAction action;

	public UsageStatsMessage(MidataId object, String objectName, UsageAction action) {
		this.object = object.toString();
		this.objectName = objectName;
		this.action = action;
	}

	public String getObject() {
		return object;
	}

	public String getObjectName() {
		return objectName;
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
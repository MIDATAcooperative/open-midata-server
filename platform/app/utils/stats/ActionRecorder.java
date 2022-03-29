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
import akka.actor.AbstractActor.Receive;
import models.MidataId;
import models.Plugin;
import models.enums.UsageAction;
import models.stats.UsageStats;
import play.Logger;
import play.Logger.ALogger;
import utils.AccessLog;
import utils.ErrorReporter;
import utils.exceptions.InternalServerException;

public class ActionRecorder {

	private static ActorRef statsRecorder;

	private static ActorSystem system;
	
	private static boolean enabled = true;

	public static void init(ActorSystem system1) {
		system = system1;
		statsRecorder = system.actorOf(Props.create(ActionRecorderActor.class)/*.withDispatcher("medium-work-dispatcher")*/, "actionRecorderActor");
	}

	public static long start(String what) {
		if (enabled) {
		  String context = Thread.currentThread().getName();
		  long now = System.currentTimeMillis();
		  statsRecorder.tell(new ActionMessage(context, what, false, now, 0), ActorRef.noSender());
		  return now;
		} else return 0;
	}

	public static void end(String what, long started) {
		if (enabled) {
		  String context = Thread.currentThread().getName();
		  long now = System.currentTimeMillis();
		  statsRecorder.tell(new ActionMessage(context, what, true, now, now-started), ActorRef.noSender());
		}
	}

	public static void flush() {
		statsRecorder.tell(new ActionFlushMessage(), ActorRef.noSender());
	}
}

class ActionRecorderActor extends AbstractActor {
	
	private int running = 0;	
	private Cancellable dayChange;
	private SimpleDateFormat ff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
	private static final ALogger actions = Logger.of("actions");
	
	public ActionRecorderActor() {
	}

	private String formatTs(long timestamp) {
		return ff.format(new Date(timestamp));
	}

	@Override
	public void preStart() throws Exception {		
		running = 0;	
		dayChange = getContext().system().scheduler().schedule(Duration.ofMinutes(5), Duration.ofMinutes(5), getSelf(), new FlushMessage(), getContext().system().dispatcher(), null);
	}

	@Override
	public void postStop() throws Exception {
		flush();
		if (dayChange != null) {
			dayChange.cancel();
			dayChange = null;
		}	
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(ActionMessage.class, this::recordAction).match(ActionFlushMessage.class, this::flush).build();
	}

	public void recordAction(ActionMessage msg) throws Exception {
		if (msg.isEnd()) {			
			actions.info(formatTs(msg.getTimestamp())+" E "+running+" "+msg.getContext()+" "+msg.getWhat()+" "+msg.getDuration()+" ms");
			running--;
		} else {
			running++;
			actions.info(formatTs(msg.getTimestamp())+" S "+running+" "+msg.getContext()+" "+msg.getWhat());
		}
	}

	public void flush(ActionFlushMessage msg) {
		flush();
	}

	public void flush() {
		AccessLog.newRequest();
	}

}

class ActionMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2705265087423275414L;

	public final String what;
	public final boolean isEnd;
	public final String context;
	public final long timestamp;
	public final long duration;

	public ActionMessage(String context, String what, boolean isEnd, long timestamp, long duration) {
		this.context = context;
		this.what = what;
		this.isEnd = isEnd;
		this.timestamp = timestamp;
		this.duration = duration;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getWhat() {
		return what;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public String getContext() {
		return context;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getDuration() {
		return duration;
	}
	
}

class ActionFlushMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4563742382381551939L;

}
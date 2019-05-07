package utils.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import models.MidataId;
import models.stats.PluginDevStats;
import play.mvc.Http.Request;
import utils.ErrorReporter;
import utils.InstanceConfig;
import utils.ServerTools;


public class Stats {

	private static ActorRef statsRecorder;
		
	private static ActorSystem system;
	
	private final static ThreadLocal<PluginDevStats> currentStats = new ThreadLocal<PluginDevStats>();
	
	public final static boolean enabled = InstanceConfig.getInstance().getInstanceType().doAppDeveloperStats();
	
	public static void init(ActorSystem system1) {
		system = system1;
		statsRecorder = system.actorOf(Props.create(StatsRecorder.class), "statsRecorder");
	}
	
	public static void startRequest() {
		startRequest(null);
	}
	
	public static void startRequest(Request req) {
		if (!enabled) return;
		
		PluginDevStats stats = new PluginDevStats();	
		stats.queryCount = new HashMap<String, Integer>();
		stats.lastrun = stats.lastExecTime = System.currentTimeMillis();
		
		currentStats.set(stats);
	}
	
	public static void setPlugin(MidataId plugin) {
		if (!enabled) return;
		PluginDevStats stats = currentStats.get();
		if (stats == null) return;
		stats.plugin = plugin;
	}
	
	public static void addComment(String comment) {
		if (!enabled) return;
		PluginDevStats stats = currentStats.get();
		if (stats == null) return;
		if (stats.comments == null) stats.comments = new HashSet<String>();
		stats.comments.add(comment);
	}
	
	public static void reportConflict() {
		if (!enabled) return;
		PluginDevStats stats = currentStats.get();
		if (stats == null) return;
		stats.conflicts++;
	}
	
	public static void reportDb(String action, String collection) {
		if (!enabled) return;
		PluginDevStats stats = currentStats.get();
		if (stats == null) return;
		stats.db++;
		String key = collection+" ("+action+")";
		Integer old = stats.queryCount.get(key);
		if (old != null) stats.queryCount.put(key, old + 1);
		else stats.queryCount.put(key, 1);
	}
	
	public static void finishRequest(Request req, String result) {		
		finishRequest(req, result, null);
	}
	
	public static void finishRequest(Request req, String result, Set<String> paramSet) {
		finishRequest(req.method(), req.path(), req.queryString(), result, paramSet);
	}
	
	public static void finishRequest(String method, String pathf, Map<String, String[]> qstring, String result, Set<String> paramSet) {
		if (!enabled) return;		
		
		PluginDevStats stats = currentStats.get();
		if (stats == null) return;
		currentStats.set(null);
		if (stats.plugin == null) return;
		
		stats.lastExecTime = System.currentTimeMillis() - stats.lastExecTime;
		
		if (stats.action == null) {
		  String[] path = pathf.split("/");
		  for (int i=0;i<path.length;i++) {
			  if (MidataId.isValid(path[i]) || path[i].matches("^-?\\d+$")) path[i] = "<id>";
		  }
		  stats.action = method+" "+String.join("/", path);
		}
		
		if (stats.params == null) {
			List<String> params = paramSet != null ? new ArrayList(paramSet) : new ArrayList(qstring.keySet());
			Collections.sort(params);
			stats.params = String.join("&", params);
		}
		
		stats.resultCount = Collections.singletonMap(result, 1);		
		
		protokoll(stats);
	}
	
	public static void protokoll(PluginDevStats stats) {
		statsRecorder.tell(new StatsMessage(stats), ActorRef.noSender());
	}
	
	
		
}

class StatsMessage {
    public final String plugin;		
	public final String action;		
	public final String params;		
	public final long lastrun;				
	public final Set<String> comments;
	public final Map<String, Integer> queryCount;
	public final String result;
	public final long lastExecTime;
	public final int conflicts;
	public final int db;
	
	public StatsMessage(PluginDevStats stats) {
		this.plugin = stats.plugin.toString();
		this.action = stats.action;
		this.params = stats.params;
		this.lastrun = stats.lastrun;
		this.comments = stats.comments;
		this.lastExecTime = stats.lastExecTime;
		this.conflicts = stats.conflicts;
		this.db = stats.db;
		this.result = stats.resultCount.keySet().iterator().next();
		this.queryCount = stats.queryCount;
	}
}

class StatsRecorder extends AbstractActor {
			
	public StatsRecorder() {							    
	}
	
	@Override
	public Receive createReceive() {
	    return receiveBuilder()
	      .match(StatsMessage.class, this::updateStats)	      
	      .build();
	}
		
	public void updateStats(StatsMessage msg) throws Exception {
		try {
					
			PluginDevStats stats = PluginDevStats.getByRequest(MidataId.from(msg.plugin), msg.action, msg.params);
			if (stats == null) {
			  stats = new PluginDevStats();
			  stats._id = new MidataId();
			  stats.plugin = MidataId.from(msg.plugin);
			  stats.action = msg.action;
			  stats.params = msg.params;
			  stats.comments = new HashSet<String>();
			  stats.resultCount = new HashMap<String, Integer>();
			  stats.firstrun = msg.lastrun;
			}
			
			stats.lastExecTime = msg.lastExecTime;
			stats.lastrun = msg.lastrun;
			stats.count++;
			stats.totalExecTime += msg.lastExecTime;
			stats.conflicts += msg.conflicts;
			stats.db += msg.db;
			if (msg.comments != null) stats.comments.addAll(msg.comments);
			
			if (stats.resultCount.containsKey(msg.result)) {
				stats.resultCount.put(msg.result, stats.resultCount.get(msg.result) + 1);
			} else {
				stats.resultCount.put(msg.result, 1);
			}
			
			if (stats.queryCount == null) stats.queryCount = new HashMap<String, Integer>();
			for (Map.Entry<String, Integer> queries : msg.queryCount.entrySet()) {
				Integer old = stats.queryCount.get(queries.getKey());
				if (old != null) stats.queryCount.put(queries.getKey(), old + queries.getValue());
				else stats.queryCount.put(queries.getKey(), queries.getValue());
			}
			
			stats.update();	
		} catch (Exception e) {
			ErrorReporter.report("Messager", null, e);	
			throw e;
		} finally {
			ServerTools.endRequest();			
		}
	}
	
}
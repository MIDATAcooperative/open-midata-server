package utils.sandbox;

import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bson.types.ObjectId;

import models.Record;

import utils.access.AccessLog;
import utils.access.RecordManager;
import utils.collections.CMaps;
import utils.collections.Sets;

public class Scripting {

	public static Scripting instance = new Scripting();
	
	private ScriptEngineManager manager = new ScriptEngineManager();
	
	public Object eval(ObjectId who) {
		ScriptEngine engine = manager.getEngineByName("javascript");
		try {
			engine.put("who", who);
			engine.put("m", CMaps.map("content", "activities/activity-calories"));
			engine.put("f", Sets.create("name", "content", "owner"));
			String s = "importPackage( Packages.utils.access );function read() { return RecordManager.instance.list(who, who, m, f); }; read()";
			
			//Collection<Record> recs = RecordManager.instance.list(who, apsId, properties, fields) 
		Object res = engine.eval(s);//"var x = { 'a' : 'b' }; JSON.stringify(x)");
		AccessLog.debug(res.toString());
		AccessLog.debug(res.getClass().getName());
		
		
		//engine.enew InputStreamReader(getClass().getResourceAsStream(name));
		return res;
		} catch (ScriptException e) {
			AccessLog.debug(e.toString());
			return null;
		}
	}
}
package models;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import models.enums.AuditEventType;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

public class RateLimitedAction extends Model {

	protected @NotMaterialized static final String collection = "ratelimited";
	
	public @NotMaterialized static final Set<String> ALL = Sets.create("_id", "account", "action", "lastDone", "count", "counterStartedAt");
	
	public String account;
	public AuditEventType action;
	public Date lastDone;
	public Date counterStartedAt;
	public int count;
	
	public static RateLimitedAction getByAccountAndAction(String account, AuditEventType action) throws InternalServerException {
		Set<RateLimitedAction> results = Model.getAll(RateLimitedAction.class, collection, CMaps.map("account", account).map("action", action), ALL);
		if (results.isEmpty()) return null;
		if (results.size()==1) return results.iterator().next();
		Iterator<RateLimitedAction> it = results.iterator();
		RateLimitedAction r = it.next();
		while (it.hasNext()) {
			Model.delete(RateLimitedAction.class, collection, CMaps.map("_id", it.next()._id));
		}
		return r;
	}
	
	public static boolean doRateLimited(MidataId account, AuditEventType action, long minDistance, int maxCounter, long counterTimeFrame) throws InternalServerException {
		return doRateLimited(account.toString(), action, minDistance, maxCounter, counterTimeFrame);
	}
	
	public static boolean doRateLimited(String account, AuditEventType action, long minDistance, int maxCounter, long counterTimeFrame) throws InternalServerException {
		RateLimitedAction ac = getByAccountAndAction(account, action);
		if (ac == null) {
			ac = new RateLimitedAction();
			ac._id = new MidataId();
			ac.account = account;
			ac.action = action;
			ac.lastDone = new Date();
			ac.counterStartedAt = new Date();
			ac.count = 1;
			Model.insert(collection, ac);
			return true;
		} 
		if (System.currentTimeMillis() - ac.lastDone.getTime() < minDistance) return false;
			
		if (ac.counterStartedAt == null || System.currentTimeMillis() - ac.counterStartedAt.getTime() > counterTimeFrame) {
			ac.count = 0;
			ac.counterStartedAt = new Date();	
		}
		
		if (ac.count >= maxCounter) {
			return false;			
		}
		
		ac.lastDone = new Date();
		ac.count++;
		
		ac.setMultiple(collection, Sets.create("lastDone", "count", "counterStartedAt"));
		
		return true;
	}
}

package utils.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Member;
import models.ModelException;
import models.Record;

public class ReferenceTool {

	private static Set<String> fullName = Sets.create("firstname", "lastname");
	
	public static void resolveOwners(Collection<Record> records, boolean owners, boolean creators) throws ModelException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (Record record : records) {
			if (owners && (record.owner != null)) {
				String key = record.owner.toString();
				String name = members.get(key);
				if (name == null) {
					Member member = Member.getById(record.owner, fullName);
					if (member != null) {
						name = member.lastname + ", " + member.firstname;
						members.put(key, name);
					}
				}
				record.ownerName = name;
			}
			if (creators && (record.creator != null)) {
				String key = record.creator.toString();
				String name = members.get(key);
				if (name == null) {
					Member member = Member.getById(record.creator, fullName);
					if (member != null) {
						name = member.lastname + ", " + member.firstname;
						members.put(key, name);
					}
				}
				record.creatorName = name;
			}
		}
	}
	
	public static void resolveOwners(Collection<Circle> circles, boolean owners) throws ModelException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (Circle circle : circles) {
			if (owners && (circle.owner != null)) {
				String key = circle.owner.toString();
				String name = members.get(key);
				if (name == null) {
					Member member = Member.getById(circle.owner, fullName);
					if (member != null) {
						name = member.lastname + ", " + member.firstname;
						members.put(key, name);
					}
				}
				circle.ownerName = name;
			}
		
		}
	}
		
}

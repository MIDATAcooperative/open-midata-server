package utils.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.Member;
import models.ModelException;
import models.Record;

public class ReferenceTool {

	private static Set<String> fullName = Sets.create("firstname", "sirname");
	
	public static void resolveOwners(Collection<Record> records, boolean owners, boolean creators) throws ModelException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (Record record : records) {
			if (owners && (record.owner != null)) {
				String key = record.owner.toString();
				String name = members.get(key);
				if (name == null) {
					Member member = Member.getById(record.owner, fullName);
					if (member != null) {
						name = member.sirname + ", " + member.firstname;
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
						name = member.sirname + ", " + member.firstname;
						members.put(key, name);
					}
				}
				record.creatorName = name;
			}
		}
	}
		
}

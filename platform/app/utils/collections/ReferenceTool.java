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
	
	public static void resolveOwners(Collection<Record> records) throws ModelException {
		Map<String, String> owners = new HashMap<String, String>();		
		
		for (Record record : records) {
			if (record.owner != null) {
				String key = record.owner.toString();
				String name = owners.get(key);
				if (name == null) {
					Member member = Member.getById(record.owner, fullName);
					if (member != null) name = member.sirname + ", " + member.firstname;
				}
				record.ownerName = name;
			}
		}
	}
		
}

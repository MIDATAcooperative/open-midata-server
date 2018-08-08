package utils.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.Consent;
import models.Member;
import models.MidataId;
import models.Record;
import models.RecordsInfo;
import models.StudyParticipation;
import utils.exceptions.InternalServerException;

public class ReferenceTool {

	private static Set<String> fullName = Sets.create("firstname", "lastname");
	
	public static void resolveOwners(Collection<Record> records, boolean owners, boolean creators) throws InternalServerException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (Record record : records) {
			if (owners && (record.owner != null && record.ownerName == null)) {
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
			if (creators && (record.creator != null && record.creatorName == null)) {
				String key = record.creator.toString();
				String name = members.get(key);
				if (name == null && record.owner != null && record.owner.equals(record.creator)) {
					name = record.ownerName;
				}
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
	
	public static void resolveOwnersForRecordsInfo(Collection<RecordsInfo> recordinfos, boolean owners) throws InternalServerException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (RecordsInfo info : recordinfos) {
			if (owners && (info.owners != null)) {
				info.ownerNames = new HashSet<String>();
				for (String key : info.owners) {
					
					String name = members.get(key);
					if (name == null) {
						Member member = Member.getById(new MidataId(key), fullName);
						if (member != null) {
							name = member.lastname + ", " + member.firstname;
							members.put(key, name);
						} else {
							StudyParticipation p = StudyParticipation.getById(new MidataId(key), Sets.create("ownerName"));
							if (p != null) {
								name = p.ownerName;
								members.put(key, name);
							}
						}
					}
					info.ownerNames.add(name);
				}
			}
			
		}
	}
	
	public static void resolveOwners(Collection<? extends Consent> circles, boolean owners) throws InternalServerException {
		Map<String, String> members = new HashMap<String, String>();		
		
		for (Consent circle : circles) {
			if (owners && (circle.owner != null && circle.getOwnerName() == null)) {
				String key = circle.owner.toString();
				String name = members.get(key);
				if (name == null) {
					Member member = Member.getById(circle.owner, fullName);
					if (member != null) {
						name = member.lastname + ", " + member.firstname;
						members.put(key, name);
					}
				}			
				circle.setOwnerName(name);
			}
		
		}
	}
		
}

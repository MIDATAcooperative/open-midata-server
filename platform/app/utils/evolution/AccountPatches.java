package utils.evolution;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import controllers.Circles;

import utils.AccessLog;
import utils.access.RecordManager;
import utils.auth.RecordToken;
import utils.collections.CMaps;
import utils.collections.Sets;
import utils.exceptions.AppException;
import utils.exceptions.InternalServerException;
import utils.fhir.PatientResourceProvider;
import models.Consent;
import models.ContentInfo;
import models.Record;
import models.Space;
import models.User;
import models.enums.ConsentStatus;

public class AccountPatches {

	public static final int currentAccountVersion = 20160407;
	
	public static void check(User user) throws AppException {		
		if (user.accountVersion < 20160324) { formatPatch20160324(user); }	
		if (user.accountVersion < 20160407) { formatPatch20160407(user); }
		if (user.accountVersion < 20160902) { formatPatch20160902(user); }
	}
	
	public static void makeCurrent(User user, int currentAccountVersion) throws AppException {
		if (user.accountVersion < currentAccountVersion) {
			user.accountVersion = currentAccountVersion;
			User.set(user._id, "accountVersion", user.accountVersion);
		}
	}
	
	public static void formatPatch20160324(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 03 24");
	   Set<String> formats = Sets.create("fhir/Observation/String", "fhir/Observation/Quantity", "fhir/Observation/CodeableConcept");
	   List<Record> recs = RecordManager.instance.list(user._id, user._id, CMaps.map("format", formats).map("owner", "self"), RecordManager.COMPLETE_DATA);
	   for (Record r : recs) {
		   ObjectId oldId = r._id;
		   r._id = new ObjectId();
		   r.subformat = r.format.substring(r.format.lastIndexOf('/')+1);
		   r.format = "fhir/Observation";
		   try {
		     RecordManager.instance.addRecord(user._id, r);
		   } catch (AppException e) {}
		   
	   }
	   RecordManager.instance.wipe(user._id, CMaps.map("format", formats).map("owner", "self"));
	   Set<Space> spaces = Space.getAllByOwner(user._id, Sets.create("_id", "type"));
	   for (Space space : spaces) {	   					
		   if (space.type != null && space.type.equals("visualization")) {
			  RecordManager.instance.deleteAPS(space._id, user._id);
			  Space.delete(user._id, space._id);
		   }
	   }	   
	   RecordManager.instance.fixAccount(user._id);
	   makeCurrent(user, 20160324);
	   AccessLog.logEnd("end patch 2016 03 24");
	}
	
	public static void formatPatch20160407(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 04 07");
		RecordManager.instance.patch20160407(user._id); 		      
		RecordManager.instance.fixAccount(user._id);
		makeCurrent(user, 20160407);
		AccessLog.logEnd("end patch 2016 04 07");
	}
	
	public static void formatPatch20160902(User user) throws AppException {
		AccessLog.logBegin("start patch 2016 09 02");
		
		PatientResourceProvider.updatePatientForAccount(user._id);
		
		/*Set<Consent> consents = Consent.getAllByOwner(user._id, CMaps.map("type", ), Consent.ALL);
		for (Consent consent : consents) {
		  Circles.autosharePatientRecord(consent);
		}*/
				
		makeCurrent(user, 20160902);
		AccessLog.logEnd("end patch 2016 09 02");
	}
		
}

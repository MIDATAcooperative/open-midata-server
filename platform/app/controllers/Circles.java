package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.collections.ReferenceTool;
import utils.collections.Sets;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.circles;

import actions.APICall;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result index() {
		return ok(circles.render());
	}

	public static Result details(String circleIdString) {
		return index();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result get() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
						
		List<Circle> circles = null;

		if (json.has("owner")) {
			ObjectId owner = new ObjectId(request().username());
			circles = new ArrayList<Circle>(Circle.getAllByOwner(owner));
		} else if (json.has("member")) {
			ObjectId member = new ObjectId(request().username());
			circles = new ArrayList<Circle>(Circle.getAllByMember(member));
			ReferenceTool.resolveOwners(circles, true);
		} else JsonValidation.validate(json, "owner");
		Collections.sort(circles);
		return ok(Json.toJson(circles));
	}
	


	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result add() throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "name");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = JsonValidation.getString(json, "name");
		
		if (Circle.existsByOwnerAndName(userId, name)) {
		  return badRequest("A circle with this name already exists.");
		}
		
		// create new circle
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = userId;
		circle.name = name;
		circle.order = Circle.getMaxOrder(userId) + 1;
		circle.members = new HashSet<ObjectId>();
		circle.aps = RecordSharing.instance.createPrivateAPS(userId, circle._id); 
		
		Circle.add(circle);
		
		return ok(Json.toJson(circle));
	}

	@APICall
	public static Result delete(String circleIdString) throws JsonValidationException, ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Circle circle = Circle.getByIdAndOwner(circleId, userId, Sets.create("members", "aps"));
		if (circle == null) {
			return badRequest("No circle with this id exists.");
		}
		
		// Remove APS
		RecordSharing.instance.deleteAPS(circle.aps, circle.owner);
		
		// Remove Rules
		RuleApplication.instance.removeRules(userId, circleId);
		
		// delete circle		
		Circle.delete(userId, circleId);
		
		// make the records of the deleted circle invisible to its former members		
		//Users.makeInvisible(userId, circle.shared, circle.members);
		
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	@APICall
	public static Result addUsers(String circleIdString) throws JsonValidationException, ModelException {
		// validate json
		JsonNode json = request().body().asJson();
		
		JsonValidation.validate(json, "users");
		
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Circle circle = Circle.getByIdAndOwner(circleId, userId, Sets.create("members","aps"));
		if (circle == null) {
			return badRequest("No circle with this id exists.");
		}
		
		// add users to circle (implicit: if not already present)
		Set<ObjectId> newMemberIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("users")));
				
		circle.members.addAll(newMemberIds);
		Circle.set(circle._id, "members", circle.members);
		
		RecordSharing.instance.shareAPS(circle.aps, userId, newMemberIds);
		
		// also make records of this circle visible		
		//Users.makeVisible(userId, circle.shared, newMemberIds);
		
		return ok();
	}

	public static Result removeMember(String circleIdString, String memberIdString) throws JsonValidationException, ModelException {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		
		Circle circle = Circle.getByIdAndOwner(circleId, userId, Sets.create("members","aps"));
		if (circle == null) {
			return badRequest("No circle with this id exists.");
		}
		
		// remove member from circle (implicit: if present)
		ObjectId memberId = new ObjectId(memberIdString);
		
		circle.members.remove(memberId);
		Circle.set(circle._id, "members", circle.members);

		Set<ObjectId> memberIds = new HashSet<ObjectId>();
		memberIds.add(memberId);
		
		RecordSharing.instance.unshareAPS(circle.aps, userId, memberIds);
			// also remove records from visible records that are no longer shared with the member
		//Users.makeInvisible(userId, circle.shared, new ChainedSet<ObjectId>().add(memberId).get());
		
		return ok();
	}

}

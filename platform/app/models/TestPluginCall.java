/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

package models;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;

import utils.collections.CMaps;
import utils.collections.Sets;
import utils.db.NotMaterialized;
import utils.exceptions.InternalServerException;

@JsonFilter("TestPluginCall")
public class TestPluginCall extends Model {

	protected @NotMaterialized static final String collection = "testcalls";
	
	public @NotMaterialized final static int NOTANSWERED = -999;
	
	public @NotMaterialized final static Set<String> ALL = 
			 Sets.create("_id", "handle", "path", "token", "lang", "owner", "resource", "resourceId", "created", "answer", "answerStatus");
	
	
	public String handle;
	
	public String path;
	
	public String token;
	
	public String lang;
	
	public String owner;
	
	public String resource;
	
	public String resourceId;
	
	public String answer;
	
	public int answerStatus;
	
	public long created;
	
	public void add() throws InternalServerException {
		Model.insert(collection, this);
	}
	
	public static List<TestPluginCall> getForHandle(String handle) throws InternalServerException {
		return Model.getAllList(TestPluginCall.class, collection, CMaps.map("handle", handle).map("answerStatus", NOTANSWERED), ALL, 1000, "created", 1);
	}
	
	public static TestPluginCall getForHandleAndId(String handle, MidataId id) throws InternalServerException {
		return Model.get(TestPluginCall.class, collection, CMaps.map("_id",id).map("handle", handle), ALL);
	}
	
	public static TestPluginCall getById(MidataId id) throws InternalServerException {
		return Model.get(TestPluginCall.class, collection, CMaps.map("_id",id), ALL);
	}
	
	public static void delete(String handle) throws InternalServerException {
		Model.delete(TestPluginCall.class, collection, CMaps.map("handle", handle));
	}
	
	public static void delete(String handle, MidataId id) throws InternalServerException {
		Model.delete(TestPluginCall.class, collection, CMaps.map("_id",id).map("handle", handle));
	}
	
	public void setAnswer(int status, String content) throws InternalServerException {
		this.answerStatus = status;
		this.answer = content;
		this.setMultiple(collection, Sets.create("answerStatus", "answer"));
	}
}

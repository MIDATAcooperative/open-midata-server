package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import models.ModelException;

import org.bson.types.ObjectId;

import play.libs.Json;
import utils.db.Database;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class LoadData {

	private static final String DATA = "test/data/db.json";

	/**
	 * Drops the database and populates it with test data.
	 */
	public static void load() {
		try {
			Database.destroy();

			// read JSON file
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(new FileReader(DATA));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();

			// parse and insert into database
			JsonNode node = Json.parse(sb.toString());
			Iterator<Entry<String, JsonNode>> collections = node.fields();
			while (collections.hasNext()) {
				Entry<String, JsonNode> curColl = collections.next();
				DBCollection collection = Database.getCollection(curColl.getKey());
				for (JsonNode curDoc : curColl.getValue()) {
					collection.insert((DBObject) JSON.parse(curDoc.toString()));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createDefaultVisualization() {
		try {
			ObjectId developerId = CreateDBObjects.createDeveloperAccount();
			CreateDBObjects.createDefaultVisualization(developerId);
		} catch (ModelException e) {
			e.printStackTrace();
		}
	}

}

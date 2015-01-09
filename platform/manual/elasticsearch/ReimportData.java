package elasticsearch;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.App;
import models.Circle;
import models.Message;
import models.NewsItem;
import models.Record;
import models.Space;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DBLayer;
import utils.search.Search;
import utils.search.Search.Type;

/**
 * Fetches the data from MongoDB and indexes it in ElasticSearch.
 */
public class ReimportData {

	public static void main(String[] args) throws Exception {
		System.out.print("Connecting...");
		// connect to MongoDB
		start(fakeApplication(fakeGlobal()));
		DBLayer.connect();

		// connect to ElasticSearch
		Search.connect();
		System.out.println("done.");

		// dropping old content
		System.out.print("Deleting existing ElasticSearch indices...");
		Search.destroy();
		Thread.sleep(1000);
		System.out.println("done.");

		// initializing
		System.out.print("Initializing...");
		Search.initialize();
		Thread.sleep(1000);
		System.out.println("done.");

		// users
		System.out.print("Importing users...");
		Map<String, Object> emptyMap = new HashMap<String, Object>();
		Set<User> users = User
				.getAll(emptyMap, new ChainedSet<String>().add("email").add("name").add("messages").get());
		for (User user : users) {
			Search.add(Type.USER, user._id, user.name, user.email);
		}

		// waiting for previous operations to finish...
		Thread.sleep(1000);
		System.out.println("done.");

		// for each user: add all the data
		for (User user : users) {
			System.out.print("Importing personal data for user '" + user.name + "'...");

			// messages
			Set<ObjectId> messageIds = user.messages.get("inbox");
			messageIds.addAll(user.messages.get("archive"));
			messageIds.addAll(user.messages.get("trash"));
			Set<Message> messages = Message.getAll(
					new ChainedMap<String, Set<ObjectId>>().put("_id", messageIds).get(),
					new ChainedSet<String>().add("title").add("content").get());
			for (Message message : messages) {
				Search.add(user._id, "message", message._id, message.title, message.content);
			}

			// spaces
			Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", user._id).get();
			Set<String> fields = new ChainedSet<String>().add("name").get();
			Set<Space> spaces = Space.getAll(properties, fields);
			for (Space space : spaces) {
				Search.add(user._id, "space", space._id, space.name);
			}

			// circles
			Set<Circle> circles = Circle.getAll(properties, fields);
			for (Circle circle : circles) {
				Search.add(user._id, "circle", circle._id, circle.name);
			}

			// records
			fields.add("description");
			Set<Record> records = Record.getAll(properties, fields);
			for (Record record : records) {
				Search.add(user._id, "record", record._id, record.name, record.description);
			}
			System.out.println("done.");
		}

		// news
		System.out.print("Importing news...");
		Set<NewsItem> newsItems = NewsItem.getAll(emptyMap, new ChainedSet<String>().add("title").add("content").get());
		for (NewsItem newsItem : newsItems) {
			Search.add(Type.NEWS, newsItem._id, newsItem.title, newsItem.content);
		}
		System.out.println("done.");

		// apps
		System.out.print("Importing apps...");
		Set<App> apps = App.getAll(new HashMap<String, Object>(),
				new ChainedSet<String>().add("name").add("description").get());
		for (App app : apps) {
			Search.add(Type.APP, app._id, app.name, app.description);
		}
		System.out.println("done.");

		// visualizations
		System.out.print("Importing visualizations...");
		Set<Visualization> visualizations = Visualization.getAll(emptyMap,
				new ChainedSet<String>().add("name").add("description").get());
		for (Visualization visualization : visualizations) {
			Search.add(Type.VISUALIZATION, visualization._id, visualization.name, visualization.description);
		}
		System.out.println("done.");

		// disconnect
		DBLayer.close();
		Search.close();
		System.out.println("Finished.");
	}
}

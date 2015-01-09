package setup;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import utils.db.DBLayer;
import utils.search.Search;

/**
 * Minimal setup that is necessary to start a fresh Health Data Cooperative platform.
 */
public class MinimalSetup {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting to create minimal setup for Health Data Cooperative platform.");

		// connecting
		System.out.print("Connecting to MongoDB...");
		start(fakeApplication(fakeGlobal()));
		DBLayer.connect();
		System.out.println("done.");
		System.out.print("Connecting to ElasticSearch...");
		Search.connect();
		System.out.println("done.");

		// initializing
		System.out.print("Setting up MongoDB...");
		DBLayer.initialize();
		System.out.println("done.");
		System.out.print("Setting up ElasticSearch...");
		Search.initialize();
		System.out.println("done.");

		// terminating
		System.out.println("Shutting down...");
		DBLayer.close();
		Search.close();
		System.out.println("Minimal setup complete.");
	}

}

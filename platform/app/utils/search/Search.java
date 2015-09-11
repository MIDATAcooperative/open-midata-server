package utils.search;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import models.Circle;
import models.ModelException;

import org.bson.types.ObjectId;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest.Suggestion;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import play.libs.Json;
import utils.collections.CollectionConversion;
import utils.db.ObjectIdConversion;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.RecordSharing;

public class Search {

	public static enum Type {
		USER, APP, VISUALIZATION, NEWS
	}

	private static final String CLUSTER_NAME = "elasticsearch";
	private static final String PUBLIC = "public"; // public index visible to all users
	private static final String TITLE = "title";
	private static final String SUGGEST = "suggest"; // used for autocompletion
	private static final String CONTENT = "content";

	private static Node node;
	private static Client client;

	public static void connect() {
		// start a client node (holds no data)
		Settings settings = ImmutableSettings.settingsBuilder().put("http.enabled", "false").put("transcport.tcp.port", "9300-9400")
				.put("discovery.zen.ping.multicast.enabled", "false").put("discovery.zen.ping.unicast.hosts", "localhost").build();
		node = NodeBuilder.nodeBuilder().clusterName(CLUSTER_NAME).client(true).settings(settings).node();
		client = node.client();
	}

	public static void connectToTest() {
		node = NodeBuilder.nodeBuilder().local(true).node();
		client = node.client();
	}

	public static void close() {
		if (node != null) {
			node.close();
		}
	}

	/**
	 * Create global indices.
	 */
	public static void initialize() throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		// check whether the public index exists and create it otherwise
		if (!client.admin().indices().prepareExists(PUBLIC).execute().actionGet().isExists()) {
			client.admin().indices().prepareCreate(PUBLIC).execute().actionGet();
		}
	}

	/**
	 * Delete all indices.
	 */
	public static void destroy() {
		// return if not connected
		if (client == null) {
			return;
		}

		client.admin().indices().prepareDelete("_all").execute().actionGet();
		client.admin().indices().prepareClearCache().execute().actionGet();
	}

	/**
	 * Create a user's index.
	 */
	private static void createIndex(ObjectId userId) throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		if (!client.admin().indices().prepareExists(userId.toString()).execute().actionGet().isExists()) {
			client.admin().indices().prepareCreate(userId.toString()).execute().actionGet();
		}
	}

	/**
	 * Delete a user's index.
	 */
	private static void deleteIndex(ObjectId userId) {
		// return if not connected
		if (client == null) {
			return;
		}

		if (client.admin().indices().prepareExists(userId.toString()).execute().actionGet().isExists()) {
			client.admin().indices().prepareDelete(userId.toString()).execute().actionGet();
			client.admin().indices().prepareClearCache(userId.toString()).execute().actionGet();
		}
	}

	private static String getType(Type type) {
		switch (type) {
		case USER:
			return "user";
		case APP:
			return "app";
		case VISUALIZATION:
			return "visualization";
		case NEWS:
			return "news";
		default:
			throw new NoSuchElementException("There is no such type.");
		}
	}

	public static void add(ObjectId userId, String type, ObjectId modelId, String title) throws SearchException {
		add(userId, type, modelId, title, null);
	}

	/**
	 * Add document to search index. Title is used for autocompletion, content for full-text search.
	 */
	public static void add(ObjectId userId, String type, ObjectId modelId, String title, String content) throws SearchException {
		add(userId.toString(), type, modelId.toString(), title, content);
	}

	public static void add(Type type, ObjectId documentId, String title, String content) throws SearchException {
		add(PUBLIC, getType(type), documentId.toString(), title, content);
		if (type == Type.USER) {
			createIndex(documentId);
		}
	}

	private static void add(String index, String type, String id, String title, String content) throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		String[] split = title.split("[ ,\\.]+");
		try {
			client.prepareIndex(index, type, id)
					.setSource(
							jsonBuilder().startObject().field(TITLE, title).startObject(SUGGEST).array("input", split)
									.field("output", title).startObject("payload").field("type", type).field("id", id).endObject()
									.endObject().field(CONTENT, content).endObject()).execute().actionGet();
		} catch (IOException e) {
			throw new SearchException(e);
		}
	}

	public static void update(ObjectId userId, String type, ObjectId modelId, String title) throws SearchException {
		update(userId, type, modelId, title, null);
	}

	public static void update(ObjectId userId, String type, ObjectId modelId, String title, String content) throws SearchException {
		delete(userId, type, modelId);
		add(userId, type, modelId, title, content);
	}

	public static void update(Type type, ObjectId documentId, String title, String content) throws SearchException {
		delete(type, documentId);
		add(type, documentId, title, content);
	}

	public static void delete(ObjectId userId, String type, ObjectId modelId) {
		delete(userId.toString(), type, modelId.toString());
	}

	public static void delete(Type type, ObjectId documentId) {
		delete(PUBLIC, getType(type), documentId.toString());
		if (type == Type.USER) {
			deleteIndex(documentId);
		}
	}

	private static void delete(String index, String type, String id) {
		// return if not connected
		if (client == null) {
			return;
		}

		client.prepareDelete(index, type, id).execute().actionGet();
	}

	/**
	 * Suggest completions within the user's index.
	 */
	public static Map<String, List<CompletionResult>> complete(ObjectId userId, String query) {
		return complete(userId.toString(), query);
	}

	/**
	 * Suggest completions of the given type within the public index.
	 */
	public static List<CompletionResult> complete(Type type, String query) {
		Map<String, List<CompletionResult>> results = complete(PUBLIC, query);
		if (results.containsKey(getType(type))) {
			return results.get(getType(type));
		} else {
			return new ArrayList<CompletionResult>();
		}
	}

	private static Map<String, List<CompletionResult>> complete(String index, String query) {
		Map<String, List<CompletionResult>> results = new HashMap<String, List<CompletionResult>>();

		// return if not connected
		if (client == null) {
			return results;
		}

		// search for completion suggestions in index
		SuggestionBuilder<CompletionSuggestionBuilder> suggestionBuilder = new CompletionSuggestionBuilder("suggestion").text(query).field(
				"suggest");
		SuggestResponse response = client.prepareSuggest(index).addSuggestion(suggestionBuilder).execute().actionGet();
		for (Suggestion<? extends Entry<? extends Option>> suggestion : response.getSuggest()) {
			for (Entry<? extends Option> entry : suggestion) {
				for (Option option : entry) {
					CompletionResult completionResult = new CompletionResult();
					try {
						// proper payload support might be introduced in version 1.0.0
						// for now: parse payload JSON object
						String xContent = option.toXContent(jsonBuilder(), null).string();
						JsonNode json = Json.parse(xContent);
						if (json.has("payload")) {
							JsonNode payload = json.get("payload");
							if (payload.has("type")) {
								completionResult.type = payload.get("type").asText();
							}
							if (payload.has("id")) {
								completionResult.id = payload.get("id").asText();
							}
						}
					} catch (IOException e) {
						// error while parsing payload, type and id stay null
					}
					completionResult.score = option.getScore();
					completionResult.value = option.getText().string();
					completionResult.tokens = new HashSet<String>();
					for (String token : completionResult.value.split("[ ,\\.]+")) {
						completionResult.tokens.add(token);
					}
					addToListInMap(results, completionResult.type, completionResult);
				}
			}
		}

		// set types of incomplete/failed results to "other"
		if (results.containsKey(null)) {
			results.put("other", results.get(null));
			results.remove(null);
		}
		return results;
	}

	public static List<SearchResult> search(Type type, String query) {
		// return if not connected
		if (client == null) {
			return null;
		}

		SearchResponse response = client.prepareSearch(PUBLIC).setTypes(getType(type))
				.setQuery(QueryBuilders.multiMatchQuery(query, TITLE, CONTENT)).execute().actionGet();
		Map<String, List<SearchResult>> searchResults = getSearchResults(response);
		if (!searchResults.containsKey(getType(type))) {
			return new ArrayList<SearchResult>();
		}
		return searchResults.get(getType(type));
	}

	public static List<SearchResult> searchRecords(ObjectId userId, Map<String, Set<ObjectId>> visibleRecords, String query) {
		// return if not connected
		if (client == null) {
			return null;
		}

		// search in user's records
		ListenableActionFuture<SearchResponse> privateRecordsQuery = searchPrivateIndex(userId, query, "record").execute();

		// search in other users' visible records
		ListenableActionFuture<SearchResponse> visibleRecordsQuery = searchVisibleRecords(visibleRecords, query).execute();

		// wait for the results
		SearchResponse privateRecordsResponse = privateRecordsQuery.actionGet();
		SearchResponse visibleRecordsResponse = visibleRecordsQuery.actionGet();

		// construct response
		Map<String, List<SearchResult>> searchResults = getSearchResults(privateRecordsResponse, visibleRecordsResponse);

		// sort the results according to score
		if (!searchResults.containsKey("record")) {
			return new ArrayList<SearchResult>();
		}
		Collections.sort(searchResults.get("record"));
		return searchResults.get("record");
	}

	public static List<SearchResult> searchRecords(ObjectId userId, Set<Circle> circles, String query) throws ModelException {
	   Map<String, Set<ObjectId>> map = new HashMap<String, Set<ObjectId>>();
	   
	   for (Circle circle : circles) {
		   Set<String> sharedids = RecordSharing.instance.listRecordIds(userId, circle._id);
		   map.put(circle.owner.toString(), ObjectIdConversion.toObjectIds(sharedids));		   
	   }
	   
	   return searchRecords(userId, map, query);
	}
	
	/**
	 * Search in all the user's data and all visible records.
	 */
	public static Map<String, List<SearchResult>> search(ObjectId userId, Map<String, Set<ObjectId>> visibleRecords, String query) {
		// return if not connected
		if (client == null) {
			return null;
		}

		// search in user's index
		ListenableActionFuture<SearchResponse> privateIndexQuery = searchPrivateIndex(userId, query).execute();

		// search in other users' visible records
		ListenableActionFuture<SearchResponse> visibleRecordsQuery = searchVisibleRecords(visibleRecords, query).execute();

		// search in public index
		ListenableActionFuture<SearchResponse> publicIndexQuery = searchPublicIndex(query).execute();

		// wait for the results
		SearchResponse privateIndexResponse = privateIndexQuery.actionGet();
		SearchResponse visibleRecordsResponse = visibleRecordsQuery.actionGet();
		SearchResponse publicIndexResponse = publicIndexQuery.actionGet();

		// construct response
		Map<String, List<SearchResult>> searchResults = getSearchResults(privateIndexResponse, visibleRecordsResponse, publicIndexResponse);

		// sort the results of the records according to its score
		// rest is already sorted
		if (searchResults.containsKey("record")) {
			Collections.sort(searchResults.get("record"));
		}
		return searchResults;
	}
	
	public static Map<String, List<SearchResult>> search(ObjectId userId, Set<Circle> circles, String query) throws ModelException {
		   Map<String, Set<ObjectId>> map = new HashMap<String, Set<ObjectId>>();
		   
		   for (Circle circle : circles) {
			   Set<String> sharedids = RecordSharing.instance.listRecordIds(userId, circle._id);
			   map.put(circle.owner.toString(), ObjectIdConversion.toObjectIds(sharedids));		   
		   }
		   
		   return search(userId, map, query);
		}

	private static SearchRequestBuilder searchPrivateIndex(ObjectId userId, String query, String... types) {
		SearchRequestBuilder builder = client.prepareSearch(userId.toString()).setQuery(
				QueryBuilders.multiMatchQuery(query, TITLE, CONTENT));
		if (types != null) {
			builder.setTypes(types);
		}
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder searchPublicIndex(String query) {
		SearchRequestBuilder builder = client.prepareSearch(PUBLIC).setQuery(QueryBuilders.multiMatchQuery(query, TITLE, CONTENT));
		return addHighlighting(builder);
	}

	/**
	 * Builds the search request for other users' visible records.
	 * 
	 * @param visibleRecords Key: User id, Value: Set of record ids of records that are visible
	 */
	private static SearchRequestBuilder searchVisibleRecords(Map<String, Set<ObjectId>> visibleRecords, String query) {
		List<String> queriedIndices = new ArrayList<String>();
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		for (String curUserId : visibleRecords.keySet()) {
			if (visibleRecords.get(curUserId).size() > 0) {
				queriedIndices.add(curUserId);
				visibleRecordIds.addAll(visibleRecords.get(curUserId));
			}
		}
		Set<String> recordIdStrings = ObjectIdConversion.toStrings(visibleRecordIds);
		String[] recordIds = new String[recordIdStrings.size()];
		recordIdStrings.toArray(recordIds);
		String[] indicesArray = new String[queriedIndices.size()];
		SearchRequestBuilder builder = client.prepareSearch(queriedIndices.toArray(indicesArray))
				.setQuery(QueryBuilders.multiMatchQuery(query, TITLE, CONTENT))
				.setPostFilter(FilterBuilders.idsFilter("record").ids(recordIds));
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder addHighlighting(SearchRequestBuilder builder) {
		return builder.addHighlightedField(CONTENT, 150).setHighlighterPreTags("<span class=\"text-info\"><strong>")
				.setHighlighterPostTags("</strong></span>");
	}

	private static Map<String, List<SearchResult>> getSearchResults(SearchResponse... responses) {
		Map<String, List<SearchResult>> searchResults = new HashMap<String, List<SearchResult>>();
		for (SearchResponse response : responses) {
			for (SearchHit hit : response.getHits()) {
				// construct search result
				SearchResult searchResult = new SearchResult();
				searchResult.id = hit.getId();
				searchResult.score = hit.getScore();
				searchResult.title = (String) hit.getSource().get("title");
				if (!hit.getHighlightFields().isEmpty()) {
					for (String field : hit.getHighlightFields().keySet()) {
						Text[] fragments = hit.getHighlightFields().get(field).getFragments();
						if (fragments.length > 0) {
							searchResult.highlighted = fragments[0].toString();
							for (int k = 1; k < fragments.length; k++) {
								searchResult.highlighted += "<br>" + fragments[k].toString();
							}
						}
					}
				}

				// add search result to result list of respective type
				addToListInMap(searchResults, hit.getType(), searchResult);
			}
		}
		return searchResults;
	}

	private static <T, V> void addToListInMap(Map<T, List<V>> map, T key, V value) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<V>());
		}
		map.get(key).add(value);
	}

}

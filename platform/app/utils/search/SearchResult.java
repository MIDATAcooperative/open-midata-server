package utils.search;

public class SearchResult implements Comparable<SearchResult> {

	public String id;
	public String token;
	public float score;
	public String title;
	public String highlighted;

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			SearchResult otherResult = (SearchResult) other;
			return id.equals(otherResult.id);
		}
		return false;
	}

	@Override
	public int compareTo(SearchResult o) {
		// higher score is "less", i.e. earlier in sorted list
		return (int) -Math.signum(score - o.score);
	}

}
package org.frameworkset.elasticsearch.client;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RestResponse  implements SearchResult,Serializable {
	
    private long took;
    @JsonProperty("timed_out")
    private boolean timedOut;
    @JsonProperty("_shards")
    private Shards shards;
    @JsonProperty("hits")
    private SearchHits searchHits;
	public RestResponse() {
		// TODO Auto-generated constructor stub
	}
	public long getTook() {
		return took;
	}
	public void setTook(long took) {
		this.took = took;
	}
	 
	public Shards getShards() {
		return shards;
	}
	public void setShards(Shards shards) {
		this.shards = shards;
	}
	public SearchHits getSearchHits() {
		return searchHits;
	}
	public void setSearchHits(SearchHits searchHits) {
		this.searchHits = searchHits;
	}
	public boolean isTimedOut() {
		return timedOut;
	}
	public void setTimedOut(boolean timedOut) {
		this.timedOut = timedOut;
	}
	

}
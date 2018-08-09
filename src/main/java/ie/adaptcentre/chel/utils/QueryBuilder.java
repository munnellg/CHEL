package ie.adaptcentre.chel.utils;

import org.apache.lucene.search.Query;

public interface QueryBuilder {
	public Query buildQuery( String query, String field );
}
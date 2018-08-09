package ie.adaptcentre.chel.utils;

import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class StandardQueryBuilder implements QueryBuilder {
	
	public StandardQueryBuilder() {}

	public Query buildQuery( String query, String field ) {

		try {
            Analyzer analyzer = new StandardAnalyzer();
            QueryParser queryParser = new QueryParser(field, analyzer);
            queryParser.setDefaultOperator(QueryParser.Operator.AND);
            return queryParser.parse(query);
        } catch ( ParseException ex ) {
            ex.printStackTrace();
        }

        return null;
	}
}
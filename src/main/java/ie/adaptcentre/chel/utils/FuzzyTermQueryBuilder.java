package ie.adaptcentre.chel.utils;

import java.io.IOException;

import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class FuzzyTermQueryBuilder implements QueryBuilder {
	public FuzzyTermQueryBuilder() {}

	public Query buildQuery( String query, String field ) {
		try {
            Analyzer analyzer = new StandardAnalyzer();

            // tokenize the surface form        
            TokenStream tokenStream = analyzer.tokenStream(field, query );
            ArrayList<Term> tmpTerms = new ArrayList<Term>();
            
            tokenStream.reset();            
            while ( tokenStream.incrementToken() ) {
                String term = tokenStream.getAttribute(CharTermAttribute.class).toString();             
                tmpTerms.add( new Term(field, term) );
            }
            tokenStream.close();
            
            Term[] terms = tmpTerms.toArray(new Term[tmpTerms.size()]);
            
            if ( terms.length > 1 ) {
                SpanQuery[] clauses = new SpanQuery[terms.length];
                for ( int i = 0; i < terms.length; i++ ) {
                    clauses[i] = new SpanMultiTermQueryWrapper(
                        new FuzzyQuery(terms[i])
                    );
                }

                return new SpanNearQuery(clauses, 10, false);               
            } else if ( terms.length == 1 ) {
                return new FuzzyQuery(terms[0]);
            } 

        } catch ( IOException ex ) {
            ex.printStackTrace();
        }

        return null;
	}
}
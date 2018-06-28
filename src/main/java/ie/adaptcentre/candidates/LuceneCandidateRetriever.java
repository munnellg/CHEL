package ie.adaptcentre.candidates;

import java.util.Scanner;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.TokenStream;

import java.util.ListIterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import ie.adaptcentre.utils.BitVector;

public class LuceneCandidateRetriever implements ICandidateRetriever {
	
	private static final String indexDir = "../../index";
	private IndexReader reader = null;
	private IndexSearcher searcher = null;
	private Analyzer analyzer = null;
	private QueryParser parser = null;
	private static final int MAX_RESULTS = 100000;

	public LuceneCandidateRetriever() {
		try {
			reader = DirectoryReader.open(
				FSDirectory.open(Paths.get(indexDir))
			);

			searcher = new IndexSearcher(reader);
			analyzer = new StandardAnalyzer();
			parser = new QueryParser("surface_form", analyzer);
		} catch ( IOException ex ) {
			System.out.println(ex.getMessage());
		} 
	}

	public void permuteBitVector ( HashMap<String,Candidate> candidates,
			String bitvector, int e1, int e2, int maxError, int depth ) {
		
		if ( depth >= bitvector.length() ) { 
			try {
				Query query = parser.parse(bitvector);				
				TopDocs results = searcher.search(query, MAX_RESULTS);
				ScoreDoc[] hits = results.scoreDocs;
				int nr = Math.min(MAX_RESULTS, Math.toIntExact(results.totalHits));

				for (int i=0; i<nr; i++) {
					Document doc = searcher.doc(hits[i].doc);
					Candidate candidate = candidates.get(doc.get("uri"));
					if ( candidate == null ) {
						candidate = new Candidate(doc.get("uri"));
						candidates.put(doc.get("uri"), candidate);
					}
					
					candidate.addSurfaceForm(doc.get("surface_form"));
				}
			} catch ( ParseException ex ) {
				System.err.println(ex.getMessage());
			} catch ( IOException ex ) {
				System.err.println(ex.getMessage());
			}
			return;
		}

		char[] bits = bitvector.toCharArray();
		int bit = (bits[depth]=='1')? 1 : 0;

		if ( maxError >= e1 + bit && maxError >= e2 + (1-bit) ) { 
			bits[depth] = (bits[depth] == '1')? '0' : '1';
			permuteBitVector( 
				candidates, new String(bits), e1 + bit, e2 + (1-bit),
				maxError, depth + 1
			);
		}

		permuteBitVector( candidates, bitvector, e1, e2, maxError, depth + 1 );
	}

	@Override
	public ArrayList<Candidate> getCandidates( String surfaceForm ) {		
		ArrayList<Candidate> result = new ArrayList<Candidate>();
		HashMap<String,Candidate> candidates = new HashMap<String,Candidate>();
		
		try {
			// tokenize the surface form		
			TokenStream tokenStream = this.analyzer.tokenStream("surface_form", surfaceForm);						
			ArrayList<Term> tmpTerms = new ArrayList<Term>();
			tokenStream.reset();			
			while ( tokenStream.incrementToken() ) {
				String term = tokenStream.getAttribute(CharTermAttribute.class).toString();			    
			    tmpTerms.add(new Term("surface_form", term));
			}
			tokenStream.close();
			Term[] terms = tmpTerms.toArray(new Term[tmpTerms.size()]);
			
			Query query;
			if ( terms.length > 1 ) {
				SpanQuery[] clauses = new SpanQuery[terms.length];
				for ( int i = 0; i < terms.length; i++ ) {
					clauses[i] = new SpanMultiTermQueryWrapper(new FuzzyQuery(terms[i]));
				}

				query = new SpanNearQuery(clauses, 10, false);				
			} else if ( terms.length == 1 ) {
				query = new FuzzyQuery(terms[0]);
			} else {
				return result;
			}

			TopDocs results = searcher.search(query, MAX_RESULTS);;	
			ScoreDoc[] hits = results.scoreDocs;
			int nr = Math.min(MAX_RESULTS, Math.toIntExact(results.totalHits));
			System.out.printf("%s:\n", surfaceForm);
			for (int i=0; i<nr; i++) {
				Document doc = searcher.doc(hits[i].doc);
				Candidate candidate = candidates.get(doc.get("uri"));
				if ( candidate == null ) {
					candidate = new Candidate(doc.get("uri"));
					candidates.put(doc.get("uri"), candidate);
				}
				System.out.printf("\t%s : %s : %s\n", surfaceForm, doc.get("surface_form"), doc.get("uri"));
				candidate.addSurfaceForm(doc.get("surface_form"));
			}
		} catch ( IOException ex ) {
			System.err.println(ex.getMessage());
		}
				
		/*String bitvector = BitVector.vectorize(surfaceForm.toLowerCase());
		permuteBitVector( candidates, bitvector, 0, 0, 2, 0 );
		result = new ArrayList<Candidate>(candidates.values());
		System.out.printf("%s : %d\n", surfaceForm, result.size()); */		
		result = new ArrayList<Candidate>(candidates.values());		
		return result;
	}
}
package ie.adaptcentre.candidates;

import java.io.IOException;

import java.util.ListIterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;

public class SolrCandidateRetriever implements ICandidateRetriever {
	
	private static final String solrUrl = "http://vma56.scss.tcd.ie:8983/solr";
	private SolrClient client;

	public SolrCandidateRetriever() {
		this.client = new HttpSolrClient.Builder(
			SolrCandidateRetriever.solrUrl
		).build();
	}

	public SolrDocumentList searchForCandidates( String surfaceForm ) {
		Map<String, String> queryParamMap = new HashMap<String, String>();
		queryParamMap.put("q", ClientUtils.escapeQueryChars(surfaceForm));
		MapSolrParams queryParams = new MapSolrParams(queryParamMap);
		try {
			QueryResponse response = client.query("wiki_people", queryParams);
			SolrDocumentList documents = response.getResults();	
			return documents;
		} catch ( SolrServerException e ) {
			e.printStackTrace();
			return null;
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ArrayList<Candidate> getCandidates( String surfaceForm ) {
		ArrayList<Candidate> result = new ArrayList<Candidate>();
		SolrDocumentList candidates = searchForCandidates(surfaceForm);

		if ( candidates != null ) { 			
			ListIterator<SolrDocument> iter = candidates.listIterator();
			while ( iter.hasNext() ) {				
				SolrDocument d = iter.next();
				ArrayList<String> surfaceForms = new ArrayList<String>();
				surfaceForms.add((String) d.getFieldValue("title"));
				Collection<Object> altnames = d.getFieldValues("altnames");
				if ( altnames != null ) {
					for ( Object o : altnames ) { 
						surfaceForms.add((String) o); 
					}	
				}				

				Candidate c = new Candidate(
					(String) d.getFieldValue("dbpedia"), surfaceForms
				);

				result.add(c);				
			}
		}
		
		return result;
	}
}
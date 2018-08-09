package ie.adaptcentre.chel.parser;

import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.SimpleSelector;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

import org.apache.log4j.Logger;

import ie.adaptcentre.chel.model.LinkableContext;
import ie.adaptcentre.chel.model.LinkablePhrase;

import static eu.freme.common.conversion.rdf.RDFConstants.*;

public class NIFParser implements Parser {
	private static final Logger logger = Logger.getLogger(NIFParser.class);

	public NIFParser () { }

	private static final String QUERY_GET_PHRASES = String.format(
		"SELECT ?phrase " +
		"WHERE { " +
		"	?phrase <%s> <%s> . " +
		"	?phrase <%s> <%%s> . " +
		"}",
		RDF.type, 
		nifPrefix + NIF_PHRASE_TYPE, 
		nifPrefix + REFERENCE_CONTEXT 
	);

	private static final String TA_IDENT_REF = "http://www.w3.org/2005/11/its/rdf#taIdentRef";
	private static final String CONFIDENCE = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#confidence";

	private ArrayList<Resource> getPhrases( Model model, Resource context ) {
		// Create a new query to search for phrases associated with context
		String queryString = String.format(
			QUERY_GET_PHRASES, context.getURI()
		);
		
		// Execute the query and obtain results
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		// Retrieve all phrases from the set of results
		ArrayList<Resource> phrases = new ArrayList<Resource>();
        while ( results.hasNext() ) {
            QuerySolution qs = results.next();
            Resource result = qs.getResource( "phrase" );
            phrases.add(result);            
        } 

		// Free up resources used running the query
		qe.close();	

		return phrases;
	}

	private HashMap<Resource, ArrayList<Resource>> getContextsAndPhrases( Model model ) {
		HashMap<Resource, ArrayList<Resource>> contexts = 
			new HashMap<Resource, ArrayList<Resource>>();

		try {
			Resource contextType = 
				model.getResource(nifPrefix + NIF_CONTEXT_TYPE);
			Property isString = model.getProperty(IS_STRING_PROP);

			StmtIterator iter = model.listStatements(null, RDF.type, contextType);
			while (iter.hasNext()) {
				Resource context = iter.nextStatement().getSubject();
				Statement hasStringContent = context.getProperty(isString);
				if (hasStringContent != null) {
					ArrayList<Resource> phrases = getPhrases(model, context);
					contexts.put( context, phrases );
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}

		return contexts;
	}

	private ArrayList<LinkableContext> deserializeContexts( Model model,
			HashMap<Resource, ArrayList<Resource>> contextsMap ) {
		
		Property anchorOfProperty = model.getProperty(ANCHOR_OF_PROP);
		Property beginIndexProperty = model.getProperty(nifPrefix + BEGIN_INDEX);
		Property endIndexProperty = model.getProperty(nifPrefix + END_INDEX);
		Property isString = model.getProperty(IS_STRING_PROP);

		ArrayList<LinkableContext> contexts = new ArrayList<LinkableContext>();

		for (HashMap.Entry<Resource, ArrayList<Resource>> e : contextsMap.entrySet()) {
		    Resource contextResource = e.getKey();
		    ArrayList<Resource> phraseResources = e.getValue();
		    ArrayList<LinkablePhrase> phrases = new ArrayList<LinkablePhrase>();
		    String content = contextResource.getProperty(isString).getObject().asLiteral().getString();
		    LinkableContext context = new LinkableContext( contextResource.getURI(), content );
		    
		    for ( Resource phraseResource : phraseResources ) {
		    	String anchorOf = phraseResource.getProperty(anchorOfProperty).getObject().asLiteral().getString();
		    	int beginIndex = phraseResource.getProperty(beginIndexProperty).getObject().asLiteral().getInt();
		    	int endIndex = phraseResource.getProperty(endIndexProperty).getObject().asLiteral().getInt();

		    	LinkablePhrase phrase = new LinkablePhrase( context, beginIndex, endIndex );
		    	phrases.add(phrase);
		    }

		    context.setPhrases(phrases);
		    contexts.add(context);
		}

		return contexts;
	}

	@Override
	public LinkableContext parseString ( String string ) {
		InputStream stream = new ByteArrayInputStream( string.getBytes() );
		return this.parseStream( stream );
	}

	@Override
	public LinkableContext parseStream ( InputStream stream ) {
		
		Model model = ModelFactory.createDefaultModel();
		model.read(stream, null, "TURTLE");
		HashMap<Resource, ArrayList<Resource>> contextsMap = getContextsAndPhrases(model);
		ArrayList<LinkableContext> contexts = deserializeContexts(model, contextsMap);
		if ( contexts.size() > 1 ) {
			logger.warn("More than one context in stream");
		} else if ( contexts.size() == 1 ) {
			return contexts.get(0);
		} 

		return null;
	}
}
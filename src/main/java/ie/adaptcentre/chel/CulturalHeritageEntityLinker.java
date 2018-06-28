package ie.adaptcentre.chel;

import java.util.HashMap;
import java.util.ArrayList;

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.rdf.model.Model;
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

import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;

import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static eu.freme.common.conversion.rdf.RDFConstants.*;

import ie.adaptcentre.linker.EntityLinker;
import ie.adaptcentre.spotter.EntitySpotter;
import ie.adaptcentre.models.LinkableContext;
import ie.adaptcentre.models.LinkablePhrase;

@RestController
public class CulturalHeritageEntityLinker extends BaseRestController {

	Logger logger = Logger.getLogger(CulturalHeritageEntityLinker.class);

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

	public ArrayList<Resource> getPhrases( Model model, Resource context ) {
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

	public HashMap<Resource, ArrayList<Resource>> getContextsAndPhrases( Model model ) {
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
			throw new InternalServerErrorException();
		}

		return contexts;
	}

	public ArrayList<LinkableContext> deserializeContexts( Model model,
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

		    for ( Resource phraseResource : phraseResources ) {
		    	String anchorOf = phraseResource.getProperty(anchorOfProperty).getObject().asLiteral().getString();
		    	int beginIndex = phraseResource.getProperty(beginIndexProperty).getObject().asLiteral().getInt();
		    	int endIndex = phraseResource.getProperty(endIndexProperty).getObject().asLiteral().getInt();

		    	LinkablePhrase phrase = new LinkablePhrase( beginIndex, endIndex, anchorOf );
		    	phrases.add(phrase);
		    }

		    String content = contextResource.getProperty(isString).getObject().asLiteral().getString();
		    LinkableContext context = new LinkableContext( contextResource.getURI(), content, phrases );
		    contexts.add(context);
		}

		return contexts;
	}

	public void serializeContexts( Model model,
			ArrayList<LinkableContext> contexts ) {

		// Property taIdentRefProperty = model.getProperty(nifPrefix + TA);

		Property taIdentRefProperty = model.getProperty(TA_IDENT_REF);
		if ( taIdentRefProperty == null ) {
			taIdentRefProperty = model.createProperty(TA_IDENT_REF);
		}

		Property confidence = model.getProperty(CONFIDENCE);
		if ( confidence == null ) {
			confidence = model.createProperty(CONFIDENCE);
		}

		for ( LinkableContext context : contexts ) {
			String contextURI = context.getContextId().split("#")[0];

			for ( LinkablePhrase phrase : context.getPhrases() ) {
				String phraseURI = String.format("%s%s%d,%d",
					contextURI, NIF20_OFFSET,
					phrase.getBeginIndex(), phrase.getEndIndex()
				);
				
				// logger.info("\"" + phrase.getAnchorOf() + "\"," + "\"" + phrase.getReferent() + "\"");
				Resource phraseResource = model.getResource(phraseURI);
				Property referent = model.createProperty(phrase.getReferent());
				Literal weight = model.createTypedLiteral(new Double(phrase.getConfidence()));
				phraseResource.addProperty(taIdentRefProperty, referent);
				phraseResource.addProperty(confidence, weight);
			}
		}
	}

	@RequestMapping(value = "/chel", method = RequestMethod.POST)
	public ResponseEntity<String> linkEntities(HttpServletRequest request) {
		
		// Extract the text that we want to proecss
		NIFParameterSet nifParameters = getRestHelper().normalizeNif(request, false);
		Model model = getRestHelper().convertInputToRDFModel(nifParameters);
		HashMap<Resource, ArrayList<Resource>> contextsMap = getContextsAndPhrases(model);
		if (contextsMap.isEmpty()) {
			throw new BadRequestException("Could not find input for enrichment");
		}

		// Extract the contexts and phrases from the input text
		ArrayList<LinkableContext> contexts = deserializeContexts(model, contextsMap);

		if (request.getParameterMap().containsKey("spot")) {
            EntitySpotter.spotAllContexts(contexts);
        }

		EntityLinker el = new EntityLinker();		
		el.linkAllContexts(contexts);
		serializeContexts(model, contexts);

		return createSuccessResponse(model,	nifParameters.getOutformatString());
	}
}
package ie.adaptcentre.chel.knowledge;

import java.io.IOException;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.nio.file.Paths;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.BooleanQuery.Builder;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.FuzzyQuery;

import ie.adaptcentre.chel.configuration.CHELProperties;
import ie.adaptcentre.chel.model.Triple;
import ie.adaptcentre.chel.model.Candidate;

import ie.adaptcentre.chel.utils.StringSim;
import ie.adaptcentre.chel.utils.MongeElkanSim;
import ie.adaptcentre.chel.utils.LevenshteinSim;

import ie.adaptcentre.chel.utils.QueryBuilder;
import ie.adaptcentre.chel.utils.FuzzyTermQueryBuilder;
import ie.adaptcentre.chel.utils.StandardQueryBuilder;

public class LuceneKnowledgeBase implements KnowledgeBase {
    
    private static final Logger LOG = LoggerFactory.getLogger( LuceneKnowledgeBase.class );
    private static int LUCENE_MAX_RESULTS = 500;

    private static final String SUBJECT_ID          = "subjectId";
    private static final String SUBJECT_URI         = "subjectURI";
    private static final String SUBJECT_EQUIVALENTS = "subjectEquivalents";
    private static final String PREDICATE           = "predicate";
    private static final String OBJECT_ID           = "objectId";
    private static final String OBJECT_URI          = "objectURI";
    private static final String OBJECT_EQUIVALENTS  = "objectEquivalents";
    private static final String OBJECT_LITERAL      = "objectLiteral";

    private IndexReader reader;
    private IndexSearcher searcher;
    
    private StringSim    stringSim;
    private QueryBuilder queryBuilder;

    public LuceneKnowledgeBase ( String knowledgeBaseDirectory ) throws IOException {
        CHELProperties properties = CHELProperties.getInstance();
        this.reader = DirectoryReader.open( 
			FSDirectory.open( Paths.get( knowledgeBaseDirectory ) )
		);

		this.searcher = new IndexSearcher( reader );

        String confStringSim = properties.getString("knowledge_base.surface_form_similarity");
        if ( confStringSim.equals("levenshtein") ) {
            LOG.info("Using Levenshtein similarity");
            this.stringSim = new LevenshteinSim();
        } else if ( confStringSim.equals("monge-elkan") ) {
            LOG.info("Using Monge Elkan similarity");
            try {
                double exponent = properties.getDouble("knowledge_base.surface_form_similarity.monge-elkan.exponent");
                LOG.info(String.format( "Setting Monge-Elkan exponent to %f", exponent ));
                this.stringSim = new MongeElkanSim(exponent);
            } catch ( NoSuchElementException ex ) {
                LOG.info("Using default Monge-Elkan exponent");
                this.stringSim = new MongeElkanSim();
            }
        } else {
            LOG.warn("Invalid string similarity in properties file. Defaulting to Monge-Elkan");
            this.stringSim = new MongeElkanSim();
        }
        
        String confLiteralQueryBuilder = properties.getString("knowledge_base.literal_query_builder");

        if ( confLiteralQueryBuilder.equals("standard") ) {
            LOG.info("Using Standard Literal Query Builder");
            this.queryBuilder = new StandardQueryBuilder();            
        } else if ( confStringSim.equals("fuzzy") ) {
            LOG.info("Using Fuzzy Literal Query Builder");
            this.queryBuilder = new FuzzyTermQueryBuilder();    
        } else {
            LOG.warn("Invalid literal query builder in properties file. Defaulting to Standard Literal Query Builder");
            this.queryBuilder = new StandardQueryBuilder();    
        }
    }

    private boolean hasPrefixInList( String label, List<String> list ) {
        for ( String s : list ) {
            if ( label.startsWith(s) ) {
                return true;
            }
        }
        return false;
    }

    private boolean isDisambiguation ( String subjectId ) {
        CHELProperties properties = CHELProperties.getInstance();
        String predicate = properties.getString("knowledge_base.disambiguation_type");
        ArrayList<Triple> triples = query( subjectId, null, predicate, null, null, null, LUCENE_MAX_RESULTS );
        return triples.size() != 0;
    }

    private boolean isValidEntityType ( String subjectId ) {
        CHELProperties properties = CHELProperties.getInstance();
        String entityTypeLabel = properties.getString("knowledge_base.entity_type_label");
        List<String> validEntityTypes = properties.getList("knowledge_base.valid_entity_types");
        for ( String type : validEntityTypes ) {
            ArrayList<Triple> triples = query( subjectId, null, entityTypeLabel, type, null, null, LUCENE_MAX_RESULTS );
            if ( triples.size() > 0 ) {
                Triple t = triples.get(0);
                if ( t.getType() == Triple.TripleType.RESOURCE ) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInCandidateNamespace( String uri ) {
        CHELProperties properties = CHELProperties.getInstance();
        List<String> namespaces = properties.getList("knowledge_base.candidate_namespace");
        return hasPrefixInList( uri, namespaces );
    }

    private String resolveRedirect ( String subjectId ) {
        CHELProperties properties = CHELProperties.getInstance();
        String redirectString = properties.getString("knowledge_base.redirect_type");
        ArrayList<Triple> triples = query( subjectId, null, redirectString, null, null, null, LUCENE_MAX_RESULTS );
        if ( triples.size() > 0 ) {
            Triple t = triples.get(0);
            if ( t.getType() == Triple.TripleType.RESOURCE ) {
                LOG.info( "Resolving redirect " + subjectId + " -> " + t.getObjectId() );
                subjectId = t.getObjectId();
            }
        }

        return subjectId;
    }

	private ArrayList<Triple> query ( String subjectId, String subjectURI,
            String predicate, String objectId, String objectURI,
            String objectLiteral, int maxResults ) {

        Builder builder = new Builder();
        
        if ( subjectId != null ) {
            Query q = new TermQuery(new Term(SUBJECT_ID, subjectId));
            builder.add(q, BooleanClause.Occur.MUST);
        }

        if ( subjectURI != null ) {
            Query q = new TermQuery(new Term(SUBJECT_URI, subjectURI));
            builder.add(q, BooleanClause.Occur.MUST);
        }

        if ( predicate != null ) {
            Query q = new TermQuery(new Term(PREDICATE, predicate));
            builder.add(q, BooleanClause.Occur.MUST);
        }

        if ( objectId != null ) {
            Query q = new TermQuery(new Term(OBJECT_ID, objectId));
            builder.add(q, BooleanClause.Occur.MUST);
        }

        if ( objectURI != null ) {
            Query q = new TermQuery(new Term(OBJECT_URI, objectURI));
            builder.add(q, BooleanClause.Occur.MUST);
        }

        if ( objectLiteral != null ) {
            Query q = this.queryBuilder.buildQuery( objectLiteral, OBJECT_LITERAL );
            builder.add(q, BooleanClause.Occur.MUST);
            /*
            try {
                Query q = null;

                Analyzer analyzer = new StandardAnalyzer();

                // tokenize the surface form        
                TokenStream tokenStream = analyzer.tokenStream(OBJECT_LITERAL, objectLiteral );
                ArrayList<Term> tmpTerms = new ArrayList<Term>();
                
                tokenStream.reset();            
                while ( tokenStream.incrementToken() ) {
                    String term = tokenStream.getAttribute(CharTermAttribute.class).toString();             
                    tmpTerms.add( new Term(OBJECT_LITERAL, term) );
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

                    q = new SpanNearQuery(clauses, 10, false);               
                } else if ( terms.length == 1 ) {
                    q = new FuzzyQuery(terms[0]);
                } 
   
                builder.add(q, BooleanClause.Occur.MUST);
            } catch ( IOException ex ) {
                ex.printStackTrace();
            }
            
            try {
                Analyzer analyzer = new StandardAnalyzer();
                QueryParser queryParser = new QueryParser(OBJECT_LITERAL, analyzer);
                queryParser.setDefaultOperator(QueryParser.Operator.AND);
                Query q = queryParser.parse(objectLiteral);
                builder.add(q, BooleanClause.Occur.MUST);
            } catch ( ParseException ex ) {
                ex.printStackTrace();
            }
            */
        }

        return executeQuery( builder.build(), maxResults );
    }

    private ArrayList<Triple> executeQuery ( Query query, int maxResults ) {
        ArrayList<Triple> triples = new ArrayList<Triple>();

        try {
            TopDocs results = searcher.search( query, maxResults );
            ScoreDoc[] hits = results.scoreDocs;

            int nr = Math.min( maxResults, Math.toIntExact( results.totalHits ) );
            for ( int i = 0; i < nr; i++ ) {
                Document doc = searcher.doc(hits[i].doc);
                
                String subjectId  = doc.get(SUBJECT_ID);
                String subjectURI = doc.get(SUBJECT_URI);
                
                ArrayList<String> subjectEquivalents = new ArrayList<String> (
                    Arrays.asList(doc.get(SUBJECT_EQUIVALENTS).split("\t"))
                );

                String predicate = doc.get(PREDICATE);

                String objectId  = doc.get(OBJECT_ID);
                String objectURI = doc.get(OBJECT_URI);
                
                ArrayList<String> objectEquivalents = new ArrayList<String> (
                    Arrays.asList(doc.get(OBJECT_EQUIVALENTS).split("\t"))
                );

                String objectLiteral = doc.get(OBJECT_LITERAL);

                if ( objectURI.length() > 0 ) {
                    triples.add( 
                        new Triple( subjectId, subjectURI, subjectEquivalents,
                            predicate, objectId, objectURI, objectEquivalents,
                            objectLiteral, Triple.TripleType.RESOURCE 
                        )
                    );
                } else if ( objectLiteral.length() > 0 )  {
                    triples.add( 
                        new Triple( subjectId, subjectURI, subjectEquivalents,
                            predicate, objectId, objectURI, objectEquivalents,
                            objectLiteral, Triple.TripleType.RESOURCE 
                        )
                    );
                } else {
                    LOG.warn("Got result with neither literal nor URI: " + subjectId + ", " + predicate);
                }
            }
        } catch ( IOException ex ) {
            ex.printStackTrace();
        }
        
        return triples;
    }

    @Override
	public ArrayList<Candidate> fetchCandidates ( String surfaceForm ) {
        ArrayList<String> result = new ArrayList<String>();
        HashMap<String, Candidate> candidates = new HashMap<String, Candidate>();
        CHELProperties properties = CHELProperties.getInstance();
        List<String> labelTypes = properties.getList("knowledge_base.surface_form_type");
        double threshold = properties.getDouble("knowledge_base.surface_form_threshold");

        ArrayList<Triple> triples = new ArrayList<Triple>();
        for ( String s : labelTypes ) {
            triples.addAll(query( null, null, s, null, null, surfaceForm, LUCENE_MAX_RESULTS ));
        }

        double max = 0;
        
        LOG.info(surfaceForm);

        for ( Triple t : triples ) {
            String candidateId  = t.getSubjectId();
            String candidateURI = t.getSubjectURI();

            // reject any candiates whose surface form is too dissimilar
            double sim = this.stringSim.similarity( t.getObjectLiteral(), surfaceForm );
            LOG.info("\t" + t.getSubjectId() + ", " + t.getObjectLiteral() + ", " + String.format("%f", sim));
            if ( sim < threshold ) { continue; }
                        
            for ( String s : t.getSubjectEquivalents() ) {
                LOG.info("\t\t" + s);
            }

            candidateId = resolveRedirect(candidateId);
            if ( isDisambiguation(candidateId) )         { continue; }
            if ( !isInCandidateNamespace(candidateURI) ) { continue; }
            
            // if ( !isValidEntityType(candidateURI) ) { continue; }
            // if ( !t.getObjectLiteral().toLowerCase().equals(surfaceForm.toLowerCase()) ) { continue; }
            Candidate c = candidates.get(candidateId);
            sim = sim + 1; // sim needs to be greater than 1 because it will be a positive boost for HITS
            if ( c == null ) {
                c = new Candidate( candidateId, t.getObjectLiteral(), sim );
                candidates.put(candidateId, c);
            } else if ( c.getSurfaceFormSim() < sim ) {
                c.setSurfaceForm( t.getObjectLiteral() );
                c.setSurfaceFormSim( sim );
            }

            if ( sim > max ) { max = sim; }
        }

        ArrayList<Candidate> results = new ArrayList<Candidate>();
        for ( Candidate c : candidates.values() ) {
            if ( c.getSurfaceFormSim() >= max ) {
                results.add(c);
            }
        }

		return results;
    }

    @Override
	public ArrayList<String> fetchOutgoingLinks ( String subjectId ) {
        ArrayList<String> result = new ArrayList<String>();
        HashSet<String> outgoing = new HashSet<String>();
        CHELProperties properties = CHELProperties.getInstance();
        List<String> edgeTypes   = properties.getList("knowledge_base.edge_type");
        List<String> vertexTypes = properties.getList("knowledge_base.vertex_type");

        ArrayList<Triple> triples = query( subjectId, null, null, null, null, null, LUCENE_MAX_RESULTS );

        for ( Triple t : triples ) {
            if ( hasPrefixInList( t.getPredicate(), edgeTypes )
                    && hasPrefixInList( t.getObjectURI(), vertexTypes ) ) {
                String object = resolveRedirect( t.getObjectId() );					
                outgoing.add(object);	
            }
        }

		return new ArrayList<String>(outgoing);
    }

    public String resolveToNamespace( String subjectId, String namespace ) {
        CHELProperties properties = CHELProperties.getInstance();
        ArrayList<Triple> triples = query( subjectId, null, null, null, null, null, LUCENE_MAX_RESULTS );
        
        for ( Triple t : triples ) {
            for ( String s : t.getSubjectEquivalents() ) {
                LOG.info(s);
                if ( s.startsWith(namespace) ) {
                    return s;
                }
            }
        }

        return "NIL";
    }
}
package ie.adaptcentre.chel;

import java.nio.file.Paths;

import java.util.ArrayList;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;

import org.apache.jena.query.ARQ;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import ie.adaptcentre.chel.resolver.Resolver;

public class KnowledgeBaseIndexer implements StreamRDF {
	private static long tripleCount = 0;
	private static String confIndexDir = "index";
	private static String confEquivalenceFile = null;
	private static ArrayList<String> confInputFiles = new ArrayList<String>();
	
	private IndexWriter writer;
	private Resolver resolver;

	public static void main ( String[] args ) {
		processArgs(args);

        if ( confInputFiles.size() == 0 ) {
        	System.out.println("Missing file for indexing\n");
        	usage();
        }

        ARQ.init();

        KnowledgeBaseIndexer kbi = new KnowledgeBaseIndexer();
		
		if ( !kbi.openIndex( confIndexDir ) ) { System.exit(0); }

		if ( confEquivalenceFile != null ) {
			if ( !kbi.loadEquivalence( confEquivalenceFile ) ) { 
				System.exit(0); 
			}
		}

		for ( String fname : confInputFiles ) {
			try {
				System.out.printf("Processing %s\n", fname);
				BufferedInputStream bis =
					new BufferedInputStream(new FileInputStream(fname));
				RDFDataMgr.parse( kbi, bis, Lang.TURTLE );
			} catch ( IOException e ) {
			 	System.out.println(e.getMessage());
			}
		}

		kbi.closeIndex();
	}

	private static void usage () {
		System.out.println("KnowledgeBaseIndexer [options] FILE ...");
		System.out.println("");
		System.out.println("options:");
		System.out.println("\t-i --index\tdirectory for storing index");
		System.out.println("\t-e --equivalence\tmapping of equivalent entities across KB sources");
		System.out.println("\t-h --help\tprint this message");
		System.exit(0);
	}

	private static void processArgs ( String[] args ) {

		for ( int i=0; i<args.length; i++ ) {
		
			if ( args[i].equals("-i") || args[i].equals("--index") ) {				
				confIndexDir = args[++i];
			} else if ( args[i].equals("-h") || args[i].equals("--help") ) {
				usage();        		
			} else if ( args[i].equals("-e") || args[i].equals("--equivalence") ) {
				confEquivalenceFile = args[++i];
			} else {
				confInputFiles.add( args[i] );
			}
		}
	}

	public KnowledgeBaseIndexer() {
        this.writer = null;
        this.resolver = new Resolver();
    }

	public boolean openIndex( String indexDir ) {
		try {
			Directory dir = FSDirectory.open(Paths.get(indexDir));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
			writerConfig.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter( dir, writerConfig );
		} catch ( IOException ex ) {
			System.err.println(ex.getMessage());
			return false;
		}

		return true;
	}

	public boolean loadEquivalence( String fname ) {
		try {
			this.resolver.loadEquivalenceFile(fname);
		} catch ( IOException ex ) {
			System.err.println(ex.getMessage());
			return false;
		}
		return true;
	}

	public void addToIndex ( String subjectId, String subjectURI,
			String subjectEquivalents, String predicate, String objectLiteral,
			String objectId, String objectURI, String objectEquivalents ) {

		try {
			Document doc = new Document();		
			System.out.printf( "Adding %d\r", ++tripleCount );
			doc.add(new StringField("subjectId", subjectId, Field.Store.YES));
			doc.add(new StringField("subjectURI", subjectURI, Field.Store.YES));
			doc.add(new StringField("subjectEquivalents", subjectEquivalents, Field.Store.YES));
			doc.add(new StringField("predicate", predicate, Field.Store.YES));
			doc.add(new StringField("objectId", objectId, Field.Store.YES));
			doc.add(new StringField("objectURI", objectURI, Field.Store.YES));
			doc.add(new StringField("objectEquivalents", objectEquivalents, Field.Store.YES));
			doc.add(new TextField("objectLiteral", objectLiteral, Field.Store.YES));
			this.writer.addDocument(doc);
		} catch ( IOException ex ) {
			System.out.println( ex.getMessage() );
		}		
	}

    public void start ( ) { tripleCount = 0; }

    public void triple ( Triple triple ) {
    	if ( triple.getSubject().isURI() && triple.getPredicate().isURI() ) {
    		String subjectURI         = triple.getSubject().getURI();
    		String subjectId          = this.resolver.getIdentifier(subjectURI);
    		String subjectEquivalents = "";
    		String predicate          = triple.getPredicate().getURI();
    		String objectLiteral      = "";
    		String objectEquivalents  = "";
    		String objectURI          = "";
    		String objectId           = "";

    		if ( triple.getObject().isURI() ) {
    			objectURI = triple.getObject().getURI();
    			objectId  = this.resolver.getIdentifier(objectURI);

    			StringBuilder objectEquivalentsBuilder = new StringBuilder();
    			for ( String s : this.resolver.getEquivalents(objectURI) ) {
				    objectEquivalentsBuilder.append(s);
				    objectEquivalentsBuilder.append("\t");
				}

				// truncate final tab character
				if (objectEquivalentsBuilder.length() > 0) {
				   	objectEquivalentsBuilder.setLength(
						objectEquivalentsBuilder.length() - 1
					);
				}

				objectEquivalents = objectEquivalentsBuilder.toString();

    		} else if ( triple.getObject().isLiteral() ) {
    			objectLiteral = triple.getObject().getLiteral().getLexicalForm();
    		} else {
    			return;
    		}

			StringBuilder subjectEquivalentsBuilder = new StringBuilder();
			for ( String s : this.resolver.getEquivalents(subjectURI) ) {
			    subjectEquivalentsBuilder.append(s);
			    subjectEquivalentsBuilder.append("\t");
			}

			// truncate final tab character
			if (subjectEquivalentsBuilder.length() > 0) {
			   	subjectEquivalentsBuilder.setLength(
					subjectEquivalentsBuilder.length() - 1
				);
			}

			subjectEquivalents = subjectEquivalentsBuilder.toString();

    		addToIndex( subjectId, subjectURI, subjectEquivalents, predicate,
    			objectLiteral, objectId, objectURI, objectEquivalents );
    	}
    }

    public void quad ( Quad quad ) { /* do nothing */ }

    public void base ( String base ) { /* do nothing */ }

    public void prefix ( String prefix, String iri ) { /* do nothing */ }

    public void finish () { System.out.println(); }

	public void closeIndex () {
		try {
			writer.close();
		} catch ( IOException ex ) {
			System.err.println(ex.getMessage());
		}
	}
}
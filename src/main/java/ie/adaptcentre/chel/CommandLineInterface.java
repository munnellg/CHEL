package ie.adaptcentre.chel;

import java.util.ArrayList;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;

import ie.adaptcentre.chel.parser.Parser;
import ie.adaptcentre.chel.parser.PlainTextParser;
import ie.adaptcentre.chel.parser.NIFParser;
import ie.adaptcentre.chel.spotter.EntitySpotter;
import ie.adaptcentre.chel.spotter.OpenNLPSpotter;
import ie.adaptcentre.chel.model.LinkableContext;
import ie.adaptcentre.chel.model.LinkablePhrase;
import ie.adaptcentre.chel.configuration.CHELProperties;
import ie.adaptcentre.chel.knowledge.KnowledgeBase;
import ie.adaptcentre.chel.knowledge.LuceneKnowledgeBase;
import ie.adaptcentre.chel.linking.EntityLinker;
import ie.adaptcentre.chel.linking.HITSEntityLinker;

import java.util.Date;
import java.text.SimpleDateFormat;

public class CommandLineInterface {

	private static ArrayList<String> inputFiles = new ArrayList<String>();
	private static boolean enableSpotting = false;
	private static EntitySpotter spotter = null;
	private static KnowledgeBase knowledgeBase = null;
	private static EntityLinker entityLinker = null;
	
	private static final Logger LOG = LoggerFactory.getLogger( CommandLineInterface.class );

	public static void usage () {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String programName = main.getClassName();
		System.out.printf("usage: %s [options] File ...\n", programName );
		System.out.printf("\n");
		System.out.printf("options:\n");
		System.out.printf("\t-s\tEnable Named Entity Recognition\n");
		System.out.printf("\t-c\tUse custom configuration file for CHEL\n");
		System.out.printf("\t-h\tPrint this help message\n");
		System.exit(0);
	}

	private static boolean loadSpotter ( CHELProperties p ) {
		// try to load path to models and quit on failure
		String spotterName = p.getString("spotters.spotter");		
		if ( spotterName == null ) {
			LOG.error("No \"spotters.spotter\" element in config file");
		} else {
			if ( spotterName.equals( "opennlp" ) ) {
				spotter = new OpenNLPSpotter();
			} else {
				LOG.error("Invalid spotter in config file \"" + spotterName + "\"");
			}
		}
		
		return spotter != null;
	}

	private static boolean initializeKnowledgeBase ( CHELProperties p ) {
		String knowledgeBaseName = p.getString("knowledge_base");		
		if ( knowledgeBaseName == null ) { 
			LOG.error("No \"knowledge_base\" element in config file");
		} else {
			if ( knowledgeBaseName.equals( "lucene" ) ) {
                String indexDir = p.getString("knowledge_base.lucene.index_dir");
				if ( indexDir != null ) { 
					try {
						knowledgeBase = new LuceneKnowledgeBase( indexDir );
					} catch ( IOException ex ) {
						ex.printStackTrace();
					}
				}
			} else {
				LOG.error("Invalid knowledge base in config file \"" + knowledgeBaseName + "\"");
			}
		}

		return knowledgeBase != null;
	}

	private static boolean initializeEntityLinker ( CHELProperties p ) {
		String entityLinkerName = p.getString("entity_linker");		
		if ( entityLinkerName == null ) { 
			LOG.error("No \"entity_linker\" element in config file");
		} else {
			if ( entityLinkerName.equals( "hits" ) ) {
                entityLinker = new HITSEntityLinker( knowledgeBase );
			} else {
				LOG.error("Invalid entity linker in config file \"" + entityLinkerName + "\"");
			}
		}

		return entityLinker != null;
	}

	private static boolean initialize () {
		// try to load project configuration and quit on failure
		CHELProperties p = CHELProperties.getInstance();
		if ( p == null ) { 
			return false; 
		}

		if ( enableSpotting ) {
			if ( !loadSpotter(p) ) {
				LOG.error("unable to initialize spotter");
				return false;
			}
		}

		if ( !initializeKnowledgeBase(p) ) {
			LOG.error("unable to initialize knowledge base");
			return false;
		}

		if ( !initializeEntityLinker(p) ) {
			LOG.error("unable to initialize entity linker");
			return false;
		}

		return true;
	}

	public static void parseArgs ( String [] args ) {
		for ( int i = 0; i < args.length; i++ ) {
			if ( args[i].startsWith("-") ) {
				if ( args[i].equals("-h") ) {
					usage();
				} else if ( args[i].equals("-s") ) {
					enableSpotting = true;					
				} else if ( args[i].equals("-c") ) {
					try {
						// allow users to load a custom configuration file at runtime
						String configFile = args[++i];
						CHELProperties config = new CHELProperties( configFile );
						CHELProperties.setInstance(config);
					} catch ( ConfigurationException ex ) {
						ex.printStackTrace();
					}
				} else {
					System.out.printf( "Invalid flag \"%s\"\n", args[i] );
				}
			} else {
				inputFiles.add(args[i]);
			}
		}
	}

	public static Parser getParser ( String fname ) {
		Parser parser = null;
		String ext = FilenameUtils.getExtension(fname);
		
		if ( ext.equals("txt") ) {
			parser = new PlainTextParser();
		} else if ( ext.equals("ttl") ) {
			parser = new NIFParser();
		}

		return parser;
	}
 
	public static void main ( String [] args ) {
		parseArgs(args);
		
		if ( inputFiles.size() == 0 ) {
			System.out.printf( "No input files. Run with \"-h\" flag for usage information\n" );
			System.exit(0);
		}
		
		if ( !initialize() ) {
			System.exit(0);
		}

		for ( String fname : inputFiles ) {
			Parser parser = getParser(fname);
			
			if ( parser == null ) {
				System.out.printf("Unknown file extension for \"%s\"\n", fname);
			} else {
				try {
					LOG.info("Processing " + fname);
					LinkableContext lc = parser.parseStream(
						new BufferedInputStream(
							new FileInputStream(fname)
						)
					);

					if ( spotter != null ) {
						spotter.spotEntities( lc );
						lc.removeOverlappingPhrases();
					}

					lc.setContextId(fname);
					entityLinker.linkContext(lc);
					ArrayList<LinkablePhrase> phrases = lc.getPhrases();
					System.out.println(lc.getContextId());
					for ( LinkablePhrase lp : phrases ) {
						System.out.printf("\t%-24s : %s\n", lp.getAnchorOf(), lp.getReferent());
					}

					entityLinker.logStatus( String.format("%s-%s.gexf", new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date()), lc.getContextId().replace(File.separator, "_")) );
				} catch ( IOException ex ) {
					ex.printStackTrace();
				}
			}
		}

		LOG.info("DONE");
	}
}
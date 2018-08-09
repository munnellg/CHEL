package ie.adaptcentre.chel;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.nio.file.Paths;

import java.net.URL;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;

import ie.adaptcentre.chel.model.*;
import ie.adaptcentre.chel.knowledge.*;
import ie.adaptcentre.chel.linking.*;
import ie.adaptcentre.chel.configuration.*;

public class CulturalHeritageEntityLinker {
	private static final Logger LOG = LoggerFactory.getLogger( CulturalHeritageEntityLinker.class );

	private KnowledgeBase knowledgeBase;
	private EntityLinker entityLinker;
	private static final String PROPERTIES_RESOURCE = "chel.properties";

	public CulturalHeritageEntityLinker () {
		this(null);
	}

	public CulturalHeritageEntityLinker ( String propertiesFile ) {
		if ( propertiesFile == null ) {
			try {
				URL resource = CulturalHeritageEntityLinker.class.getClassLoader().getResource(PROPERTIES_RESOURCE);		
				File file = Paths.get(resource.toURI()).toFile();
				propertiesFile = file.getAbsolutePath();
			} catch ( URISyntaxException ex ) {
					ex.printStackTrace();
			} 
		}
		
		CHELProperties config = null;
		try {
			config = new CHELProperties( propertiesFile );
			CHELProperties.setInstance(config);
		} catch ( ConfigurationException ex ) {
			ex.printStackTrace();
		}

		String knowledgeBaseName = config.getString("knowledge_base");		
		
		if ( knowledgeBaseName == null ) { 
			LOG.error("No \"knowledge_base\" element in config file");
		} else {
			if ( knowledgeBaseName.equals( "lucene" ) ) {
                String indexDir = config.getString("knowledge_base.lucene.index_dir");
				if ( indexDir != null ) { 
					try {
						this.knowledgeBase = new LuceneKnowledgeBase( indexDir );
					} catch ( IOException ex ) {
						ex.printStackTrace();
					}
				}
			} else {
				LOG.error("Invalid knowledge base in config file \"" + knowledgeBaseName + "\"");
			}
		}

		String entityLinkerName = config.getString("entity_linker");		
		if ( entityLinkerName == null ) { 
			LOG.error("No \"entity_linker\" element in config file");
		} else {
			if ( entityLinkerName.equals( "hits" ) ) {
                this.entityLinker = new HITSEntityLinker( knowledgeBase );
			} else {
				LOG.error("Invalid entity linker in config file \"" + entityLinkerName + "\"");
			}
		}
	}

	public void linkAllContexts( ArrayList<LinkableContext> contexts ) {
		for ( LinkableContext c : contexts ) {
			this.entityLinker.linkContext(c);
		}
	}
}
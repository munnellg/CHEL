package ie.adaptcentre.chel.configuration;

import java.util.List;

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

public class CHELProperties {
	
	private static final Logger LOG = LoggerFactory.getLogger( CHELProperties.class );

	private static final String PROPERTIES_RESOURCE = "chel.properties";

	private static CHELProperties instance = null;
	
	private Configuration config;

	public CHELProperties ( String filename ) throws ConfigurationException {
		LOG.info( "Loading configuration from \"" + filename + "\"" );
		
		Parameters params = new Parameters();
		FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
			new FileBasedConfigurationBuilder<FileBasedConfiguration>(
				PropertiesConfiguration.class
			).configure(
				params.properties()
					.setListDelimiterHandler(new DefaultListDelimiterHandler(','))	
					.setFileName(filename)
			);
		
		this.config = builder.getConfiguration();		
	}

	public static void setInstance ( CHELProperties instance ) {
		CHELProperties.instance = instance;
	}

	public static CHELProperties getInstance () {

		try {
			if ( CHELProperties.instance == null ) {
				URL resource = CHELProperties.class.getClassLoader().getResource(PROPERTIES_RESOURCE);		
				File file = Paths.get(resource.toURI()).toFile();
				String filename = file.getAbsolutePath();
				System.out.println(filename);
				CHELProperties.instance = new CHELProperties( filename );
			}
		} catch ( URISyntaxException ex ) {
			ex.printStackTrace();
		} catch ( ConfigurationException ex ) {
			ex.printStackTrace();
		}
		
		return CHELProperties.instance;
	}

	public String getString ( String field_name ) {
		return this.config.getString( field_name );
	}

	public double getDouble ( String field_name ) {
		return this.config.getDouble( field_name );
	}

	public List<String> getList ( String field_name ) {
		return this.config.getList( String.class, field_name );
	}
}
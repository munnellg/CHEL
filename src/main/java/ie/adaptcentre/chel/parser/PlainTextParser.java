package ie.adaptcentre.chel.parser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;

import ie.adaptcentre.chel.model.LinkableContext;

public class PlainTextParser implements Parser {
	
	public PlainTextParser () { }

	@Override
	public LinkableContext parseString ( String string ) {
		LinkableContext context = new LinkableContext( "", string );
		return context;
	}

	@Override
	public LinkableContext parseStream ( InputStream stream ) {
		try {
			String content = IOUtils.toString( stream, StandardCharsets.UTF_8 );
			return this.parseString( content );
		} catch ( IOException ex ) {
			System.out.println( ex.getMessage() );
		}
		
		return null;
	}
}
package ie.adaptcentre.chel.parser;

import java.io.InputStream;

import ie.adaptcentre.chel.model.LinkableContext;

public interface Parser {	
	public LinkableContext parseString ( String string );
	public LinkableContext parseStream ( InputStream stream );
}
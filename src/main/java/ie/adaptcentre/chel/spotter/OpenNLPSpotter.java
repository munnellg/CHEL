package ie.adaptcentre.chel.spotter;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import ie.adaptcentre.chel.model.LinkablePhrase;
import ie.adaptcentre.chel.model.LinkableContext;
import ie.adaptcentre.chel.configuration.CHELProperties;

public class OpenNLPSpotter implements EntitySpotter {

    private ArrayList<TokenNameFinderModel> models;

	public OpenNLPSpotter () {		
		this.models = new ArrayList<TokenNameFinderModel>();

		// try to load project configuration and quit on failure
		CHELProperties p = CHELProperties.getInstance();
		if ( p == null ) { return; }

		// try to load path to models and quit on failure
		String models_dir = p.getString("spotters.opennlp.entities.models_dir");
		if ( models_dir == null ) { return; }

		// load all files in the specified directory
		File dir = new File( models_dir );
		for ( File f : dir.listFiles() ) {
			if ( f.isFile() ) {
				TokenNameFinderModel model = loadModel(f);
				if ( model != null ) { this.models.add(model); }
			}
		}
	}
	
	private TokenNameFinderModel loadModel ( File file ) {
		TokenNameFinderModel model = null;

		try {
			model = new TokenNameFinderModel( file );
		} catch ( IOException ex ) {
			System.out.println( ex.getMessage() );
		}

		return model;
	}

	private void spot ( LinkableContext context, TokenNameFinderModel model ) {
		String content = context.getContent();
        
        NameFinderME finder = new NameFinderME(model);
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        Span[] tokens = tokenizer.tokenizePos(content);
        String[] tokenStrings = Span.spansToStrings(tokens, content);
        Span[] nameSpans = finder.find(tokenStrings);
        
        for ( Span s : nameSpans ) {
            int startToken = s.getStart();
            int endToken   = s.getEnd() - 1;
            int startChar  = tokens[startToken].getStart();
            int endChar    = tokens[endToken].getEnd();
            
            LinkablePhrase lp = new LinkablePhrase( context, startChar, endChar );
            context.addPhrase(lp);
        }
	}

	@Override
	public void spotEntities ( LinkableContext context ) {
		for ( TokenNameFinderModel model : this.models ) {
			this.spot ( context, model );
		}
	}
}
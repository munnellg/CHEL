package ie.adaptcentre.spotter;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Triple;

import java.io.IOException;

import org.apache.log4j.Logger;

import java.util.Properties;

import ie.adaptcentre.models.LinkablePhrase;
import ie.adaptcentre.models.LinkableContext;

public class EntitySpotter {
	private static final String model = "models/english.all.3class.distsim.crf.ser.gz";
	
	private static CRFClassifier nerClassifier;
	private static Logger logger = Logger.getLogger(EntitySpotter.class);
	
	static {
		try {
			nerClassifier = CRFClassifier.getClassifierNoExceptions(model);
		} catch ( Exception ex ) {
			logger.warn("Failed to initialize Stanford NLP spotter");
		}     
	} 

	public static void spotContext( LinkableContext context ) {
        List<Triple<String,Integer,Integer>> entities = nerClassifier.classifyToCharacterOffsets(context.getContent());
		for ( Triple t : entities ) {
			int start = ((Integer) t.second()).intValue();
			int end = ((Integer)t.third()).intValue();
			String anchor = context.getContent().substring(start, end);
			context.addPhrase(new LinkablePhrase(start, end, anchor));
		}
	}

	public static void spotAllContexts( ArrayList<LinkableContext> contexts ) {
		for ( LinkableContext context : contexts ) {
			EntitySpotter.spotContext(context);
		}
	}
}
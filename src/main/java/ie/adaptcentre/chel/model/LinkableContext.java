package ie.adaptcentre.chel.model;

import java.util.ArrayList;
import java.util.Collections;

public class LinkableContext {

	String contextId;
	String content;
	ArrayList<LinkablePhrase> phrases;

	public LinkableContext () {
		this( "", "" );
	}

	public LinkableContext ( String contextId, String content ) {
		this( contextId, content, new ArrayList<LinkablePhrase>() );
	}

	public LinkableContext ( String contextId, String content,
			ArrayList<LinkablePhrase> phrases ) {
		this.contextId = contextId;
		this.content = content;
		this.phrases = phrases;
	}

	public String getContextId () {
		return this.contextId;
	}

	public void setContextId ( String contextId ) {
		this.contextId = contextId;
	}

	public String getContent () {
		return this.content;
	}

	public void setContent ( String content ) {
		this.content = content;
	}

	public void addPhrase ( LinkablePhrase phrase ) {
		this.phrases.add(phrase);
	}

	public ArrayList<LinkablePhrase> getPhrases () {
		return this.phrases;
	}

	public void setPhrases ( ArrayList<LinkablePhrase> phrases ) {
		this.phrases = phrases;
	}

	public void removeOverlappingPhrases () { 
		// Sort the list of phrases as this makes it easier to find overlaps
		Collections.sort(this.phrases);

		for ( int i = this.phrases.size() - 1; i > 0; i--) {
			LinkablePhrase p1 = this.phrases.get(i);
			LinkablePhrase p2 = this.phrases.get(i-1);

			if ( p1.getBeginIndex() < p2.getEndIndex() ) {
				// compute phrase length
				int l1 = p1.getEndIndex() - p1.getBeginIndex();
				int l2 = p2.getEndIndex() - p2.getBeginIndex();

				// keep the longer of the two phrases
				if ( l1 > l2 ) { this.phrases.set( i-1, p1 ); }

				// delete the shorter phrase
				this.phrases.remove(i);
			}
		}
	}
}
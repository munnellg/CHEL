package ie.adaptcentre.linker;

import java.util.ArrayList;

import ie.adaptcentre.candidates.LuceneCandidateRetriever;
import ie.adaptcentre.candidates.ICandidateRetriever;
import ie.adaptcentre.candidates.Candidate;

import ie.adaptcentre.models.LinkablePhrase;
import ie.adaptcentre.models.LinkableContext;

public class EntityLinker {
	private ICandidateRetriever retriever;
	private ICandidateRanker ranker;
	double threshold;

	public EntityLinker() {
		this( new LuceneCandidateRetriever(), new SurfaceFormCandidateRanker(), 0.95 );
	}

	public EntityLinker( ICandidateRetriever retriever, ICandidateRanker ranker, double threshold ) {
		this.retriever = retriever;
		this.ranker = ranker;
		this.threshold = threshold;
	}

	public void linkContext( LinkableContext context ) {
		ArrayList<LinkablePhrase> phrases = context.getPhrases();
		ArrayList<ArrayList<Candidate>> candidates =
			new ArrayList<ArrayList<Candidate>>();

		for ( LinkablePhrase p : phrases ) {
			ArrayList<Candidate> phraseCandidates = this.retriever.getCandidates(p.getAnchorOf());
			candidates.add(phraseCandidates);
		}

		this.ranker.rankCandidates( phrases, candidates );
		
		for ( int i = 0; i < phrases.size(); i++ ) {
			LinkablePhrase p = phrases.get(i);
			ArrayList<Candidate> phraseCandidates = candidates.get(i);
			if ( phraseCandidates.size() < 1 ) { p.setReferent("NIL"); continue; }
			Candidate c = phraseCandidates.get(0);
			if ( c.getWeight() < this.threshold ) { p.setReferent("NIL"); }
			else { p.setReferent(c.getUri()); p.setConfidence(c.getWeight()); }
		}
	}

	public void linkAllContexts( ArrayList<LinkableContext> contexts ) {
		for ( LinkableContext context : contexts ) {
			this.linkContext(context);
		}
	}
}
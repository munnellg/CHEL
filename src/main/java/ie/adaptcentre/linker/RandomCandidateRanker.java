package ie.adaptcentre.linker;

import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

import ie.adaptcentre.models.LinkablePhrase;
import ie.adaptcentre.candidates.Candidate;

public class RandomCandidateRanker implements ICandidateRanker {
	
	public int rankCandidates ( ArrayList<LinkablePhrase> mentions, ArrayList<ArrayList<Candidate>> candidates ) {
		Random random = new Random();

		for ( ArrayList<Candidate> phraseCandidates : candidates ) {
			for ( Candidate c : phraseCandidates ) {
				c.updateWeight(random.nextDouble());
			}
		}

		return 0;
	}
}
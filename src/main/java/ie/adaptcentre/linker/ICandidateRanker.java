package ie.adaptcentre.linker;

import java.util.ArrayList;
import java.util.Random;

import ie.adaptcentre.models.LinkablePhrase;
import ie.adaptcentre.candidates.Candidate;

public interface ICandidateRanker {
	// return type is just in case we ever want to return error codes for an
	// expecially complex ranking process
	public int rankCandidates ( ArrayList<LinkablePhrase> mentions, ArrayList<ArrayList<Candidate>> candidates );
}
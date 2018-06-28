package ie.adaptcentre.candidates;

import java.util.ArrayList;

public interface ICandidateRetriever {
	public ArrayList<Candidate> getCandidates( String surface_form );
}
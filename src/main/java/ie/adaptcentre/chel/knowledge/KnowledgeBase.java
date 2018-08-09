package ie.adaptcentre.chel.knowledge;

import java.util.ArrayList;
import ie.adaptcentre.chel.model.Candidate;

public interface KnowledgeBase {
    public abstract ArrayList<Candidate> fetchCandidates ( String surfaceForm );
    public abstract ArrayList<String> fetchOutgoingLinks ( String surfaceForm );
    public abstract String resolveToNamespace( String uri, String namespace );
}
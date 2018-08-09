package ie.adaptcentre.chel.resolver;

import java.lang.Comparable;

import java.util.UUID;
import java.util.HashSet;

public class Bucket {
	private String uid;
	private HashSet<String> terms;

	public Bucket( String term ) {
		UUID uuid = UUID.randomUUID();
        this.uid = uuid.toString();

		terms = new HashSet<String>();
		terms.add(term);
	}

	public String getIdentifier() {
		if ( this.terms.size() == 1 ) {
			for ( String s : this.terms ) {
				return s;
			}	
		} 

		return this.uid;
	}

	public HashSet<String> getTerms() {
		return this.terms;
	}

	public boolean contains( String term ) {
		return this.terms.contains(term);
	}

	public void merge( Bucket bucket ) {
		for ( String term : bucket.terms ) {
			this.terms.add(term);
		}
	}

	public int size( ) {
		return this.terms.size();
	}

	public boolean equals ( Bucket b ) {        
        return  this.getIdentifier().equals(b.getIdentifier());
    }
}
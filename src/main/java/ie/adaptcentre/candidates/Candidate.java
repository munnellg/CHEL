package ie.adaptcentre.candidates;

import java.util.ArrayList;

import java.lang.Comparable;

public class Candidate implements Comparable<Candidate> {	
	private String uri;	
	private double weight;
	private ArrayList<String> surfaceForms;

	public Candidate ( ) { 
		this( "", new ArrayList<String>() );
	}

	public Candidate( String uri ) {
		this( uri, new ArrayList<String>() );
	}

	public Candidate( String uri, ArrayList<String> surfaceForms ) { 
		this.uri = uri;
		this.surfaceForms = surfaceForms;
		this.weight = 1.0; 
	}

	public String getUri() { return this.uri; }

	public void setUri( String uri ) { this.uri = uri; }

	public double getWeight() { return this.weight; }

	public void setWeight( double weight ) { this.weight = weight; }

	public void updateWeight( double weight ) { this.weight *= weight; }

	public void addSurfaceForm( String surfaceForm ) { 
		if (!this.surfaceForms.contains(surfaceForm)) {
			this.surfaceForms.add(surfaceForm);
		}
	}

	public ArrayList<String> getSurfaceForms() { return this.surfaceForms; }

	public void setSurfaceForms( ArrayList<String> surfaceForms ) { 
		this.surfaceForms = surfaceForms;
	}

	public int compareTo( Candidate candidate ) {
		if ( this.weight > candidate.weight ) { return  1; }
		if ( this.weight < candidate.weight ) { return -1; }
		return  0;
    }
}
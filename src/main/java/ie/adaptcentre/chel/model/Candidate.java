package ie.adaptcentre.chel.model;

import java.lang.Comparable;

public class Candidate implements Comparable<Candidate> {
    private String uri;
    private double weight;
    private String surfaceForm;
    private double surfaceFormSim;

    public Candidate () {
        this("");        
    }

    public Candidate ( String uri ) {
        this(uri, uri, 1.0);
    }

    public Candidate ( String uri, String surfaceForm, double surfaceFormSim ) {
        this.uri = uri;
        this.weight = 1.0;
        this.surfaceForm = surfaceForm;
        this.surfaceFormSim = surfaceFormSim;
    }

    public String getURI() {
        return this.uri;
    }

    public void setURI( String uri ) {
        this.uri = uri;
    }

    public void setWeight( double weight ) {
        this.weight = weight;
    }

    public double getWeight( ) {
        return this.weight;
    }

    public String getSurfaceForm() {
        return this.surfaceForm;
    }

    public void setSurfaceForm ( String surfaceForm ) {
        this.surfaceForm = surfaceForm;
    }

    public double getSurfaceFormSim() {
        return this.surfaceFormSim;
    }

    public void setSurfaceFormSim( double surfaceFormSim ) {
        this.surfaceFormSim = surfaceFormSim;
    }

	public int compareTo( Candidate candidate ) {
		if ( this.weight > candidate.weight ) { return  1; }
		if ( this.weight < candidate.weight ) { return -1; }
		return  0;
    }
}
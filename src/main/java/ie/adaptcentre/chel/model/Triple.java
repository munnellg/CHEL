package ie.adaptcentre.chel.model;

import java.util.ArrayList;

public class Triple {
	public enum TripleType { RESOURCE, LITERAL };

	private TripleType type;

	private String subjectId;
	private String subjectURI;
	private String predicate;
	private String objectId;
	private String objectURI;
	private String objectLiteral;

    private ArrayList<String> subjectEquivalents;
    private ArrayList<String> objectEquivalents;

	public Triple () {
		this(
			"", "", new ArrayList<String>(), "", "", "",
			new ArrayList<String>(), "", TripleType.RESOURCE
		);
	}

	public Triple ( String subjectId, String subjectURI, 
			ArrayList<String> subjectEquivalents, String predicate,
			String objectId, String objectURI, ArrayList<String> equivalent,
			String objectLiteral ) {

		this(
			subjectId, subjectURI, subjectEquivalents, predicate, objectId,
			objectURI, equivalent, objectLiteral, TripleType.RESOURCE
		);
	}

	public Triple ( 
			String subjectId, String subjectURI, 
			ArrayList<String> subjectEquivalents, String predicate,
			String objectId, String objectURI, ArrayList<String> equivalent,
			String objectLiteral, TripleType type ) {

		this.subjectId          = subjectId;
		this.subjectURI         = subjectURI;
	    this.subjectEquivalents = subjectEquivalents;
		this.predicate          = predicate;
		this.objectId           = objectId;
		this.objectURI          = objectURI;
	    this.objectEquivalents  = objectEquivalents;
		this.objectLiteral      = objectLiteral;
	    this.type               = type;
	}

	public String getSubjectId () {
		return this.subjectId;
	}

	public void setSubjectId ( String subjectId ) {
		this.subjectId = subjectId;
	}

	public String getSubjectURI () {
		return this.subjectURI;
	}

	public void setSubjectURI ( String subjectURI ) {
		this.subjectURI = subjectURI;
	}

	public ArrayList<String> getSubjectEquivalents () {
		return this.subjectEquivalents;
	}

	public void setSubjectEquivalents ( ArrayList<String> subjectEquivalents ) {
		this.subjectEquivalents = subjectEquivalents;
	}

	public String getPredicate () {
		return this.predicate;
	}

	public void setPredicate ( String predicate ) {
		this.predicate = predicate;
	}

	public String getObjectId () {
		return this.objectId;
	}

	public void setObjectId ( String objectId ) {
		this.objectId = objectId;
	}

	public String getObjectURI () {
		return this.objectURI;
	}

	public void setObjectURI ( String objectURI ) {
		this.objectURI = objectURI;
	}

	public ArrayList<String> getObjectEquivalents () {
		return this.objectEquivalents;
	}

	public void setObjectEquivalents ( ArrayList<String> objectEquivalents ) {
		this.objectEquivalents = objectEquivalents;
	}

	public String getObjectLiteral () {
		return this.objectLiteral;
	}

	public void getObjectLiteral ( String objectLiteral ) {
		this.objectLiteral = objectLiteral;
	}

	public TripleType getType () {
		return this.type;
	}

	public void setType ( TripleType type ) {
		this.type = type;
	}
}
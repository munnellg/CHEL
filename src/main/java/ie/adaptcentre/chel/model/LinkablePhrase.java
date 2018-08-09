package ie.adaptcentre.chel.model;

import java.lang.Comparable;

public class LinkablePhrase implements Comparable<LinkablePhrase> {

    private int beginIndex;
    private int endIndex;
    private String referent;
    private double confidence;
    private LinkableContext linkableContext;

    public LinkablePhrase () {
        this( null, 0, 0, "" );
    }

    public LinkablePhrase ( LinkableContext linkableContext, int beginIndex,
            int endIndex ) {
        this( linkableContext, beginIndex, endIndex, "" );
    }

    public LinkablePhrase ( LinkableContext linkableContext, int beginIndex,
            int endIndex, String referent ) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;       
        this.referent = referent;
        this.confidence = 0;
        this.linkableContext = linkableContext;
    }

    public String getAnchorOf () {
        String content = this.linkableContext.getContent();
        return content.substring( this.beginIndex, this.endIndex );
    }

    public LinkableContext getLinkableContext () {
        return this.linkableContext;
    }

    public void setLinkableContext ( LinkableContext linkableContext ) {
        this.linkableContext = linkableContext;
    }

    public void setBeginIndex ( int beginIndex ) { 
        this.beginIndex = beginIndex;
    }

    public int getBeginIndex () { 
        return this.beginIndex; 
    }

    public void setEndIndex ( int endIndex ) { 
        this.endIndex = endIndex; 
    }

    public int getEndIndex () { 
        return this.endIndex; 
    }

    public void setReferent ( String referent ) { 
        this.referent = referent; 
    }

    public String getReferent () { 
        return this.referent; 
    }

    public void setConfidence ( double confidence ) { 
        this.confidence = confidence;
    }

    public double getConfidence () { 
        return this.confidence; 
    }

    public int compareTo ( LinkablePhrase lp ) {
        if ( this.beginIndex > lp.beginIndex ) { return  1; }
        if ( this.beginIndex < lp.beginIndex ) { return -1; }
        return  0;
    }
}
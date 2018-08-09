package ie.adaptcentre.chel.linking;

import ie.adaptcentre.chel.model.LinkableContext;

public interface EntityLinker {
    public void linkContext ( LinkableContext context );
    public boolean logStatus ( String fname );
}
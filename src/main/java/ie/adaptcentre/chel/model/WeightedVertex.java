package ie.adaptcentre.chel.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

public class WeightedVertex implements Comparable<WeightedVertex> {
    public static enum WeightedVertexType { CANDIDATE, INCIDENTAL };    

    public String identifier;
    public double hubScore;
    public double authScore;
    public double unnormalizedHubScore;
    public double unnormalizedAuthScore;
    public double weight;
    public WeightedVertexType vertexType;

    public WeightedVertex( String identifier ) {
        this(identifier, WeightedVertexType.CANDIDATE, 1.0, 1.0, 1.0);
    }

    public WeightedVertex( String identifier, double weight ) {
        this(identifier, WeightedVertexType.CANDIDATE, weight, 1.0, 1.0);
    }

    public WeightedVertex( String identifier, WeightedVertexType vertexType ) {
        this(identifier, vertexType, 1.0, 1.0, 1.0);
    }

    public WeightedVertex( String identifier, WeightedVertexType vertexType,
            double weight, double hubScore, double authScore ) {
        this.identifier = identifier;
        this.hubScore = hubScore;
        this.authScore = authScore;
        this.vertexType = vertexType;
        this.weight = weight;
        this.unnormalizedHubScore = hubScore;
        this.unnormalizedAuthScore = authScore;
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if (this == o) { return true; }
        if (!(o instanceof WeightedVertex)) { return false; }
        WeightedVertex v = (WeightedVertex) o;
        return this.identifier.equals(v.identifier);
    }

    @Override
    public int compareTo( WeightedVertex v ) {
        return this.identifier.compareTo(v.identifier);
    }
}
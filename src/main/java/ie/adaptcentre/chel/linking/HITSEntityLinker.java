package ie.adaptcentre.chel.linking;

import java.io.File;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Collections;

import ie.adaptcentre.chel.configuration.CHELProperties;
import ie.adaptcentre.chel.knowledge.KnowledgeBase;
import ie.adaptcentre.chel.model.Candidate;
import ie.adaptcentre.chel.model.LinkableContext;
import ie.adaptcentre.chel.model.LinkablePhrase;
import ie.adaptcentre.chel.model.WeightedVertex;
import ie.adaptcentre.chel.model.Pair;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

public class HITSEntityLinker implements EntityLinker {
    
    private static int BFS_MAX_DEPTH = 2;
    private static int MAX_HITS_ITERATIONS = 20;
    
    private double threshold;
    private KnowledgeBase knowledgeBase;

    Graph<WeightedVertex, DefaultWeightedEdge> graph;

    public HITSEntityLinker ( KnowledgeBase knowledgeBase ) {
        this.knowledgeBase = knowledgeBase;
        this.threshold = 0;
    }

    public void setKnowledgeBase ( KnowledgeBase knowledgeBase ) {
        this.knowledgeBase = knowledgeBase;
    }

    public KnowledgeBase getKnowledgeBase ( ) {
        return this.knowledgeBase;
    }

    public void linkContext ( LinkableContext context ) {
        this.graph = new DefaultDirectedWeightedGraph<WeightedVertex, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        ArrayList<LinkablePhrase> phrases = context.getPhrases();
        HashMap<String, ArrayList<Candidate>> surfaceFormCandidates = new HashMap<String, ArrayList<Candidate>>();
        CHELProperties properties = CHELProperties.getInstance();
        String resultNamespace = properties.getString("entity_linker.result_namespace");

        for ( LinkablePhrase lp : phrases ) {
            String surfaceForm = lp.getAnchorOf();
            if ( !surfaceFormCandidates.containsKey(surfaceForm) ) {
                ArrayList<Candidate> candidates = this.knowledgeBase.fetchCandidates( lp.getAnchorOf() );
                surfaceFormCandidates.put( surfaceForm, candidates );
            }
        }

        for ( Entry<String, ArrayList<Candidate>> entry : surfaceFormCandidates.entrySet() ) {
            String surfaceForm = entry.getKey();
            ArrayList<Candidate> cl = entry.getValue();
            for ( Candidate c : cl ) {
                WeightedVertex v = new WeightedVertex(c.getURI(), c.getSurfaceFormSim());
                graph.addVertex(v);
            }
        }

        growBFS( graph, BFS_MAX_DEPTH );
        doHITS( graph );
        
        for ( ArrayList<Candidate> cl : surfaceFormCandidates.values() ) {			
            updateCandidateWeights( graph, cl );
            Collections.sort(cl);
			Collections.reverse(cl);			
        }
       
		for ( LinkablePhrase p : phrases ) {
			ArrayList<Candidate> phraseCandidates = surfaceFormCandidates.get(p.getAnchorOf());
			if ( phraseCandidates.size() < 1 ) { 
				p.setReferent("NIL"); 
			} else {
				Candidate c = phraseCandidates.get( 0 );
				if ( c.getWeight() < this.threshold ) { 
					p.setReferent("NIL"); 
				} else { 
					System.out.println( "Candidate referent: " + c.getURI() );
					p.setReferent(c.getURI()); 
					p.setConfidence(c.getWeight()); 
				}	
			}
		}

		if ( resultNamespace != null ) {
			for ( LinkablePhrase p : phrases ) {
				if ( !p.getReferent().startsWith(resultNamespace) && !p.getReferent().equals("NIL") ) {
					p.setReferent(this.knowledgeBase.resolveToNamespace( p.getReferent(), resultNamespace) );
				}
			}
		}
    }

    private void growBFS ( Graph<WeightedVertex, DefaultWeightedEdge> graph, int maxDepth ) {
        LinkedList<Pair<Integer, WeightedVertex>> queue = new LinkedList<>();
		Set<WeightedVertex> vertices = graph.vertexSet();
		for ( WeightedVertex wv : vertices ) {
			queue.add(new Pair( 0, wv));
		}

		while ( queue.size() > 0 ) {
			Pair p = queue.remove();
			int depth = (int) p.getA();
			WeightedVertex v = (WeightedVertex) p.getB();
			ArrayList<String> outgoing = this.knowledgeBase.fetchOutgoingLinks(v.identifier);
			for ( String link : outgoing ) {			
				WeightedVertex target = new WeightedVertex (
					link, WeightedVertex.WeightedVertexType.INCIDENTAL
				);

				graph.addVertex(target);
				DefaultWeightedEdge e = graph.addEdge(v, target);
				
				// e will be null if edge is already in the graph
				if ( e != null ) { 
					graph.setEdgeWeight( e, 1.0 );
				}

				if (depth + 1 < maxDepth) { 
					queue.add( new Pair( depth + 1, target ) );
				}
			}
		}
    }

    private void doHITS ( Graph<WeightedVertex, DefaultWeightedEdge> graph ) {
        Set<WeightedVertex> vertices = graph.vertexSet();

		for ( int iter = 0; iter < MAX_HITS_ITERATIONS; iter++ ) {
			for ( WeightedVertex wv : vertices ) {
				double hubScore = 0.0;				
				Set<DefaultWeightedEdge> edges = graph.outgoingEdgesOf(wv);
				for ( DefaultWeightedEdge e : edges ) {					
					WeightedVertex target = graph.getEdgeTarget(e);
					double weight = target.weight;
					hubScore += weight * target.authScore;
				}
				wv.unnormalizedHubScore = hubScore;
			}

			for ( WeightedVertex wv : vertices ) {
				double authScore = 0.0;
				Set<DefaultWeightedEdge> edges = graph.incomingEdgesOf(wv);
				for ( DefaultWeightedEdge e : edges ) {
					WeightedVertex src = graph.getEdgeSource(e);
					double weight = src.weight;
					authScore += weight * src.hubScore;
				}
				wv.unnormalizedAuthScore = authScore;
			}

			// normalize the scores
			double hubSum = 0, authSum = 0;
			for ( WeightedVertex wv : vertices ) {
				hubSum  += wv.unnormalizedHubScore;
				authSum += wv.unnormalizedAuthScore;
			}

			for ( WeightedVertex wv : vertices ) {
				wv.hubScore  = ( hubSum == 0 )?  1.0 : wv.unnormalizedHubScore / hubSum;
				wv.authScore = ( authSum == 0 )? 1.0 : wv.unnormalizedAuthScore / authSum;
			}
		}
    }

    private void updateCandidateWeights ( Graph<WeightedVertex, DefaultWeightedEdge> graph, ArrayList<Candidate> candidates ) {

		Set<WeightedVertex> vset = graph.vertexSet();
		HashMap<String, WeightedVertex> lookup = new HashMap<>();
		for ( WeightedVertex wv : vset ) {
			lookup.put( wv.identifier, wv );
		}

		for ( Candidate c : candidates ) {
			WeightedVertex v = lookup.get(c.getURI());
			if ( v == null ) { 
				System.out.printf("Should never get here\n"); 
				continue;
			}

			c.setWeight( (v.authScore + v.hubScore) / 2 );
		}
	}

	private String truncateLabel( String label, ArrayList<String> prefixes ) {
		for ( String s : prefixes ) {
			if ( label.startsWith(s) ) {
				label = label.substring(s.length());
			}
		}
		return label;
	}

	@Override
	public boolean logStatus ( String fname ) {
		Set<WeightedVertex> vertices = graph.vertexSet();
		Set<DefaultWeightedEdge> edges = graph.edgeSet();

		CHELProperties properties = CHELProperties.getInstance();
		// ArrayList<String> vertexTypes = properties.getList("knowledge_base.vertex_type");
		try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
 
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
 
            org.w3c.dom.Document document = documentBuilder.newDocument();
 
            // root element
            Element gexfElement = document.createElement("gexf");
            gexfElement.setAttribute("xmlns", "http://www.gexf.net/1.2draft");
            gexfElement.setAttribute("xmlns:viz", "http://www.gexf.net/1.1draft/viz");
            gexfElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            gexfElement.setAttribute("xsi:schemaLocation", "http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd");
            gexfElement.setAttribute("version", "1.2");
            document.appendChild(gexfElement);

            Element graphElement = document.createElement("graph");
            graphElement.setAttribute("mode", "static");
            graphElement.setAttribute("defaultedgetype", "directed");
            gexfElement.appendChild(graphElement);

            Element nodesElement = document.createElement("nodes");            
            gexfElement.appendChild(nodesElement);

            Element edgesElement = document.createElement("edges");
            gexfElement.appendChild(edgesElement);

			for ( WeightedVertex wv : vertices ) {
				Element nodeElement = document.createElement("node");
				// String label = truncateLabel( wv.identifier, vertexTypes );
				String label = wv.identifier;
				nodeElement.setAttribute("id", wv.identifier);
				nodeElement.setAttribute("label", label);
				nodeElement.setAttribute("hub", String.format("%f", wv.hubScore) );
				nodeElement.setAttribute("authority", String.format("%f", wv.authScore));
				
				Element colorElement = document.createElement("viz:color");
				colorElement.setAttribute("r", String.format("%d", (wv.vertexType == WeightedVertex.WeightedVertexType.CANDIDATE)? 255 : 0 ));
				colorElement.setAttribute("g", String.format("%d", 0) );
				colorElement.setAttribute("b", String.format("%d", (wv.vertexType != WeightedVertex.WeightedVertexType.CANDIDATE)? 255 : 0 ));
				colorElement.setAttribute("a", String.format("%f", 1.0f) );
				nodeElement.appendChild(colorElement);

				Element sizeElement = document.createElement("viz:size");
				sizeElement.setAttribute("value", String.format("%d", 50));
				nodeElement.appendChild(sizeElement);

				Element shapeElement = document.createElement("viz:shape");
				shapeElement.setAttribute("value", "disc");
				nodeElement.appendChild(shapeElement);

				nodesElement.appendChild(nodeElement);
			}

			for ( DefaultWeightedEdge we : edges ) {
				WeightedVertex src = (WeightedVertex) graph.getEdgeSource(we);
				WeightedVertex dst = (WeightedVertex) graph.getEdgeTarget(we);
				
				Element edgeElement = document.createElement("edge");
				edgeElement.setAttribute(
					"id", String.format("%s;%s", src.identifier, dst.identifier)
				);
				edgeElement.setAttribute("source", src.identifier);
				edgeElement.setAttribute("target", dst.identifier);
				edgesElement.appendChild(edgeElement);
			}

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(fname));

            transformer.transform(domSource, streamResult);
 
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            return false;
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
            return false;
        }
        return true;
	}	
}
package ie.adaptcentre.linker;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import com.wcohen.ss.JaroWinkler;

import ie.adaptcentre.models.LinkablePhrase;
import ie.adaptcentre.candidates.Candidate;

public class SurfaceFormCandidateRanker implements ICandidateRanker {

	private static JaroWinkler distance = new JaroWinkler();

	private String[] tokenize(String s) {
		return s.split(" ");
	}

	private double generalisedMean ( double[] scores, double m ) {
		double sum = 0;	
		for ( double s : scores ) { sum += Math.pow(s, m); }
		return Math.pow( sum/scores.length, 1/m );
	}
	
	private void bipartiteMap( double[] edges, int w, int h, int y,
			int[] matches ) {
		
		int best = -1;		
		for ( int x = 0; x < w; x++ ) {
			if ( edges[ x + y * w ] > 0 ) {
				if ( best < 0 || edges[ x + y * w ] > edges[ best + y * w ] ) {
					if ( matches[x] < 0 || 
							edges[ x + matches[x] * w ] < edges[ x + y * w ] ) {
						best = x;
					}					
				}
			}
		}

		if ( best >= 0 ) {
			if ( matches[best] >= 0 ) {
				int tmpy = matches[best];
				matches[best] = y;
				bipartiteMap( edges, w, h, tmpy, matches );
			} else {
				matches[best] = y;
			}
		}
	}

	private int[] bestMapping( double[] edges, int w, int h ) {
		int[] matches = new int[w];
		Arrays.fill(matches, -1);
				
		for ( int y = 0; y < h; y++ ) {
			bipartiteMap( edges, w, h, y, matches );
		}

		return matches;
	}

	private double similarity( String[] t1, String[] t2 ) {		
		// make s1 the longer of the two strings
		if ( t1.length < t2.length ) { 
			String[] tmp = t2;
			t2 = t1;
			t1 = tmp; 
		}

		// compute similarity between all combinations of tokens in two token 
		double[] weights = new double[ t1.length * t2.length ];
		for ( int i = 0; i < t1.length; i++ ) {
			for ( int j = 0; j < t2.length; j++ ) {
				weights[ i + j * t1.length ] = 
					distance.score(t1[i], t2[j]);
			}
		}

		int[] mapping = bestMapping( weights, t1.length, t2.length );

		double[] optimal = new double[t1.length];
		for ( int i = 0; i < t1.length; i++ ) {
			optimal[i] = (mapping[i] >= 0) ? 
				weights[ i + mapping[i] * t1.length ] : 0;
		}

		return generalisedMean( optimal, 5 );
	}

	public int rankCandidates ( ArrayList<LinkablePhrase> mentions,
			ArrayList<ArrayList<Candidate>> candidates ) {
		
		for ( int i = 0; i < mentions.size(); i++ ) {
			String[] mentionTokens = this.tokenize( 
				mentions.get(i).getAnchorOf().toLowerCase()
			);

			for ( Candidate c : candidates.get(i) ) {
				double weight = 0;
				for ( String sf : c.getSurfaceForms() ) {
					String[] sfTokens = this.tokenize(sf.toLowerCase());
					double sim = this.similarity(mentionTokens, sfTokens);
					weight = Math.max( weight, sim );
				}

				c.updateWeight(weight);
			}

			Collections.sort(candidates.get(i));
			Collections.reverse(candidates.get(i));
		}

		return 0;
	}
}
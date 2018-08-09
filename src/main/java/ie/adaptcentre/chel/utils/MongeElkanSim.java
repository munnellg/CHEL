package ie.adaptcentre.chel.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;

import com.wcohen.ss.JaroWinkler;

public class MongeElkanSim implements StringSim {
	
	double exponent;
	JaroWinkler distance;
	HashSet<String> stopwords;

	public MongeElkanSim () {
		this( 5.0 );
	}

	public MongeElkanSim ( double exponent ) {
		this.exponent = exponent;
		distance = new JaroWinkler();
		String stop[] = {
			"a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
			"if", "in", "into", "is", "it", "no", "not", "of", "on", "or",
			"such", "that", "the", "their", "then", "there", "these", "they",
			"this", "to", "was", "will", "with"
		};
	
		this.stopwords = new HashSet(Arrays.asList(stop));
	}

	@Override
	public double similarity ( String sf1, String sf2 ) {
		String[] sf1Tokens = removeStopwords(this.tokenize(sf1.toLowerCase()));
		String[] sf2Tokens = removeStopwords(this.tokenize(sf2.toLowerCase()));
		return this.similarity( sf1Tokens, sf2Tokens );
	}

	private String[] removeStopwords( String[] t ) {
		ArrayList<String> terms = new ArrayList();
		for ( String s : t ) {
			if ( !this.stopwords.contains(s) ) {
				terms.add(s);
			}
		}

		return terms.toArray(new String[terms.size()]);
	}

	private double similarity ( String[] t1, String[] t2 ) {
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

		return generalisedMean( optimal, this.exponent );
	}

	private String[] tokenize ( String s ) {
		// dumb, but it'll do the job for now
		return s.split(" ");
	}

	private double generalisedMean ( double[] scores, double m ) {
		double sum = 0;	
		for ( double s : scores ) { sum += Math.pow(s, m); }
		return Math.pow( sum/scores.length, 1/m );
	}
	
	private void bipartiteMap ( double[] edges, int w, int h, int y,  int[] matches ) {
		
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

	private int[] bestMapping ( double[] edges, int w, int h ) {
		int[] matches = new int[w];
		Arrays.fill(matches, -1);
				
		for ( int y = 0; y < h; y++ ) {
			bipartiteMap( edges, w, h, y, matches );
		}

		return matches;
	}
}
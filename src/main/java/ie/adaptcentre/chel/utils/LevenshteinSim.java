package ie.adaptcentre.chel.utils;

import com.wcohen.ss.Levenstein;

public class LevenshteinSim implements StringSim {
	
	Levenstein levenshtein;

	public LevenshteinSim () {
		this.levenshtein = new Levenstein();
	}

	public double similarity( String s1, String s2 ) {
		double distance = Math.abs( this.levenshtein.score( s1, s2 ) );
		double maxLength = Math.max( 1.0, Math.max(s1.length(), s2.length()) );
		return 1.0 -  distance / maxLength; 
	}
}
package ie.adaptcentre.utils;

import java.util.Arrays;

import java.lang.StringBuilder;

public class BitVector {
	private static String vectarr2str( int[] vector ) {
		StringBuilder strvect = new StringBuilder();
	    for ( int i : vector ) { strvect.append(i); }
	    return new String(strvect);
	}

	public static String vectorize(String s) {		
		int[] vector = new int[26];
		char[] letters = s.toLowerCase().toCharArray();
		for ( char c : letters ) { 
			int ord = (int) ( c - 'a' );
			if ( ord >= 0 && ord < 26 ) {
				vector[ord] = 1;
			}
		}
		
		return vectarr2str(vector);
	}
}
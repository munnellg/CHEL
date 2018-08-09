package ie.adaptcentre.chel.resolver;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import java.io.Reader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import com.opencsv.CSVReader;

public class Resolver {
	HashMap<String, Bucket> buckets;

	public Resolver() {
		this.buckets = new HashMap<String, Bucket>();
    }

    public void loadEquivalenceFile( String filename ) throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(filename));
        CSVReader csvReader = new CSVReader(reader);
        String[] record;
        while ((record = csvReader.readNext()) != null) {
            this.addEquivalence( record[0], record[1] );
        }
        reader.close();
    }

    public void addEquivalence( String t1, String t2 ) {
    	Bucket b1 = this.buckets.get(t1);
    	if ( b1 == null ) {
    		b1 = new Bucket(t1);
    		this.buckets.put( t1, b1 );
    	}

    	Bucket b2 = this.buckets.get(t2);
    	if ( b2 == null ) {
    		b2 = new Bucket(t2);
    		this.buckets.put( t2, b2 );
    	}

    	if ( !b1.equals(b2) ) {
    		if ( b1.size() > b2.size() ) {
    			b1.merge(b2);
    			buckets.put( t2, b1 );
    		} else {
    			b2.merge(b1);
    			buckets.put( t1, b2 );
    		}
    	}
    }

    public String getIdentifier( String term ) {
    	Bucket b = this.buckets.get(term);
    	if ( b == null ) {
    		return term;
    	}

    	return b.getIdentifier();
    }

    public HashSet<String> getEquivalents( String term ) {
    	Bucket b = this.buckets.get(term);
    	if ( b == null ) {
    		HashSet<String> tmp = new HashSet<String>();
    		tmp.add(term);
    		return tmp;
    	}

    	return b.getTerms();
    }
}
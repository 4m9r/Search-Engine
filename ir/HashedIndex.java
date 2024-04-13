/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    

    /**
     *  Inserts this token in the hashtable.
     */
    public void insert(String token, int docID, int offset ) {
        
        if(!Index.tremFrequency.containsKey(docID)){
            HashMap<String,Double> temp = new HashMap<String,Double>();
            temp.put(token, 1.0);
            Index.tremFrequency.put(docID, temp);
        }
        else{
            if(!Index.tremFrequency.get(docID).containsKey(token))
                Index.tremFrequency.get(docID).put(token, 1.0);
            else{
                double freq = Index.tremFrequency.get(docID).get(token);
                Index.tremFrequency.get(docID).put(token, freq + 1);
            }
                
        }

        if(getPostings(token) != null){
            getPostings(token).add(docID, offset);
            
        }
        else{
            PostingsList temp = new PostingsList();
            temp.add(docID, offset);
            index.put(token, temp);
        }
            
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        if (index.get(token) == null)
            return null;
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}

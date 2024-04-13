/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;
import java.util.LinkedList;


public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public LinkedList<Integer> positions = new LinkedList<Integer>();

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }


    public void add(int docID){
        this.docID = docID;
    }
    public void addPos(int offset){
        positions.add(offset);
    }
    public void setScore(double score){
        this.score = score;
    }
    public int size(){
        return positions.size();
    }
}


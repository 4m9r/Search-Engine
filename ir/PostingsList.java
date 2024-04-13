/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;
public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    private int oldDoc = -1;
    private int counter = -1;

    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        if(i >= list.size())
            return null;
    return list.get( i );
    }

    /** For creating the posting list structure*/
    public void add(int docID, int offset){
        if(docID == oldDoc)
            get(counter).addPos(offset);
        else{
            PostingsEntry temp = new PostingsEntry();
            temp.add(docID);
            temp.addPos(offset);
            list.add(temp);
            oldDoc = docID;
            counter++;
        }
    }
        
        /** For small posting list when we want to do intersect
        search and we dont need the offset*/
        public void add(int docID){
        boolean docExist = false;

        for (int i = 0; i < list.size(); i++){
            if(get(i).docID == docID){
                docExist = true;
            }
                
        }
        if(!docExist){
            PostingsEntry temp = new PostingsEntry();
            temp.add(docID);
            list.add(temp);
        }
    }

    public void add(PostingsEntry p){
        list.add(p);
    }

    public boolean contains(PostingsEntry e){
        return list.contains( e);
    }
    public int indexOf(PostingsEntry e){
        return list.indexOf( e);
    }


    // public void calculateScore(int docId){
    //      for (int i = 0; i < list.size(); i++){
    //         if(get(i).docID == docID){
    //             docExist = true;
    //         }
    // }

    public  String toString(){
        StringBuilder s = new StringBuilder();
        LinkedList<Integer> p;
        for(int i = 0; i < list.size(); i++){
            s.append(list.get(i).docID);
            s.append(" ");
            p = list.get(i).positions;
            for(int item : p ){
                s.append(item);
                s.append(" ");
            }
            s.append("\n");
            
        }
        return s.toString();
    }

    public void sortScore(){
        Collections.sort(list);
    }




        //     public void add(int docID, int offset){
    //     boolean docExist = false;

    //     for (int i = 0; i < list.size(); i++){
    //         if(get(i).docID == docID){
    //             get(i).addPos(offset);
    //             docExist = true;
    //         }              
    //     }
    //     if(!docExist){
    //         PostingsEntry temp = new PostingsEntry();
    //         temp.add(docID);
    //         temp.addPos(offset);
    //         list.add(temp);
    //     }
    // }
}


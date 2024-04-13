/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;
import java.lang.Math;
import java.util.*;
import java.io.*;


/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    // private PostingsList pageRankPosting = new PostingsList();
    private HashMap<String,Double> pageRankHash = new HashMap<String,Double>();
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
        initilizeRankPage();
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        PostingsList result = new PostingsList();
        if(query.queryterm.size() == 0)
            return null;


        // Kgram
        if(queryType != QueryType.RANKED_QUERY){
                List<KGramPostingsEntry> kGramPostings = null;
        for(int i = 0; i < query.size(); i++){
            String kgram = query.queryterm.get(i).term;
            if (kGramPostings == null) {
                // if(kgIndex.getPostings(kgram) == null)
                // break;
                kGramPostings = kgIndex.getPostings(kgram);
                } 
                else {
                kGramPostings = kgIndex.intersect(kGramPostings, kgIndex.getPostings(kgram));
                }
        }
        if (kGramPostings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = kGramPostings.size();
            System.err.println("Found " + resNum + " posting(s)");
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(kGramPostings.get(i).tokenID));
            }
        }
        }
        
        // end Kgram


        if(query.queryterm.size() == 1 && queryType != QueryType.RANKED_QUERY)
            result = index.getPostings(query.queryterm.get(0).term);
        else{   
            if(queryType == QueryType.PHRASE_QUERY){
                result = positionalIntersect(index.getPostings(query.queryterm.get(0).term),
                index.getPostings(query.queryterm.get(1).term ), 1);

                for(int i = 2; i < query.size(); i++){
                    result = positionalIntersect(result, index.getPostings(query.queryterm.get(i).term), 1);
                }
            }
            else if (queryType == QueryType.INTERSECTION_QUERY){
                result =  intersect(index.getPostings(query.queryterm.get(0).term ),
                index.getPostings(query.queryterm.get(1).term ));

                for(int i = 2; i < query.size(); i++){
                    result = intersect(result, index.getPostings(query.queryterm.get(i).term));
                }


            }
            else if(queryType == QueryType.RANKED_QUERY){
                if(rankingType == RankingType.TF_IDF){
                    // long startTime = System.currentTimeMillis();
                    result = fastCosineScore(query);
                    // long endTime = System.currentTimeMillis();
                    // long executionTime = endTime - startTime;
                    // System.out.println("score: " + executionTime + " milliseconds");
                    if(result == null)
                        return null;
                    result.sortScore();
                }
                else if(rankingType == RankingType.PAGERANK){
                    result = combinedScore(unionSearch(query), "page", 1);
                    result.sortScore();
                }else{
                    result = fastCosineScore(query);
                    if(result == null)
                        return null;
                    
                    result = combinedScore(result, "combo", 5);
                    result.sortScore();
                }                
                
            }
        }
        return result;
    }



    public PostingsList intersect(PostingsList p1List, PostingsList p2List){
        PostingsList answer =  new PostingsList();

        int counterP1 = 0;
        int counterP2 = 0;

        PostingsEntry p1Entry = p1List.get(counterP1);
        PostingsEntry p2Entry = p2List.get(counterP2);

        while (p1Entry != null && p2Entry != null){
            if (p1Entry.docID == p2Entry.docID){
                answer.add(p1Entry.docID);
                p1Entry = p1List.get(++counterP1);
                p2Entry = p2List.get(++counterP2);
            }
            else if(p1Entry.docID < p2Entry.docID)
                    p1Entry = p1List.get(++counterP1);
                else
                    p2Entry = p2List.get(++counterP2);

               
        }
        return answer;
        
    }

    public PostingsList positionalIntersect(PostingsList p1List, PostingsList p2List, int k){
        PostingsList answer =  new PostingsList();

        int p1 = 0;
        int p2 = 0;

        PostingsEntry p1Entry = p1List.get(p1);
        PostingsEntry p2Entry = p2List.get(p2);
    
        while (p1Entry != null && p2Entry != null){
             if (p1Entry.docID == p2Entry.docID){
                LinkedList<Integer> l = new LinkedList<Integer>();

                LinkedList<Integer> posListP1 = p1Entry.positions;
                LinkedList<Integer> posListP2 = p2Entry.positions;

                int counterPP1 = 0;
                while ( counterPP1 != posListP1.size()){
                    int counterPP2 = 0;
                    while ( counterPP2 != posListP2.size()){
                        if(posListP2.get(counterPP2) - posListP1.get(counterPP1) == k)
                            l.add(posListP2.get(counterPP2));
                        else if (posListP2.get(counterPP2) > posListP1.get(counterPP1))
                            break;

                        counterPP2++;
                    }
                    // while(l.size() !=0 && Math.abs(l.get(0) - posListP1.get(counterPP1)) > k)
                    //     l.remove(0);
                    for(int i =0; i < l.size(); i++){
                        answer.add(p1Entry.docID, l.get(i));
                    }
                    l.clear();
                    counterPP1++;
                }

                p1Entry = p1List.get(++p1);
                p2Entry = p2List.get(++p2);

            }
            else if(p1Entry.docID < p2Entry.docID)
                    p1Entry = p1List.get(++p1);
                else
                    p2Entry = p2List.get(++p2);

        }
        
        return answer;

    }

    public PostingsList fastCosineScore(Query query){
        PostingsList answer =  new PostingsList(); 

        int querySize = query.size(); 
        // int answerSize = answer.size(); 
        int indexDocLengthsSize = index.docLengths.size(); 
        HashMap<Integer,Boolean> con = new HashMap<Integer,Boolean>();
        HashMap<Integer,Integer> checkout = new HashMap<Integer,Integer>();

        int counter = 0;
        for(int i = 0; i < querySize; i++){
            int answerSize = answer.size();
            PostingsList temp = index.getPostings(query.queryterm.get(i).term);
            int tempSize = temp.size();
            double queryWeight = query.queryterm.get(i).weight;
            // double check = 0;
            if(temp != null){    
                for(int j = 0; j < tempSize; j++){
                    PostingsEntry doc = temp.get(j);
                    double idf = Math.log((double) indexDocLengthsSize / (double) tempSize);
                    double score = (queryWeight * idf * doc.size())/index.docLengths.get(doc.docID);
                    doc.setScore(score);
                    
                    // if(index.docNames.get(doc.docID).substring(10, (index.docNames.get(doc.docID)).length()).equals("EmilyMaas.f")){
                    //     check+=score;
                    //     System.out.println(check);
                    // }

                    if(con.containsKey(doc.docID)){
                        int k = checkout.get(doc.docID);
                        answer.get(k).setScore(answer.get(k).score + score);
                        }
                        else{
                            answer.add(doc);
                            con.put(doc.docID, true);
                            checkout.put(doc.docID, counter);
                            counter++;
                        }

                    //   boolean c = true;
                    // for(int k = 0; k < answerSize; k++){
                    //     if(doc.docID == answer.get(k).docID){
                    //         c = false;
                    //         answer.get(k).setScore(answer.get(k).score + score);
                    //     }
                    // }
                    // if(c)
                    //     answer.add(doc);
                    
                    }
            }
        } 
        return answer;
    }

    private void initilizeRankPage(){
        try {
	    System.err.println( "Reading pagerank... " );
	    BufferedReader in = new BufferedReader( new FileReader( "pagerank/pageranksTitle.txt"));
	    String line;
	    while ((line = in.readLine()) != null){
            int index = line.indexOf( ":" );
		    String title = line.substring( 0, index );
		    double score = Double.parseDouble(line.substring(index + 1));
            // PostingsEntry entry = new PostingsEntry();
            
            // entry.setScore(score);
            // entry.add(Integer.parseInt(docID));
            pageRankHash.put(title, score);
            }
        }catch ( IOException e ) {
	        System.err.println( "Error reading file ");
	    }
    }


    private PostingsList unionSearch(Query query){
     PostingsList answer =  new PostingsList(); 
        for(int i = 0; i < query.size(); i++){
            int answerSize = answer.size();
            PostingsList temp = index.getPostings(query.queryterm.get(i).term);
            if(temp != null)
                for(int j = 0; j < temp.size(); j++){
                    PostingsEntry doc = temp.get(j);
                    boolean con = true;
                    for(int k = 0; k < answerSize; k++){
                        if(doc.docID == answer.get(k).docID){
                            con = false;
                            break;
                        }
                    }
                    if(con)
                        answer.add(doc);
                    }
        } 
        return answer;
    }

    private PostingsList combinedScore(PostingsList p, String con, double weight){
        PostingsList answer = new PostingsList(); 
         for(int i = 0; i < p.size(); i++){
            String docName = index.docNames.get(p.get(i).docID);
            int position = docName.lastIndexOf( '\\' ) + 1;
            String fileIdentifier = docName.substring(position);
            if(con == "combo"){
                p.get(i).setScore((weight * pageRankHash.get(fileIdentifier)) + (double)(p.get(i).score / Math.pow(weight, 2)));
                answer.add(p.get(i)); 
            }
            else if(con == "page"){
                p.get(i).setScore(pageRankHash.get(fileIdentifier));
                answer.add(p.get(i));
            } 

        }
        return answer;
    }



}
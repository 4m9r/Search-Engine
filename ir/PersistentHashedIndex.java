/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;
    // entry size long + int + int = 16
    public static final int ENTRYSIZE = 16;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    private List<Long> usedHashes = new ArrayList<>();

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        public long loc;
        public int byteSize;
        public int checker;

        public Entry(long loc, int byteSize, int checker){
            this.loc = loc;
            this.byteSize = byteSize;
            this.checker = checker;
        }
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        try {
            dictionaryFile.seek(ptr); 
            dictionaryFile.writeLong(entry.loc);
            dictionaryFile.writeInt(entry.byteSize);
            dictionaryFile.writeInt(entry.checker);

        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(long ptr) {   
         try {
            dictionaryFile.seek(ptr);
            long loc = dictionaryFile.readLong();
            dictionaryFile.seek(ptr + 8);
            int byteSize = dictionaryFile.readInt();
            dictionaryFile.seek(ptr + 4);
            int checker = dictionaryFile.readInt();

            return new Entry(loc, byteSize, checker);
        } catch ( IOException e ) {
            // e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
                
                
            for(Map.Entry<String, PostingsList> ent : index.entrySet()){
                long location = free;
                String token = ent.getKey();
                PostingsList posting = ent.getValue();
                long hashedToken = hashFunction(token);
                int counter = 0;
                while(usedHashes.contains(hashedToken)){

                    collisions++;
                    counter++;
                    hashedToken =  hashedToken + counter;
                    if(hashedToken < 0||hashedToken >= TABLESIZE )
                        hashedToken = free;
                }

                
                usedHashes.add(hashedToken);
                int byteWritten = writeData(posting.toString(), location);
                Entry temp = new Entry(location, byteWritten, token.hashCode());
                writeEntry(temp, hashedToken * ENTRYSIZE);
                location = location + byteWritten;

            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }



    public boolean checkCollision(long hash, int checker){
       try {
            dictionaryFile.seek((hash * ENTRYSIZE) + 12);
            int check = dictionaryFile.readInt();
            if(check == checker)
                return false;


       }  catch ( IOException e ) {
            // e.printStackTrace();
        }     
        return true;
    }

    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        long hashedToken = hashFunction(token);
        Entry ent = readEntry(hashedToken * ENTRYSIZE);
        int collisions = 0;
        String posting;
        if(ent == null)
            return null;

        // while(checkCollision(hashedToken, token.hashCode())){
        //     collisions++;
        //     hashedToken = hashedToken + collisions;
        //      if(hashedToken < 0||hashedToken >= TABLESIZE )
        //         hashedToken = free;
                
        // }
        ent = readEntry(hashedToken * ENTRYSIZE);
        long location = ent.loc;
        int size = ent.byteSize;
        posting = readData(location, size);
        

    return toPosting(posting);

    }
    public PostingsList toPosting(String posting){
        PostingsList pl = new PostingsList();
        String [] s = posting.split("\n");
        for(String item : s){
            String [] temp = item.split(" ");

            for(int i = 1; i < temp.length; i++){
                pl.add(Integer.parseInt(temp[0]), Integer.parseInt(temp[i]));
            }
        }
            

        return pl;
    }

    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        if(getPostingsInt(token) != null)
            getPostingsInt(token).add(docID, offset);
        else{
            PostingsList temp = new PostingsList();
            temp.add(docID, offset);
            index.put(token, temp);
        }
    }
    public PostingsList getPostingsInt( String token ) {
        if (index.get(token) == null)
            return null;
        return index.get(token);
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }

    // https://stackoverflow.com/questions/2624192/good-hash-function-for-strings
    //https://computinglife.wordpress.com/2008/11/20/why-do-hash-functions-use-prime-numbers/
    public long hashFunction(String term){
        long hash = 7;
        for(int i = 0; i < term.length(); i++)
            hash = (hash * 31 + term.charAt(i));
        return Math.floorMod(hash, TABLESIZE);
    }
}

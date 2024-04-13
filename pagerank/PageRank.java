import java.util.*;
import java.io.*;

public class PageRank  {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

	private HashMap<String,String> title = new HashMap<String,String>();
    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;



	public ArrayList<DocumentRaking> pageRanked = new ArrayList<DocumentRaking>();
	public class DocumentRaking implements Comparable<DocumentRaking>{
		public double probability;
        public String docName;

		 public DocumentRaking(String docName, double probability) {
            this.docName = docName;
            this.probability = probability;
        }

        public int compareTo(DocumentRaking other) {
            return Double.compare(other.probability, probability);
        }
	}


	// private ArrayList<ArrayList<Double>> G = new ArrayList<ArrayList<Double>>();

       
    /* --------------------------------------------- */


    public PageRank( String filename ) throws Exception {
	int noOfDocs = readDocs( filename );
	iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
			int index = line.indexOf( ";" );
			String title = line.substring( 0, index );
			Integer fromdoc = docNumber.get( title );
			//  Have we seen this document before?
			if ( fromdoc == null ) {	
				// This is a previously unseen doc, so add it to the table.
				fromdoc = fileIndex++;
				docNumber.put( title, fromdoc );
				docName[fromdoc] = title;
			}
			// Check all outlinks.
			StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
			while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
				String otherTitle = tok.nextToken();
				Integer otherDoc = docNumber.get( otherTitle );
				if ( otherDoc == null ) {
				// This is a previousy unseen doc, so add it to the table.
				otherDoc = fileIndex++;
				docNumber.put( otherTitle, otherDoc );
				docName[otherDoc] = otherTitle;
				}
				// Set the probability to 0 for now, to indicate that there is
				// a link from fromdoc to otherDoc.
				if ( link.get(fromdoc) == null ) {
				link.put(fromdoc, new HashMap<Integer,Boolean>());
				}
				if ( link.get(fromdoc).get(otherDoc) == null ) {
				link.get(fromdoc).put( otherDoc, true );
				out[fromdoc]++;
				}
			}
			}
			if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
			System.err.print( "stopped reading since documents table is full. " );
			}
			else {
			System.err.print( "done. " );
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + fileIndex + " number of documents" );
		return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) throws Exception {
		// gClaculator(numberOfDocs);
		double [] a = new double[numberOfDocs];
		Arrays.fill(a, 0);
		double [] aCurrent = new double[numberOfDocs];
		Arrays.fill(aCurrent, 0);
		aCurrent[0] = 1;
		double sumDif = 1;
		int counter = 0;
		while(sumDif > EPSILON && counter < maxIterations){
			sumDif = 0;	
			a = Arrays.copyOf(aCurrent, aCurrent.length);
			for(int i = 0; i < numberOfDocs; i++){
				double temp = 0;
				for(int j = 0; j < numberOfDocs; j++){
					HashMap <Integer,Boolean> link_temp =  link.get(j);
					if(link_temp == null){
						temp += a[j] * (((1 - BORED) * (double)1/numberOfDocs) + (BORED * (double)1/numberOfDocs));
					}else if(link_temp.get(i) != null){
						temp += a[j] * (((1 - BORED) * (double)1/out[j]) + (BORED * (double)1/numberOfDocs));
					}
					else
						temp += a[j] *(BORED * (double)1/numberOfDocs);
				}
				aCurrent[i] = temp;
			}
			double normalTmep = 0;
			for (double element: aCurrent){
				normalTmep += element;
			}
			for(int i = 0; i < numberOfDocs; i++)
				aCurrent[i] = (double)aCurrent[i] / normalTmep;

			for(int i = 0; i < numberOfDocs; i++){
				sumDif += Math.abs(a[i] - aCurrent[i]);
			}
			counter++;
			
		}
		for (int i = 0; i < numberOfDocs; i++){
			pageRanked.add(new DocumentRaking(docName[i], aCurrent[i]));
			
		}
            
        
		Collections.sort(pageRanked);
		// try{
		// writeArrayToFile();
		// }catch(IOException ex){
		// 	System.out.println(ex);
		// }
		for (int i = 0; i < 30; i++) {
			System.out.print(pageRanked.get(i).docName); 
			System.out.print(" : ");
			System.out.println(pageRanked.get(i).probability);
		}
    }

	// private void gClaculator(int numberOfDocs){	
	// 	for(int i = 0; i < numberOfDocs; i++){
	// 		System.out.println("here");
	// 		G.add(i, new ArrayList<Double>(Collections.nCopies(numberOfDocs, 0.0)));	
	// 		for(int k = 0; k < numberOfDocs; k++)
	// 			if(out[i] == 0){
	// 				// G.get(i).add(t, (double)1/numberOfDocs)
	// 				G.add(i, new ArrayList<Double>(Collections.nCopies(numberOfDocs, (double)1/numberOfDocs)));
	// 			}
	// 			else if(link.get(i).get(k)){
	// 				G.get(i).add(k, (double)1/out[i]);
	// 				System.out.println("here3");
	// 			}

	// 	}
	// 	for(int i = 0; i < numberOfDocs; i++)
	// 		for(int k = 0; k < numberOfDocs; k++)
	// 			G.get(i).add(k, (1 - BORED)* G.get(i).get(k) + BORED * (double)1/numberOfDocs);

	// }


    /* --------------------------------------------- */




	
	public void makeTitle() throws Exception{
		int fileIndex = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader("davisTitles.txt"));
			String line;
			while ((line = in.readLine()) != null) {
			int index = line.indexOf( ";" );
			String title_doc = line.substring( 0, index );
			String identifier = line.substring( index + 1);

			title.put(title_doc, identifier);
			}
		}catch ( IOException e ) {
				System.err.println( "Error reading file ");
			}
	}

	public void writeArrayToFile() throws Exception{
		makeTitle();
		BufferedWriter writer = new BufferedWriter(new FileWriter("pagerankstest.txt", false));
		for (int i = 0; i < pageRanked.size(); i++){
			// String identifier = title.get(pageRanked.get(i).docName);
			// writer.write(identifier);
			writer.write(";");
			writer.write(pageRanked.get(i).docName);
			writer.write(":");
			writer.write(Double.toString(pageRanked.get(i).probability));
			writer.newLine();
				
		}
		writer.flush();
	
		System.out.println("file is written succesfully");
	}

    public static void main( String[] args ) throws Exception {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}
package com.raudra.searchengine.search;

import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.services.PageParser;
import com.raudra.searchengine.services.Stemmer;
import com.raudra.searchengine.services.Main;
import com.raudra.searchengine.services.PageParser.Fields;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class SearchMain {

	
	//private static Map<String,Long> offsets;
	private static Stemmer stemmer=new Stemmer();
	private static List<RandomAccessFile> primayIndexeFiles;
	private static List<RandomAccessFile> indexFiles;
	private static List<TreeMap<String,Long>> secIndexes;
	
	private static RandomAccessFile titlePrimIndex;
	private static RandomAccessFile titleIndex;
	private static TreeMap<Integer, Long> titleSec;
	
	
	/**
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void loadSecondryIndexes(String indexFolder) throws Exception{
		String tokens[];
		TreeMap<String,Long> secIndex;
		secIndexes = new ArrayList<TreeMap<String,Long>>();
		primayIndexeFiles = new ArrayList<RandomAccessFile>();
		indexFiles = new ArrayList<RandomAccessFile>();
		File auxFile=null;
		for(int i = 0; i< WikiPageParsingConstants.NUM_OF_INDEXFILES; i++){
			
			BufferedReader reader=new BufferedReader(new FileReader(new File(indexFolder,i+ WikiPageParsingConstants.SECONDRY_SUFFIX)));
			secIndex=new TreeMap<String,Long>();
			for(String line; (line = reader.readLine())!= null; ){
				tokens=line.split(WikiPageParsingConstants.wordDelimiter);
				if(tokens.length != 2)
					continue;
				secIndex.put(tokens[0], Long.parseLong(tokens[1]));
			}
			secIndexes.add(secIndex);
			
			auxFile=new File(indexFolder,i+ WikiPageParsingConstants.OFFSET_SUFFIX);
			primayIndexeFiles.add( new RandomAccessFile(auxFile.getAbsoluteFile(), "r"));
			
			auxFile=new File(indexFolder,i+ WikiPageParsingConstants.INDEX_SUFFIX);
			indexFiles.add( new RandomAccessFile(auxFile.getAbsoluteFile(), "r"));
		}
		//Title indexes
		BufferedReader reader=new BufferedReader(new FileReader(new File(indexFolder, WikiPageParsingConstants.TITLES_FILE_PREFIX+ WikiPageParsingConstants.SECONDRY_SUFFIX)));
		titleSec=new TreeMap<Integer,Long>();
		for(String line; (line = reader.readLine())!= null; ){
			tokens=line.split(WikiPageParsingConstants.wordDelimiter);
			if(tokens.length != 2)
				continue;
			titleSec.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
		}
		
		
		auxFile=new File(indexFolder, WikiPageParsingConstants.TITLES_FILE_PREFIX+ WikiPageParsingConstants.OFFSET_SUFFIX);
		titlePrimIndex = new RandomAccessFile(auxFile.getAbsoluteFile(), "r");
		
		auxFile=new File(indexFolder, WikiPageParsingConstants.TITLES_FILE_PREFIX+ WikiPageParsingConstants.INDEX_SUFFIX);
		titleIndex = new RandomAccessFile(auxFile.getAbsoluteFile(), "r");
	}
	
	public static void main(String[] args) throws Exception  {
		// TODO Auto-generated method stub

		long start = System.currentTimeMillis();
		final String indexFolder = "/home/toothless/Desktop/SearchEngine/src/main/output";
		loadSecondryIndexes(indexFolder);
		Main.loadStopWords();
		String line = null;
		String tokens[] = null;

		Scanner reader=new Scanner(System.in);
		System.out.println("enter num of queries...");
		int numOfQueries=Integer.parseInt(reader.nextLine());


		for(int i=0;  i < numOfQueries;i++){
			List<QueryWord> queryWords=new ArrayList<QueryWord>();
			QueryWord queryWord;
            System.out.println("enter query....");
            line = reader.nextLine();
			start = System.currentTimeMillis();
			
			if(line != null){
				tokens =line.toLowerCase().split("\\s+");
				for(String word:tokens){
					queryWord=new QueryWord();
					if( word.length()>2 && word.charAt(1) == ':'){
						
						queryWord.setField(Fields.getField(word.charAt(0)));
						if(PageParser.stopWords.contains(word.substring(2)))
							continue;
						queryWord.setWord(stemmer.stemWord(word.substring(2)));
					}
					else{
						if(PageParser.stopWords.contains(word))
							continue;
						queryWord.setWord(stemmer.stemWord(word));
					}
					queryWords.add(queryWord);
				}
			}
			queryWords = getPostingList(queryWords);
			for(QueryWord qWord:queryWords){
				qWord.makeDocDetails();
			}
			
			Set<Integer> shownResults=new HashSet<Integer>(); //to avoid duplicates
			
			if(queryWords.size() == 1){
				queryWord=queryWords.get(0);
				displayTitleByTf(queryWord, WikiPageParsingConstants.MIN_RESULTSET , shownResults);
				System.out.println(System.currentTimeMillis()-start+ "ms");
				continue;
			}
			Collections.sort(queryWords, QueryWord.SORT_BY_IDF);
			
			List<List<DocDetails>> andDocDetails;
			andDocDetails = findAndOfDocDetails(queryWords);
			int resultIndex=0,resultsCount=0; boolean canDesplay=false;
			

			for(int index=andDocDetails.size()-1;index>=1;index--){
				if(shownResults.size() >= WikiPageParsingConstants.MIN_RESULTSET)
					break;
				displayResults(queryWords,andDocDetails.get(index),index+1,shownResults);
				
			}
			int remainig = WikiPageParsingConstants.MIN_RESULTSET-shownResults.size();
			
			
			int numberOfWords=queryWords.size();
			int resultCount;
			
			for( int index=0;shownResults.size() < WikiPageParsingConstants.MIN_RESULTSET
					&&index<queryWords.size(); index++){
				remainig = WikiPageParsingConstants.MIN_RESULTSET-shownResults.size();
				resultCount=index==queryWords.size()-1? remainig:(remainig/2+1);
				displayTitleByTf(queryWords.get(index),resultCount,shownResults);
				numberOfWords--;
			}
			System.out.println(System.currentTimeMillis()-start+ "ms");
		}
	}
	
	private static void displayTitleByTf(QueryWord queryWord,int numberResults,Set<Integer> shownResults) throws IOException{
		List<Integer> docIds=new ArrayList<Integer>();
		queryWord.sortDocDetailsByTf();
		Integer docId=null;
		for(int index=0; numberResults > 0 && index < queryWord.getDocDetails().size() ;index++){
			docId=queryWord.getDocDetails().get(index).getDocId();
			if(shownResults.contains(docId))
				continue;
			docIds.add(docId);
			shownResults.add(docId);
			numberResults--;
		}
		displayTitles(docIds);
	}
	
	private static void displayResults(List<QueryWord> queryWords,
			List<DocDetails> relevantDocs, int depthIndex, Set<Integer> shownResults) throws IOException{

		List<Double> tfs=new ArrayList<Double>();
		Map<Double,List<Integer>> cosAngle=new TreeMap<Double,List<Integer>>(Collections.reverseOrder());

		List<DocDetails> matchedDocs;
		List<Double> docUnitVector;
		List<Integer> docIds;
		double distance=0d;
		for(DocDetails docDetail:relevantDocs){
			
			if(shownResults.contains(docDetail.getDocId()))
				continue;
			
			tfs=new ArrayList<Double>();
			distance=0d;
			matchedDocs=docDetail.getResultDocs();
			for( int i=0;i<depthIndex;i++){
				//tfs.add(matchedDocs.get(i).getTf());
				distance+=matchedDocs.get(i).getTf();
			}
			docIds = cosAngle.get(distance);
			if(docIds == null){
				docIds=new ArrayList<Integer>();
			}
			docIds.add(docDetail.getDocId());
			cosAngle.put(distance, docIds);
		}
		
		int numOfResults=0,beginIndex,endIndex;
		String title;
		
		List<Integer> showingDocIds=new ArrayList<Integer>();
		for(Double dist:cosAngle.keySet()){
			for(Integer docId:cosAngle.get(dist)){
				if(shownResults.size() >= WikiPageParsingConstants.MIN_RESULTSET)
					break;
				title=getTitle(docId);
				if(title == null)
					continue;
				beginIndex=title.indexOf(WikiPageParsingConstants.wordDelimiter);
				//System.out.println(docId+"  "+ dist +"  " +title.substring(beginIndex+1));
				showingDocIds.add(docId);
				shownResults.add(docId);	
				
			}
			if(shownResults.size() >= WikiPageParsingConstants.MIN_RESULTSET)
				break;
		}
		displayTitles(showingDocIds);
	}
	private static void displayTitles(List<Integer> docIds) throws IOException{
		String title;
		for(Integer docId:docIds){
			title=getTitle(docId);
			if(title == null)
				continue;
			System.out.println(title);
		}
	}
	private static List<Double> getUnitVector(List<Double> idfs,List<Double> tfs){
		List<Double> unitVector=new ArrayList<Double>(idfs.size());
		List<Double> temp =new ArrayList<Double>();
		double length=0;
		for(int i=0;i<idfs.size(); i++){
			temp.add(idfs.get(i)*tfs.get(i));
			length+=temp.get(i)*temp.get(i);
		}
		length=Math.sqrt(length);
		for(int i=0;i<temp.size();i++){
			unitVector.add(temp.get(i)/length);
		}
		return unitVector;
	}
	public static List<QueryWord> getPostingList(List<QueryWord> queryWords){
		try {
			int beginIndex,endIndex;
			double idf;
			String postingList=null;
			List<QueryWord> validWords=new ArrayList<QueryWord>();
			for( QueryWord queryWord: queryWords){
				
				if( (postingList = getPostingList(queryWord.getWord())) != null){
					queryWord.setRawPostingList(postingList);
					beginIndex = postingList.indexOf(WikiPageParsingConstants.wordDelimiter);
					endIndex = postingList.indexOf(WikiPageParsingConstants.CHAR_WORD_DELIMITER);
					idf = Double.parseDouble(postingList.substring(beginIndex+1, endIndex));
					queryWord.setIdf(idf);
					validWords.add(queryWord);
				}
			}
			queryWords=validWords;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryWords;
	}
	
	private static String getPostingList(String word) throws IOException{
		if(word == null || word.length() == 0)
			return null;
		int filePrefix=word.charAt(0) - 'a';
		if(filePrefix < 0 ||filePrefix > 26 ){
			return null;
		}
		TreeMap<String,Long> secIndex = secIndexes.get(filePrefix);
		Entry<String,Long> entry = secIndex.floorEntry(word);
		if(entry == null)
			return null;
		long startOffset=entry.getValue(),offset=-1;
		RandomAccessFile primaryIndex = primayIndexeFiles.get(filePrefix),indexFile;
		
		primaryIndex.seek(startOffset);
		String line,tokens[];
		int diff;
		
		while((line = primaryIndex.readLine()) != null){
			tokens=line.split(WikiPageParsingConstants.wordDelimiter);
			if(tokens.length != 2)
				continue;
			diff=word.compareTo(tokens[0]);
			if(diff > 0)
				continue;
			else if(diff == 0)
				offset = Long.parseLong(tokens[1]);
			else
				offset = -1;
			break;
		}
		if(offset == -1){
			return null;
		}else{
			indexFile = indexFiles.get(filePrefix);
			indexFile.seek(offset);
			return indexFile.readLine();
		}
	}
	
	private static String getTitle(Integer word) throws IOException{
		
		if(word == null )
			return null;
        Entry<Integer,Long> entry = titleSec.floorEntry(word);
		if(entry == null)
			return null;
		long startOffset=entry.getValue(),offset=-1;
		
		titlePrimIndex.seek(startOffset);
		String line,tokens[];
		int diff;
		
		while((line = titlePrimIndex.readLine()) != null){
			tokens=line.split(WikiPageParsingConstants.wordDelimiter);
			if(tokens.length != 2)
				continue;
			diff= word.compareTo(Integer.parseInt(tokens[0]));
			if(diff > 0)
				continue;
			else if(diff == 0)
				offset = Long.parseLong(tokens[1]);
			else
				offset = -1;
			break;
		}
		if(offset == -1){
			return null;
		}else{
			titleIndex.seek(offset);
			String title=null;
			int start,id;
			while( (title=titleIndex.readLine()) != null){
				  start=title.indexOf(WikiPageParsingConstants.WORD_IDF_DELIMITER);
				  if(start < 1)
					  continue;
				  id=Integer.parseInt(title.substring(0, start));
				  if(id == word)
					  return title.substring(start+1);
				  else if(id > word)
					  return null;
			}
			return null;
		}
	}
	
	//word#idf=docid-freq:weight;
	
	public static List<List<DocDetails>> findAndOfDocDetails(List<QueryWord> queryWords) {
		
		//List <  List<DocDetails> >
		List<DocDetails> andDocs,tempRelevenceDocs,orDocs;
		List<List<DocDetails>> andDocsList= new ArrayList<List<DocDetails>>();
		if(queryWords.size() == 0 )
			return andDocsList;
		QueryWord queryWord=queryWords.get(0);
 		andDocs = DocDetails.intersection(queryWord.getDocDetails(),null,queryWord.getField()); // to check field
		andDocsList.add(andDocs);
		
		for(int i=1;i<queryWords.size();i++){
			queryWord=queryWords.get(i);
			andDocs=DocDetails.intersection(
					andDocsList.get(andDocsList.size()-1),queryWord.getDocDetails() ,null);
			andDocsList.add(andDocs);
		}
		
		return andDocsList;
	}
	

	public static void displayWordLine(String line){
		
		String prev="1",cur;
		String tokens[] = line.split(WikiPageParsingConstants.WORD_DELIMITER); // divide word and [doc,count]
		
		
		String docCounts[] = tokens[1].split(WikiPageParsingConstants.DOC_DELIMITER); //divide docs to doc-count
		
		//TODO: sort result
		StringBuilder result=new StringBuilder(docCounts.length * 9);
		
		Set<Integer> docIds=new TreeSet<Integer>();
		
		
		for(String docCount:docCounts ){
			cur=docCount.split(WikiPageParsingConstants.DOC_COUNT_DELIMITER)[0];
			docIds.add(Integer.parseInt(cur));
		}

		for(Integer docId:docIds){
		     result.append(docId +",");	
		}
		result.delete(result.length() - 1 , result.length() );
	}
}

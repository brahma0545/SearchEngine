package com.raudra.searchengine.services;


import com.raudra.searchengine.config.WikiPageParsingConstants;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ExternalSort {
	
	public static Map<String,Long> getOffsets(String offSetFileDir) {
		Map<String,Long> offsets=new HashMap<String,Long>();
		BufferedReader address=null;
		String prevWord="a",curWord;
		System.out.println("hello:");
		int count=0;
		try{
			address=new BufferedReader(new FileReader(new File(offSetFileDir, WikiPageParsingConstants.OFFSETS_FILE)));
		    
			for(String line; (line = address.readLine()) != null ;){
			
			String tokens[]= line.split(WikiPageParsingConstants.WORD_DELIMITER);
			if(tokens.length != 2)
				continue;
			if(offsets.containsKey(tokens[0])){
				System.out.println( tokens[0] + "is already present");
			}
			offsets.put(tokens[0], Long.parseLong(tokens[1]));
			curWord=tokens[0];
			//compare test code start
			if( curWord.compareTo(prevWord) <= 0 ){
				System.out.println( curWord + "    "+ prevWord);
				count++;
			}
			prevWord=curWord;
			//compare test code end
			
			}
			
			//System.out.println(count + "  count");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		/*RandomAccessFile index=new RandomAccessFile(new File("index.txt"), "r");
		  int lines=0;
		for( Long offset: offsets.values()){
			index.seek(offset);
			System.out.println( index.readLine() + ++lines );
		}*/
		
		return offsets;
	}
	
	/**
		 find starting address of each line from indexFile and save to offsetsFile 
	*/
	public static void createOffsetsFile(String indexFile, String offsetsFileDir,int offsetFilePrefix){
		PrintWriter wordsWriter=null, secondryIndexWriter=null;
		BufferedReader br=null;
		try{
			File wordsFile=new File(offsetsFileDir,offsetFilePrefix+ WikiPageParsingConstants.OFFSET_SUFFIX);
			File secondryIndex=new File(offsetsFileDir,offsetFilePrefix+ WikiPageParsingConstants.SECONDRY_SUFFIX);
			//System.out.println(indexFileDir+  "  "+WikiPageParsingConstants.INDEX_FILE_NAME);
			br = new BufferedReader(new FileReader(new File(indexFile)));
			
            String word=null;
            String line=null;
            wordsWriter=new PrintWriter(new FileOutputStream(wordsFile));
            secondryIndexWriter=new PrintWriter(new FileOutputStream(secondryIndex));
            long lineStart=0,secLineStart=0;
            long lineLength=0;
            int lineNumber=0;
            //String words[];
            String wordDelimiter= WikiPageParsingConstants.WORD_IDF_DELIMITER + "";
            String offsetLine;
            
            //TODO: optimize by using byte array instead of converting to String
            while( (line = br.readLine()) != null ){
            	 lineLength = line.length();
            	 word=line.split(wordDelimiter)[0]; // get word
            	 if(lineNumber++ % WikiPageParsingConstants.SECONDRY_INDEX_GAP == 0 ){
            		 secondryIndexWriter.println(word + wordDelimiter + secLineStart); 
            	 }
            	 offsetLine = word + wordDelimiter + lineStart;
            	 wordsWriter.println(offsetLine); 
            	 lineStart+=lineLength+1; // include newline character
            	 secLineStart+=offsetLine.length()+1;
            }
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(wordsWriter != null)
				wordsWriter.close();
			if(secondryIndexWriter != null)
				secondryIndexWriter.close();
		}
	}
}

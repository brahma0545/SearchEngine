package com.raudra.searchengine.services;

import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.config.StopWordsConfig;
import com.raudra.searchengine.util.TimeUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

public class Main {

	/** Pass Two arguments corpus file and index directory*/
	public static void main(String[] args)  {
		System.out.println(new Date().toString());
		WikiPageParsingConstants.startTime = TimeUtil.getCurrentTimeInMs();
		WikiPageParsingConstants.lastDump = TimeUtil.getCurrentTimeInMs();
		try {
			buildIndex("/home/toothless/Desktop/SearchEngine/src/main/input/2000pages.dat",
					"/home/toothless/Desktop/SearchEngine/src/main/output/");
			
		}
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void buildIndex(String corpusFile,String indexDirectory) throws IOException, ParserConfigurationException, SAXException{
		  loadStopWords();
		  SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		  SAXParser saxParser = saxParserFactory.newSAXParser();
		  WikiPageParsingConstants.indexFileDir = indexDirectory;
		  saxParser.parse(corpusFile, new WikiSAXHandler());
		  System.out.println(new Date().toString() );
	}

	public static void loadStopWords() throws IOException{

		BufferedReader br=new BufferedReader(new FileReader(new File(StopWordsConfig.STOP_WORD_FILE)));
		
		String line;
		PageParser.stopWords = new HashSet<String>();

		while( (line = br.readLine()) != null){
			  String tokens[] = line.toLowerCase().split(StopWordsConfig.STOP_WORD_DELIMITER);
			  for(String token:tokens){
				  PageParser.stopWords.add(token);
			  }
		}
		if(br != null)
			br.close();
	}
}

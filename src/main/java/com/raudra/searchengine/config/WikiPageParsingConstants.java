package com.raudra.searchengine.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class WikiPageParsingConstants {
	public static final String ELE_TITLE = "title";
	public static final String ELE_ID = "id";
	public static final String ELE_TEXT = "text";
	

	
	
	//word#idf=docid-freq:weight;
//	public static final String INDEX_FILE_NAME="index.txt";
	public static final int NUM_OF_INDEXFILES = 26;
	public static final List<String> indexFiles = new ArrayList<String>(NUM_OF_INDEXFILES);
	public static final String WORD_DELIMITER = "=";
	public static final char  CHAR_WORD_DELIMITER = '=';
	public static final char WORD_IDF_DELIMITER = '#';
	public static String wordDelimiter = "#";
	public static final String DOC_DELIMITER = ";";  // hardcoded at 2 places in PageParser for perf if(';' == s.charAt(i))
	public static final char CHAR_DOC_DELIMITER = ';';
	public static final String DOC_COUNT_DELIMITER = "-";
	public static final char CHAR_DOC_COUNT_DELIMITER = '-';
	public static final String WEIGHT_DELIMITER = ":";
	public static final char CHAR_WEIGHT_DELIMITER = ':';
	
	public static final String DOC_PARSIGN_REGEX = "[^a-z]";
	
	public static final String OFFSETS_FILE = "offsets.txt";
	public static int pageNumber = 0;
	public static int NumOfPagesInMap = 0;
	public static final int NUM_OF_PAGES_PER_CHUNK = 5000;
	public static final int MAX_MERGE_LINE_LENGTH = 100000;
	public static String indexFileDir;
	
	public static List<String> subIndexFiles=new ArrayList<String>();
	
	public static int lastSubIndexFile=1000;
//	public static String absoluteIndexFilePath;
	
	public static DecimalFormat decimalFormat=new DecimalFormat("#.##");
	
	public static final String OFFSET_SUFFIX="_offsets.txt";
	public static final String SECONDRY_SUFFIX="_secondry.txt";
	public static final String INDEX_SUFFIX="_index.txt";
	
	public static final int SECONDRY_INDEX_GAP = 20;
	
	
	public static final int TITLES_FILE_PREFIX = 26;
	public static File titleFile;
	public static BufferedWriter titleIndexWriter;
	
	public static int MIN_RESULTSET=10;
	public static long startTime;
	public static long dump=0;
	public static long indexCount=0;
	public static long lastDump;
	public static Writer prevWriter=null;
	
	public static BufferedWriter dynamicStopwordsWriter;
	public static String ALL_WORDS_FILE="allWords.txt";
}

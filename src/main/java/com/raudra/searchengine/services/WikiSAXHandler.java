package com.raudra.searchengine.services;

import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.config.XMLPageConfig;
import com.raudra.searchengine.model.WikiPage;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class WikiSAXHandler extends DefaultHandler{

	private enum CurrentElement{
		TITLE,ID,TEXT
	}

	private Map<String,Boolean> requiredElements;
	private boolean parse=false;
	
	private CurrentElement currentElement;
	
	private WikiPage page;

	private enum TextFields {
		INFOBOX("{{infobox "),
		EXTERNAL_LINKS("external links"),
		TEXT("text"),
		CATEGORY("[[category:");
		private String pattern;

		TextFields(String pattern) {
			this.pattern=pattern;
		}
	}
	
	private PageParser pageParser=new PageParser();

	WikiSAXHandler(){
		requiredElements = new HashMap<String,Boolean>();
		requiredElements.put(WikiPageParsingConstants.ELE_TITLE, true);
		requiredElements.put(WikiPageParsingConstants.ELE_ID, false);
		requiredElements.put(WikiPageParsingConstants.ELE_TEXT, true);
	}

	@Override
	public void startDocument() throws SAXException {
		
		//Titles File
		WikiPageParsingConstants.titleFile = new File(WikiPageParsingConstants.indexFileDir,
				WikiPageParsingConstants.TITLES_FILE_PREFIX +
						WikiPageParsingConstants.INDEX_SUFFIX);
		try {
			WikiPageParsingConstants.titleIndexWriter = new BufferedWriter(
                    new FileWriter(WikiPageParsingConstants.titleFile)
            );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
		//System.out.println("End of Doc...");
		
		try {
            WikiPageParsingConstants.titleIndexWriter.close();
            pageParser.dumpAllWords();
            pageParser.mergeSubIndexFiles();
            WikiPageParsingConstants.indexFiles.add(WikiPageParsingConstants.titleFile.getAbsolutePath());
            for(int i = 0; i < WikiPageParsingConstants.NUM_OF_INDEXFILES + 1; i++) {
				ExternalSort.createOffsetsFile(WikiPageParsingConstants.indexFiles.get(i),
                        WikiPageParsingConstants.indexFileDir,i);
			}
			ExternalSort.createOffsetsFile(WikiPageParsingConstants.titleFile.getAbsolutePath(),
					WikiPageParsingConstants.indexFileDir,WikiPageParsingConstants.TITLES_FILE_PREFIX);
			WikiPageParsingConstants.titleIndexWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
		qName = qName.toLowerCase();
		if(requiredElements.get(qName) != null && requiredElements.get(qName) ) {
            switch (qName) {
                case XMLPageConfig.ELEMENT_TITLE:
                    requiredElements.put(XMLPageConfig.ELEMENT_ID, true);
                    currentElement = CurrentElement.TITLE;
                    page = new WikiPage();
                    countOfIBCurl = 0;
                    prevField = TextFields.TEXT;
                    curField = TextFields.TEXT;
                    infoboxDone = false;
                    break;
                case WikiPageParsingConstants.ELE_ID:
                    currentElement = CurrentElement.ID;
                    break;
                default:
                    currentElement = CurrentElement.TEXT;
                    break;
            }
			parse=true;		
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// TODO Auto-generated method stub
		qName = qName.toLowerCase();
		if(parse){
			if(qName.equals(WikiPageParsingConstants.ELE_ID)){
				requiredElements.put(WikiPageParsingConstants.ELE_ID, false);
			}else if(qName.equals(WikiPageParsingConstants.ELE_TEXT)){
				try {
					pageParser.parse(page);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			parse=false;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
        if (parse) {
            switch (currentElement) {
                case TEXT:
                    divideFields(ch, start, length);
                    break;
                case TITLE:
                    page.getTitle().append(ch, start, length);
                    break;
                case ID:
                    String id = new String(ch, start, length);
                    page.setId(id);
                    break;
            }
        }
	}
	
	private int countOfIBCurl=0;
	private TextFields prevField;
	private TextFields curField;
	private boolean infoboxDone;
	

	private void divideFields(char[] ch, int start, int length){
		int i=start;
		boolean match=false;
		int matchIndex=0;
		
		if(curField == TextFields.INFOBOX || curField == TextFields.CATEGORY){
			if(curField == TextFields.INFOBOX){ //match curl braces for infobox
				for(;i<start+length;i++){
					if( ch[i] == '{')
						countOfIBCurl++;
					else if(ch[i] == '}')
						countOfIBCurl--;
					if(countOfIBCurl == 0){
						addStringToPrevField(ch, start, i-start+1, curField);
						curField= TextFields.TEXT;
						infoboxDone=true;
						divideFields(ch, i , length-(i - start));
						return;
					}
				}
			}
            else if(curField == TextFields.CATEGORY){
				for(;i<start+length;i++){
					if( ch[i] == '[')
						countOfIBCurl++;
					else if(ch[i] == ']')
						countOfIBCurl--;
					if(countOfIBCurl == 0){
						addStringToPrevField(ch, start, i-start+1, curField);
						addStringToPrevField(ch, start, i-start+1, curField);
						curField= TextFields.TEXT;
						divideFields(ch, i , length-(i - start));
						return;
					}
				}
			}
			addStringToPrevField(ch, start, length, curField);
			return;
		}
		for(; i< start+length ; i++ ){
			matchIndex=i;
			if(  !infoboxDone && ch[i] == '{'){
				match=isMatch(ch, start, length,i, TextFields.INFOBOX.pattern);
				if(match){
					prevField=curField;
					curField= TextFields.INFOBOX;
					countOfIBCurl=0;
				}
			} else if(curField != TextFields.EXTERNAL_LINKS &&ch[i] == '='){
				match=isExternalLink(ch, start, length, i, TextFields.EXTERNAL_LINKS.pattern);
				if(match){
					prevField=curField;
					curField= TextFields.EXTERNAL_LINKS;
				}
				
			} else if( ch[i] == '['){
				match = isMatch(ch, start, length,i, TextFields.CATEGORY.pattern);
				if(match){
					prevField=curField;
					curField= TextFields.CATEGORY;
					countOfIBCurl=0;
				}
			}
			if(match){
				break;
			}
		}
		if(match){
			addStringToPrevField(ch, start, matchIndex-start, prevField);
			divideFields(ch, matchIndex, length-(matchIndex - start));
		}else {
			addStringToPrevField(ch, start, length, curField);
		}
	}
	
	private void addStringToPrevField(char[] ch,int start,int length,TextFields field ){
		switch(field){
			case TEXT:
                page.getText().append(ch, start, length);
                break;
			case INFOBOX:
                page.getInfoBox().append(ch,start,length);
                break;
			case CATEGORY:
                page.getCategory().append(ch,start,length);
                break;
			case EXTERNAL_LINKS:
                page.getExternalLinks().append(ch,start,length);
                break;
		}
		
	}
	
	private boolean isMatch(char ch[], int start, int length ,int firstCharPos, String  pattern) {
		
			int index = 0;
			while( firstCharPos + index < start + length  &&
					index < pattern.length() &&
					Character.toLowerCase(ch[firstCharPos + index]) == pattern.charAt(index)) {
				index++;
			}
			if( index == pattern.length())
				return true;
			return false;
	}

	private boolean isExternalLink(char ch[], int start, int length ,int firstCharPos, String  pattern) {

		int auxFirst = firstCharPos;
		//only two equals should be there
		while( auxFirst < start + length && ch[auxFirst] == '=' ) {
			auxFirst++;
		}
		if(auxFirst == start + length || auxFirst - firstCharPos != 2)
			return false;
		while( auxFirst < start + length && ch[auxFirst] == ' ')
            auxFirst++;
		return isMatch(ch, start, length, auxFirst, pattern);
	}
}

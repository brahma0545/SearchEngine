package com.raudra.searchengine.search;


import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.services.PageParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QueryWord {

	private String rawPostingList;
	private String word;
	private PageParser.Fields field;
	private double idf;
	public List<DocDetails> docDetails;
	
	public String getRawPostingList() {
		return rawPostingList;
	}
	public void setRawPostingList(String rawPostingList) {
		this.rawPostingList = rawPostingList;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public PageParser.Fields getField() {
		return field;
	}
	public void setField(PageParser.Fields field) {
		this.field = field;
	}
	public double getIdf() {
		return idf;
	}
	public void setIdf(double idf) {
		this.idf = idf;
	}
	
	
	public List<DocDetails> getDocDetails() {
		return docDetails;
	}
	public void setDocDetails(List<DocDetails> docDetails) {
		this.docDetails = docDetails;
	}


	public static CompareByIDF SORT_BY_IDF=new CompareByIDF();
	private static class CompareByIDF implements Comparator<QueryWord> {

		public int compare(QueryWord o1, QueryWord o2) {
			// TODO Auto-generated method stub
			double diff=o1.getIdf() - o2.getIdf();
			 if(diff > 0)
				 return -1;
			 else if(diff < 0)
				 return 1;
			 return 0;
		}
		
	}
	public List<DocDetails> makeDocDetails(){
		docDetails=new ArrayList<DocDetails>();
		DocDetails doc=null;
		int  len=rawPostingList.length(), endIndex,beginIndex=0;
		beginIndex=rawPostingList.indexOf(WikiPageParsingConstants.CHAR_WORD_DELIMITER);
		beginIndex++;
		while(true){
			endIndex=rawPostingList.indexOf(WikiPageParsingConstants.CHAR_DOC_DELIMITER,beginIndex);
			if(endIndex < 0)
				break;
			doc = new DocDetails(rawPostingList.substring(beginIndex,endIndex));
        	 
			if(field != null && (doc.getFieldType()&field.getSetbit()) != field.getSetbit()){
				beginIndex=endIndex+1;
				continue;
			}
		//	System.out.println(doc.getDocId());
			docDetails.add(doc);
			beginIndex = endIndex+1;
		}
		return docDetails;
	}
	
	public void sortDocDetailsByTf(){
		Collections.sort(docDetails, SORT_BY_TF);
	}
	
	public static CompareByTf SORT_BY_TF=new CompareByTf();
	private static class CompareByTf implements Comparator<DocDetails>{

		public int compare(DocDetails o1, DocDetails o2) {
			// TODO Auto-generated method stub
			return Double.compare(o1.getTf(), o2.getTf()) * -1;
			/*double diff=o1.getTf()-o2.getTf();
			if(diff > 0) return 1;
			else if (diff < 0) return -1;
			return 0;*/
		}
	}
}

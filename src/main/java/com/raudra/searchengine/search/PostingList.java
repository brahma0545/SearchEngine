package com.raudra.searchengine.search;

import com.raudra.searchengine.config.WikiPageParsingConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostingList {
	
	private String word;
	private double idf;
	private String docIds;
	private List<DocDetails> docDetails;
	
	public List<DocDetails> makeDocDetails() {
		docDetails=new ArrayList<DocDetails>();
		int  len=docIds.length(), endIndex,beginIndex=0;
		while(true) {
			endIndex = docIds.indexOf(WikiPageParsingConstants.CHAR_DOC_DELIMITER,beginIndex);
			//docDetails.add(new DocDetails(docIds.substring(beginIndex,endIndex)));
			if(endIndex == len-1)
				break;
			beginIndex=endIndex+1;
		}
		return docDetails;
	}
	
	public List<DocDetails> getDocDetails() {
		return docDetails;
	}

	public void setDocDetails(List<DocDetails> docDetails) {
		this.docDetails = docDetails;
	}

	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public double getIdf() {
		return idf;
	}
	public void setIdf(double idf) {
		this.idf = idf;
	}
	public String getDocIds() {
		return docIds;
	}
	public void setDocIds(String docIds) {
		this.docIds = docIds;
	}
	
	public static CompareByIDF SORT_BY_IDF=new CompareByIDF();
	private static class CompareByIDF implements Comparator<PostingList> {

		public int compare(PostingList o1, PostingList o2) {
			// TODO Auto-generated method stub
			double diff = o1.getIdf() - o1.getIdf();
			 if(diff > 0)
				 return -1;
			 else if(diff < 0)
				 return 1;
			 return 0;
		}
	}
}

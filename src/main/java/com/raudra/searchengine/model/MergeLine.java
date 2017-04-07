package com.raudra.searchengine.model;


import com.raudra.searchengine.config.WikiPageParsingConstants;

public class MergeLine implements Comparable<MergeLine> {

	private String word;
	private  int fileNum;
	private String docIds;

	public MergeLine(int fileNum, String word, String docIds){
		this.word=word;
		this.fileNum=fileNum;
		this.docIds=docIds;
	}
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getFileNum() {
		return fileNum;
	}

	public void setFileNum(int fileNum) {
		this.fileNum = fileNum;
	}

	public String getDocIds() {
		return docIds;
	}

	public void setDocIds(String docIds) {
		this.docIds = docIds;
	}


	public int compareTo(MergeLine inputLine) {
		int diff=0;
		if(inputLine == null)
			return 1;
	//	return word.compareTo(o.getWord());
		diff = word.compareTo(inputLine.getWord());
		if( diff == 0){
			int end1,end2, docId1, docId2;
			end1 = docIds.indexOf(WikiPageParsingConstants.CHAR_DOC_COUNT_DELIMITER);
			docId1 = Integer.parseInt(docIds.substring(0, end1));
			end2 = inputLine.getDocIds().indexOf(WikiPageParsingConstants.CHAR_DOC_COUNT_DELIMITER);
			docId2 = Integer.parseInt(inputLine.getDocIds().substring(0, end2));
			return docId1 > docId2 ? 1 : -1;
		}
		return diff;
	}
}

package com.raudra.searchengine.search;


import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.services.PageParser;

import java.util.ArrayList;
import java.util.List;

public class DocDetails {

	private Integer docId;
	private int fieldType;
	private double tf;
	private List<DocDetails> resultDocs;
	
	//word#idf=docid-freq:weight;
	public String toString(){
		return docId + "-" + fieldType + ":" + tf +";";
	}
	public DocDetails(String details){
		//word#idf=docid-freq:weight;
		 int startIndex,endIndex;
		 startIndex= details.indexOf(WikiPageParsingConstants.CHAR_DOC_COUNT_DELIMITER);
		 docId=Integer.parseInt(details.substring(0, startIndex));
		 
		 endIndex=details.indexOf(WikiPageParsingConstants.CHAR_WEIGHT_DELIMITER, startIndex);
		 fieldType= Integer.parseInt(details.substring(startIndex+1, endIndex));
		 
		 //startIndex = details.indexOf(WikiPageParsingConstants.CHAR_DOC_DELIMITER); // save asgn startIndex=endIndex :)
		 tf=Double.parseDouble(details.substring(endIndex+1));
		 resultDocs=new ArrayList<DocDetails>();
		 resultDocs.add(this);
	}
	
	
	public Integer getDocId() {
		return docId;
	}
	public void setDocId(Integer docId) {
		this.docId = docId;
	}
	public int getFieldType() {
		return fieldType;
	}
	public void setFieldType(int fieldType) {
		this.fieldType = fieldType;
	}
	public double getTf() {
		return tf;
	}
	public void setTf(double tf) {
		this.tf = tf;
	}
	
	public List<DocDetails> getResultDocs() {
		return resultDocs;
	}
	public void setResultDocs(List<DocDetails> resultDocs) {
		this.resultDocs = resultDocs;
	}
	public static List<DocDetails> intersection(List<DocDetails> l1,List<DocDetails> l2,PageParser.Fields field){
		List<DocDetails> common=new ArrayList<DocDetails>();
		
		if(l2 == null){
			if(field == null) //no field type
				common.addAll(l1);
			else{
				for(DocDetails docDetail:l1){ //check field type
					if( (field.getSetbit() & docDetail.getFieldType()) == field.getSetbit() ){
						common.add(docDetail);
					}
				}
			}
			return common;
		}
		
		int len1=l1.size(),len2=l2.size(),diff,docId1,docId2;
		
		for(int i=0,j=0;i<len1 && j<len2; ){
			//diff=l1.get(i).getDocId().compareTo(l2.get(j).getDocId());
			/*if(l1.get(i).getDocId()== 38539){
				System.out.print(l1.get(i));
			}*/
			
			docId1=l1.get(i).getDocId();
			docId2=l2.get(j).getDocId();
			diff=docId1-docId2;
			if(diff > 0)
				j++;
			else if(diff < 0)
				i++;
			else{
				if( field == null){
					l1.get(i).getResultDocs().add(l2.get(j));
					common.add(l1.get(i));
					
				}
				//field match
				else if( (field.getSetbit() & l2.get(j).getFieldType()) == field.getSetbit() ){
					l1.get(i).getResultDocs().add(l2.get(j));
					common.add(l1.get(i));
				}
				i++;
				j++;
			}
		}
		return common;
	}
}
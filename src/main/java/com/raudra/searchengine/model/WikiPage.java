package com.raudra.searchengine.model;

public class WikiPage {
	
	private StringBuilder title;
	private String id;
	private StringBuilder text;
	private StringBuilder category;
	private StringBuilder infoBox;
	private StringBuilder externalLinks;
	
	public WikiPage(){
		title=new StringBuilder(32);
		text=new StringBuilder(4096);
		category=new StringBuilder(64);
		infoBox=new StringBuilder(256);
		externalLinks=new StringBuilder(128);
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public StringBuilder getTitle() {
		return title;
	}
	public void setTitle(StringBuilder title) {
		this.title = title;
	}
	public StringBuilder getText() {
		return text;
	}
	public void setText(StringBuilder text) {
		this.text = text;
	}

	public StringBuilder getCategory() {
		return category;
	}

	public void setCategory(StringBuilder category) {
		this.category = category;
	}

	public StringBuilder getInfoBox() {
		return infoBox;
	}

	public void setInfoBox(StringBuilder infoBox) {
		this.infoBox = infoBox;
	}

	public StringBuilder getExternalLinks() {
		return externalLinks;
	}

	public void setExternalLinks(StringBuilder externalLinks) {
		this.externalLinks = externalLinks;
	}
}

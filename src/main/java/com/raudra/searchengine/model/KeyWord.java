package com.raudra.searchengine.model;

public class KeyWord {
	private String word;
	private int t;
	private int c;
	private int b;

    public KeyWord(String word, int t, int c, int b) {
        this.word = word;
        this.t = t;
        this.c = c;
        this.b = b;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }
}

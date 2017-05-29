package com.raudra.searchengine.services;

import com.raudra.searchengine.config.WikiPageParsingConstants;
import com.raudra.searchengine.model.MergeLine;
import com.raudra.searchengine.model.WikiPage;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by satheeesh on 22/11/16.
 */
public class PageParser {

    public static int totalNumberOfDoc=0;

    public static double maxWeight=0;

    private static Map<String,StringBuilder> allWords=new TreeMap<String,StringBuilder>();
    public static Set<String> stopWords;
    private Stemmer stemmer=new Stemmer();
    public static enum Fields{
        TITLE('t',10000,4), INFOBOX('i',20,3), LINKS('l',10,2), CATAGORY('c',50,1), BODY('b',1,0);
        private char shortForm;
        private int weight;
        private int setbit;
        private Fields(char shortForm,int weight,int setbit){
            this.shortForm=shortForm;
            this.weight=weight;
            this.setbit= 1 << setbit;
        }
        public char getShortForm() {
            return shortForm;
        }
        public int getWeight(){
            return weight;
        }
        public int getSetbit(){
            return setbit;
        }
        public static Fields getField(Character shortForm){
            switch(shortForm){
                case 't': return TITLE;
                case 'i' : return INFOBOX;
                case 'l' :return LINKS;
                case 'c' :return CATAGORY;
                case 'b' :return BODY;
            }
            return null;
        }
    }

    public void parse(WikiPage page) throws IOException {

        if(page.getTitle().indexOf("Wikipedia:") == 0 || page.getTitle().indexOf("File:") == 0){
            return;
        }
        totalNumberOfDoc++;
        page.setId(String.valueOf(totalNumberOfDoc));
        Map<String,Integer[]> wordCount=new HashMap<String, Integer[]>(256);

        String aux = page.getTitle().toString();

        //TODO move this seperate TITLE file
        WikiPageParsingConstants.titleIndexWriter.write(page.getId());
        WikiPageParsingConstants.titleIndexWriter.write(WikiPageParsingConstants.wordDelimiter);
        WikiPageParsingConstants.titleIndexWriter.write(aux);
        WikiPageParsingConstants.titleIndexWriter.write('\n');
        //TODO Writing file is done


        parseText(aux.toLowerCase(), wordCount,Fields.TITLE);

        aux = page.getText().toString().toLowerCase();
        parseText(aux,wordCount, Fields.BODY);

        aux = page.getInfoBox().toString().toLowerCase();
        parseText(aux,wordCount, Fields.INFOBOX);

        aux=page.getExternalLinks().toString().toLowerCase();
        parseText(aux,wordCount, Fields.LINKS);

        aux=page.getCategory().toString().toLowerCase();
        parseText(aux,wordCount, Fields.CATAGORY);

        insertToAllWords(page, wordCount);
		/*maxWeight=maxFreq(wordCount);

		maxWeight=0;*/
    }
/*    private int maxFreq(Map<String,Integer[]> freq){
        Iterator<Map.Entry<String, Integer[]>> it=freq.entrySet().iterator();
        int max=0;
        int weight=0;
        while(it.hasNext()){
            Map.Entry<String,Integer[]> en=it.next();
            weight = getWeight(en.getValue());
            if(weight > max)
                max=weight;
        }
        return max;
    }*/

    public void insertToAllWords(WikiPage page, Map<String,Integer[]> wordCount)

            throws IOException{

        Iterator<Map.Entry<String,Integer[]> > entries = wordCount.entrySet().iterator();

        StringBuilder docList=null;
        while(entries.hasNext()){
            Map.Entry<String, Integer[]> entry = entries.next();
            docList = allWords.get(entry.getKey());
            if(docList == null){
                docList=new StringBuilder();
                docList.append(page.getId())
                        .append( WikiPageParsingConstants.DOC_COUNT_DELIMITER)
                        .append(getFieldsString(entry.getValue())
                                .append( WikiPageParsingConstants.CHAR_DOC_DELIMITER));
                allWords.put(entry.getKey(), docList);
            }else{
                docList.append(page.getId())
                        .append( WikiPageParsingConstants.DOC_COUNT_DELIMITER)
                        .append(getFieldsString(entry.getValue())
                                .append( WikiPageParsingConstants.CHAR_DOC_DELIMITER));
            }
            //System.out.println(entry.getKey()+ ":"+page.getId()+"-"+entry.getValue());
        }

        if( WikiPageParsingConstants.NUM_OF_PAGES_PER_CHUNK == ++WikiPageParsingConstants.NumOfPagesInMap ){
            System.out.println("dump number "+ ++WikiPageParsingConstants.dump
                    +" last dump dur:(s) " + TimeUnit.SECONDS.
                    convert(System.currentTimeMillis()- WikiPageParsingConstants.lastDump, TimeUnit.MILLISECONDS)
                    +" total time(m) " + TimeUnit.MINUTES.
                    convert(System.currentTimeMillis()- WikiPageParsingConstants.startTime, TimeUnit.MILLISECONDS));
            WikiPageParsingConstants.lastDump = System.currentTimeMillis();
            dumpAllWords();
            WikiPageParsingConstants.NumOfPagesInMap = 0;
            allWords = new TreeMap<>();
            System.gc();
        }
    }


    public void dumpAllWords() throws IOException{

        if(allWords.size() == 0)
            return;

        Writer writer = getWriterForDump();
        Iterator<Map.Entry<String, StringBuilder>> entries = allWords.entrySet().iterator();
        StringBuffer blockOfData;
        Map.Entry<String,StringBuilder> entry=null;

        while( entries.hasNext()){
            blockOfData = new StringBuffer(2048);
            entry = entries.next();
            blockOfData.append(entry.getKey())
                    .append(WikiPageParsingConstants.WORD_DELIMITER)
                    .append(entry.getValue())
                    .append("\n");
            writer.write(blockOfData.toString());
            //}
        }
        try {
            if(writer != null)
                writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeData(String blockOfData, Writer writer ){
        try {
            if(WikiPageParsingConstants.prevWriter != writer){
                WikiPageParsingConstants.prevWriter=writer;
                System.out.println("file number "+ ++WikiPageParsingConstants.indexCount
                        +" last file dur:(s) " +TimeUnit.SECONDS.
                        convert(System.currentTimeMillis()- WikiPageParsingConstants.lastDump, TimeUnit.MILLISECONDS)
                        +" total time(m) " + TimeUnit.MINUTES.
                        convert(System.currentTimeMillis()- WikiPageParsingConstants.startTime, TimeUnit.MILLISECONDS));
                WikiPageParsingConstants.lastDump=System.currentTimeMillis();
            }
            writer.write(blockOfData);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Writer getWriterForDump() {

        File dumpFile=new File(WikiPageParsingConstants.indexFileDir,"" +
                WikiPageParsingConstants.lastSubIndexFile++ );
        WikiPageParsingConstants.subIndexFiles.add(dumpFile.getAbsolutePath());
        try {
            return new BufferedWriter(new FileWriter(dumpFile,false));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }







    public void parseText(String text, Map<String, Integer[]> wordCount, Fields type){
        String []tokens = text.split(WikiPageParsingConstants.DOC_PARSIGN_REGEX);
        Integer[] count;
        for(String token:tokens){

            if(token.isEmpty()){
                continue;
            }

            if( stopWords.contains(token))
                continue;

            token = stemmer.stemWord(token);
            count = wordCount.get(token);
            if(count == null){
                count = new Integer[]{0,0,0,0,0};
                count[type.ordinal()]++;
                wordCount.put(token, count);
            }else{
                count[type.ordinal()]++;
                wordCount.put(token, count);
            }
        }
    }

/*    public void printPostingList(){
        Iterator< Map.Entry<String, StringBuilder> > entries=allWords.entrySet().iterator();
        PrintWriter writer=null;
        File indexFile=null;
        try {
            indexFile =new File(WikiPageParsingConstants.indexFileDir, "posting_list.txt") ;


            writer=new PrintWriter(new FileOutputStream(indexFile));


            while(entries.hasNext()){
                Map.Entry<String,StringBuilder> entry=entries.next();

                writer.println(entry.getKey() + WikiPageParsingConstants.WORD_DELIMITER + entry.getValue());

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally{
            if(writer != null)
                writer.close();
        }
        ExternalSort.createOffsetsFile(indexFile.getAbsolutePath(), WikiPageParsingConstants.OFFSETS_FILE,100000);
    }*/

    public void mergeSubIndexFiles() throws IOException {


        List<BufferedReader> readers = getReaderOfSubIndexFiles();
        boolean reachedEOF[] = new boolean[readers.size()];

        PriorityQueue<MergeLine> pq=new PriorityQueue<MergeLine>
                (WikiPageParsingConstants.subIndexFiles.size());
        List<BufferedWriter> indexFileWriters = new ArrayList<BufferedWriter>
                (WikiPageParsingConstants. NUM_OF_INDEXFILES);
        BufferedWriter allWordsWriter = new BufferedWriter(new FileWriter
                (new File(WikiPageParsingConstants.indexFileDir, WikiPageParsingConstants.ALL_WORDS_FILE)));
        File auxIndexFile=null;
        for(int i = 0; i< WikiPageParsingConstants. NUM_OF_INDEXFILES; i++){
            auxIndexFile = new File(WikiPageParsingConstants.indexFileDir,
                    i + WikiPageParsingConstants.INDEX_SUFFIX);
            WikiPageParsingConstants.indexFiles.add(auxIndexFile.getAbsolutePath());
            indexFileWriters.add(new BufferedWriter(new FileWriter(auxIndexFile,false)));
        }

        for(int i=0; i < readers.size(); i++){
            nextMergeLine(pq, readers, i,reachedEOF);
        }

        List<MergeLine> sameWords = new ArrayList<MergeLine>();;
        MergeLine mergeLine=null;
        String currentWord=null;
        int numOfDocsWordPresent=0;
        String auxDocIds;
        String idf;
        // get least line from PQ and set current word
        mergeLine = pq.poll();
        nextMergeLine(pq, readers,mergeLine.getFileNum(),reachedEOF);
        currentWord=mergeLine.getWord();
        Writer currentWriter=indexFileWriters.get(currentWord.charAt(0) - 'a');
        sameWords.add(mergeLine);

        while(!pq.isEmpty()){
            mergeLine = pq.poll();
            // if same word then we have to append
            if(currentWord.equals(mergeLine.getWord())){
                sameWords.add(mergeLine);
            }else{  // append all docIds of currentWord
                currentWriter=indexFileWriters.get(currentWord.charAt(0) - 'a');
                //cal IDF
                numOfDocsWordPresent = 0;
                for(MergeLine sameWord:sameWords){
                    auxDocIds = sameWord.getDocIds();
                    for(int i=0;i<auxDocIds.length();i++){
                        if(WikiPageParsingConstants.CHAR_DOC_DELIMITER == auxDocIds.charAt(i))
                            numOfDocsWordPresent++;
                    }
                }
                allWordsWriter.write(currentWord + WikiPageParsingConstants.WORD_IDF_DELIMITER +
                        numOfDocsWordPresent +"\n");
                idf = invertedDocumentFreq(numOfDocsWordPresent);
                StringBuffer wholeLine = new StringBuffer();
                //Add word
                wholeLine.append(currentWord).append(WikiPageParsingConstants.WORD_IDF_DELIMITER)
                        .append(idf).append(WikiPageParsingConstants.WORD_DELIMITER);
                //append docIds
                for(MergeLine sameWord:sameWords){
                    wholeLine.append(sameWord.getDocIds());
                    if(wholeLine.length()> WikiPageParsingConstants.MAX_MERGE_LINE_LENGTH){
                        writeData(wholeLine.toString(), currentWriter);
                        wholeLine=new StringBuffer();
                    }
                }

                wholeLine.append("\n");

                writeData(wholeLine.toString(), currentWriter);
                sameWords=new ArrayList<MergeLine>();
                sameWords.add(mergeLine);
                currentWord=mergeLine.getWord();
                //		printLineToIndexFile(sameWords, currentWord, indexFileWriter, mergeLine);
            }
            nextMergeLine(pq, readers, mergeLine.getFileNum(), reachedEOF);
        }


        numOfDocsWordPresent=0;
        for(MergeLine sameWord:sameWords){
            auxDocIds=sameWord.getDocIds();
            for(int i=0;i<auxDocIds.length();i++){
                if(';' == auxDocIds.charAt(i))
                    numOfDocsWordPresent++;
            }
        }
        allWordsWriter.write(currentWord+ WikiPageParsingConstants.WORD_IDF_DELIMITER+numOfDocsWordPresent+"\n");
        idf=invertedDocumentFreq(numOfDocsWordPresent);
        StringBuffer wholeLine=new StringBuffer();
        //Add word
        currentWriter=indexFileWriters.get(currentWord.charAt(0) - 'a');
        wholeLine.append(currentWord).append(WikiPageParsingConstants.WORD_IDF_DELIMITER)
                .append(idf).append(WikiPageParsingConstants.WORD_DELIMITER);
        //append docIds
        for(MergeLine sameWord:sameWords){
            wholeLine.append(sameWord.getDocIds());
            if(wholeLine.length()> WikiPageParsingConstants.MAX_MERGE_LINE_LENGTH){
                writeData(wholeLine.toString(), currentWriter);
                wholeLine=new StringBuffer();
            }
        }

        wholeLine.append("\n");
        writeData(wholeLine.toString(), currentWriter);
        //printLineToIndexFile(sameWords, currentWord, indexFileWriter, mergeLine);



        for(BufferedReader reader:readers){
            if(reader != null)
                reader.close();
        }
        for(BufferedWriter indexFileWriter:indexFileWriters){
            if(indexFileWriter != null)
                indexFileWriter.close();
        }
        if(allWordsWriter!=null)
            allWordsWriter.close();
        deleteFiles(WikiPageParsingConstants.subIndexFiles);
    }

/*    public void printLineToIndexFile(List<MergeLine> sameWords, String currentWord,
                                     BufferedWriter indexFileWriter,MergeLine mergeLine)
    {
        StringBuffer wholeLine=new StringBuffer();
        //Add word
        wholeLine.append(currentWord).append(WikiPageParsingConstants.WORD_DELIMITER);
        //append docIds
        for(MergeLine sameWord:sameWords){
            wholeLine.append(sameWord.getDocIds());
        }

        wholeLine.append("\n");

        writeData(wholeLine.toString(), indexFileWriter);
        sameWords=new ArrayList<MergeLine>();
        sameWords.add(mergeLine);
        currentWord=mergeLine.getWord();

    }*/

    public void deleteFiles(List<String> filePaths){
        for(String filePath:filePaths){
            new File(filePath).deleteOnExit();
        }
    }


    public void nextMergeLine(PriorityQueue<MergeLine> pq ,List<BufferedReader> readers,
                              int readerNum, boolean[] reachedEOF) throws IOException{

        String line = null, word = null, docIds = null;
        int delimiterIndex=0;
        int numberOfFileReachedEnd=0;
        int numberOfReaders = readers.size();
        while( numberOfFileReachedEnd != numberOfReaders && reachedEOF[readerNum] ){
            readerNum = (readerNum+1) % numberOfReaders;
            numberOfFileReachedEnd++;
        }

        if( numberOfFileReachedEnd == numberOfReaders)
            return;

        if ( (line = readers.get(readerNum).readLine() ) != null){
            delimiterIndex = line.indexOf(WikiPageParsingConstants.WORD_DELIMITER);
            word=line.substring(0,delimiterIndex);
            docIds=line.substring(delimiterIndex+1);
            pq.add(new MergeLine(readerNum, word, docIds));
        }else{
            reachedEOF[readerNum]=true;
            nextMergeLine(pq, readers, (readerNum+1) % numberOfReaders, reachedEOF);
        }
    }

    public List<BufferedReader> getReaderOfSubIndexFiles(){
        List<BufferedReader> readers=new ArrayList<BufferedReader>(WikiPageParsingConstants.subIndexFiles.size());
        for(String file: WikiPageParsingConstants.subIndexFiles){
            try {
                readers.add(new BufferedReader(new FileReader(new File(file))));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return readers;
    }
    private StringBuilder getFieldsString(Integer[] values){

        StringBuilder valueString = new StringBuilder();
        int weight=0;
        int setBit=0;
        for(Fields field:Fields.values()){
            if(values[field.ordinal()] == 0)
                continue;
            weight = weight + (values[field.ordinal()].intValue() * field.getWeight());
            setBit = setBit | field.getSetbit();
            //valueString.append(field.getShortForm());
        }
        valueString.append(setBit).append(WikiPageParsingConstants.WEIGHT_DELIMITER)
                .append( (termFrequence(weight)) );
        return valueString;
    }

    private int getWeight(Integer[] values){

        int weight=0;
        for(Fields field:Fields.values()){
            if(values[field.ordinal()] == 0)
                continue;
            weight = weight + (values[field.ordinal()].intValue() * field.getWeight());
            //valueString.append(field.getShortForm());
        }
        return weight;
    }

    private String termFrequence(int weight){
        double result=0;
        if(weight == 0)
            return "0";
        else{
            result= 1 + Math.log10(weight);
            //result= weight/maxWeight;
        }
        return WikiPageParsingConstants.decimalFormat.format(result);
    }

    private String invertedDocumentFreq(int numOfDocsWordPresent){
        double result=0;
        if(numOfDocsWordPresent == 0)
            return "0";
        else{
            result=  Math.log10( ((double)PageParser.totalNumberOfDoc) / numOfDocsWordPresent);
        }
        return WikiPageParsingConstants.decimalFormat.format(result);
    }

/*    private int countChar(String s,char ch){
        int times=0;
        for(int i=0;i<s.length();i++){
            if(ch == s.charAt(i))
                times++;
        }
        return times;
    }*/

}

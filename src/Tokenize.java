
import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;

import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.MapDictionary;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.InputSource;

public class Tokenize {

	static final double CHUNK_SCORE = 1.0;
	static StringBuilder result;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();

        boolean saveXML = false;
        MedlineParser parser = new MedlineParser(saveXML);
        result = new StringBuilder("<results>\n");
        CitationHandler handler = new CitationHandler(result);
        parser.setHandler(handler);
        for (String arg : args) {
            //System.out.println("Processing file=" + arg);
            if (arg.endsWith(".xml")) {
                InputSource inputSource = new InputSource(arg);
                parser.parse(inputSource);
        	} else {
        		throw new IllegalArgumentException("arguments must end with .xml");
        	}
        }
        result.append("</results>");
        System.out.println(result);
        System.out.println("time: " + (System.currentTimeMillis() - startTime)/1000.0);
	}

    static class CitationHandler implements MedlineHandler {
        long mCitationCount = 0L;
        ObjectToCounterMap<String> mCounter = new ObjectToCounterMap<String>();
        
        HashSet<String> stopSet = new HashSet<String>(),
        			geneSet = new HashSet<String>();

        String pmid;
        StringBuilder result;
        LowerCaseTokenizerFactory lcFactory;
        
        public CitationHandler(StringBuilder result) throws IOException{
        	 BufferedReader stopWords = new BufferedReader(new FileReader("english_stop_words.txt"));
        	 String line;
        	 while ((line = stopWords.readLine()) != null){
        		 if(line.equals(""))
        			 continue;
        		 stopSet.add(line);

        	 }
        	 stopWords.close();
        	 
        	 BufferedReader geneNames = new BufferedReader(new FileReader("human-genenames.txt"));
        	 //String line;
        	 while ((line = geneNames.readLine()) != null){
        		 if(line.length() == 0)
        			 continue;
        		 geneSet.add(line);
        	 }
        	 geneNames.close();
        	 
        	 //set up tokenizers
             //TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
        	 RegExTokenizerFactory factory = new RegExTokenizerFactory("\\S+");
             StopTokenizerFactory stopFactory = new StopTokenizerFactory(factory, stopSet);
             lcFactory = new LowerCaseTokenizerFactory(stopFactory);
        	 
        	 this.result = result;
        }
        
        public void handle(MedlineCitation citation) {
            ++mCitationCount;
            result.append("<MedlineCitation>\n");

            pmid = citation.pmid();
            result.append("<PMID>"+pmid+"</PMID>\n");
            // System.out.println("processing pmid=" + id);

            Article article = citation.article();
            String titleText = article.articleTitleText();
            result.append("<ArticleTitle>\n");
            addText(titleText);
            result.append("\n</ArticleTitle>\n");

            Abstract abstrct = article.abstrct();
            String abstractText;
            if (abstrct != null && !(abstractText = abstrct.textWithoutTruncationMarker()).equals(""))
            {
                result.append("<AbstractText>\n");
                addText(abstractText);
                result.append("\n</AbstractText>\n");
            }
            
            result.append("</MedlineCitation>\n");

            /*MeshHeading[] headings = citation.meshHeadings();
            for (MeshHeading heading : headings) {
                for (Topic topic : heading.topics()) { 
                    String topicText = topic.topic();
                    addText(topicText);
                }
            }*/
        }
        public void delete(String pmid) {
            throw new UnsupportedOperationException("not expecting deletes. found pmid=" + pmid);
        }
        public void addText(String text) {
        	StringBuilder localResult = new StringBuilder(text);
        	int numFoundGenes = 0;
            char[] cs = text.toCharArray();
                       
            Tokenizer tokenizer = lcFactory.tokenizer(cs,0,cs.length);
            for (String token : tokenizer) {
            	
                mCounter.increment(token);
                if(geneSet.contains(token))
                {
                	//System.out.println(token);
                	//System.out.println("start: " + tokenizer.lastTokenStartPosition());
                	localResult.insert(tokenizer.lastTokenStartPosition()+numFoundGenes*13, "<gene>");
                	localResult.insert(tokenizer.lastTokenEndPosition()+numFoundGenes*13+6, "</gene>");
                	numFoundGenes++;
                }
            }
            result.append(localResult);
        }
    }
}

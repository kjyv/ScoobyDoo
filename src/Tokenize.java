
import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;
import com.aliasi.lingmed.medline.parser.CommentOrCorrection;
import com.aliasi.lingmed.medline.parser.MeshHeading;
import com.aliasi.lingmed.medline.parser.Topic;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;

import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.MapDictionary;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.InputSource;

public class Tokenize {

	static final double CHUNK_SCORE = 1.0;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		/* possibility one: manually, maybe like this
		 * (read in xml without parser) */
		
		MapDictionary<String> dictionary = new MapDictionary<String>();
		
		//for each gene name do this
	    dictionary.addEntry(new DictionaryEntry<String>("50 Cent","PERSON",CHUNK_SCORE));
		
	    //create chunker (stop words?)
		ExactDictionaryChunker dictionaryChunkerTF
        = new ExactDictionaryChunker(dictionary,
                                     IndoEuropeanTokenizerFactory.INSTANCE,true,false);
		
		//then chunk our medline articles (per article?)
		String text = "50 cent is not in Medline";
        chunk(dictionaryChunkerTF,text);
        
        
        /* possibility two: use lingmed */
        
        boolean saveXML = false;
        MedlineParser parser = new MedlineParser(saveXML);
        CitationHandler handler = new CitationHandler();
        parser.setHandler(handler);
        for (String arg : args) {
            System.out.println("Processing file=" + arg);
            if (arg.endsWith(".xml")) {
                InputSource inputSource = new InputSource(arg);
                parser.parse(inputSource);
        	} else {
        		throw new IllegalArgumentException("arguments must end with .xml");
        	}
        }
        handler.report();
	}
	
    static void chunk(ExactDictionaryChunker chunker, String text) {
        System.out.println("\nChunker."
                           + " All matches=" + chunker.returnAllMatches()
                           + " Case sensitive=" + chunker.caseSensitive());
        Chunking chunking = chunker.chunk(text);
        for (Chunk chunk : chunking.chunkSet()) {
            int start = chunk.start();
            int end = chunk.end();
            String type = chunk.type();
            double score = chunk.score();
            String phrase = text.substring(start,end);
            System.out.println("     phrase=|" + phrase + "|"
                               + " start=" + start
                               + " end=" + end
                               + " type=" + type
                               + " score=" + score);
        }
    }

    static class CitationHandler implements MedlineHandler {
        long mCitationCount = 0L;
        ObjectToCounterMap<String> mCounter = new ObjectToCounterMap<String>();
        
        Set<String> stopSet = new HashSet();
        String pmid;
        
        public CitationHandler() throws IOException{
        	 BufferedReader stopWords = new BufferedReader(new FileReader("english_stop_words.txt"));
        	 String line;
        	 while ((line = stopWords.readLine()) != null){
        		 stopSet.add(line);
        	 }
        }
        
        public void handle(MedlineCitation citation) {
            ++mCitationCount;

            pmid = citation.pmid();
            // System.out.println("processing pmid=" + id);

            Article article = citation.article();
            String titleText = article.articleTitleText();
            addText(titleText);

            Abstract abstrct = article.abstrct();
            if (abstrct != null) {
                String abstractText = abstrct.textWithoutTruncationMarker();
                addText(abstractText);
            }

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
            char[] cs = text.toCharArray();
            
            //set up tokenizers
            TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
            StopTokenizerFactory stopFactory = new StopTokenizerFactory(factory, stopSet);
            LowerCaseTokenizerFactory lcFactory = new LowerCaseTokenizerFactory(stopFactory);
            
            Tokenizer tokenizer = lcFactory.tokenizer(cs,0,cs.length);
            for (String token : tokenizer) {
                mCounter.increment(token);
                //TODO: go through all our genes here and search for token.. slooow
            }
        }
        public void report() {
            System.out.println("\nCitation Count=" + mCitationCount);
            System.out.println("\nWord Counts");
            List<String> keysByCount = mCounter.keysOrderedByCountList();
            for (String key : keysByCount) {
                int count = mCounter.getCount(key);
                if (count < 10) break;
                System.out.printf("%9d %s\n",count,key);
            }
        }
    }
}

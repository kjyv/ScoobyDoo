
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
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;

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
    	//TODO: just copied from WordCountHandler for now, make me useful
        long mCitationCount = 0L;
        ObjectToCounterMap<String> mCorrexCounter = new ObjectToCounterMap<String>();
        ObjectToCounterMap<String> mCounter = new ObjectToCounterMap<String>();
        public void handle(MedlineCitation citation) {
            ++mCitationCount;

            for (CommentOrCorrection cc : citation.commentOrCorrections())
                mCorrexCounter.increment(cc.type());

            String id = citation.pmid();
            // System.out.println("processing pmid=" + id);

            Article article = citation.article();
            String titleText = article.articleTitleText();
            addText(titleText);

            Abstract abstrct = article.abstrct();
            if (abstrct != null) {
                String abstractText = abstrct.textWithoutTruncationMarker();
                addText(abstractText);
            }

            MeshHeading[] headings = citation.meshHeadings();
            for (MeshHeading heading : headings) {
                for (Topic topic : heading.topics()) { 
                    String topicText = topic.topic();
                    addText(topicText);
                }
            }
        }
        public void delete(String pmid) {
            throw new UnsupportedOperationException("not expecting deletes. found pmid=" + pmid);
        }
        public void addText(String text) {
            char[] cs = text.toCharArray();
            TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
            Tokenizer tokenizer = factory.tokenizer(cs,0,cs.length);
            for (String token : tokenizer) {
                mCounter.increment(token);
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
            System.out.println("\nCorrection Counts");
            List<String> ccKeysByCount = mCorrexCounter.keysOrderedByCountList();
            for (String ccKey : ccKeysByCount) {
                int count = mCorrexCounter.getCount(ccKey);
                System.out.printf("%9d %s\n",count,ccKey);
            }
        }
    }
}

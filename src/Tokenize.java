
import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;

import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.MapDictionary;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

/*import com.aliasi.dict.TrieDictionary;
import com.aliasi.dict.Dictionary;*/

public class Tokenize {

	static final double CHUNK_SCORE = 1.0;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MapDictionary<String> dictionary = new MapDictionary<String>();
	    dictionary.addEntry(new DictionaryEntry<String>("50 Cent","PERSON",CHUNK_SCORE));
		
		ExactDictionaryChunker dictionaryChunkerTT
        = new ExactDictionaryChunker(dictionary,
                                     IndoEuropeanTokenizerFactory.INSTANCE,
                                     true,true);
	}

}

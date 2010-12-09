import com.aliasi.lingmed.medline.parser.Article;
import com.aliasi.lingmed.medline.parser.Abstract;
import com.aliasi.lingmed.medline.parser.MedlineCitation;
import com.aliasi.lingmed.medline.parser.MedlineHandler;
import com.aliasi.lingmed.medline.parser.MedlineParser;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

		boolean saveXML = true;
		MedlineParser parser = new MedlineParser(saveXML);
		result = new StringBuilder("<results>\n");
		CitationHandler handler = new CitationHandler(result);
		parser.setHandler(handler);
		for (String arg : args) {
			if (arg.endsWith(".xml")) {
				InputSource inputSource = new InputSource(arg);
				parser.parse(inputSource);
			} else {
				throw new IllegalArgumentException(
						"arguments must end with .xml");
			}
		}
		result.append("</results>");
		System.out.println(result);
		System.err.println("genes tagged: " + handler.numFoundGenes);
		System.err.println("time: " + (System.currentTimeMillis() - startTime)
				/ 1000.0 + "s");
	}

	static class CitationHandler implements MedlineHandler {
		long numFoundGenes = 0L;

		HashSet<String> stopSet = new HashSet<String>(),
				geneSet = new HashSet<String>();

		String pmid;
		StringBuilder result;
		LowerCaseTokenizerFactory lcFactory;
		
		Pattern pGt;
		Pattern pLt;
		
		public CitationHandler(StringBuilder result) throws IOException {
			pGt = Pattern.compile("\\>", Pattern.DOTALL | Pattern.UNICODE_CASE);
			pLt = Pattern.compile("\\<", Pattern.DOTALL | Pattern.UNICODE_CASE);
			
			// read in stop words
			BufferedReader stopWords = new BufferedReader(new FileReader(
					"english_stop_words.txt"));
			String line;
			while ((line = stopWords.readLine()) != null) {
				if (line.equals(""))
					continue;
				stopSet.add(line);
			}
			stopWords.close();

			// read in tag names
			BufferedReader geneNames = new BufferedReader(new FileReader(
					"human-genenames.txt"));
			while ((line = geneNames.readLine()) != null) {
				if (line.length() == 0)
					continue;
				geneSet.add(line);
			}
			geneNames.close();

			// set up tokenizers
			TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;

			// this could be improved by using whitespace tokenizer and also add
			// tokens
			// which are substrings of tokens splitting on / and -
			// we would need a new tokenizer for that though
			// RegExTokenizerFactory factory = new
			// RegExTokenizerFactory("\\S+");
			// ...

			StopTokenizerFactory stopFactory = new StopTokenizerFactory(
					factory, stopSet);
			lcFactory = new LowerCaseTokenizerFactory(stopFactory);

			this.result = result;
		}

		public void handle(MedlineCitation citation) {
			result.append("<MedlineCitation>\n");

			pmid = citation.pmid();
			result.append("<PMID>" + pmid + "</PMID>\n");
			// System.out.println("processing pmid=" + id);

			Article article = citation.article();
			String titleText = article.articleTitleText();
			result.append("<ArticleTitle>\n");
			addText(titleText);
			result.append("\n</ArticleTitle>\n");

			Abstract abstrct = article.abstrct();
			String abstractText;
			if (abstrct != null
					&& !(abstractText = abstrct.textWithoutTruncationMarker())
							.equals("")) {
				result.append("<AbstractText>\n");
				addText(abstractText);
				result.append("\n</AbstractText>\n");
			}

			result.append("</MedlineCitation>\n");
		}

		public void delete(String pmid) {
			throw new UnsupportedOperationException(
					"not expecting deletes. found pmid=" + pmid);
		}

		public void addText(String text) {
			/*Matcher m = pGt.matcher(text);
			m.replaceAll("&gt;");
			m = pLt.matcher(text);
			m.replaceAll("&lt;");*/
			text = text.replaceAll(">", "&gt;").replaceAll("<", "&gt;");
			
			StringBuilder localResult = new StringBuilder(text);
			int numFoundGenes = 0;
			char[] cs = text.toCharArray();

			Tokenizer tokenizer = lcFactory.tokenizer(cs, 0, cs.length);
			for (String token : tokenizer) {
				if (geneSet.contains(token)) {
					// System.out.println(token);
					// System.out.println("start: " +
					// tokenizer.lastTokenStartPosition());
					localResult.insert(tokenizer.lastTokenStartPosition()
							+ numFoundGenes * 13, "<gene>");
					localResult.insert(tokenizer.lastTokenEndPosition()
							+ numFoundGenes * 13 + 6, "</gene>");
					numFoundGenes++;
				}
			}
			this.numFoundGenes += numFoundGenes;
			result.append(localResult);
		}
	}
}

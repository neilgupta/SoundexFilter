package com.metamorphium.lucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;


/**
 *
 * @author Neil Gupta
 *
 * This is mostly boiler-plate code taken from StandardAnalyzer in Lucene 3.0.3 and
 * modified to use my SoundexFilter.
 */

/**
 * Filters {@link StandardTokenizer} with {@link StandardFilter}, {@link
 * LowerCaseFilter}, {@link StopFilter}, and {@link SoundexFilter} using a list of
 * English stop words.
 *
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating SoundexAnalyzer:
 * <ul>
 *   <li> As of 2.9, StopFilter preserves position
 *        increments
 *   <li> As of 2.4, Tokens incorrectly identified as acronyms
 *        are corrected (see <a href="https://issues.apache.org/jira/browse/LUCENE-1068">LUCENE-1608</a>
 * </ul>
 */

public class SoundexAnalyzer extends Analyzer {
  private Set<?> stopSet;

  /**
   * Specifies whether deprecated acronyms should be replaced with HOST type.
   * See {@linkplain https://issues.apache.org/jira/browse/LUCENE-1068}
   */
  private final boolean replaceInvalidAcronym,enableStopPositionIncrements;

  /** An unmodifiable set containing some common English words that are usually not
  useful for searching. */
  public static final Set<?> STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
  private final Version matchVersion;

  /** Builds an analyzer with the default stop words ({@link
   * #STOP_WORDS_SET}).
   * @param matchVersion Lucene version to match See {@link
   * <a href="#version">above</a>}
   */
  public SoundexAnalyzer(Version matchVersion) {
    this(matchVersion, STOP_WORDS_SET);
  }

  /** Builds an analyzer with the given stop words.
   * @param matchVersion Lucene version to match See {@link
   * <a href="#version">above</a>}
   * @param stopWords stop words */
  public SoundexAnalyzer(Version matchVersion, Set<?> stopWords) {
    stopSet = stopWords;
    setOverridesTokenStreamMethod(SoundexAnalyzer.class);
    enableStopPositionIncrements = StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion);
    replaceInvalidAcronym = matchVersion.onOrAfter(Version.LUCENE_24);
    this.matchVersion = matchVersion;
  }

  /** Builds an analyzer with the stop words from the given file.
   * @see WordlistLoader#getWordSet(File)
   * @param matchVersion Lucene version to match See {@link
   * <a href="#version">above</a>}
   * @param stopwords File to read stop words from */
  public SoundexAnalyzer(Version matchVersion, File stopwords) throws IOException {
    this(matchVersion, WordlistLoader.getWordSet(stopwords));
  }

  /** Builds an analyzer with the stop words from the given reader.
   * @see WordlistLoader#getWordSet(Reader)
   * @param matchVersion Lucene version to match See {@link
   * <a href="#version">above</a>}
   * @param stopwords Reader to read stop words from */
  public SoundexAnalyzer(Version matchVersion, Reader stopwords) throws IOException {
    this(matchVersion, WordlistLoader.getWordSet(stopwords));
  }

  /** Constructs a {@link StandardTokenizer} filtered by a {@link
  StandardFilter}, a {@link LowerCaseFilter}, a {@link StopFilter}, and a {@link SoundexFilter}. */
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    StandardTokenizer tokenStream = new StandardTokenizer(matchVersion, reader);
    tokenStream.setMaxTokenLength(maxTokenLength);
    TokenStream result = new StandardFilter(tokenStream);
    result = new LowerCaseFilter(result);
    result = new StopFilter(enableStopPositionIncrements, result, stopSet);
    result = new SoundexFilter(result);
    return result;
  }

  private static final class SavedStreams {
    StandardTokenizer tokenStream;
    TokenStream filteredTokenStream;
  }

  /** Default maximum allowed token length */
  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

  /**
   * Set maximum allowed token length.  If a token is seen
   * that exceeds this length then it is discarded.  This
   * setting only takes effect the next time tokenStream or
   * reusableTokenStream is called.
   */
  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }

  /**
   * @see #setMaxTokenLength
   */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }

  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    if (overridesTokenStreamMethod) {
      // LUCENE-1678: force fallback to tokenStream() if we
      // have been subclassed and that subclass overrides
      // tokenStream but not reusableTokenStream
      return tokenStream(fieldName, reader);
    }
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      setPreviousTokenStream(streams);
      streams.tokenStream = new StandardTokenizer(matchVersion, reader);
      streams.filteredTokenStream = new StandardFilter(streams.tokenStream);
      streams.filteredTokenStream = new LowerCaseFilter(streams.filteredTokenStream);
      streams.filteredTokenStream = new StopFilter(enableStopPositionIncrements,
                                                   streams.filteredTokenStream, stopSet);
      streams.filteredTokenStream = new SoundexFilter(streams.filteredTokenStream);
    } else {
      streams.tokenStream.reset(reader);
    }
    streams.tokenStream.setMaxTokenLength(maxTokenLength);

    streams.tokenStream.setReplaceInvalidAcronym(replaceInvalidAcronym);

    return streams.filteredTokenStream;
  }
}
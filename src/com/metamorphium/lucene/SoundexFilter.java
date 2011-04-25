package com.metamorphium.lucene;

import java.io.IOException;
import java.util.HashMap;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 *
 * @author Neil Gupta
 *
 * This is a simple Soundex algorithm implementation for Lucene that converts letters to their
 * respective digits, treats unrecognized characters as 0, and ignores digits entirely.
 *
 */
public final class SoundexFilter extends TokenFilter {

  private final HashMap<Character, Character> letterMap = new HashMap<Character, Character>();

  public SoundexFilter(TokenStream in) {
    super(in);
    // create map of characters to digits for soundex filtering
    letterMap.put('a', '0');
    letterMap.put('e', '0');
    letterMap.put('i', '0');
    letterMap.put('o', '0');
    letterMap.put('u', '0');
    letterMap.put('h', '0');
    letterMap.put('w', '0');
    letterMap.put('y', '0');
    letterMap.put('b', '1');
    letterMap.put('f', '1');
    letterMap.put('p', '1');
    letterMap.put('v', '1');
    letterMap.put('c', '2');
    letterMap.put('g', '2');
    letterMap.put('j', '2');
    letterMap.put('k', '2');
    letterMap.put('q', '2');
    letterMap.put('s', '2');
    letterMap.put('x', '2');
    letterMap.put('z', '2');
    letterMap.put('d', '3');
    letterMap.put('t', '3');
    letterMap.put('l', '4');
    letterMap.put('m', '5');
    letterMap.put('n', '5');
    letterMap.put('r', '6');
    termAtt = addAttribute(TermAttribute.class);
  }

  private TermAttribute termAtt;

  @Override
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {

      final char[] buffer = termAtt.termBuffer();
      final int length = termAtt.termLength();
      for(int t = 0; t<length; t++) {
          if(Character.isDigit(buffer[t]))
              return true; // ignore any digits
      }

      StringBuilder temp = new StringBuilder(length+3); // max size can be length of token + 3 0's
      temp.append(buffer[0]); // 1. add first letter of token to temp

      for(int i=1;i<length;i++)
         if(letterMap.get(buffer[i]) != null)
            temp.append(letterMap.get(buffer[i])); // 2 and 3. change letters to digits
         else
            temp.append('0');   // default to 0 for unrecognized characters

      if(temp.length() > 2) {
          // 4. remove all consecutive identical digits
          int k = 2;
          while(k<temp.length()) {
            if(temp.charAt(k) == temp.charAt(k-1))
               temp.deleteCharAt(k);
            else
               k++;
          }

          // 5. remove all instances of 0 from string
          k = 1;
          while(k<temp.length()) {
            if(temp.charAt(k) == '0')
               temp.deleteCharAt(k);
            else
               k++;
          }
      }
  
      // pad with 0 just in case string is not long enough
      temp.append('0');
      temp.append('0');
      temp.append('0');

      // update buffer with new term, truncate string to only first 4 chars
      termAtt.setTermBuffer(temp.substring(0, 4));

      return true;
    } else
      return false;
  }
}
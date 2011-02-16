package org.lockss.exporter.kbart;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;
import org.lockss.util.Logger;
import org.lockss.util.RegexpUtil;
import org.lockss.util.StringUtil;

/**
 * Sort a set of strings alphanumerically. The alphanumeric ordering tokenizes each 
 * string into number and non-number sections, and then performs a sequential pair-wise comparison on 
 * each token; alphabetically for text, and by magnitude for numbers, until a 
 * difference is found. As a last resort, natural string ordering is used.
 * <p>
 * By default the alphabetic comparison is case sensitive; an alternative constructor provides
 * the option to specify case-sensitivity.
 * <p>
 * The class can be extended, specifying a type for the parameter T, and that type will be compared 
 * based on its string value. If a different string should be used in comparison, this can be specified
 * by overriding the <code>getComparisonString()</code> method.
 *	
 * @author neil
 * @param <T>
 */
public class AlphanumericComparator<T> implements Comparator<T>  {

  private static Logger log = Logger.getLogger("AlphanumericComparator");
  private static final String numberRegex = "\\d+";
  private static final String nonNumberRegex = "[^\\d]+";
  //private static final Pattern number = RegexpUtil.uncheckedCompile(numberRegex);
  //private static final Pattern nonNumber = RegexpUtil.uncheckedCompile(nonNumberRegex);
  
  //private static final Pattern numWordBoundary = RegexpUtil.uncheckedCompile("(?=\\d[^\\d]|[^\\d]\\d)");
  private static final Pattern numOrNonNum = RegexpUtil.uncheckedCompile("(\\d+|[^\\d]+)");
  private static final Pattern numAtStart = RegexpUtil.uncheckedCompile("^"+numberRegex);
  
  private static final Perl5Matcher matcher = RegexpUtil.getMatcher();

  /** Default case-sensitivity for instances of this comparator. May be overridden in the constructor. */
  private static final boolean CASE_SENSITIVE_DEFAULT = true;

  /** Whether this comparator compares case-sensitively. */
  private boolean caseSensitive;

  // Tokenisation of string 1
  private AlphanumericTokenisation strTok1;
  // Tokenisation of string 2
  private AlphanumericTokenisation strTok2;

  /**
   * Construct a default comparator whose alphabetical comparisons are performed case-sensitively.
   */
  public AlphanumericComparator() {
    this(CASE_SENSITIVE_DEFAULT);
  }
    
  /**
   * Construct a comparator whose case-sensitivity is defined by the argument.
   * 
   * @param caseSensitive
   */
  public AlphanumericComparator(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * Get the string value of a parameterized object. This method can be overridden by subclasses 
   * to order objects by comparison of particular properties. The default is to return the result 
   * of the <code>toString()</code> method for comparison.
   * 
   * @param obj object whose string value to get 
   * @return the comparable string value of this object
   */  
  protected String getComparisonString(T obj) {
    return obj.toString();
  }
  
  /**
   * Compare using natural string ordering. This method encapsulates the case-sensitivity aspect of the comparison.
   * 
   * @param str1
   * @param str2
   * @return
   */
  private int compareStrings(String str1, String str2) {
    int res = caseSensitive ? str1.compareTo(str2) : str1.toLowerCase().compareTo(str2.toLowerCase()); 
    //System.out.printf("[%b] %s %s %s\n", caseSensitive, str1, (res>0?">":res<0?"<":"="), str2);
    //return res > 0 ? 1 : res < 0 ? -1 : 0; 
    return res;
  }
  
  @Override
  public int compare(T obj1, T obj2) {
    String str1 = getComparisonString(obj1);
    String str2 = getComparisonString(obj2);
    
    // If there is an empty string, just use natural ordering (empty should come first)
    if (StringUtil.isNullString(str1) || StringUtil.isNullString(str2)) {
      // Use natural ordering      
      return compareStrings(str1, str2);
    }

    // If only one of the strings starts with a number, that string should come first; no tokenisation required. 
    boolean numFirstStr1 = matcher.contains(str1, numAtStart);
    boolean numFirstStr2 = matcher.contains(str2, numAtStart);
    if (numFirstStr1 && !numFirstStr2) return -1;
    if (!numFirstStr1 && numFirstStr2) return 1;
    
    this.strTok1 = AlphanumericTokenisation.tokenise(str1);
    this.strTok2 = AlphanumericTokenisation.tokenise(str2);
    
    // By this point the tokenisations should start with the same type - set a flag to indicate which
    if (strTok1.numFirst != strTok2.numFirst) {
      // This should not happen
      //throw new RuntimeException("AlphanumericComparator is misbehaving.");
      log.warning(String.format("Invalid state: strings start with different types of token but have been not been recognised as such (%s, %s).\n", str1, str2));
      // The effect of continuing should be that every pair gets compared as strings, but we explicitly return a string comparison at this stage
      return compareStrings(str1, str2);
    }
    
    boolean numFirst = strTok1.numFirst && strTok2.numFirst;
    
    // Check for empty arrays
    if (strTok1.numTokens()<=0 || strTok2.numTokens()<=0) {
      log.warning(String.format("Could not tokenise '%s' and '%s'\n", str1, str2));
      // Use natural ordering      
      return compareStrings(str1, str2);
    }

    // For each pair of number or text tokens, do a pairwise comparison, casting to number if possible
    try {
      int result = 0;
      // Number of token pairs
      int numPairs = Math.min(strTok1.numTokens(), strTok2.numTokens());
      // Get a pair of text or num tokens for each string and compare them
      for (int i=0; i<numPairs; i++) {
	String tok1 = strTok1.tokens.get(i);
	String tok2 = strTok2.tokens.get(i);
	// If these are number tokens (numFirst and even index, or text first and odd index)
	if (numFirst == (i%2==0)) {
	  result = compareNumberTokens(tok1, tok2);
	} else {
	  result = compareTextTokens(i, tok1, tok2);
	}
	// If this pair of tokens has produced a difference, return it
	if (result!=0) return result;
      }
    } catch (Exception e) {
      // There was some problem parsing the strings; just return a natural ordering.
      log.warning(String.format("Could not compare strings (%s, %s).\n", str1, str2), e);
    }
    // Alphabetical (natural string) ordering by default
    return compareStrings(str1, str2);
  }


  /**
   * Parse tokens as numbers if possible and compare by magnitude; otherwise fall back to text comparison.
   * @param tok1 first token
   * @param tok2 second token
   * @return
   */
  protected int compareNumberTokens(String tok1, String tok2) {
    try {
      // Parse number tokens as integers
      Integer i1 = new Integer(tok1);
      Integer i2 = new Integer(tok2);
      return i1.compareTo(i2);
    } catch (NumberFormatException e) {
      log.warning(String.format("Could not compare as numbers: '%s' and '%s'\n", tok1, tok2));
      // Compare as text
      return compareStrings(tok1, tok2);
    }
  }
  
  /**
   * Compare tokens as text - case-insensitively if required. Note that the tokens will be 
   * lower-cased if necessary.
   * 
   * @param tokIndex the index of the token pair (in the token lists)
   * @param tok1 first token
   * @param tok2 second token
   * @return
   */
  private int compareTextTokens(int tokIndex, String tok1, String tok2) {
    if (!caseSensitive) {
      tok1 = tok1.toLowerCase();
      tok2 = tok2.toLowerCase(); 
    }

    // -------------------------------------------------------------------------------------------
    // NOTE
    // During comparison of a pair of text tokens, natural string ordering is appropriate, 
    // *except* where the longer token starts with the shorter one followed by a space, and 
    // the shorter is not the last token (i.e. there is a numerical one after it).
    // For example, "Green Apples" should be sorted before "Green100", but the tokenisation 
    // will produce the following comparisons:
    // "Green" < "Green Apples"
    // "100"   > ""
    // .. leading to "Green100" being sorted first because of the shorter first token it produces.
    // If there was no space, i.e. the first title is "GreenApples" then we get the correct result.
    // -------------------------------------------------------------------------------------------
    if (tok1.length()!=tok2.length()) {
      if (tok1.startsWith(tok2+" ") && strTok2.numTokens()-1 > tokIndex) {
	//System.out.printf("Putting token '%s' before '%s'\n", tok1, tok2);
	return -1;
      } else if (tok2.startsWith(tok1+" ") && strTok1.numTokens()-1 > tokIndex) {
	//System.out.printf("Putting token '%s' before '%s'\n", tok2, tok1);
	return 1;
      }
    }
    // Basic string compare by default 
    return compareStrings(tok1, tok2);
  }
  

  /**
   * A class to represent the tokenisation of a string into number and non-number tokens.
   * Provides access to the tokens, the original string, and a boolean indicating whether 
   * the first token is numerical.
   * The list of tokens should consist of alternating numbers and text.
   *  
   * @author Neil Mayo
   *
   */
  protected static class AlphanumericTokenisation {
    
    String originalString;
    List<String> tokens;
    boolean numFirst;
    
    public AlphanumericTokenisation(String str) {
      this.originalString = str;
      this.tokens = new Vector<String>();
      this.numFirst = matcher.contains(str, numAtStart);

      // Tokenise the string
      PatternMatcherInput input = new PatternMatcherInput(str);
      while (matcher.contains(input, numOrNonNum)) {
        tokens.add(matcher.getMatch().group(0));
      }
    }

    public static AlphanumericTokenisation tokenize(String str) { return tokenise(str); }
    public static AlphanumericTokenisation tokenise(String str) {
      return new AlphanumericTokenisation(str); 
    }
    
    public int numTokens() {
      return tokens.size(); 
    }
  }
  
  
}


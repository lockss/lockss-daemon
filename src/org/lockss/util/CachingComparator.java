/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.util;
import java.util.*;
import static org.lockss.util.CachingComparator.NormalisationOption.*;

/**
 * Comparator that translates sort keys and caches them, so performance will 
 * be enhanced by reusing the same instance of the comparator.  
 * The {@link getSingleton()} method is provided for that purpose, and can be
 * overridden/redeclared by subclasses wishing to provide their own singleton.
 * For some classes it makes sense to disallow singletons as caching would be 
 * too expensive; in this case they need do nothing as the default behaviour 
 * of getSingleton() is to throw an {@link UnsupportedOperationException}.  
 * <p>
 * In fact it is only necessary to maintain a single caching singleton per
 * normalisation policy (represented by the boolean {@link NormalisationOption}
 * switches). This is enough to keep the different normalisations in separate 
 * key maps. However these would need to be managed as static instances by 
 * this class and made available to subclasses based on their normalisation 
 * policies. Currently this is not implemented as it may not be appropriate to 
 * keep such instances around if they generate a very large sort key map and 
 * are used infrequently.
 * <p>
 * Singletons should therefore be managed by subclasses according to their 
 * needs.
 * <p>
 * This comparator only compares based on String values, and does not make  
 * use of <code>equals()</code> on non-String objects, meaning it is not 
 * <i>consistent with equals()</i>. This means that the comparator is 
 * unsuitable for providing the ordering in a SortedSet or SortedMap.
 * <p>
 * This class can be extended, specifying a type for the 
 * parameter T, and that type will be compared based on its string value. 
 * If a different string should be used in comparison, this can be specified
 * by overriding the <code>getComparisonString()</code> method.
 * <p>
 * There are options available to perform case-sensitive alphabetic comparison, 
 * to translate accented characters to their unaccented counterparts before 
 * comparison, and to remove initial determiners from the comparison string. 
 * By default the maximum amount of normalisation is performed, though 
 * subclasses may wish to override this.
 * An alternative constructor is provided to allow the construction of an
 * instance with full default normalisation plus a specific approach to 
 * case.
 * <p>
 * Note that setting different normalisation policies, and operating on 
 * different comparison strings, implies different behaviour in the comparator,
 * and therefore a keyMap with different values cached per key. With mixed 
 * normalisation policies in the same comparator, the key map would get 
 * polluted with conflicting normalisations. For this reason it is not 
 * possible to override the comparison options after construction.
 * 
 * @param <T> the type being compared
 */
public class CachingComparator<T> implements Comparator<T> {
  static final String PUNCTUATION = ".,-:;\"\'/?()[]{}<>!#";
  static final int PADLEN = 9;

  /**
   * Enumeration of the boolean options available for affecting the way sort 
   * key strings are normalised. Each option has a default enabled setting.
   * By default, full normalisation is performed.
   */
  public static enum NormalisationOption {
    CASE_SENSITIVE(false),
    TRANSLATE_ACCENTS(true), 
    REMOVE_INITIAL_DETERMINERS(true);
    private NormalisationOption(boolean on) { this.defaultEnabled = on; }
    public boolean defaultEnabled;
  }
  
  /** 
   * A map of all the normalisation switches for this instance. This
   * should not be modifiable; use Collections.unmodifiableMap()
   * when assigning the map.  
   */
  protected final Map<NormalisationOption, Boolean> normalisationOptions;
  
  /**
   * A method to get a singleton. Subclasses should override this if they want 
   * to provide access to a singleton. By default an 
   * {@link UnsupportedOperationException} is thrown. The exception is not
   * declared in the method signature as clients of the method should know 
   * whether the method is implemented for their usage. If it is not, a
   * runtime error should be thrown during testing. 
   */
  public static <T> CachingComparator<T> getSingleton() {
    throw new UnsupportedOperationException(
	"The getSingleton method should be overridden by subclasses."
    ); 
  }

  /** 
   * This class instance's sort key cache. This maps String to normalised 
   * String despite the generic interface. It might be expected that it
   * would map T to String, but it is not uncommon to get the same comparison 
   * string from different types of object, therefore the caching will be more 
   * useful if we use comparison strings as the key. 
   */
  Map<String,String> keyMap = new HashMap<String,String>();

  /**
   * Generate a map containing all normalisation options set to their defaults.
   * @return
   */
  static Map<NormalisationOption, Boolean> generateDefaultNormalisationOptions() {
    return new HashMap<NormalisationOption, Boolean>() {{
      for (NormalisationOption no : NormalisationOption.values())
	put(no, no.defaultEnabled);
    }};
  }

  /**
   * Create a CachingComparator with default normalisation options.
   */
  public CachingComparator() {
     this(generateDefaultNormalisationOptions());
  }

  /**
   * Create a CachingComparator with the supplied normalisation options.
   * Any options which are not included will be set to their defaults.
   * @param options a map of normalisation switches to boolean values
   */
  public CachingComparator(Map<NormalisationOption, Boolean> options) {
    // First create a map of default options
    Map<NormalisationOption, Boolean> opts = generateDefaultNormalisationOptions();
    // Override with supplied options
    //for (NormalisationOption no : options.keySet()) opts.put(no, opts.get(no));
    opts.putAll(options);
    // Assign the options as an unmodifiable map
    this.normalisationOptions = Collections.unmodifiableMap(opts);
  }
  
  /**
   * Create a CachingComparator with specified case sensitivity and 
   * default accent translation and determiner processing.
   * @param caseSensitive whether to compare strings case-sensitively
   */
  public CachingComparator(final boolean caseSensitive) {
    this(new HashMap<NormalisationOption, Boolean>() {{
      put(CASE_SENSITIVE, caseSensitive);
    }});
  }
  

  /**
   * We explicitly check the types being compared so that we can throw a 
   * ClassCastException if they differ. This is to account for the fact
   * that such an exception is not thrown when a parameterised instance 
   * of the Comparator is passed to Collections.sort() along with a 
   * Collection containing mixed types.
   * <p>
   * This method does not impose an ordering consistent with equals.
   */
  @Override
  public final int compare(T o1, T o2) {
    // Return 0 if the objects are equal
    if (o1 == o2) return 0;

    // Don't allow null to cause NPE
    if (o1 == null) {
      return (o2 == null ? 0 : -1);
    } else if (o2 == null) {
      return 1;
    }
    // By here it is possible for the objects to be of different types 
    // (or Object) even if the list being sorted has been parameterised 
    // as something more specific.
    if (!o1.getClass().equals(o2.getClass())) 
      throw new ClassCastException("Objects are of different types.");

    // Do the proper comparison on cached keys
    String sk1 = getSortKey(o1);
    String sk2 = getSortKey(o2);
    // Set result based on sort string comparison
    return normalisationOptions.get(CASE_SENSITIVE) ? 
	sk1.compareTo(sk2) : sk1.compareToIgnoreCase(sk2);
  }

  /**
   * Get the string value of a parameterized object. This method can be 
   * overridden by subclasses to order objects by comparison of particular 
   * properties. The default is to return the result of the 
   * <code>toString()</code> method for comparison. This method should not 
   * return a null string, but instead should return the empty string.
   * 
   * @param obj object whose string value to get 
   * @return the comparable string value of this object, or the empty string
   */  
  protected String getComparisonString(T obj) {
    return obj.toString();
  }
  
  synchronized final String getSortKey(T o) {
    String s = getComparisonString(o);
    String key = keyMap.get(s);
    if (key == null) {
      key = xlate(s);
      keyMap.put(s, key);
    }
    return key;
  }

  /**
   * The translate method may be overriden by subclasses to 
   * provide behaviour. 
   * @param s the original string for translation
   * @return the string translated according to the options that are in effect
   */
  protected String xlate(String s) {
    s = s.trim();
    s = padNumbers(s, PADLEN);  
    s = deleteAll(s, PUNCTUATION);
    s = deleteSpaceBetweenInitials(s);
    if (normalisationOptions.get(REMOVE_INITIAL_DETERMINERS))
      s = removeInitialDeterminers(s); 
    if (normalisationOptions.get(TRANSLATE_ACCENTS)) 
      s = replaceAccentedChar(s);
    return s;
  }
  
  String removeInitialDeterminers(String s) {
    // Note that deleteInitial() is case-insensitive
    s = deleteInitial(s, "a");
    s = deleteInitial(s, "an");
    s = deleteInitial(s, "the");
    return s; 
  }
  
  String padNumbers(String s, int padLen) {
    int len = s.length();
    StringBuilder sb = new StringBuilder(len + padLen - 1);
    int ix = 0;
    while (ix < len) {
      char ch = s.charAt(ix);
      if (Character.isDigit(ch)) {
	int jx = ix;
	while (++jx < len) {
	  if (!Character.isDigit(s.charAt(jx))) {
	    break;
	  }
	}
	// jx now points one beyond end of number (or end of string)
	for (int padix = padLen - (jx - ix); padix > 0; padix--) {
	  sb.append('0');
	}
	do {
	  sb.append(s.charAt(ix++));
	} while (ix < jx);
      } else {
	sb.append(ch);
	ix++;
      }
    }
    return sb.toString();
  }

  String deleteInitial(String s, String sub) {
    int sublen = sub.length();
    if (StringUtil.startsWithIgnoreCase(s, sub) &&
	s.length() > sublen &&
	Character.isWhitespace(s.charAt(sublen))) {
      s = s.substring(sublen + 1, s.length());
      s = s.trim();
    }
    return s;
  }

  // assume all the letter in the abbreviation are in uppercase
  // The method will also turn consecutive whitespace in to one whitespace
  String deleteSpaceBetweenInitials(String s) {
    boolean deleteStart = false;
    StringBuffer sTmp = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s," ");
    while (st.hasMoreTokens()) {
	String token = st.nextToken();
        //check the token is a single uppercase character
	if (token.length() == 1 &&
            Character.isUpperCase(token.charAt(0))) {
	    if (deleteStart) {
	      sTmp = sTmp.append(token);
            }
	    else { //when encounter the first Letter in the abbreviation
	      sTmp = sTmp.append(" ").append(token);
	      deleteStart = true;
	    }
	}
	else {
	  sTmp = sTmp.append(" ").append(token);
	  deleteStart = false;
	}
    }
    return sTmp.toString().trim();
  }

  String deleteAll(String s, String chars) {
    for (int ix = 0; ix < chars.length(); ix++) {
      String c = chars.substring(ix, ix + 1);
      s = StringUtil.replaceString(s, c, "");
    }
    return s;
  }

  /**
   * This method now delegates to the {@link StringUtil} toUnaccented() method.
   * 
   * @param s the string to convert
   * @return string with accented characters converted to unaccented equivalents
   */
  String replaceAccentedChar(String s) {
    return StringUtil.toUnaccented(s);
  }
  
}

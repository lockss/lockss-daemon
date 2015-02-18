/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.biblio;

import java.util.*;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.oro.text.regex.*;
import org.lockss.util.*;

/**
 * Utility methods for {@link BibliographicItem}s and their data.
 * 
 * @author Neil Mayo
 */
public class BibliographicUtil {

  /**
   * Range sets can contain ranges delimited by either comma or semicolon.
   * Multiple separators are ignored.
   */
  private static final Pattern rangeSetDelimiterPattern =
      RegexpUtil.uncheckedCompile(
          "\\s*[,;]+\\s*"
      );

  /**
   * Match a substring which consists of all digits, all letters or all
   * non-digits and non-letters. Iteration of matches on this pattern should
   * match every part of a string, leading to a full tokenisation of a string.
   */
  private static final Pattern numOrNonNum = RegexpUtil.uncheckedCompile(
      "(\\d+|[a-zA-Z]+|[^\\da-zA-Z]+)"
  );

  /**
   * Match a block of letters that could be a Roman number. This pattern omits
   * 'N' which might be used on its own to represent 0.
   */
  private static final Pattern upperRomanNumerals = RegexpUtil.uncheckedCompile(
      "(MDCLXVI+)"
  );

  /**
   * Match a number at the start of a string.
   */
  private static final Pattern numAtStart = RegexpUtil.uncheckedCompile("^\\d+");

  /** Pattern for characters considered to delimit tokens in a volume string.
   * Matches whitespace surrounding the delimiters. Includes the transition between cases. */
  private static final Pattern VOL_TOK_DELIM_PTN =
      RegexpUtil.uncheckedCompile("\\s*[.,-:;\"\'/?()\\[\\]{}<>!#]\\s*");
  /*private static final Pattern VOL_TOK_DELIM_PTN =
      RegexpUtil.uncheckedCompile(
          "\\s*(?:[.,-:;\"\'/?()\\[\\]{}<>!#]|(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[a-z]))\\s*"
      );*/


  /**
   * Sort a set of {@link BibliographicItem} objects for a single title by
   * start date, volume and finally name. There is no guaranteed way to order
   * chronologically due to the frequent problem of missing or inconsistent
   * metadata. This sorting is provided as a second choice when volume ordering
   * fails to provide a consistent enough sequence of years or volumes. This
   * often happens if the formats occurring in the volume field are mixed - see
   * in particular BMJ.
   * 
   * @param items a list of <code>BibliographicItem</code>s
   */
  public static void sortByYearVolume(List<? extends BibliographicItem> items) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(BibliographicComparatorFactory.getStartDateComparator());
    cc.addComparator(BibliographicComparatorFactory.getVolumeComparator());
    cc.addComparator(BibliographicComparatorFactory.getNameComparator());
    Collections.sort(items, cc);
  }

  /**
   * Sort a set of {@link BibliographicItem} objects for a single title by
   * volume, start date and finally name. There is no guaranteed way to order
   * chronologically due to the frequent problem of missing or inconsistent
   * metadata, but in general sorting by volumes tends to give the most correct
   * ordering.
   *
   * @param items a list of <code>BibliographicItem</code>s
   */
  public static void sortByVolumeYear(List<? extends BibliographicItem> items) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(BibliographicComparatorFactory.getVolumeComparator());
    cc.addComparator(BibliographicComparatorFactory.getStartDateComparator());
    cc.addComparator(BibliographicComparatorFactory.getNameComparator());
    Collections.sort(items, cc);
  }

  /**
   * Sort a set of {@link BibliographicItem} objects for a single title by
   * issn, name, volume, and finally start date. This yields an ordering that
   * should be appropriate for identifying title changes.
   *
   * @param items a list of <code>BibliographicItem</code>s
   */
  public static void sortByIdentifiers(List<? extends BibliographicItem> items) {
    ComparatorChain cc = new ComparatorChain();
    cc.addComparator(BibliographicComparatorFactory.getIssnComparator());
    cc.addComparator(BibliographicComparatorFactory.getNameComparator());
    cc.addComparator(BibliographicComparatorFactory.getVolumeComparator());
    cc.addComparator(BibliographicComparatorFactory.getStartDateComparator());
    Collections.sort(items, cc);
  }

  /**
   * Check whether two <code>BibliographicItem</code>s are equivalent, that is
   * they share the same values for their primary fields. The fields that are
   * checked are:
   * <code>year</code>, <code>volume</code>, <code>name</code>,
   * <code>journalTitle</code>,<code>issn</code> and <code>isbn</code>.
   *
   * @param item1 a BibliographicItem
   * @param item2 another BibliographicItem
   * @return <code>true</code> if they have equivalent primary fields; false if any differ or either item is null
   */
  public static boolean areEquivalent(BibliographicItem item1,
                                      BibliographicItem item2) {
    if (item1==null || item2==null) return false;
    return
        StringUtil.equalStrings(item1.getPrintIssn(), item2.getPrintIssn()) &&
            StringUtil.equalStrings(item1.getYear(), item2.getYear()) &&
            StringUtil.equalStrings(item1.getPublicationTitle(), 
                                    item2.getPublicationTitle()) &&
            StringUtil.equalStrings(item1.getName(), item2.getName()) &&
            StringUtil.equalStrings(
                BibliographicUtil.normaliseIdentifier(item1.getVolume()),
                BibliographicUtil.normaliseIdentifier(item2.getVolume())
            ); // Normalise the volumes before comparison
  }

  /**
   * Compares two <code>BibliographicItem</code>s to see if they appear to come
   * from the same title, by comparing their identifying fields. If the ISSNs,
   * eISSNs, ISBNs, eISBNs, ISSN-Ls, journal titles and publishers all match,
   * returns <code>true</code>.
   * <p>
   * Note that matching values can both be null, so even BibliographicItems
   * with no field values will be considered to be from the same title. This
   * may not be desirable.
   * Note also that the method specifically checks the ISSNs and ISBNs,,
   * not the best "best available info" methods such as getIssn() and getIsbn().
   *
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if they have the same issn and journal title
   */
  public static boolean areFromSameTitle(BibliographicItem au1, BibliographicItem au2) {
    return StringUtil.equalStrings(au1.getPrintIssn(), au2.getPrintIssn())
        && StringUtil.equalStrings(au1.getPrintIsbn(), au2.getPrintIsbn())
        && StringUtil.equalStrings(au1.getEissn(), au2.getEissn())
        && StringUtil.equalStrings(au1.getEisbn(), au2.getEisbn())
        && StringUtil.equalStrings(au1.getIssnL(), au2.getIssnL())
        && StringUtil.equalStrings(au1.getPublicationTitle(), 
                                   au2.getPublicationTitle())
        && StringUtil.equalStrings(au1.getPublisherName(), au2.getPublisherName())
        ;
  }

  /**
   * Compares two <code>BibliographicItem</code>s to see if they appear to have
   * the same identity, by comparing their identifying fields. If the ISSNs are
   * both non-empty, returns whether they match; otherwise returns
   * <code>true</code> if the names are equal and non-empty. Note that the names
   * often include a reference to the volume.
   * <p>
   * If either argument is null, an exception will be thrown.
   *
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if they have the same issn, or no issn and same name
   */
  public static boolean haveSameIdentity(BibliographicItem au1, BibliographicItem au2) {
    String au1issn = au1.getIssn();
    String au2issn = au2.getIssn();
    String au1name = au1.getName(); //au1.getJournalTitle();
    String au2name = au2.getName(); //au2.getJournalTitle();
    /*boolean issn = !StringUtil.isNullString(au1issn) &&
        !StringUtil.isNullString(au2issn) && au1issn.equals(au2issn);
    boolean name = !StringUtil.isNullString(au1name) &&
        !StringUtil.isNullString(au2name) && au1name.equals(au2name);
    return issn || name;*/

    if (!StringUtil.isNullString(au1issn) && !StringUtil.isNullString(au2issn))
      return au1issn.equals(au2issn);
    else
      return !StringUtil.isNullString(au1name) &&
          !StringUtil.isNullString(au2name) && au1name.equals(au2name);
  }

  /**
   * Compares two <code>BibliographicItem</code>s to see if they appear to have
   * the same index metadata, by comparing their indexing fields
   * <code>volume</code> and <code>year</code>. Returns true only if every
   * field pair is equally valued, including null values. That is, if one item
   * has or lacks a particular value, the other must have or lack that value too.
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if each pair of non-null volume or year strings is equal
   */
  public static boolean haveSameIndex(BibliographicItem au1, BibliographicItem au2) {
    String au1year = au1.getYear();
    String au2year = au2.getYear();
    // Volumes are normalised for Roman numerals
    String au1vol  = BibliographicUtil.normaliseIdentifier(au1.getVolume());
    String au2vol  = BibliographicUtil.normaliseIdentifier(au2.getVolume());
    // Are the strings equal on both fields, including null values
    return StringUtil.equalStrings(au1year, au2year) &&
        StringUtil.equalStrings(au1vol, au2vol);
  }

  /**
   * Check whether two <code>BibliographicItem</code>s appear to be equivalent,
   * in that they have matching values in at least one identifying field
   * (ISSN or name), and all available indexing fields (volume or year). This is
   * to try and match up duplicate bibliographic records which arise from
   * duplicate releases of the same volume of a title under a different plugin
   * for example. If the ISSNs are available, they take precedence; different
   * ISSNs means non-equivalent items, regardless of the name.
   *
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if they appear to have equivalent identities and indexes
   */
  public static boolean areApparentlyEquivalent(BibliographicItem au1,
                                                BibliographicItem au2) {
    return haveSameIdentity(au1, au2) && haveSameIndex(au1, au2);
  }

  /**
   * Compare two strings which are supposed to be representations of years.
   * Returns less than 0 if the first is less than the second, greater than 0 if
   * the first is greater than the second, and 0 if they are the same. If the
   * strings cannot be parsed the default NumberFormatException is propagated to
   * the caller.
   *
   * @param year1 a string representing a year
   * @param year2 a string representing a year
   * @return the value 0 if the years are the same; less than 0 if the first is less than the second; and greater than 0 if the first is greater than the second
   * @throws NumberFormatException if either of the strings does not parse as an integer
   * @deprecated no longer useful
   */
  public static int compareStringYears(String year1, String year2)
      throws NumberFormatException {
    // Note that in practise if the strings do represent comparable publication years,
    // they should be 4 digits long and so comparable as strings with the same results.
    // Return zero if the strings are equal
    if (year1.equals(year2)) return 0;
    Integer i1 = NumberUtil.parseInt(year1);
    Integer i2 = NumberUtil.parseInt(year2);
    return i1.compareTo(i2);
  }

  /**
   * Parse a string representation of an integer year.
   * @param yr a string representing an integer year
   * @return the year as an int, or 0 if it could not be parsed
   */
  public static int stringYearAsInt(String yr) {
    if (!StringUtil.isNullString(yr)) try {
      return NumberUtil.parseInt(yr);
    } catch (NumberFormatException e) { /* Do nothing */ }
    return 0;
  }

  /**
   * Get an integer representation of the given AU's first year.
   * @param au a BibliographicItem
   * @return the first year as an int, or 0 if it could not be parsed
   */
  public static int getFirstYearAsInt(BibliographicItem au) {
    return stringYearAsInt(au.getStartYear());
  }

  /**
   * Get an integer representation of the given AU's last year.
   * @param au a BibliographicItem
   * @return the last year as an int, or 0 if it could not be parsed
   */
  public static int getLastYearAsInt(BibliographicItem au) {
    return stringYearAsInt(au.getEndYear());
  }

  /**
   * Get the latest end year specified in the list of BibliographicItems.
   * @param aus a list of BibliographicItem
   * @return
   */
  public static int getLatestYear(List<? extends BibliographicItem> aus) {
    int l = 0;
    for (BibliographicItem au : aus) l = Math.max(l, getLastYearAsInt(au));
    return l;
  }

  /**
   * Determines whether a String appears to represent a range. In general, a
   * range is considered to be two strings which are both either numeric
   * (representing an integer or a Roman numeral) or non-numeric,
   * separated by a hyphen '-' and with optional whitespace.
   * The second value is expected to be numerically or lexically greater than
   * or equal to the first. For example, "s1-4" would not qualify as either a
   * numeric or a non-numeric range, while "I-4" ("I" being a Roman number) and
   * the string range "a-baa" would. Numerical ordering is checked if the
   * strings are numerical, otherwise case-insensitive lexical ordering is
   * checked.
   * <p>
   * A numeric range must have two numeric values separated by a '-', while a
   * non-numeric range has two non-numeric values. In theory a string like
   * "s1-4" could represent a range, as could "1-s4", but it is impossible to
   * say whether either of these represents a genuine range with
   * different-format endpoints, or a single identifier. This is why the
   * condition is imposed that the strings either side of the hyphen must be
   * both numeric, or both non-numeric and of roughly the same format.
   * That is, either they are both parsable as integers, or they both involve
   * non-digit tokens that cannot be parsed as integers.
   * <p>
   * To allow for identifiers that themselves incorporate a hyphen, the input
   * string is only split around the centremost hyphen. If there is an even
   * number of hyphens, the input string is assumed not to represent a parsable
   * range.
   * <p>
   * Further restrictions are enforced upon non-numerical strings that may be
   * considered valid endpoints of a range; if one side of a possible range can
   * be parsed as a normalised Roman number, the string will be considered a
   * range <i>only if</i> the tokens either side of the hyphen are the same
   * length and case, <i>and</i> they are lexically either equal or increasing.
   * <p>
   * Note that the motive for this method was to identify volume ranges, but
   * it should work just as well for other ranges that have the same kind of
   * characteristics, such as issue ranges, and numerical ranges.
   *
   * @param range the input String
   * @return <tt>true</tt> if the input string represents a range
   */
  public static boolean isRange(String range) {
    if (range == null) return false;
    // Check if it is a numerical range
    if (NumberUtil.isNumericalRange(range)) return true;
    // We are now dealing with either a non-range, or at least one non-numerical
    // endpoint. Find the range-separating hyphen.
    int hyphenPos = NumberUtil.findRangeHyphen(range);
    if (hyphenPos < 0) return false;
    // Split string at middlemost hyphen
    String s1 = range.substring(0, hyphenPos).trim();
    String s2 = range.substring(hyphenPos + 1).trim();
    // Check format of strings
    if (changeOfFormats(s1, s2)) return false;
    // Check if one side of the hyphen can be interpreted as a Roman number;
    // we already know that it is not a numerical range (both sides numerical).
    // If one side only might be Roman numerical, and is either a different size
    // or case to the other side, or suggests a descending alphabetical range,
    // return false (not a range)
    if ((NumberUtil.isNumber(s1) || NumberUtil.isNumber(s2)) &&
        (s1.length()!=s2.length() || !areSameCasing(s1, s2) || s1.compareTo(s2) > 0)
        ) return false;

    // Normalise the Roman tokens and test lexically
    if (normaliseIdentifier(s2).compareToIgnoreCase(normaliseIdentifier(s1)) >= 0)
      return true;
    // Check lexical order as a last resort
    return (s2.compareToIgnoreCase(s1) >= 0);
  }

  /**
   * Check whether the strings consist of characters of the same case at every
   * point. If the strings differ in length, false is returned.
   * @param s1
   * @param s2
   * @return false if the strings are different lengths or vary in case at any character
   */
  private static boolean areSameCasing(String s1, String s2) {
    if (s1.length()!=s2.length()) return false;
    char c1, c2;
    char[] s1arr = s1.toCharArray();
    char[] s2arr = s2.toCharArray();
    for (int i=0; i<s1arr.length; i++) {
      if (Character.isUpperCase(s1arr[i]) != Character.isUpperCase(s2arr[i]) )
        return false;
    }
    return true;
  }

  /**
   *
   * @param s
   * @return
   */
  private static boolean containsUpperCaseChars(String s) {
    for (char c : s.toCharArray()) if (Character.isUpperCase(c)) return true;
    return false;
  }


  /**
   * Normalise an identifier by tokenising it and converting Roman numerical
   * tokens into numbers, and then padding all numbers. The normalised string
   * can then be compared to other normalised strings based on a lexical
   * comparison.
   * @param s an identifier
   * @return a normalised identifier, or null
   */
  public static String normaliseIdentifier(String s) {
    if (s==null) return null;
    s = translateRomanTokens(s);
    // Zero-padding up to 4 positions should be ample for volumes
    int pad = 4;
    // Zero-pad numerical tokens
    s = NumberUtil.padNumbers(s.trim(), pad);
    return s;
  }

  /**
   * Split a string which may contain a list of ranges separated by the tokens
   * specified in {@link rangeSetDelimiterPattern}, and return the resulting
   * list of strings.
   * @param rangeSetStr
   * @return
   */
  public static List<String> splitRangeSet(String rangeSetStr) {
     return Arrays.asList(
         rangeSetStr.split(rangeSetDelimiterPattern.getPattern().toString())
     );
  }

  /**
   * Get the range start of a set of ranges. Whitespace is trimmed. This method
   * will split the range set, then get the range start of the first range.
   * If the first range string does not appear to represent a range, it is
   * returned whole.
   * @param rangeSet a comma/semicolon-delimited list of ranges
   * @return the range start <code>null</code> if not specified
   */
  static public String getRangeSetStart(String rangeSet) {
    if (rangeSet == null) {
      return null;
    }
    List<String> ranges = BibliographicUtil.splitRangeSet(rangeSet);
    String range;
    if (ranges.size()==0) range = rangeSet;
    else range = ranges.get(0);
    return BibliographicUtil.isRange(range) ? NumberUtil.getRangeStart(range) : range;
  }

  /**
   * Get the range end of a set of ranges. Whitespace is trimmed. This method
   * will split the range set, then get the range end of the last range.
   * If the last range string does not appear to represent a range, it is
   * returned whole.
   * @param rangeSet a comma/semicolon-delimited list of ranges
   * @return the range end or <code>null</code> if not specified
   */
  static public String getRangeSetEnd(String rangeSet) {
    if (rangeSet == null) {
      return null;
    }
    List<String> ranges = BibliographicUtil.splitRangeSet(rangeSet);
    String range;
    if (ranges.size()==0) range = rangeSet;
    else range = ranges.get(ranges.size()-1);
    return BibliographicUtil.isRange(range) ? NumberUtil.getRangeEnd(range) : range;
  }

  /**
   * Determine whether any of the ranges implied by a coverage string includes
   * a given value. The coverage string is a list of one or more ranges
   * separated by characters matching <tt>rangeSetDelimiterPattern</tt>. Each
   * range can be a single value or a start/stop range separated by a dash. If
   * the range string is a single value, it will be used as both the start and
   * stop values of the range.
   *
   * @param coverageStr a list of ranges separated by comma or semicolon, optionally with whitespace
   * @param value an identifier that may occur in the ranges
   * @return <code>true</code> if the coverage includes the value
   */
  public static boolean coverageIncludes(String coverageStr, String value) {
    if ((coverageStr == null) || (value == null)) {
      return false;
    }
    // Check each range in turn
    RangeIterator ranges = new RangeIterator(coverageStr);
    while (ranges.hasNext()) {
      String range = ranges.next();
      // Return true if the string is a range containing the value, or
      // is equal to the value.
      if (isRange(range)) {
        if (BibliographicUtil.rangeIncludes(range, value)) return true;
      } else {
        if (range.equals(value)) return true;
      }
    }
    return false;
  }


  /**
   * Determine whether a coverage range includes a given value. This version
   * of the method parses the start and end of the range from a string. The
   * range can be a single value or a start/stop range separated by a dash. If
   * the range is a single value, it will be used as both the start and stop
   * values.
   *
   * @param rangeStr a string representing a range or a single value
   * @param value an identifier that may occur in the range
   * @return <code>true</code> if this range includes the value
   */
  protected static boolean rangeIncludes(String rangeStr, String value) {
    if (rangeStr == null || value == null) {
      return false;
    }
    // If not a range, return straight case-insensitive comparison
    if (!isRange(rangeStr)) return rangeStr.equalsIgnoreCase(value);
    // Treat as range
    int i = NumberUtil.findRangeHyphen(rangeStr);
    String start = (i > 0) ? rangeStr.substring(0,i).trim() : rangeStr.trim();
    String end   = (i > 0) ? rangeStr.substring(i+1).trim() : rangeStr.trim();
    //System.out.format("Testing range %s to %s with %s\n", startRange, endRange, value);
    return rangeIncludes(start, end, value);
  }

  /**
   * Determine whether a coverage range includes a given value.
   * <p>
   * If the range endpoints and the value can be interpreted as numbers (Arabic
   * or Roman), the value is compared numerically with the range. Otherwise,
   * the value is compared alphabetically to the endpoints of the range,
   * excluding any common prefix, and zero-padding numbers.
   * <p>
   * The range string can also include whitespace, which is trimmed from the
   * resulting year. Java's <code>String.trim()</code> method trims blanks and
   * also control characters, but doesn't trim all the forms of blank
   * specified in the Unicode character set. The value to search for is
   * <i>not</i> trimmed.
   * <p>
   * This method is protected and only intended for internal use, as it
   * stipulates that the range string must represent a single range. Clients
   * should instead call {@link #coverageIncludes(String, String)}, which
   * will handle a wider range of input strings.
   *
   * @param start a single value representing the start of the range
   * @param end a single value representing the end of the range
   * @param value an identifier that may occur in the range
   * @return <code>true</code> if this range includes the value
   */
  protected static boolean rangeIncludes(String start, String end, String value) {
    try {
      // See if value is within a numerical range
      int s = NumberUtil.parseInt(start);
      int e = NumberUtil.parseInt(end);
      int ival = NumberUtil.parseInt(value);
      return (ival >= s && ival <= e);
    } catch (NumberFormatException ex) {
      // True if start or end equal to value
      if (value.equals(start) || value.equals(end)) {
        return true;
      }
      // Identify a common prefix to the identifiers
      String pre = BibliographicUtil.commonTokenBasedPrefix(start, end);
      int n = pre.length();
      // If the value does not share the prefix, it is not in the range
      if (!value.startsWith(pre)) return false;
      // Assume that the changing sequential phrase is the rest of the string
      String seqPhrS = NumberUtil.padNumbers(start.substring(n), 4);
      String seqPhrE = NumberUtil.padNumbers(end.substring(n), 4);
      String seqPhr = NumberUtil.padNumbers(value.substring(n), 4);
      // The remaining phrase may still be a complex string or it may be numerical
      return NumberUtil.rangeIncludes(seqPhrS, seqPhrE, seqPhr);
    }
  }

  /**
   * Find the longest common prefixing substring between the two Strings, based
   * on tokens defined by the supplied <tt>boundaryMatch</tt> pattern. Instead
   * of consisting of every character up to the first character that differs,
   * the common prefix consists of every full <i>token</i> up to the first
   * token that differs.
   * @param s1 the first string
   * @param s2 the second string
   * @return a String representing the longest token-granular prefix common to the two strings
   */
  public static String commonTokenBasedPrefix(String s1, String s2) {
    AlphanumericTokenisation st1 = new AlphanumericTokenisation(s1);
    AlphanumericTokenisation st2 = new AlphanumericTokenisation(s2);
    StringBuilder commonPrefix = new StringBuilder();
    int n = Math.min(st1.numTokens(), st2.numTokens());
    for (int i=0; i<n; i++) {
      String tok1 = st1.tokens.get(i);
      String tok2 = st2.tokens.get(i);
      if (tok1.equals(tok2)) {
        commonPrefix.append(tok1);
      } else break;
    }
    return commonPrefix.toString();
  }


  /**
   * Check whether the supplied strings indicate a change of format in their
   * field; this is inevitably a fuzzy concept, and for the moment we check
   * for the very simple distinction between digit-numerical and
   * non-numerical strings. A numerical string is one that represents an integer
   * with either digits or Roman numerals. Note that digit-numerical and
   * non-numerical are not complementary concepts.
   * <p>
   * A change between Roman numerals and Arabic numbers is not considered a
   * change of formats. Strings that consist of only Roman numerals can be
   * interpreted as either integers or strings (depending on the normalisation
   * applied to Roman numerals). It is therefore not possible to say strictly
   * whether a particular string that meets
   * {@link org.lockss.util.NumberUtil.isRomanNumeral()} is intended as an
   * integer or a string, so we allow it to meet either criterion. If either
   * <code>lastVal</code> or <code>thisVal</code> can be parsed as a Roman
   * numeral, and the other mixes digits and non-digits, it is considered a
   * change of formats; however if either value can be parsed as a Roman numeral
   * while the other consists only of digits (is parsable as an integer) or of
   * non-digits (does not contain digits), we cannot decide if there is a change
   * of formats and return false.
   * <p>
   * If both values are empty, the result is false.
   * <p>
   * In future it might be desirable to use a more complex measure
   * of similarity or difference, for example Levenstein distance or a regexp
   * and tokenisation. (For example if the formats are both string, test that
   * there is a common substring at the start.)
   *
   * @param lastVal the last value
   * @param thisVal the current value
   * @return true if one string contains only digits while the other is not parsable as a number
   */
  public static final boolean changeOfFormats(String lastVal, String thisVal) {
    if (lastVal==null && thisVal==null) return false;
    boolean rn1 = NumberUtil.isRomanNumber(lastVal);
    boolean rn2 = NumberUtil.isRomanNumber(thisVal);
    // TODO It might be better to only allow normalised Roman numbers using NumberUtil.parseRomanNumber(s, true);
    boolean mf1 = NumberUtil.isMixedFormat(lastVal);
    boolean mf2 = NumberUtil.isMixedFormat(thisVal);

    // If one is Roman and the other is mixed, consider it a change of formats
    //if ((rn1 && mf2) || (rn2 && mf1)) return true;

    // If either is a Roman number while the other is not mixed formats,
    // we can't be sure there is a change
    //if ((rn1 && !mf2) || (rn2 && !mf1)) return false;

    // If one is parsable as a Roman numeral, the return value depends upon
    // whether the other is mixed format.
    if (rn1) return mf2;
    if (rn2) return mf1;

    // Are these strings digit-numerical and/or numerical?
    boolean i1 = NumberUtils.isDigits(lastVal);
    boolean i2 = NumberUtils.isDigits(thisVal);
    boolean n1 = NumberUtil.isNumber(lastVal);
    boolean n2 = NumberUtil.isNumber(thisVal);
    // If one is digit-numerical and the other is non-numerical,
    // there is a definite change of formats.
    return (i1 && !n2) || (!n1 && i2);
  }

  /**
   * Generate a series of strings incrementing or decrementing by 1.
   * @param start the start string
   * @param end the end string, which may be less than the start number
   * @return a range of strings, incrementing or decrementing by 1
   */
  public static List<String> constructSequence(String start, String end)
      throws IllegalArgumentException {
    return constructSequence(start, end, 1);
  }

  /**
   * Generate a series of strings, producing a set covering the range from the
   * start string to the end string inclusive. If the start and end values are
   * equal, an array containing the single value is returned.
   * <p>
   * The method can handle different types of identifier. Arabic and Roman
   * number sequences are easily generated; string-based sequences are much
   * harder to generate based purely on analysis, but if the form of the start
   * and end identifiers is not known in advance we have to take a best guess
   * approach. Roman numbers must be in normalised form or they are assumed to
   * be alphabetic tokens.
   * <p>
   * The method looks in the start and end strings for a common substring
   * comprised of a series of alphabetical or numerical (not Roman) tokens, and
   * then attempts to create a sequence by varying the final tokens, that follow
   * the common prefix. The changing part of the identifiers must occur in the
   * final part of the string. Ranges like sI to sIII are not supported, though
   * we could create tokens based on case as well as character type.
   * <p>
   * Exceptions will be thrown if there are inconsistent or unsupported
   * arguments. All sequences can be generated in both ascending and descending
   * orientations and with a variety of values for the delta, including
   * Arabic- or Roman-numerical sequences, and alphabetical sequences.
   *
   * @param start the start string
   * @param end the end string
   * @param delta the magnitude of the increment or decrement, expressed as a positive integer
   * @return a sequence of strings, incrementing or decrementing in the changeable token
   * @throws NumberFormatException if number formats do not allow a consistent and unambiguous sequence to be created
   * @throws IllegalArgumentException if the parameters are inconsistent
   */
  public static List<String> constructSequence(String start, String end, int delta)
      throws IllegalArgumentException {
    List<String> seq = new ArrayList<String>();
    String pre = commonTokenBasedPrefix(start, end);
    // Assume that the changing sequential token is the rest of the string
    String seqTokS = start.substring(pre.length());
    String seqTokE = end.substring(pre.length());

    // Create a sequence based on the type of these tokens

    // Simple integer sequence
    //if (NumberUtil.isInteger(seqTokS) && NumberUtil.isInteger(seqTokE)) {
    if (NumberUtil.isIntegerDigits(seqTokS) && NumberUtil.isIntegerDigits(seqTokE)) {
      int s = Integer.parseInt(seqTokS);
      int e = Integer.parseInt(seqTokE);
      int[] intSeq = NumberUtil.constructSequence(s, e, delta);
      // Default is not to pad numbers in the seq
      int padlen = 0;
      // If a number starts with a zero, maintain zero-padded length
      // in generated tokens.
      if (seqTokS.startsWith("0") || seqTokE.startsWith("0")) {
        int len = seqTokS.length();
        // Check if the zero-padded numbers are the same length
        if (seqTokE.length() != len)
          throw new IllegalArgumentException(String.format(
              "Can't generate sequence with different length " +
                  "zero-padded numbers %s and %s.",
              seqTokS, seqTokE
          ));
        padlen = len;
      }
      // Zero-pad the numbers as necessary  and add to result list
      for (int i : intSeq) seq.add(pre + NumberUtil.padNumbers(i, padlen));
    }
    // Roman numbers, allowing lower case
    else if (NumberUtil.isNormalisedRomanNumber(StringUtils.upperCase(seqTokS)) &&
        NumberUtil.isNormalisedRomanNumber(StringUtils.upperCase(seqTokE))) {
      List<String> romSeq = NumberUtil.constructRomanSequence(seqTokS, seqTokE, delta);
      // Convert the numbers back to Roman and add to result list
      for (String s : romSeq) seq.add(pre + s);
    }
    // Alphabetic identifiers
    else {
      List<String> tokSeq = NumberUtil.constructAlphabeticSequence(seqTokS, seqTokE, delta);
      // Convert the numbers back to Roman and add to result list
      for (String s : tokSeq) seq.add(pre + s);
    }
    return seq;
  }


  /**
   * Get the current year.
   * @return an integer representing the current year
   */
  public static final int getThisYear() {
    return TimeBase.nowCalendar().get(Calendar.YEAR);
  }

  /**
   * Translate Roman number tokens in a string to Arabic numbers. This method
   * may recognise false positives when looking for Roman numbers. It would do
   * better if it had an idea of the nature of all the tokens in the same
   * position within the whole range of strings being manipulated.
   * @param s
   * @return
   */
  public static String translateRomanTokens(String s) {
    if (s==null) return null;
    // NOTE: Consider using BibliographicUtil.commonTokenBasedPrefix() instead
    // Tokenise the string on punctuation
    StringBuilder sb = new StringBuilder();
    PatternMatcherInput input = new PatternMatcherInput(s);
    try {
      Perl5Matcher volTokenMatcher = RegexpUtil.getMatcher();
      while (volTokenMatcher.contains(input, VOL_TOK_DELIM_PTN)) {
        // Check if the string is normalised Roman when it is upper cased;
        // if so it is converted to Arabic to normalise the string
        String pre = input.preMatch();
        if (NumberUtil.isNormalisedRomanNumber(pre.toUpperCase())) {
          sb.append(NumberUtil.toArabicNumber(pre));
        } else sb.append(pre);
        sb.append(input.match());
        // Set input to the remainder of the input
        input.setInput(input.postMatch());
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      // Ignore - this seems to occur occasionally through some combination of
      // back button, reload or stopping the submission of a report request..
    }
    // Finally, try and convert the end of the string
    if (NumberUtil.isNormalisedRomanNumber(input.toString().toUpperCase())) {
      sb.append(NumberUtil.toArabicNumber(input.toString()));
    } else sb.append(input);
    return sb.toString();
  }


  /**
   * A class for convenience in passing back title year ranges after year
   * processing. A year can be set to zero if it is not available. The onus
   * is on the client to check whether the year is valid.
   * <p>
   * The class holds a list of <code>BibliographicItem</code> references
   * covering the whole range.
   */
  public static class TitleRange {
    /** The first BibliographicItem in the range. */
    public final BibliographicItem first;
    /** The last BibliographicItem in the range. */
    public final BibliographicItem last;
    /** The year range of the title. */
    YearRange yearRange;
    /** List of the <code>BibliographicItem</code>s making up the range. */
    public final List<? extends BibliographicItem> items;

    /** Gets the first year in the range. */
    public int getFirstYear() { return yearRange.first; }
    /** Gets the last year in the range. */
    public int getLastYear() { return yearRange.last; }

    /**
     * Constructor extracts the years from the end points of the given
     * <code>BibliographicItem</code>s.
     *
     * @param items the list of <code>BibliographicItem</code>s which underlie the title range
     */
    public TitleRange(List<? extends BibliographicItem> items) {
      this.items = items;
      this.first = items.get(0);
      this.last = items.get(items.size()-1);
      this.yearRange = getYearRange(items);
    }

    /**
     * Produces a YearRange from the values available in the list of
     * <code>BibliographicItem</code>s. The year range of a list of
     * BibliographicItems is considered to be from the start date of the first
     * item, to the end date of the last item. Some journals show unusual end
     * years for intermediate items, such as a year which is later than the end
     * of the subsequent issues. This may suggest that we should look for the
     * <i>latest</i> year in the whole list, however in most cases these appear
     * to be mistakes, so we adhere to using the end year of the final item.
     * <p>
     * If years are not available or not parseable they are set to zero.
     *
     * @param items the ordered range of BibliographicItems which inform the year range
     * @return a YearRange covering the whole range of the list of BibliographicItems
     */
    private static YearRange getYearRange(
        List<? extends BibliographicItem> items) {
      return new YearRange(items.get(0), items.get(items.size()-1));
    }

    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("TitleRange %s %s",
          first.getSeriesTitle(), yearRange);
    }
  }

  /**
   * Represents a volume range with two strings.
   *
   */
  public static class VolumeRange {
    /** The first volume string in the range. */
    public final String first;
    /** The last volume string in the range. */
    public final String last;

    /**
     * Create a volume range from a single BibliographicItem. If its volume
     * string cannot be parsed, a <code>NumberFormatException</code> is thrown.
     * @param item a single BibliographicItem representing a whole range
     * @throws NumberFormatException
     */
    public VolumeRange(BibliographicItem item) throws NumberFormatException {
      this.first = item.getStartVolume();
      this.last = item.getEndVolume();
    }

    /**
     * Create a volume range from items. If there is a volume range in the first
     * item, the first volume of the range is taken; if the last item has a
     * range, the last volume is taken. If parsing fails, a
     * <code>NumberFormatException</code> is thrown.
     * @param first first BibliographicItem of the range
     * @param last last BibliographicItem of the range
     * @throws NumberFormatException
     */
    public VolumeRange(BibliographicItem first, BibliographicItem last)
        throws NumberFormatException {
      this.first = first.getStartVolume();
      this.last = last.getEndVolume();
    }

    /**
     * Whether the range contains two valid volume strings, that is non-null
     * and not empty.
     * @return <code>true</code> if both values are available
     */
    public boolean isValid() {
      return !StringUtils.isEmpty(first) &&
          !StringUtils.isEmpty(last);
    }

    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("(Volumes %s-%s)", first, last);
    }
  }

  /**
   * Represents a year range without any contextual assumptions - a first year
   * and a last year. Can be used to represent the range of a particular
   * <code>BibliographicItem</code>, of a range of
   * <code>BibliographicItem</code>s, or anything else that has a year range.
   * Invalid years will usually be set to 0.
   *
   */
  public static class YearRange {
    /** The first year in the range. */
    public final int first;
    /** The last year in the range. */
    public final int last;

    /**
     * Create a year range from a single BibliographicItem which contains either
     * a single year or a hyphenated year range. If the string cannot be parsed
     * according to these criteria, the years are set to zero.
     * @param item a single BibliographicItem representing a whole range
     */
    public YearRange(BibliographicItem item) { this(item, item);  }

    /**
     * Create a year range with pre-parsed values.
     * @param first first year of the range
     * @param lastYear last year of the range
     */
    YearRange(int first, int lastYear) {
      this.first = first;
      this.last = lastYear;
    }

    /**
     * Create a year range from <code>BibliographicItem</code>s. If there is a
     * year range in the first item, the first year of the range is taken; if
     * the last item has a range, the last year is taken. If parsing fails, the
     * year is set to 0.
     * @param first first <code>BibliographicItem</code> of the range
     * @param last last <code>BibliographicItem</code> of the range
     */
    YearRange(BibliographicItem first, BibliographicItem last) {
      this.first = NumberUtil.isNumber(first.getStartYear()) ?
          NumberUtil.parseInt(first.getStartYear()) : 0;
      this.last = NumberUtil.isNumber(last.getEndYear()) ?
          NumberUtil.parseInt(last.getEndYear()) : 0;
    }

    /**
     * Whether the range contains two valid year values, that is not zero.
     * @return <code>true</code> if both values are available
     */
    public boolean isValid() {
      return first !=0 && last != 0;
    }

    /** Produces a friendly String representation of the range. */
    public String toString() {
      return String.format("(Years %d-%d)", first, last);
    }
  }

  /**
   * A class to represent the tokenisation of a string into number and
   * non-number tokens. Provides access to the tokens, the original string,
   * and a boolean indicating whether the first token is numerical.
   * The list of tokens should consist of alternating numbers and text.
   * <p>
   * Roman number tokens must be in normalised form or they will be assumed for
   * safety to be alphabetic tokens. Similarly, Roman number tokens can only be
   * recognised on full alphabetical (non-number) tokens.
   *
   * @author Neil Mayo
   */
  protected static class AlphanumericTokenisation {
    /** A thread-local and non-static perl matcher. */
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    /** The string that was tokenised. */
    String originalString;
    /** An ordered list of tokens parsed from the string. */
    List<String> tokens;
    /** Whether the first token is a number. */
    boolean isNumFirst;

    /**
     * Perform a tokenisation and just return the tokens.
     * @param str
     */
    public static List<String> tokenise(String str) {
      return new AlphanumericTokenisation(str).tokens;
    }

    public AlphanumericTokenisation(String str) {
      this.originalString = str;
      this.tokens = new ArrayList<String>();
      this.isNumFirst = matcher.contains(str, numAtStart);

      // Tokenise the string
      PatternMatcherInput input = new PatternMatcherInput(str);
      while (matcher.contains(input, numOrNonNum)) {
        tokens.add(matcher.getMatch().group(0));
      }
    }

    public int numTokens() {
      return tokens.size();
    }
  }


  /**
   * An iterator for ranges specified as a comma- or semicolon-separated list
   * in a string. Encapsulates a scanner on the range set string.
   * Does not support the <code>remove()</code> method.
   */
  public static class RangeIterator implements Iterator<String> {

    private final Scanner rangeSetScanner;

    public RangeIterator(String rangeSetStr) {
      this.rangeSetScanner = new Scanner(rangeSetStr)
          .useDelimiter(rangeSetDelimiterPattern.getPattern());
    }

    public boolean hasNext() {
      return rangeSetScanner.hasNext();
    }

    public String next() {
      return rangeSetScanner.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }



  /**
   * An iterator for volume identifiers specified as a list of comma- or
   * semicolon-separated ranges. Does not support the <code>remove()</code>
   * method. Parses the endpoints of the range, then attempts to identify a
   * common prefix to the endpoints of the range, before iterating the 
   * remaining string using a SequenceIterator and then recombining to
   * produce values.
   */
  public static class VolumeIterator implements Iterator<String> {

    private final String rangeSetStr;
    private final RangeIterator ranges;
    private SequenceIterator sequence;
    /** The longest token-based prefix common to the current start and end strings. */
    protected String commonPrefix;

    /**
     * Create a VolumeIterator on the specified range string, which may contain
     * anything from a single value to a list of ranges. The string is split
     * into ranges, for each of which a sequence iterator is generated as
     * required.
     * @param rangeSetStr a comma- or semicolon-separated list of ranges, a single range or a single value
     */
    public VolumeIterator(String rangeSetStr) {
      this.rangeSetStr = rangeSetStr;
      this.ranges = new RangeIterator(rangeSetStr);
    }

    public boolean hasNext() {
      // Start a new sequence if necessary
      if (sequence==null || !sequence.hasNext()) {
        // False if no more range strings
        if (!ranges.hasNext()) return false;
        sequence = getSequenceIterator(ranges.next());
        //System.out.println("SequenceIterator "+sequence.getClass());
      }
      return sequence.hasNext();
    }

    /**
     * Create a new SequenceIterator on the range. The sequence is constructed
     * by looking in the start and end strings for a common substring prefix
     * comprised of a series of alphabetical or digit-numerical tokens,
     * and then varying the final tokens that follow the common prefix.
     * The changing part of the identifiers must occur in the final part of the
     * string. Ranges like sI to sIII are not supported, though we could do this
     * by creating tokens based on case as well as character type, so that Roman
     * numbers may be parsed.
     * <p>
     * Roman number tokens must be in normalised form or they are assumed for
     * safety to be alphabetic tokens. Similarly, Roman number tokens can only
     * be recognised on full alphabetical tokens, not within.
     * @param rangeStr
     * @return
     */
    private SequenceIterator getSequenceIterator(String rangeStr) {
      // Identify the likely start and end values
      String start, end;
      if (BibliographicUtil.isRange(rangeStr)) {
        start = NumberUtil.getRangeStart(rangeStr);
        end = NumberUtil.getRangeEnd(rangeStr);
      } else {
        start = rangeStr;
        end = rangeStr;
      }
      // Identify a common prefix to the identifiers
      this.commonPrefix = start.equals(end) ? "" :
        BibliographicUtil.commonTokenBasedPrefix(start, end);
      // Assume that the changing sequential token is the rest of the string
      int n = commonPrefix.length();
      String s = start.substring(n);
      String e = end.substring(n);
      return SequenceIterator.getInstance(s, e);
    }

    public String next() {
      if (sequence==null)
        throw new NoSuchElementException("SequenceIterator is null.");
      return commonPrefix + sequence.next();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * An iterator for sequences, circumventing the need to generate a
   * whole sequence in one go.
   */
  public static abstract class SequenceIterator implements Iterator<String> {

    protected final String start;
    protected final String end;
    
    /**
     * Create an iterator from the start value to the end value.
     * @param start
     * @param end
     */
    protected SequenceIterator(String start, String end) {
      this.start = start;
      this.end = end;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
    /**
     * Create a SequenceIterator instance of the appropriate type, based on the
     * given start and end values. If parsing fails at any point,
     * a PredefinedSequenceIterator is returned by default.
     * @param s the start value of the range
     * @param e the end value of the range
     * @return a SequenceIterator on the sequence implied by the start and end values
     */
    public static SequenceIterator getInstance(String s, String e) {
      if (s==null || e==null || s.equals(e)) return new PredefinedSequenceIterator(s);
      try {
        if (NumberUtil.isIntegerDigits(s) && NumberUtil.isIntegerDigits(e)) {
          return new IntegerSequenceIterator(s, e);
        }
        else if (NumberUtil.isNormalisedRomanNumber(StringUtils.upperCase(s)) &&
                 NumberUtil.isNormalisedRomanNumber(StringUtils.upperCase(e))) {
          return new RomanSequenceIterator(s, e);
        }
        // Try and create an alphabetic iterator if the strings are the same length
        else if (s.length()==e.length()) {
          //System.out.format("Creating AlphabeticSequenceIterator(%s, %s)\n", s, e);
          return new AlphabeticSequenceIterator(s, e);
        } else {
          return new PredefinedSequenceIterator(s, e);
        }
      } catch (IllegalArgumentException ex) {
        // As a last resort, just return an iterator on the two values
        return new PredefinedSequenceIterator(s, e);
      }
    }
  }

  /**
   * An iterator for a predefined small set of values, regardless of their
   * format.
   */
  public static class PredefinedSequenceIterator extends SequenceIterator {
    private final String[] values;
    private int pointer = 0;

    protected PredefinedSequenceIterator(String... values) {
      super(values[0], values[values.length-1]);
      this.values = values;
    }
    
    public boolean hasNext() {
      return pointer < values.length;
    }
    public String next() {
      if (pointer>=values.length) throw new NoSuchElementException();
      return values[pointer++];
    }
  }

  /**
   * A SequenceIterator for sequences that are essentially numerical, that is
   * Arabic integer sequences, Roman number sequences, and alphabetical
   * sequences treated as base-26 representations of an integer sequence.
   * If either the start and end strings are zero-padded, zero-padding up to the
   * length of the longest string will be applied to each element in the
   * sequence.
   */
  public static class IntegerSequenceIterator extends SequenceIterator {

    /** The character representing zero; used for padding. */
    protected char zeroChar = '0';
    protected final int s;
    protected final int e;
    protected int padLength;
    protected final boolean descending;
    protected int nextNumber;

    /**
     * Create an iterator from the start value to the end value.
     * @param start the start string in the sequence
     * @param end the end string in the sequence
     */
    protected IntegerSequenceIterator(String start, String end) {
      super(start, end);
      // If the strings don't parse as ints, an exception is thrown
      this.s = NumberUtil.parseInt(start);
      this.e = NumberUtil.parseInt(end);
      // Initialise the nextNumber to s
      this.nextNumber = s;
      // Is the sequence descending
      this.descending = s>e;

      // Calculate padding - default is not to pad numbers in the sequence
      int len = 0;
      // If a number starts with a zero, maintain zero-padded length
      // in generated tokens.
      if (start.charAt(0)==zeroChar || end.charAt(0)==zeroChar) {
        len = Math.max(start.length(), end.length());
      }
      this.padLength = len;
    }

    protected IntegerSequenceIterator(int start, int end) {
      this(""+start, ""+end);
    }

    public boolean hasNext() {
      return descending ? nextNumber >= e : nextNumber <= e;
    }

    public String next() {
      // Zero-pad the number as necessary and return
      return NumberUtil.padNumbers(getNextNumber(), padLength);
    }

    /**
     * Generate the next number as an int, and update the nextNumber variable.
     * @return an integer one greater or less than the last, depending on the direction of the sequence
     */
    protected int getNextNumber() {
      return descending ? nextNumber-- : nextNumber++;
    }

  }


  /**
   * An iterator for Roman number sequences, preventing the need to generate a
   * whole sequence in one go.
   */
  public static class RomanSequenceIterator extends IntegerSequenceIterator {
    private final boolean lowerCase;
    
    protected RomanSequenceIterator(String start, String end) {
      super(start, end);
      // Try and maintain the case - if it is mixed we use the default upper
      // case as we cannot decide what is appropriate
      this.lowerCase = StringUtils.isAllLowerCase(start) &&
        StringUtils.isAllLowerCase(end);
    }
    
    @Override
    public String next() {
      String res = NumberUtil.toRomanNumber(getNextNumber());
      return lowerCase ? res.toLowerCase() : res;
    }
  }

  /**
   * An iterator for base-26 alphabetical sequences, circumventing the need to
   * generate a whole sequence in one go. Note that the idea of an alphabetical
   * <i>sequence</i> uses a much more strict definition of "alphabetical
   * range" than {@link NumberUtil.rangeIncludes), which checks topic ranges,
   * that is whether a string falls alphabetically between two endpoints.
   * In order to produce a consistent and bounded sequence from
   * start and end points, it is necessary to vary the characters while
   * maintaining the length of the string, so for incrementing purposes each
   * string is considered to represent a base-26 number. Therefore a range from
   * "a" to "c" will contain "b" but will not contain "abc". The end string can
   * be longer than the start string.
   * <p>
   * The start and end strings are considered case-insensitively, and all output
   * elements will be lower case unless the start and end strings are fully
   * upper case. Mixed case is not supported.
   */
  public static class AlphabeticSequenceIterator extends IntegerSequenceIterator {

    protected final boolean upperCase;

    protected AlphabeticSequenceIterator(String start, String end) {
      super(NumberUtil.fromBase26(start), NumberUtil.fromBase26(end));
      // Will throw exception if numbers don't parse
      // Set the character representing zero in base26.
      this.zeroChar = NumberUtil.toBase26(0).charAt(0);
      // Set the casing
      this.upperCase = StringUtils.isAllUpperCase(start) &&
          StringUtils.isAllUpperCase(end);
    }

    @Override
    public String next() {
      String next = NumberUtil.toBase26(getNextNumber());
      // Pad the string to the correct length with zeroChar 'a'
      next = StringUtils.leftPad(next, start.length(), zeroChar);
      return upperCase ? next.toUpperCase() : next;
    }
  }

}

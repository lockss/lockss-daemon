/*
 * $Id: BibliographicUtil.java,v 1.1 2011-12-01 17:39:32 easyonthemayo Exp $
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

package org.lockss.exporter.biblio;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import java.util.Collections;
import java.util.List;

import org.lockss.util.NumberUtil;
import org.lockss.util.StringUtil;

/**
 * Utility methods for {@link BibliographicItem}s and their data.
 * 
 * @author Neil Mayo
 */
public class BibliographicUtil {

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
   * Check whether two <code>BibliographicItem</code>s are equivalent, that is
   * they share the same values for their primary fields. The fields that are
   * checked are:
   * <code>year</code>, <code>volume</code>, <code>name</code> and 
   * <code>issn</code>. The method will return <code>false</code> if
   * any field is null.
   * 
   * @param item1 a BibliographicItem
   * @param item2 another BibliographicItem
   * @return <code>true</code> if they have equivalent primary fields
   */
  public static boolean areEquivalent(BibliographicItem item1,
                                      BibliographicItem item2) {
    try {
      return 
      item1.getIssn().equals(item2.getIssn()) &&
      item1.getYear().equals(item2.getYear()) &&
      item1.getName().equals(item2.getName()) &&
      item1.getVolume().equals(item2.getVolume());
    } catch (NullPointerException e) {
      return false; 
    }
  }
  
  /**
   * Compares two <code>BibliographicItem</code>s to see if they appear to have
   * the same identity, by comparing their identifying fields. Returns
   * <code>true</code> if either the ISSNs or the names are equal.
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if they have the same issn or name
   */
  public static boolean haveSameIdentity(BibliographicItem au1, BibliographicItem au2) {
    String au1issn = au1.getIssn();
    String au2issn = au2.getIssn();
    String au1name = au1.getName();
    String au2name = au2.getName();
    boolean issn = au1issn!=null && au2issn!=null && au1issn.equals(au2issn);
    boolean name = au1name!=null && au2name!=null && au1name.equals(au2name);
    return issn || name;
  }
  
  /**
   * Compares two <code>BibliographicItem</code>s to see if they appear to have
   * the same index metadata, by comparing their indexing fields
   * <code>volume</code> and <code>year</code>. Returns true only if every
   * available (non-null) field pair is equal.
   * @param au1 a BibliographicItem
   * @param au2 another BibliographicItem
   * @return <code>true</code> if each pair of non-null volume or year strings is equal 
   */
  public static boolean haveSameIndex(BibliographicItem au1, BibliographicItem au2) {
    String au1year = au1.getYear();
    String au2year = au2.getYear();
    String au1vol  = au1.getVolume();
    String au2vol  = au2.getVolume();
    // Null if either year is null, otherwise whether they match
    Boolean year = au1year!=null && au2year!=null ? au1year.equals(au2year) : null;
    // Null if either volume is null, otherwise whether they or their values match
    Boolean vol  = au1vol !=null && au2vol !=null ?
        (au1vol.equals(au2vol) || NumberUtil.areEqualValue(au1vol, au2vol)) : null;
    // Require both year and volume fields to be equal if they are available
    if (year!=null && vol!=null) return year && vol;
    // Otherwise return available year or volume match, or false
    else if (year!=null) return year;
    else if (vol!=null) return vol;
    else return false;
  }

  /**
   * Check whether two <code>BibliographicItem</code>s appear to be equivalent,
   * in that they have matching values in at least one identifying field
   * (ISSN or name), and all available indexing fields (volume or year). This is
   * to try and match up duplicate bibliographic records which arise from
   * duplicate releases of the same volume of a title under a different plugin
   * for example.
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
   * the caller. Currently this method just calls <code>compareIntStrings()</code>.
   *
   * @param year1 a string representing a year
   * @param year2 a string representing a year
   * @return the value 0 if the years are the same; less than 0 if the first is less than the second; and greater than 0 if the first is greater than the second
   */
  public static int compareStringYears(String year1, String year2)
      throws NumberFormatException {
    // Note that in practise if the strings do represent comparable publication years,
    // they will be 4 digits long and so comparable as strings with the same results.
    return compareIntStrings(year1, year2);
  }

  /**
   * Compare two strings that represent integers.
   * @param int1 a string which should parse as an integer
   * @param int2 a string which should parse as an integer
   * @return the value 0 if the ints are the same; less than 0 if the first is less than the second; and greater than 0 if the first is greater than the second
   * @throws NumberFormatException
   */
  public static int compareIntStrings(String int1, String int2)
      throws NumberFormatException {
    // Return zero if the strings are equal
    if (int1.equals(int2)) return 0;
    Integer i1 = NumberUtil.parseInt(int1);
    Integer i2 = NumberUtil.parseInt(int2);
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
   * To allow for identifers that themselves incorporate a hyphen, the input
   * string is only split around the centremost hyphen. If there is an even
   * number of hyphens, the input string is assumed not to represent a parsable
   * range.
   * <p>
   * It might also be useful to enforce further restrictions upon non-numerical
   * strings that are considered valid endpoints of a range - for example, that
   * when tokenised, each pair of non-numerical tokens are equivalent, or the
   * same length, or lexically either equal or increasing.
   *
   * @param range the input String
   * @return <tt>true</tt> if the input string represents a range
   */
  public static boolean isVolumeRange(String range) {
    if (range == null) return false;
    // Check first if it is a numerical range
    if (NumberUtil.isNumericalRange(range)) return true;
    // We are now dealing with either a non-range, or at least one non-numerical
    // endpoint. Find the range-separating hyphen.
    int hyphenPos = NumberUtil.findRangeHyphen(range);
    if (hyphenPos < 0) return false;
    // Zero-padding up to 4 positions should be ample for volumes
    int pad = 4;
    // Split string at middlemost hyphen, and pad numerical tokens with 0
    String s1 = NumberUtil.padNumbers(range.substring(0, hyphenPos).trim(), pad);
    String s2 = NumberUtil.padNumbers(range.substring(hyphenPos+1).trim(),  pad);
    // Check format of strings
    if (changeOfFormats(s1, s2)) return false;
    // TODO further check tokens if required
    // Check lexical order
    return s2.compareToIgnoreCase(s1) >= 0;
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
    boolean i1 = NumberUtil.isInteger(lastVal);
    boolean i2 = NumberUtil.isInteger(thisVal);
    boolean n1 = NumberUtil.isNumber(lastVal);
    boolean n2 = NumberUtil.isNumber(thisVal);
    // If one is digit-numerical and the other is non-numerical,
    // there is a definite change of formats.
    return (i1 && !n2) || (!n1 && i2);
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
          first.getJournalTitle(), yearRange);
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

}

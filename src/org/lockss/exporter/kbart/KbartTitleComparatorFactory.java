/*
 * $Id$
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.Comparator;

import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.exporter.kbart.KbartTitle.Field.SortType;
import org.lockss.util.*;

/**
 * A factory for comparators that will order <code>KbartTitle</code>s by a 
 * specified field. Depending on the field, the comparator will use natural 
 * string ordering, numerical ordering or the hybrid alphanumeric comparator. 
 * String comparison is the default fallback option. Only the alphanumeric 
 * and general string comparisons uses caching of the translated comparison 
 * strings. 
 * <p>
 * Any string comparison performed by any of the comparators is case-sensitive 
 * or not, as specified by <code>CASE_SENSITIVITY_DEFAULT</code>. The default 
 * for KBART records is currently set to false, as it is not currently clear 
 * from the recommendations what "alphabetical ordering" means. It is also 
 * possible to convert strings to unaccented forms for performing a comparison.
 * <p>
 * Methods are package-access.
 * <p>
 * <b>NOTE</b> This class requires a proper comparator for dates in ISO 8601 
 * date format.
 * 
 * @author Neil Mayo
 */
public class KbartTitleComparatorFactory {

  private static Logger log = Logger.getLogger("KbartTitleComparatorFactory");
 
  /** Whether KbartTitle string comparison is case-sensitive by default. */
  private static final boolean CASE_SENSITIVITY_DEFAULT = 
    Field.CASE_SENSITIVITY_DEFAULT;
  /** Whether string comparison ignores accents by default. */
  private static final boolean UNACCENTED_COMPARISON_DEFAULT = 
    Field.UNACCENTED_COMPARISON_DEFAULT;
  
  /**
   * Make a comparator that will compare <code>KbartTitle</code>s on a 
   * particular field. An appropriate comparator is returned for each of 
   * the sort types specified in the Field class. By default a standard 
   * string comparator is returned.
   * 
   * @param field the field on which the title should be sorted by the comparator
   * @return comparator which compares on natural ordering of the specified field
   */
  static Comparator<KbartTitle> getComparator(final Field field) {
    if (field.getSortType()==SortType.ALPHANUMERIC) 
      return getAlphanumericComparator(field);
    else if (field.getSortType()==SortType.NUMERIC) 
      return getNumericComparator(field);
    else if (field.getSortType()==SortType.DATE) 
      return getDateComparator(field);
    else return getStringComparator(field);
  }

  /**
   * Get a comparator which pads numeric tokens before comparing strings,
   * effectively comparing numbers by magnitude and text by natural string 
   * ordering. 
   * 
   * @param field the field on which to compare
   * @return an alphanumeric comparator on KbartTitles
   */
  static Comparator<KbartTitle> getAlphanumericComparator(final Field field) {
    return KbartFieldOrderComparator.getSingleton(field);
  }

  /**
   * Get a string comparator which compares on natural ordering of the string
   * values of the given field. Case-sensitivity is the default specified in 
   * the field.
   * 
   * @param field the field on which to compare
   * @return a comparator with the appropriate properties
   */
  static Comparator<KbartTitle> getStringComparator(final Field field) {
    //return getStringComparator(field, CASE_SENSITIVITY_DEFAULT);
    return KbartFieldOrderComparator.getSingleton(field);
  }
 
  /**
   * Get a date comparator. Currently this just compares on natural string 
   * ordering, as the field will only contain 4-digit years. Eventually this 
   * must return a proper comparator for dates in ISO 8601 date format.
   * 
   * @param field the field on which to compare
   * @return a case-insensitive comparator with the appropriate properties
   */
  static Comparator<KbartTitle> getDateComparator(final Field field) {
    return getStringComparator(field);    
  }

  /**
   * Get a numerical comparator which attempts to treat the field values as 
   * integers. If they are not parsable, the strings are compared as strings.
   * @param field the field on which to compare
   * @return a comparator which attempts to compare field values as ints
   */
  static Comparator<KbartTitle> getNumericComparator(final Field field) {
    return new Comparator<KbartTitle>() {
      @Override
      public int compare(KbartTitle f1, KbartTitle f2) {
	String tok1 = f1.getField(field);
	String tok2 = f2.getField(field);
	try {
	  // Parse number tokens as integers
	  Integer i1 = NumberUtil.parseInt(tok1);
	  Integer i2 = NumberUtil.parseInt(tok2);
	  return i1.compareTo(i2);
	} catch (NumberFormatException e) {
	  log.debug(
	      String.format("Could not compare as numbers: '%s' and '%s'\n", 
		  tok1, tok2)
	  );
	  // Compare as text
	  return compareStrings(tok1, tok1, false);
	}
      }
      @Override
      public String toString() {
	return field.getLabel()+" (numeric)";
      }
    };
  }

  /**
   * Compare using natural string ordering via <code>String.compareTo()</code>; 
   * used as a backup when other comparison fails. This method encapsulates the 
   * case-sensitivity aspect of the comparison.
   * 
   * @param str1
   * @param str2
   * @return
   */
  private static int compareStrings(String str1, String str2, 
      boolean caseSensitive) {
    if (caseSensitive) {
      str1 = str1.toLowerCase();
      str2 = str2.toLowerCase();
    }
    if (UNACCENTED_COMPARISON_DEFAULT) {
      str1 = StringUtil.toUnaccented(str1);
      str2 = StringUtil.toUnaccented(str2);
    }
    //log.debug(String.format("[%b] %s %s %s\n", caseSensitive, str1, (res>0?">":res<0?"<":"="), str2));
    return str1.compareTo(str2);
  }
  
}


package org.lockss.exporter.kbart;

import java.util.Comparator;

import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.exporter.kbart.KbartTitle.Field.SortType;
import org.lockss.util.Logger;

/**
 * A factory for comparators that will order <code>KbartTitle</code>s by a specified field.
 * Depending on the field, the comparator will use natural string ordering, numerical ordering 
 * or the hybrid alphanumeric comparator. String comparison is the default fallback option.
 * Any string comparison performed by any of the comparators is case-sensitive or not, as specified 
 * by <code>CASE_SENSITIVITY_DEFAULT</code>. The default for KBART records is currently set to false.
 * <p>
 * Methods are package-access.
 * <p>
 * <b>NOTE</b> This class requires a proper comparator for dates in ISO 8601 date format.
 * 
 * @author Neil Mayo
 */
public class KbartTitleComparatorFactory {

  private static Logger log = Logger.getLogger("KbartTitleComparatorFactory");
 
  /** Whether KbartTitle string comparison is case-sensitive by default. */
  private static final boolean CASE_SENSITIVITY_DEFAULT = Field.CASE_SENSITIVITY_DEFAULT;
  
  /**
   * Make a comparator that will compare <code>KbartTitle</code>s on a particular field.
   * An appropriate comparator is returned for each of the sort types specified in the
   * Field class. By default a standard string comparator is returned.
   * 
   * @param field the field on which the title should be sorted by the comparator
   * @return a comparator which compares on the natural ordering of the specified field
   */
  static Comparator<KbartTitle> getComparator(final Field field) {
    if (field.getSortType()==SortType.ALPHANUMERIC) return getAlphanumericComparator(field);
    else if (field.getSortType()==SortType.NUMERIC) return getNumericComparator(field);
    else if (field.getSortType()==SortType.DATE) return getDateComparator(field);
    else return getStringComparator(field);
  }

  /**
   * Compare using natural string ordering via <code>String.compareTo()</code>; used as a backup 
   * when other comparison fails. This method encapsulates the case-sensitivity aspect of the comparison.
   * 
   * @param str1
   * @param str2
   * @return
   */
  private static int compareStrings(String str1, String str2, boolean caseSensitive) {
    int res = caseSensitive ? str1.compareTo(str2) : str1.toLowerCase().compareTo(str2.toLowerCase()); 
    //System.out.printf("[%b] %s %s %s\n", caseSensitive, str1, (res>0?">":res<0?"<":"="), str2);
    //return res > 0 ? 1 : res < 0 ? -1 : 0; 
    return res;
  }
  
  
  /**
   * Get a string comparator which compares on natural ordering of the string
   * values of the given field. Case-sensitivity is the default specified in 
   * <code>CASE_SENSITIVITY_DEFAULT</code>.
   * 
   * @param field the field on which to compare
   * @return a comparator with the appropriate properties
   */
  static Comparator<KbartTitle> getStringComparator(final Field field) {
    return getStringComparator(field, CASE_SENSITIVITY_DEFAULT);
  }
    
  /**
   * Get a string comparator with the given attitude to case.
   * @param field the field on which to compare
   * @param caseSensitive whether to compare case-sensitively
   * @return a comparator with the appropriate properties
   */
  static Comparator<KbartTitle> getStringComparator(final Field field, final boolean caseSensitive) {
    return new Comparator<KbartTitle>() {
      @Override
      public int compare(KbartTitle f1, KbartTitle f2) {
	  String s1 = f1.getField(field);
	  String s2 = f2.getField(field);
	  return compareStrings(s1, s2, caseSensitive);
      }
    };
  }

  /**
   * Get a date comparator. Currently this just compares on natural string ordering, 
   * as the field will only contain 4-digit years. Eventually this must return a
   * proper comparator for dates in ISO 8601 date format.
   * 
   * @param field the field on which to compare
   * @return a case-insensitive comparator with the appropriate properties
   */
  static Comparator<KbartTitle> getDateComparator(final Field field) {
    return getStringComparator(field);    
  }

  /**
   * Get a numerical comparator which attempts to treat the field values as integers.
   * If they are not parsable, the strings are compared as strings.
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
	  Integer i1 = new Integer(tok1);
	  Integer i2 = new Integer(tok2);
	  return i1.compareTo(i2);
	} catch (NumberFormatException e) {
	  log.debug(String.format("Could not compare as numbers: '%s' and '%s'\n", tok1, tok2));
	  // Compare as text
	  return compareStrings(tok1, tok1, false);
	}
      }
    };
  }

  /**
   * Get an alphanumeric comparator which tokenizes the strings into numeric and non-numeric 
   * tokens, and compares each using string or numerical comparison as appropriate.
   * 
   * @param field the field on which to compare
   * @return a comparator which performs a string comparison but treats numbers by magnitude
   */
  static Comparator<KbartTitle> getAlphanumericComparator(final Field field) {
    return new KbartTitleAlphanumericComparator(field);
  }

  /**
   * An extension of <code>AlphanumericComparator</code> which orders <code>KbartTitle</code>s
   * by comparison on the specified sort field. By default the string comparison is performed 
   * with the case-sensitivity defined in <code>KbartTitle.Field</code>. 
   * This can be specified with an extra parameter in the constructor. 
   * 
   * @author Neil Mayo
   *
   */
  static class KbartTitleAlphanumericComparator extends AlphanumericComparator<KbartTitle> {
    
    /** The field on which to sort. */
    private final Field sortField;

    public KbartTitleAlphanumericComparator(Field field) {
      this(field, CASE_SENSITIVITY_DEFAULT);
    }
    
    public KbartTitleAlphanumericComparator(Field field, boolean caseSensitive) {
      super(caseSensitive);
      this.sortField = field;
    }
    
    /** 
     * Get from this title the string which informs the comparison.
     * @param title the title under comparison
     * @return the string value which informs the comparison
     */
    protected String getComparisonString(KbartTitle title) {
      return title.getField(sortField);
    }
  }
  
}


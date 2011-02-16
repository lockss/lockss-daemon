package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * An object representing a tuple of information about a KBART title. 
 * The list of default KBART data fields is ordinal, and so any method 
 * that provides default output such as a concatenated string representation 
 * or a property iterator, should provide them in the appropriate order.
 * For that reason they are stored as a SortedMap with a value for every field.
 * Null values are not allowed; an unused field returns the empty string as its value.
 * <p>
 * Future enhancements might include format checking (currently performed externally) 
 * and normalisation, and defaults for field values. In particular, here are some of the 
 * more pressing recommendations taken from 
 * <emph>KBART Phase I Recommended Practice</emph> document NISO-RP-9-2010
 * ({@url http://www.uksg.org/kbart/s1/summary})
 * along with their section references:
 * <ul> 
 *  <li><del>values should not contain tab characters (5.3.1.1) or markup (5.3.1.5)</del></li>
 *  <li><del>text should be encoded as UTF-8 (5.3.1.6)</del></li>
 *  <li><del>ISSN should match the 9-digit hyphenated format (5.3.2.3)</del></li>
 *  <li>Date formats should be ISO 8601, using as much of the YYYY-MM-DD format as necessary (5.3.2.5)</li>
 * </ul>
 * <p>
 * Note that this object implements <code>Comparable</code> so that a collection may be ordered.
 * This ordering is simply an alphabetic ordering of <code>KbartTitle</code> names.
 * This should perhaps be removed or adapted in favour of the more sophisticated AlphanumericComparator,
 * which accounts for the <i>magnitude</i> of numerical tokens. 
 * 
 * @author Neil Mayo
 */
public class KbartTitle implements Comparable<KbartTitle>, Cloneable {

  protected static Logger log = Logger.getLogger("KbartTitle");   
  
  /**
   * Implementation of the <code>Object.clone()</code> method. Creates a new 
   * KbartTitle and fills the fields map with values copied from this instance.
   */
  public KbartTitle clone()  {
    return new KbartTitle(this); 
  }

  /**
   * An alternative to using the <code>clone()</code> method, which requires one to catch
   * a <code>CloneNotSupportedException</code>.
   * 
   * @param other another KbartTitle whose fields to clone
   */
  public KbartTitle(KbartTitle other) {
    fields.putAll(other.fields);
  }

  public KbartTitle() {
    // Setup the fields map with empty defaults. This is so it has the same number
    // of elements as there are fields, for output iteration.
    for (Field f : Field.values()) {
      fields.put(f, "");
    }
  }
  
  /**
   * A sortable map of the fields to their values.
   */
  private final SortedMap<Field, String> fields = 
    new TreeMap<Field, String>(new Comparator<Field>() {
      public int compare(Field f1, Field f2) {
	return new Integer(f1.ordinal()).compareTo(f2.ordinal());
      }
    });
    
  /**
   * Set the value of a field. If the field is null, nothing is added. If the value is null, 
   * an empty value is added for the field. Some normalisation is performed; tabs are converted 
   * to spaces and the string is made conformant to UTF-8.
   * 
   * @param field the Field to set
   * @param value the string value to give the field
   */
  public void setField(Field field, String value) {
    if (field==null) return;
    if (value==null) value = "";
    fields.put(field, normalise(value));
  }

  /**
   * Get the recorded value of the field. If the field or the value is null, 
   * return an empty string.
   * 
   * @param field the Field object whose value is required
   * @return the string value of the field, or an empty string
   */
  public String getField(Field field) {
    if (field==null) return "";
    String value = fields.get(field);
    return value==null ? "" : value;
  }

  /**
   * Check whether the title has a value for the given field.
   * @param f the Field to check
   * @return whether the field has a non-empty value
   */
  public boolean hasFieldValue(Field f) {
    return !StringUtil.isNullString(fields.get(f)); 
  }
  
  /**
   * Normalise the string by removing tabs and converting characters to fit UTF-8.
   * 
   * @param s the string to normalise
   * @return the string without tabs, and in UTF-8
   */
  protected static String normalise(String s) {
    s = s.replaceAll("\\t", " ");
    return s;
  }
  
  /**
   * A concession to US sensibilities :).
   * @param s the string to normalise
   * @return the string without tabs, and in UTF-8
   */
  private static String normalize(String s) {
    return normalise(s);
  }
  
  
  /**
   * Return the field values as a collection whose iterator will return
   * the values in the natural order of the <code>Field</code> objects.
   * 
   * @return a collection of field values
   */
  public Collection<String> fieldValues() {
    return fields.values();
  }
  
  /**
   * Return the values of the fields listed in the argument, in the same order.
   * 
   * @param fieldIds a list of fields
   * @return a list of field values in the same order
   */
  public List<String> fieldValues(final List<Field> fieldIds) {
    if (fieldIds==null) return Collections.emptyList();
    return new ArrayList<String>() {{
      for (Field f: fieldIds) add(getField(f));
    }};
  }

  @Override
   public int compareTo(KbartTitle o) {
    String thisName = getField(Field.PUBLICATION_TITLE);
    String thatName = o.getField(Field.PUBLICATION_TITLE);
    return thisName.compareTo(thatName); 
  }

  public String toString() {
   return String.format("KbartTitle {%s [%s] (%s-%s)}", 
       fields.get(Field.PUBLICATION_TITLE), 
       fields.get(Field.PRINT_IDENTIFIER), 
       fields.get(Field.DATE_FIRST_ISSUE_ONLINE), 
       fields.get(Field.DATE_LAST_ISSUE_ONLINE)
   ); 
  }
  
  
  /**
   * Representation of a predefined KBART metadata field.
   * The following field definitions (name and description) are taken from 
   * the KBART recommended practices document NISO-RP-9-2010.
   * Note that they are ordinal - the fields should be emitted in the order
   * given below, which the Java Enum class will number from zero.
   * <p>
   * Each field also has a sort type, which specifies how each field will be ordered
   * if such a thing is requested. The options are enumerated in the SortType enum and 
   * allow for date ordering, numeric (magnitude) ordering, alphanumeric ordering (hybrid
   * of string and numerical ordering as appropriate), or plain string ordering.
   * By default any string comparison is performed according to the 
   * <code>CASE_SENSITIVITY_DEFAULT</code> flag in Field but this can be overridden in the 
   * comparator constructors.
   * <p>
   * If a sort type is not specified, the field defaults to plain string comparison. 
   * Note that if there is any chance of a supposedly numerical field containing
   * text, it might be safer to declare that field as alphanumerical. Although this will be less 
   * efficient it should provide the desired effect. Examples of such fields are the 
   * volume and issue number fields, which very occasionally have textual or mixed 'numbers'.
   * 
   */
  public static enum Field {

    /** Publication title. */
    PUBLICATION_TITLE("publication_title",
    "Publication title",
    SortType.ALPHANUMERIC),

    /** Print-format identifier (i.e., ISSN, ISBN, etc.). */
    PRINT_IDENTIFIER("print_identifier",
    "Print-format identifier (i.e., ISSN, ISBN, etc.)"),

    /** Online-format identifier (i.e., eISSN, eISBN, etc.). */
    ONLINE_IDENTIFIER("online_identifier",
    "Online-format identifier (i.e., eISSN, eISBN, etc.)"),

    /** Date of first issue available online. */
    DATE_FIRST_ISSUE_ONLINE("date_first_issue_online",
    "Date of first issue available online",
    SortType.DATE),

    /** Number of first volume available online. */
    NUM_FIRST_VOL_ONLINE("num_first_vol_online",
    "Number of first volume available online",
    SortType.ALPHANUMERIC),

    /** Number of first issue available online. */
    NUM_FIRST_ISSUE_ONLINE("num_first_issue_online",
    "Number of first issue available online",
    SortType.ALPHANUMERIC),

    /** Date of last issue available online (or blank, if coverage is to present). */
    DATE_LAST_ISSUE_ONLINE("date_last_issue_online",
    "Date of last issue available online (or blank, if coverage is to present)",
    SortType.DATE),

    /** Number of last volume available online (or blank, if coverage is to present). */
    NUM_LAST_VOL_ONLINE("num_last_vol_online",
    "Number of last volume available online (or blank, if coverage is to present)",
    SortType.ALPHANUMERIC),

    /** Number of last issue available online (or blank, if coverage is to present). */
    NUM_LAST_ISSUE_ONLINE("num_last_issue_online",
    "Number of last issue available online (or blank, if coverage is to present)",
    SortType.ALPHANUMERIC),

    /** Title-level URL. */
    TITLE_URL("title_url",
    "Title-level URL"),

    /** First author (for monographs). */
    FIRST_AUTHOR("first_author",
    "First author (for monographs)"),

    /** Title ID */
    TITLE_ID("title_id",
    "Title ID"),

    /** Emvargo information */
    EMBARGO_INFO("embargo_info",
    "Embargo information"),

    /** Coverage depth (e.g., abstracts or full text). */
    COVERAGE_DEPTH("coverage_depth",
    "Coverage depth (e.g., abstracts or full text)"),

    /** Coverage notes. */
    COVERAGE_NOTES("coverage_notes",
    "Coverage notes"),

    /** Publisher name (if not given in the file's title). */
    PUBLISHER_NAME("publisher_name",
    "Publisher name (if not given in the file's title)"),
    ;

    /** A list of the fields that indicate range information. */
    public static final EnumSet<Field> rangeFields = EnumSet.of(
        Field.DATE_FIRST_ISSUE_ONLINE, 
        Field.DATE_LAST_ISSUE_ONLINE,
        Field.NUM_FIRST_ISSUE_ONLINE,
        Field.NUM_LAST_ISSUE_ONLINE,
        Field.NUM_FIRST_VOL_ONLINE,
        Field.NUM_LAST_VOL_ONLINE
    );
    
    /** A list of the fields that indicate title id information. */
    public static final EnumSet<Field> idFields = EnumSet.of(
        Field.PUBLICATION_TITLE,
        Field.PRINT_IDENTIFIER,
        Field.ONLINE_IDENTIFIER
    ); 
    

    /** The default case-sensitivity of string comparisons on fields. */
    public static boolean CASE_SENSITIVITY_DEFAULT = false;
    
    /**
     * Each field has a label dictated by the KBART recommendations, and 
     * a description copied from the recommendations. It is also characterised 
     * as having a particular ordering.
     * @param label
     * @param description
     * @param sortType
     */
    Field(String label, String description, SortType sortType) {
      this.label = label;
      this.description = description;
      this.sortType = sortType;
    }
    /**
     * Create a field which is ordered by natural string ordering by default.
     * @param label
     * @param description
     */
    Field(String label, String description) {
      this(label, description, SortType.STRING);
    }

    /** A label for the field, used in output. */
    private final String label;
    /** The KBART description of the field. */
    private final String description;
    /** The sort type of the field - string, numerical, or alphanumerical, wherein numerical parts are converted and compared by magnitude. */
    private final SortType sortType;
 
    public String getLabel() {
      return label;
    }

    public String getDescription() {
      return description;
    }

    public SortType getSortType() {
      return sortType;
    }

    /** Whether the field should be considered alphanumeric for comparison - that is, numerical parts are converted and compared by magnitude. */
    public boolean isAlphanumeric() { 
      return sortType == SortType.ALPHANUMERIC; 
    }
    
    /** Whether the field should be considered numeric for comparison - that is, converted and compared by magnitude. */
    public boolean isNumeric() { 
      return sortType == SortType.NUMERIC;
    }
    
    /** A static ordered list of all the Field labels. */
    private static List<String> labels = new ArrayList<String>() {{
        for (Field f : values()) add(f.getLabel());
    }};
    
    /** Get the ordered list of labels. */
    public static List<String> getLabels() { return labels; }
    
    /** 
     * Get an ordered list of labels for a specific list of fields. 
     * The list of labels is in the same order as the field list. 
     * If an unordered collection is supplied, the labels will be
     * in whatever order the fields are returned by the collections's 
     * iterator.
     */
    public static List<String> getLabels(final Collection<Field> fields) {
      return new ArrayList<String>() {{
	for (Field f: fields) add(f.label);
      }};
    }
        
    /**
     * Return the EnumSet containing all enums from first to last.
     * 
     * @return an EnumSet
     */
    public static EnumSet<Field> getFieldSet() {
      return EnumSet.allOf(Field.class);
    }

    /** Each field will be of a particular ype which requires a particular type of sorting. */
    public static enum SortType {
      STRING, ALPHANUMERIC, NUMERIC, DATE;
    }

  }  

}

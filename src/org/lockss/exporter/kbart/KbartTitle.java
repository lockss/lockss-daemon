package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An object representing a tuple of information about a KBART title. 
 * The list of default KBART data fields is ordinal, and so any method 
 * that provides default output such as a concatenated string representation 
 * or a property iterator, should provide them in the appropriate order.
 * For that reason they are stored as a SortedMap with a value for every field.
 * Null values are not allowed; an unused field returns the empty string as its value.
 * <p>
 * Future enhancements might include format checking and normalisation, and defaults 
 * for field values. In particular, here are some of the more pressing recommendations taken from 
 * <emph>KBART Phase I Recommended Practice</emph> document NISO-RP-9-2010
 * ({@link http://www.uksg.org/kbart/s1/summary})
 * along with their section references:
 * <ul> 
 *  <li>values should not contain tab characters (5.3.1.1) or markup (5.3.1.5)</li>
 *  <li>text should be encoded as UTF-8 (5.3.1.6)</li>
 *  <li>ISSN should match the 9-digit hyphenated format (5.3.2.3)</li>
 *  <li>Date formats should be ISO 8601, using as much of the YYYY-MM-DD format as necessary (5.3.2.5)</li>
 * </ul>
 * <p>
 * Note that this object implements <code>Comparable</code> so that a collection may be ordered.
 * This ordering is simply an alphabetic ordering of <code>KbartTitle</code> names.
 * 
 * @author Neil Mayo
 */
public class KbartTitle implements Comparable<KbartTitle>, Cloneable {

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
   * an empty value is added for the field.
   * 
   * @param field the Field to set
   * @param value the string value to give the field
   */
  public void setField(Field field, String value) {
    if (field==null) return;
    if (value==null) value = "";
    fields.put(field, value);
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
   * Returns the field values as a collection whose iterator will return
   * the values in the natural order of the <code>Field</code> objects.
   * 
   * @return a collection of field values
   */
  public Collection<String> fieldValues() {
    return fields.values();
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
   * */
  public static enum Field {

    PUBLICATION_TITLE("publication_title",
    "Publication title"),       

    PRINT_IDENTIFIER("print_identifier",
    "Print-format identifier (i.e., ISSN, ISBN, etc.)"),

    ONLINE_IDENTIFIER("online_identifier",
    "Online-format identifier (i.e., eISSN, eISBN, etc.)"),

    DATE_FIRST_ISSUE_ONLINE("date_first_issue_online",
    "Date of first issue available online"),

    NUM_FIRST_VOL_ONLINE("num_first_vol_online",
    "Number of first volume available online"),

    NUM_FIRST_ISSUE_ONLINE("num_first_issue_online",
    "Number of first issue available online"),

    DATE_LAST_ISSUE_ONLINE("date_last_issue_online",
    "Date of last issue available online (or blank, if coverage is to present)"),

    NUM_LAST_VOL_ONLINE("num_last_vol_online",
    "Number of last volume available online (or blank, if coverage is to present)"),

    NUM_LAST_ISSUE_ONLINE("num_last_issue_online",
    "Number of last issue available online (or blank, if coverage is to present)"),

    TITLE_URL("title_url",
    "Title-level URL"),

    FIRST_AUTHOR("first_author",
    "First author (for monographs)"),

    TITLE_ID("title_id",
    "Title ID"),

    EMBARGO_INFO("embargo_info",
    "Embargo information"),

    COVERAGE_DEPTH("coverage_depth",
    "Coverage depth (e.g., abstracts or full text)"),

    COVERAGE_NOTES("coverage_notes",
    "Coverage notes"),

    PUBLISHER_NAME("publisher_name",
    "Publisher name (if not given in the file's title)");

    /** 
     * Each field has a label dictated by the KBART recommendations, and 
     * a description copied from the recommendations. 
     */
    Field(String label, String description) {
      this.label = label;
      this.description = description;
    }

    /** A label for the field, used in output. */
    private final String label;
    /** The KBAT description of the field. */
    private final String description;

    public String getLabel() {
      return label;
    }

    public String getDescription() {
      return description;
    }

    /** A static ordered list of all the Field labels. */
    private static List<String> labels = new ArrayList<String>() {{
        for (Field f : values()) {
          add(f.getLabel());
        }
      }};

    // static methods
    /** Get the ordered list of labels. */
    public static List<String> getLabels() { return labels; }

  }


}

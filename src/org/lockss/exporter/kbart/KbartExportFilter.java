package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.lockss.exporter.kbart.KbartTitle.Field;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

import org.lockss.util.CollectionUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.Logger;


/**
 * A filter for a KBART exporter, which processes the KBART-format data 
 * before export. This allows the rather strict output to be customised
 * for other purposes. A filter is created with an ordered list of fields, 
 * indicating the output columns, and an option to omit empty fields.
 * <p>  
 * The exporter can then use the filter transparently to get ordered field 
 * values.
 * <p>
 * The filter also takes account of the fields which are visible in the output
 * to decide whether a record is a duplicate of the previous one. For example 
 * if the output omits range fields (volume and issue information), the multiple 
 * ranges which you get on a title with a coverage gap are undesirable when there 
 * are no other distinguishing fields. The filter does <b>not</b> remove duplicates 
 * that might occur across the whole output set, for example from setting the output 
 * to only range information.
 * <p>
 * If there are no id fields (title, issn, eissn) specified in the field ordering, 
 * the output will be essentially meaningless anyway.
 * 
 * 
 * @author Neil Mayo
 */
public class KbartExportFilter {
  
  private static Logger log = Logger.getLogger("KbartExportFilter");

  public static final boolean OMIT_EMPTY_FIELDS_DEFAULT = false;
  public static final FieldOrdering FIELD_ORDERING_DEFAULT = PredefinedFieldOrdering.KBART;
  
  /** 
   * A list of the fields that indicate range information. If any of these is visible it is 
   * sensible to show multiple ranges for a title. 
   */
  private static final EnumSet<Field> rangeFields = Field.rangeFields;
  
  /** 
   * A list of the fields that indicate title id information. If any of these which is visible 
   * differs between consecutive titles, we should display both titles. 
   */
  private static final EnumSet<Field> idFields = Field.idFields;

  /** A caveat for field orderings that may have duplicates. No longer necessary. */
  private static String duplicatesCaveat = "";
  //private static String duplicatesCaveat = "Note that there will be duplicates if a journal has several coverage ranges.";

  
  // --------------------------------------------------------------------
  // Instance variables
  // --------------------------------------------------------------------
  /** A reference to the previous title so that subsequent properties can be compared. */
  private KbartTitle lastOutputTitle;
  /** Whether or not any range fields are included in the display output. That is, after taking account of omissions. */
  private boolean rangeFieldsIncludedInDisplay = true;
  /** Whether or not any id fields are included in the display output. That is, after taking account of omissions. */
  private boolean idFieldsIncludedInDisplay = true;
  
  /** An ordering of the fields that should be displayed. */
  private final FieldOrdering fieldOrdering;
  /** The titles which will be filtered. */  
  private final List<KbartTitle> titles;
  
  /** Whether to omit empty field columns from the output. False by default. */
  private final boolean omitEmptyFields;
  /** A set of fields which have no entries in any of the titles. This will be filled in if omitEmptyFields is true. */
  private final EnumSet<Field> emptyFields;
  
  /** 
   * The filtered list of fields which will be shown. This is the field ordering of this filter,
   * minus any empty fields if <code>omitEmptyFields</code> is set.
   */  
  private final List<Field> visibleFieldOrder;

  /**
   * Make an identity filter on the title set. This uses the default KBART field ordering and 
   * does not omit empty fields.
   * @param titles the titles to be exported
   * @return a filter with pure KBART settings
   */
  public static KbartExportFilter identityFilter(List<KbartTitle> titles) {
    return new KbartExportFilter(titles, PredefinedFieldOrdering.KBART, false);
  }
  
  /**
   * Make a filter on the title set using default values. This uses the default field ordering and 
   * the default approach to empty fields.
   * @param titles the titles to be exported
   * @return a filter with default settings
   */
  public KbartExportFilter(List<KbartTitle> titles) {
    this(titles, FIELD_ORDERING_DEFAULT, OMIT_EMPTY_FIELDS_DEFAULT);
  }
  
  /**
   * Default constructor takes a list of KbartTitles to be exported, and a
   * field ordering.
   * The exporter then does some reasonably costly processing, iterating 
   * through the titles in order to establish which columns are entirely 
   * empty.
   * 
   * @param titles the titles to be exported
   * @param ordering an ordering to impose on the fields of each title
   */
  public KbartExportFilter(List<KbartTitle> titles, FieldOrdering ordering) {
    this(titles, ordering, OMIT_EMPTY_FIELDS_DEFAULT);
  }
  
  /**
   * An alternative constructor that allows one to specify whether or not to show columns 
   * which are entirely empty. If <code>omitEmptyFields</code> is true, the 
   * <code>emptyFields</code> set is filled with the names of 
   * the empty fields. This is quite an expensive operation so is performed here at construction.
   * 
   * @param titles the titles to be exported
   * @param format the output format
   * @param ordering an ordering to impose on the fields of each title
   * @param omitEmptyFields whether to omit empty field columns from the output
   */
  public KbartExportFilter(List<KbartTitle> titles, FieldOrdering ordering, boolean omitEmptyFields) {
    this.titles = titles;
    this.fieldOrdering = ordering;
    this.omitEmptyFields = omitEmptyFields;
    // Work out the list of empty fields if necessary
    this.emptyFields = omitEmptyFields ? findEmptyFields() : EnumSet.noneOf(Field.class);
    // Create a list of the visible (non-omitted) fields out of the supplied ordering
    this.visibleFieldOrder = new Vector<Field>(fieldOrdering.getOrdering());
    this.visibleFieldOrder.removeAll(emptyFields);
    this.rangeFieldsIncludedInDisplay = !CollectionUtil.isDisjoint(visibleFieldOrder, rangeFields);
    this.idFieldsIncludedInDisplay = !CollectionUtil.isDisjoint(visibleFieldOrder, idFields);
  } 
  
  /**
   * Calculate which fields have no values across the whole range of titles. Iterates through all the 
   * titles for each field value, until an entry is found or the end of the title list is reached.
   * 
   * @return a set of fields which are empty
   */
  private EnumSet<Field> findEmptyFields() {
    long s = System.currentTimeMillis();
    EnumSet<Field> empty = EnumSet.allOf(Field.class);
    for (Field f : Field.getFieldSet()) {
      // Check if any title has a value for this field
      for (KbartTitle kbt : titles) {
	if (kbt.hasFieldValue(f)) {
	  empty.remove(f);
	  break;
	}
      }
    }
    // Log the time, as this is an expensive way to monitor empty fields.
    // It should be done somehow during title conversion.
    log.debug("findEmptyFields() took "+(System.currentTimeMillis()-s)+"s");
    return empty;
  }
  
  /**
   * Accessor for definitive field order. This is the field ordering of this filter,
   * minus any empty fields if <code>omitEmptyFields</code> is set.
   * 
   * @return the field order produced by this filter  
   */
  public List<Field> getVisibleFieldOrder() {
    return this.visibleFieldOrder;
  }
  
  /**
   * Whether this filter is set to omit empty fields.
   * 
   * @return whether this filter is set to omit empty fields 
   */
  public boolean isOmitEmptyFields() {
    return omitEmptyFields;
  }
  
  /**
   * Get the field ordering with which this filter is configured.
   * @return the filter's field ordering 
   */
  public FieldOrdering getFieldOrdering() {
    return fieldOrdering; 
  }
  
  /**
   * Get the set of fields from the filter's field ordering which are empty.
   * @return the set of empty fields form the ordering
   */
  public Set<Field> getEmptyFields() {
    return emptyFields; 
  }
  
  /**
   * Whether fields were omitted manually. That is, the specified field
   * ordering is shorter than the available number of fields. 
   * @return whether empty fields were omitted.
   */
  public boolean omittedFieldsManually() {
    return fieldOrdering.getOrdering().size() < Field.values().length; 
  }
  
  /**
   * Whether empty fields were omitted. That is, <code>omitEmptyFields</code> 
   * is set and there were empty fields in the ordering. 
   * @return whether empty fields were omitted.
   */
  public boolean omittedEmptyFields() {
    if (!omitEmptyFields) return false;
    EnumSet<Field> omittableFields = EnumSet.copyOf(fieldOrdering.getFields());
    omittableFields.retainAll(emptyFields);
    return omitEmptyFields && omittableFields.size() > 0; 
  }
  
  /**
   * Extract a title's field values based on the definitive field ordering. This is the list
   * of values matching the fields specified by <code>getVisibleFieldOrder()</code>.
   *  
   * @param title the title whose fields to extract
   * @return field values produced by this filter for the title  
   */
  public List<String> getVisibleFieldValues(KbartTitle title) {
    return title.fieldValues(getVisibleFieldOrder());
  }
  
  /**
   * Decide whether the current title record should be displayed, based on whether
   * it is a duplicate of the previous record, given the combination of output fields
   * included in the ordering. If range fields are included in the output, or if 
   * identifying properties change between titles, the title will be shown.
   * <p>
   * If there are neither range fields nor id fields in the output, we can't tell
   * if it is a duplicate so we show it anyway. Output with no id fields is pretty 
   * meaningless though.
   * <p>
   * Note that the method also sets the last output title if the response is true.
   * Thus calling this method multiple times on the same title will give false after 
   * the first (unless there are no identifying fields).
   * 
   *   
   * @param currentTitle the current title
   * @return whether to show currentTitle
   */
  public boolean isTitleForOutput(KbartTitle currentTitle) {
    // The approach is to trueify this variable. If it becomes true, at the end of the method 
    // the lastOutputTitle is set before the result is returned. Do not return early!
    boolean isOutput = false;
    // Show title if it has no range or id fields by which we can decide duplicates
    if (!rangeFieldsIncludedInDisplay && !idFieldsIncludedInDisplay) {
      isOutput = true;      
    }

    // Show the title if it is the first or the output includes range fields
    if (lastOutputTitle==null || rangeFieldsIncludedInDisplay) {
      isOutput = true;
    } else if (!isOutput) { // don't do this check if we've already trueified the var
      // At this point there are no range fields and this is not the first title
      // Show the title if any visible idField differs between titles  
      for (Field f : idFields) {
	if (visibleFieldOrder.contains(f) && 
	    !lastOutputTitle.getField(f).equals(currentTitle.getField(f))) { 
	  isOutput = true;
	  break;
	}
      }
    }
    // Record the previous title
    if (isOutput) lastOutputTitle = currentTitle;
    return isOutput;
  }
  
  
  
  /**
   * A convenient way of storing a field ordering as both an ordered list and 
   * an unordered but more efficient set. This is to save converting between them
   * unnecessarily. The list and the set must contain the same elements.
   * <p>
   * Setting an order implies a list of fields. The output will include all of the 
   * fields in the ordering unless they are omitted via <code>omitEmptyField</code>. 
   * Fields not mentioned in the ordering will be omitted.
   * 
   * @author Neil Mayo
   */
  public static interface FieldOrdering {
    public abstract EnumSet<Field> getFields();
    public abstract List<Field> getOrdering();
    //public abstract FieldOrdering getDefaultOrdering();
  }
  
  /**
   * A custom field ordering allows an arbitrary order of Fields to be specified at construction.
   * 
   * @author Neil Mayo
   */
  public static class CustomFieldOrdering implements FieldOrdering {
    public final EnumSet<Field> fields;
    public final List<Field> ordering;
    //private static final CustomFieldOrdering defaultOrdering = new CustomFieldOrdering(Arrays.asList(Field.values()));
    
    //public static final CustomFieldOrdering getDefaultOrdering() {
    //public final CustomFieldOrdering getDefaultOrdering() { return defaultOrdering; }
    
    /** A mapping of labels to fields, for interpreting a user-supplied ordering. */
    private static final Map<String, Field> labelFields = new HashMap<String, Field>() {{
      for (Field f : Field.values()) put(f.getLabel(), f);
    }};
    
    
    /**
     * Create a custom field ordering from a list of fields. 
     * @param ordering the ordering which will be used
     */
    public CustomFieldOrdering(List<Field> ordering) {
      this.ordering = ordering;
      this.fields = EnumSet.copyOf(ordering);
    }
    /**
     * Create a custom field ordering from a string containing a list of field labels
     * separated by whitespace. 
     * @param orderStr a string of field labels separated by white space
     */
    public CustomFieldOrdering(String orderStr) throws CustomFieldOrderingException {
      String[] labs = StringUtils.split(orderStr);
      this.ordering = new ArrayList<Field>(); 
      for (String s : labs) {
	// Ignore white space lines
	if (s.trim().isEmpty()) continue;
	Field f = labelFields.get(s);
	// Throw exception if the label is not valid
	if (f==null) throw new CustomFieldOrderingException(s, "String '"+s+"' does not refer to a valid field.");
	this.ordering.add(labelFields.get(s));
      }
      // Sanity checks
      if (this.ordering.isEmpty()) throw new CustomFieldOrderingException("", "No valid fields specified.");
      
      if (!Field.idFields.clone().removeAll(this.ordering)) {
	String idFieldStr = String.format("(%s)", StringUtils.join(Field.getLabels(Field.idFields), ", "));
	throw new CustomFieldOrderingException("", String.format("No identifying fields specified. You must include one of %s.", idFieldStr));
      }
      this.fields = EnumSet.copyOf(this.ordering);
    }
    public EnumSet<Field> getFields() {
      return this.fields;
    }
    public List<Field> getOrdering() {
      return this.ordering;
    }
    
    /** 
     * A custom exception caused when an invalid custom ordering string is passed to the constructor 
     * of the CustomFieldOrdering.
     */
    public class CustomFieldOrderingException extends Exception {
      /** A record of the string label for which a field could not be found. */
      private final String errorLabel;
      public CustomFieldOrderingException(String s) {
	super();
	this.errorLabel = s;
      }
      public CustomFieldOrderingException(String s, String msg) {
	super(msg);
	this.errorLabel = s;
      }
      public String getErrorLabel() { return errorLabel; }
    }
  }
      
  /**
   * An enum for specifying a variety of <code>FieldOrdering</code>s. It is possible to specify 
   * an incomplete list of fields, and other fields will be omitted. 
   * 
   * @author Neil Mayo
   */
  public static enum PredefinedFieldOrdering implements FieldOrdering {
        
    // Default KBART ordering
    KBART("KBART Default", "Standard KBART fields and ordering", Field.values()),

    // An ordering that puts publisher and publication first; mainly for use by Vicky
    PUBLISHER_PUBLICATION("Publisher oriented", "Standard KBART fields with publisher name at the start",
	new Field[] {
	    PUBLISHER_NAME, 
	    PUBLICATION_TITLE,
	    PRINT_IDENTIFIER,
	    ONLINE_IDENTIFIER,
	    DATE_FIRST_ISSUE_ONLINE,
	    NUM_FIRST_VOL_ONLINE,
	    NUM_FIRST_ISSUE_ONLINE,
	    DATE_LAST_ISSUE_ONLINE,
	    NUM_LAST_VOL_ONLINE,
	    NUM_LAST_ISSUE_ONLINE,
	    TITLE_URL,
	    FIRST_AUTHOR,
	    TITLE_ID,
	    EMBARGO_INFO,
	    COVERAGE_DEPTH,
	    COVERAGE_NOTES
	}
    ),
    
    // Minimal details
    MINIMAL("Minimal Details", "Publisher, Publication, ISSN, eISSN",
	new Field[] {
	    PUBLISHER_NAME, 
	    PUBLICATION_TITLE,
	    PRINT_IDENTIFIER,
	    ONLINE_IDENTIFIER
	}
    ),
	
    // Title and ISSN only (will have duplicates)
    TITLE_ISSN("Title and ISSN", "Title and ISSN only. "+duplicatesCaveat, 
	new Field[] {
	    PUBLICATION_TITLE,
	    PRINT_IDENTIFIER
        }
    ),
    
    // ISSN only (will have duplicates)
    ISSN_ONLY("ISSN only", "ISSN only. "+duplicatesCaveat, new Field[] {PRINT_IDENTIFIER});

    // These are used in an interface to describe the ordering. */
    /** A name to identify the ordering. */
    public final String displayName;
    /** A description of the ordering. */
    public final String description;
    /** A set of fields included in this ordering. */
    public final EnumSet<Field> fields;
    /** A ordered list of fields in this ordering. */
    public final List<Field> ordering;
     
    public EnumSet<Field> getFields() { return this.fields; }
    public List<Field> getOrdering() { return this.ordering; }

    PredefinedFieldOrdering(String displayName, String description, Field[] fieldOrder) {
      this.displayName = displayName;      
      this.description = description;      
      ordering = Arrays.asList(fieldOrder);
      fields = EnumSet.copyOf(ordering);
    }
    
  }


}

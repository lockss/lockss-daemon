/*
 * $Id: KbartExportFilter.java,v 1.10 2012-06-15 16:55:35 easyonthemayo Exp $
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

import java.util.*;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.exporter.kbart.KbartTitle.Field;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

import org.lockss.util.CollectionUtil;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

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
 * if the output omits range fields (volume and issue information), the 
 * multiple ranges which you get on a title with a coverage gap are undesirable 
 * when there are no other distinguishing fields. The filter does <b>not</b> 
 * remove duplicates that might occur across the whole output set, for example 
 * from setting the output to only range information.
 * <p>
 * If there are no id fields (title, issn, eissn) specified in the field 
 * ordering, the output will be essentially meaningless anyway.
 * <p>
 * This class is aware of whether a title's health should be shown in the 
 * output, but this should be maintained completely separately to the actual 
 * list of KBART Fields which are included.
 *
 * <h3>Note: Iteration</h3>
 * The export filter cannot accept an iterator instead of a list, because
 * it needs to sort KbartTitles alphabetically on title as per KBART Phase I
 * recommendation 5.3.1.11, or according to the first two fields in a custom
 * field ordering. It also needs to produce summary information from analysis of
 * the entire list of KbartTitles, if the custom option omitEmptyFields is true.
 * <p>
 * Using an iterator instead of a list would be less memory intensive, but this
 * would only work with uncustomised, standard KBART exports, and would require
 * the input list to be ordered.
 *
 * @author Neil Mayo
 */
public class KbartExportFilter {
  
  private static Logger log = Logger.getLogger("KbartExportFilter");

  public static final boolean OMIT_EMPTY_FIELDS_DEFAULT = 
      KbartExporter.omitEmptyFieldsByDefault;
  public static final boolean SHOW_HEALTH_RATINGS_DEFAULT = 
      KbartExporter.showHealthRatingsByDefault;
  public static final PredefinedFieldOrdering FIELD_ORDERING_DEFAULT =
      PredefinedFieldOrdering.KBART;

  public static final String HEALTH_FIELD_LABEL = "Health";
  
  /** 
   * A list of the fields that indicate range information. If any of these is 
   * visible it is sensible to show multiple ranges for a title. 
   */
  private static final EnumSet<Field> rangeFields = Field.rangeFields;
  
  /** 
   * A list of the fields that indicate title id information. If any of these 
   * which is visible differs between consecutive titles, we should display 
   * both titles. 
   */
  private static final EnumSet<Field> idFields = Field.idFields;

  // --------------------------------------------------------------------
  // Instance variables
  // --------------------------------------------------------------------
  /**
   * A reference to the previous title so that subsequent properties can be 
   * compared. 
   */
  private KbartTitle lastOutputTitle;
  /** 
   * Whether or not any range fields are included in the display output. 
   * That is, after taking account of omissions. 
   */
  private boolean rangeFieldsIncludedInDisplay = true;
  /**
   * Whether or not any id fields are included in the display output. 
   * That is, after taking account of omissions. 
   */
  private boolean idFieldsIncludedInDisplay = true;
  
  /** An ordering of the fields that should be displayed. */
  private final FieldOrdering fieldOrdering;
  /** The titles which will be filtered. */  
  private final List<KbartTitle> titles;
  
  /** Whether to omit empty field columns from the output. False by default. */
  private final boolean omitEmptyFields;
  /** 
   * A set of fields which have no entries in any of the titles. This will 
   * be filled in if omitEmptyFields is true. 
   */
  private final EnumSet<Field> emptyFields;
  /** Whether to show health ratings for each title range. */
  private final boolean showHealthRatings;
  
  /** 
   * The filtered list of fields which will be shown. This is the field 
   * ordering of this filter, minus any empty fields if 
   * <code>omitEmptyFields</code> is set.
   */  
  private final List<Field> visibleFieldOrder;

  /**
   * Make an identity filter on the title set. This uses the default KBART 
   * field set and ordering and does not omit empty fields.
   * @param titles the titles to be exported
   * @return a filter with pure KBART settings
   */
  public static KbartExportFilter identityFilter(List<KbartTitle> titles) {
    return new KbartExportFilter(titles, PredefinedFieldOrdering.KBART, false, false);
    //return new KbartExportFilter(titles, PredefinedFieldOrdering.KBART, false);
  }
 
  /**
   * Whether there are fields included in the given list which provide
   * range information for a title. 
   * @param fields a list of the fields to check  
   * @return whether the list includes any range fields
   */
  public static boolean includesRangeFields(List<Field> fields) {
    return !CollectionUtil.isDisjoint(fields, rangeFields); 
  }

  /**
   * Make a filter on the title set using default values. This uses the 
   * default field ordering and the default approach to empty fields.
   * @param titles the titles to be exported
   * @return a filter with default settings
   */
  public KbartExportFilter(List<KbartTitle> titles) {
    this(titles, FIELD_ORDERING_DEFAULT, OMIT_EMPTY_FIELDS_DEFAULT, 
        SHOW_HEALTH_RATINGS_DEFAULT);
    //this(titles, FIELD_ORDERING_DEFAULT, OMIT_EMPTY_FIELDS_DEFAULT);
  }
  
  /**
   * Default constructor takes a list of KbartTitles to be exported, and a
   * field ordering. The exporter then does some reasonably costly processing,
   * iterating through the titles in order to establish which columns are
   * entirely empty.
   * <p>
   * Due to this processing, it is not possible to accept an iterator instead of
   * a list, which would be less memory intensive.
   * 
   * @param titles the titles to be exported
   * @param ordering an ordering to impose on the fields of each title
   */
  public KbartExportFilter(List<KbartTitle> titles, FieldOrdering ordering) {
    this(titles, ordering, OMIT_EMPTY_FIELDS_DEFAULT, SHOW_HEALTH_RATINGS_DEFAULT);
    //this(titles, ordering, OMIT_EMPTY_FIELDS_DEFAULT);
  }


  /**
   * An alternative constructor that allows one to specify whether or not to
   * show columns which are entirely empty, and uses the default for show health
   * ratings.
   *
   * @param titles the titles to be exported
   * @param ordering an ordering to impose on the fields of each title
   * @param omitEmptyFields whether to omit empty field columns from the output
   */
  public KbartExportFilter(List<KbartTitle> titles, FieldOrdering ordering,
                           boolean omitEmptyFields) {
    this(titles, ordering, omitEmptyFields, SHOW_HEALTH_RATINGS_DEFAULT);
  }

  /**
   * An alternative constructor that allows one to specify whether or not to 
   * show columns which are entirely empty. If <code>omitEmptyFields</code> is 
   * true, the <code>emptyFields</code> set is filled with the names of the 
   * empty fields. This is an expensive operation which requires iteration over
   * potentially all of the titles, so is performed here at construction.
   * <p>
   * Due to this processing, it is not possible to accept an iterator instead of
   * a list, which would be less memory intensive.
   *
   * @param titles the titles to be exported
   * @param ordering an ordering to impose on the fields of each title
   * @param omitEmptyFields whether to omit empty field columns from the output
   */
  public KbartExportFilter(List<KbartTitle> titles, FieldOrdering ordering,
                           boolean omitEmptyFields, boolean showHealthRatings) {
    this.titles = titles;
    this.fieldOrdering = ordering;
    this.omitEmptyFields = omitEmptyFields;
    this.showHealthRatings = showHealthRatings;
    // Work out the list of empty fields if necessary
    this.emptyFields = omitEmptyFields ? findEmptyFields() : 
        EnumSet.noneOf(Field.class);
    // Create a list of the visible (non-omitted) fields out of the supplied ordering
    this.visibleFieldOrder = new Vector<Field>(fieldOrdering.getOrdering());
    this.visibleFieldOrder.removeAll(emptyFields);
    this.rangeFieldsIncludedInDisplay = 
        !CollectionUtil.isDisjoint(visibleFieldOrder, rangeFields);
    this.idFieldsIncludedInDisplay = 
        !CollectionUtil.isDisjoint(visibleFieldOrder, idFields);
  } 

  public void sortTitlesByFirstTwoFields() {
    // Sort on just the first 2 columns (max):
    StringBuilder sb = new StringBuilder("Sort by ");
    sb.append(visibleFieldOrder.get(0));
    if (visibleFieldOrder.size() > 1)
      sb.append(" | ").append(visibleFieldOrder.get(1));
    log.debug(sb.toString());
    sortTitlesByFields( 
        visibleFieldOrder.subList(0, Math.min(2, visibleFieldOrder.size()))
    );
  }

  /**
   * Sort the titles by each of the fields specified; the first field is the 
   * primary sort field, then subsequent fields are consulted if the previous 
   * does not result in an absolute ordering.
   * 
   * @param fields a list of fields to sort on
   */
  private void sortTitlesByFields(List<Field> fields) {
    if (fields==null || fields.size()==0) return;
    ComparatorChain cc = new ComparatorChain();
    for (Field f : fields) {
      Comparator<KbartTitle> minor = KbartTitleComparatorFactory.getComparator(f);
      cc.addComparator(minor);
    }
    log.debug("Sorting titles by "+cc);
    Collections.sort(this.titles, cc);
  }
  
  /**
   * Calculate which fields have no values across the whole range of titles. 
   * Iterates through all the titles for each field value, until an entry is 
   * found or the end of the title list is reached.
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
   * Get a list of strings representing the labels for display columns. This
   * will include all the labels of visible fields, plus any extra columns 
   * such as health rating.
   * 
   * @param scope the scope of the export
   * @return a list of labels for the exported columns of data
   */
  public List<String> getColumnLabels(ContentScope scope) {
    List<String> l = new ArrayList(this.visibleFieldOrder);
    // Health rating is only added if showHealthRatings is true and the scope
    // is not ALL - neither of these should be true when processing external
    // data.
    if (showHealthRatings && scope!=ContentScope.ALL) l.add(HEALTH_FIELD_LABEL);
    return l;
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
   * Whether this filter is set to show health ratings.
   * 
   * @return whether this filter should show health ratings 
   */
  public boolean isShowHealthRatings() {
    return showHealthRatings;
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
   * @return the set of empty fields from the ordering
   */
  public Set<Field> getEmptyFields() {
    return emptyFields; 
  }
  
  /**
   * Get the set of fields from the filter's field ordering which were omitted 
   * due to being empty.
   * @return the set of empty fields which were omitted from the ordering
   */
  public Set<Field> getOmittedEmptyFields() {
    Set<Field> empties = EnumSet.copyOf(fieldOrdering.getFields());
    empties.retainAll(emptyFields);
    return empties;
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
    Set<Field> omittableFields = getOmittedEmptyFields();
    return omitEmptyFields && omittableFields.size() > 0; 
  }
  
  /**
   * Extract a title's field values based on the definitive field ordering. 
   * This is the list of values matching the fields specified by 
   * <code>getVisibleFieldOrder()</code>, plus any extra columns 
   * such as health rating. Extra columns are added transparently by the
   * {@link KbartTitleHealthWrapper} where necessary.
   *  
   * @param title the title whose fields to extract
   * @return field values produced by this filter for the title  
   */
  public List<String> getVisibleFieldValues(KbartTitle title) {
    List<String> res = title.fieldValues(getVisibleFieldOrder());
    //if (showHealthRatings) res.add(HEALTH_FIELD_LABEL);
    return res;
  }
  
  /**
   * Decide whether the current title record should be displayed, based on 
   * whether it is a duplicate of the previous record, given the combination 
   * of output fields included in the ordering. If range fields are included 
   * in the output, or if identifying properties change between titles, the 
   * title will be shown.
   * <p>
   * If there are neither range fields nor id fields in the output, we can't 
   * tell if it is a duplicate so we show it anyway. Output with no id fields 
   * is pretty meaningless though.
   * <p>
   * Note that the method also sets the last output title if the response is 
   * true. Thus calling this method multiple times on the same title will give 
   * false after the first (unless there are no identifying fields).
   * 
   *   
   * @param currentTitle the current title
   * @return whether to show currentTitle
   */
  public boolean isTitleForOutput(KbartTitle currentTitle) {
    // The approach is to trueify this variable. If it becomes true, at the 
    // end of the method the lastOutputTitle is set before the result is 
    // returned. Do not return early!
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
    public abstract List<String> getOrderedLabels();
  }
  
  /**
   * A custom field ordering allows an arbitrary order of Fields to be 
   * specified at construction.
   * 
   * @author Neil Mayo
   */
  public static class CustomFieldOrdering implements FieldOrdering {
    /** A set of fields included in this ordering. */
    public final EnumSet<Field> fields;
    /** 
     * An ordered list of Fields in this ordering. 
     */
    public final List<Field> ordering;
    /** An ordered list of labels for the fields. */ 
    public final List<String> orderedLabels;
    
    /** The standard KBART field ordering. */
    private static final FieldOrdering KBART_ORDERING = 
        new CustomFieldOrdering(new ArrayList<Field>(Field.getFieldSet()));
    
    /** 
     * A separator used to separate field names in the specification of a 
     * custom ordering. This may be any combination of whitespace. 
     */
    public static final String CUSTOM_ORDERING_FIELD_SEPARATOR = "\n";
    
    //private static final CustomFieldOrdering defaultOrdering = new CustomFieldOrdering(Arrays.asList(Field.values()));
    
    //public static final CustomFieldOrdering getDefaultOrdering() {
    //public final CustomFieldOrdering getDefaultOrdering() { return defaultOrdering; }
    
    /** A mapping of labels to fields, for interpreting a user-supplied ordering. */
    private static final Map<String, Field> labelFields = 
        new HashMap<String, Field>() {{
          for (Field f : Field.values()) put(f.getLabel(), f);
        }};
      
    public static FieldOrdering getDefaultOrdering() {
      return KBART_ORDERING;
    }
    
    /**
     * Create a custom field ordering from a list of fields. 
     * @param ordering the ordering which will be used
     */
    public CustomFieldOrdering(final List<Field> ordering) {
      this.ordering = ordering;
      this.fields = EnumSet.copyOf(ordering);
      this.orderedLabels = new ArrayList<String>() {{
        for (Field f : ordering) add(f.getLabel());
      }};
    }

    /**
     * Create a custom field ordering from a string containing a list of field 
     * labels separated by whitespace (specifically the 
     * <code>CUSTOM_ORDERING_FIELD_SEPARATOR</code>).
     * @param orderStr a string of field labels separated by whitespace
     */
    public CustomFieldOrdering(String orderStr) throws CustomFieldOrderingException {
      // Use ListUtil to make list, so we get an ArrayList which implements 
      // both add() methods
      this.orderedLabels = ListUtil.fromArray(StringUtils.split(orderStr.toLowerCase(),
          CUSTOM_ORDERING_FIELD_SEPARATOR));
      // Create the Field ordering
      this.ordering = new ArrayList<Field>();
      for (String s : orderedLabels) {
        s = s.trim();
        // Ignore white space lines
        if (s.isEmpty()) continue;
        Field f = labelFields.get(s);
        // Throw exception if the label is not valid
        if (f==null) throw new CustomFieldOrderingException(s,
            "String '"+s+"' does not refer to a valid field.");
        this.ordering.add(labelFields.get(s));
      }
      // Sanity checks
      if (this.ordering.isEmpty()) 
        throw new CustomFieldOrderingException("", "No valid fields specified.");
      
      if (!Field.idFields.clone().removeAll(this.ordering)) {
        String idFieldStr = String.format("(%s)",
            StringUtils.join(Field.getLabels(Field.idFields), ", ")
        );
        throw new CustomFieldOrderingException("", String.format(
            "No identifying fields specified. You must include one of %s.",
            idFieldStr
        ));
      }
      this.fields = EnumSet.copyOf(this.ordering);
    }
    
    public EnumSet<Field> getFields() {
      return this.fields;
    }
    public List<Field> getOrdering() {
      return this.ordering;
    }
    public List<String> getOrderedLabels() {
      return orderedLabels;
    }
 
    public String toString() {
      return "" + StringUtil.separatedString(orderedLabels, 
          "CustomFieldOrdering(", " | ", ")");
    }
    
    /** 
     * A custom exception caused when an invalid custom ordering string is 
     * passed to the constructor of the CustomFieldOrdering.
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
   * An enum for specifying a variety of <code>FieldOrdering</code>s. It is 
   * possible to specify an incomplete list of fields, and other fields will 
   * be omitted. 
   * 
   * @author Neil Mayo
   */
  public static enum PredefinedFieldOrdering implements FieldOrdering {
        
    // Default KBART ordering
    KBART("KBART Default",
        "Full KBART format in default ordering; one line per coverage range",
        Field.values()
    ),

    // An ordering that puts publisher and publication first; mainly for use by Vicky
    PUBLISHER_PUBLICATION("Publisher oriented", 
        "Standard KBART fields with publisher name at the start",
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

    // An ordering that puts publisher and publication first, and only shows
    // title identifying information and coverage ranges.
    TITLE_COVERAGE_RANGES("Title Coverage Ranges",
        "List coverage ranges for each title; one per line, publisher first",
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
            NUM_LAST_ISSUE_ONLINE
        }
    ),

    // Minimal details - Title identifiers
    TITLES_BASIC("Basic Title Details", "Publisher, Publication, ISSN, eISSN",
        new Field[] {
            PUBLISHER_NAME,
            PUBLICATION_TITLE,
            PRINT_IDENTIFIER,
            ONLINE_IDENTIFIER
        }
    ),

    // Title and ISSN only (will have duplicates)
    TITLE_ISSN("Title and ISSN", "List ISSNs and Title only",
        new Field[] {
            PRINT_IDENTIFIER,
            PUBLICATION_TITLE
        }
    ),

    // ISSN only (will have duplicates)
    ISSN_ONLY("ISSN only", "Produce a list of ISSNs",
        new Field[] {PRINT_IDENTIFIER}
    ),

    // SFX DataLoader format only requires title_id and coverage_notes
    /*SFX_DATALOADER("SFX DataLoader", "SFX DataLoader (title_id and coverage_notes)",
        new Field[] {
            TITLE_ID,
            COVERAGE_NOTES
        }
    )*/
    ;

    // These are used in an interface to describe the ordering. */
    /** A name to identify the ordering. */
    public final String displayName;
    /** A description of the ordering. */
    public final String description;
    /** A set of fields included in this ordering. */
    public final EnumSet<Field> fields;
    /** An ordered list of fields in this ordering. */
    public final List<Field> ordering;
    /** An ordered list of labels for the fields. */ 
    public final List<String> orderedLabels;

    public EnumSet<Field> getFields() { return this.fields; }
    public List<Field> getOrdering() { return this.ordering; }
    public List<String> getOrderedLabels() { return this.orderedLabels; }

    PredefinedFieldOrdering(String displayName, String description, Field[] fieldOrder) {
      this.displayName = displayName;      
      this.description = description;      
      ordering = Arrays.asList(fieldOrder);
      fields = EnumSet.copyOf(ordering);
      this.orderedLabels = new ArrayList<String>() {{
        for (Field f : ordering) add(f.getLabel());
      }};
    }

    public String toString() {
      return "" + StringUtil.separatedString(orderedLabels, 
          "PredefinedFieldOrdering(", " | ", ")"
      );
    }

  }


}

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

package org.lockss.exporter.kbart;

import java.util.*;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang3.StringUtils;
import org.lockss.config.TdbUtil.ContentScope;
import org.lockss.exporter.kbart.KbartTitle.Field;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

import org.lockss.util.*;

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
  
  private static final Logger log = Logger.getLogger(KbartExportFilter.class);

  public static final boolean OMIT_EMPTY_FIELDS_DEFAULT =
      KbartExporter.omitEmptyFieldsByDefault;
  public static final boolean OMIT_HEADER_ROW_BY_DEFAULT =
      KbartExporter.omitHeaderRowByDefault;
  public static final boolean EXCLUDE_NOID_TITLES_BY_DEFAULT =
      KbartExporter.excludeNoIdTitlesByDefault;
  public static final boolean SHOW_HEALTH_RATINGS_DEFAULT =
      KbartExporter.showHealthRatingsByDefault;
  public static final PredefinedColumnOrdering COLUMN_ORDERING_DEFAULT =
      PredefinedColumnOrdering.KBART;

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
  
  /** An ordering of the columns that should be displayed. */
  private final ColumnOrdering columnOrdering;
  /** The titles which will be filtered. */  
  private final List<KbartTitle> titles;

  /** Whether to omit the header row. False by default. */
  private final boolean omitHeaderRow;
  /** Whether to omit empty field columns from the output. False by default. */
  private final boolean omitEmptyFields;
  /** Whether to exclude id-less titles from the output. False by default. */
  private final boolean excludeNoIdTitles;
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

  /**
   * The filtered list of columns which will be shown. This is the specified
   * ordering of this filter, minus any empty fields if
   * <code>omitEmptyFields</code> is set.
   */
  private ColumnOrdering visibleColumnOrdering;

  /**
   * Make an identity filter on the title set. This uses the default KBART 
   * field set and ordering and does not omit empty fields or header, nor does
   * it exclude identifier-less titles.
   * @param titles the titles to be exported
   * @return a filter with pure KBART settings
   */
  public static KbartExportFilter identityFilter(List<KbartTitle> titles) {
    return new KbartExportFilter(titles, PredefinedColumnOrdering.KBART,
        false, false, false);
  }
 
  /**
   * Whether there are fields included in the given set which provide
   * range information for a title. 
   * @param fields a set of fields to check
   * @return whether the list includes any range fields
   */
  public static boolean includesRangeFields(Set<Field> fields) {
    return !CollectionUtil.isDisjoint(fields, rangeFields);
  }

  /**
   * Make a filter on the title set using default values. This uses the 
   * default field ordering and the default approach to empty fields.
   * @param titles the titles to be exported
   * @return a filter with default settings
   */
  public KbartExportFilter(List<KbartTitle> titles) {
    this(titles, COLUMN_ORDERING_DEFAULT, OMIT_EMPTY_FIELDS_DEFAULT,
        OMIT_HEADER_ROW_BY_DEFAULT, EXCLUDE_NOID_TITLES_BY_DEFAULT,
        SHOW_HEALTH_RATINGS_DEFAULT);
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
  public KbartExportFilter(List<KbartTitle> titles, ColumnOrdering ordering) {
    this(titles, ordering, OMIT_EMPTY_FIELDS_DEFAULT,
        OMIT_HEADER_ROW_BY_DEFAULT, EXCLUDE_NOID_TITLES_BY_DEFAULT,
        SHOW_HEALTH_RATINGS_DEFAULT);
  }


  /**
   * An alternative constructor that allows one to specify whether or not to
   * show columns which are entirely empty, and uses the default for show health
   * ratings.
   *
   * @param titles the titles to be exported
   * @param ordering an ordering to impose on the columns of each title
   * @param omitEmptyFields whether to omit empty field columns from the output
   */
  public KbartExportFilter(List<KbartTitle> titles, ColumnOrdering ordering,
                           boolean omitEmptyFields, boolean omitHeader,
                           boolean excludeNoIdTitles) {
    this(titles, ordering, omitEmptyFields, omitHeader, excludeNoIdTitles,
        SHOW_HEALTH_RATINGS_DEFAULT);
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
  public KbartExportFilter(List<KbartTitle> titles, ColumnOrdering ordering,
                           boolean omitEmptyFields, boolean omitHeader,
                           boolean excludeNoIdTitles,
                           boolean showHealthRatings) {
    this.titles = titles;
    this.columnOrdering = ordering;
    this.omitEmptyFields = omitEmptyFields;
    this.omitHeaderRow = omitHeader;
    this.excludeNoIdTitles = excludeNoIdTitles;
    this.showHealthRatings = showHealthRatings;
    // Work out the list of empty fields if necessary
    this.emptyFields = omitEmptyFields ? findEmptyFields() : 
        EnumSet.noneOf(Field.class);
    // Create a list of the visible (non-omitted) fields out of the supplied ordering
    this.visibleColumnOrdering = CustomColumnOrdering.copy(columnOrdering).removeFields(emptyFields);
    this.rangeFieldsIncludedInDisplay =
        !CollectionUtil.isDisjoint(visibleColumnOrdering.getFields(), rangeFields);
    this.idFieldsIncludedInDisplay = 
        !CollectionUtil.isDisjoint(visibleColumnOrdering.getFields(), idFields);
  } 

  public void sortTitlesByFirstTwoFields() {
    // Sort on just the first 2 field columns (max):
    List<Field> fields = visibleColumnOrdering.getOrderedFields();
    if (fields.size() < 1) return;
    StringBuilder sb = new StringBuilder("Sort by ");
    sb.append(fields.get(0));
    if (fields.size() > 1) sb.append(" | ").append(fields.get(1));
    log.debug(sb.toString());
    sortTitlesByFields( 
        fields.subList(0, Math.min(2, fields.size()))
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
  public ColumnOrdering getVisibleColumnOrdering() {
    return this.visibleColumnOrdering;
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
    List<String> l = visibleColumnOrdering.getOrderedLabels();
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
   * Whether this filter is set to omit the header.
   *
   * @return whether this filter is set to omit the header
   */
  public boolean isOmitHeader() {
    return omitHeaderRow;
  }

  /**
   * Whether this filter is set to omit titles lacking unique identifiers.
   *
   * @return whether this filter is set to exclude identifier-less titles
   */
  public boolean isExcludeNoIdTitles() {
    return excludeNoIdTitles;
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
  public ColumnOrdering getColumnOrdering() {
    return columnOrdering;
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
    Set<Field> empties = EnumSet.copyOf(columnOrdering.getFields());
    empties.retainAll(emptyFields);
    return empties;
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
   * Whether fields were omitted manually. That is, the specified field
   * ordering is shorter than the available number of fields.
   * @return whether empty fields were omitted.
   */
  public boolean omittedFieldsManually() {
    return columnOrdering.getFields().size() < Field.values().length;
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
  public List<String> getVisibleFieldValues(final KbartTitle title) {
    //List<String> res = title.fieldValues(getVisibleColumnOrdering().getOrderedFields());
    List<String> res = new ArrayList<String>() {{
      for (ReportColumn col : getVisibleColumnOrdering().getOrderedColumns()) {
        add(col.getValue(title));
      }
    }};
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
        if (visibleColumnOrdering.getFields().contains(f) &&
            !lastOutputTitle.getField(f).equals(currentTitle.getField(f))) {
          isOutput = true;
          break;
        }
      }
    }
    // Finally, we refuse to output the title if it has no id and
    // excludeNoIdTitles is true.
    if (excludeNoIdTitles &&
        StringUtil.isNullString(currentTitle.getField(Field.TITLE_ID)))
      isOutput = false;

    // Record the previous title
    if (isOutput) lastOutputTitle = currentTitle;
    return isOutput;
  }


  //////////////////////////////////////////////////////////////////////////////
  /**
   * A convenient way of storing the Fields in a column ordering as both an
   * ordered list and an unordered but more efficient set. This is to save
   * converting between them unnecessarily. The list and the set must contain
   * the same elements.
   * <p>
   * Setting an order implies a list of fields. The output will include all of the 
   * fields in the ordering unless they are omitted via <code>omitEmptyField</code>. 
   * Fields not mentioned in the ordering will be omitted.
   * </p>
   * 
   * @author Neil Mayo
   */
  public static interface FieldOrdering {
    /**
     * Get all the Fields involved in the ordering. This may not be the full set
     * of ReportColumns.
     * @return an EnumSet of Fields from the ordering
     */
    public EnumSet<Field> getFields();
    /**
     * Get an ordered list of Fields in the ordering; this will not include any
     * custom columns.
     * @return
     */
    public List<Field> getOrderedFields();
  }


  /**
   * Extends the FieldOrdering representation to support the ordering of more
   * generic columns, represented as ReportColumn objects. This interface adds
   * accessors for the full list of ordered columns, of column labels, and
   * of the descriptors which can be used to recreate the ordering.
   * <p>
   * This way it is possible to include extra string values in the ordering
   * which are not Fields. It is up to implementing classes whether to allow this
   * and how to mediate the discrepancy between Fields and labels. The inherited
   * methods getFields() and getOrderedFields() should be used to inform reasoning
   * about the Fields that are involved, and should not be taken to represent
   * all the columns that may appear in the output.
   * </p>
   *
   * @author Neil Mayo
   */
  public static interface ColumnOrdering extends FieldOrdering {
    /**
     * Get all the Columns in the ordering, as an ordered list.
     * @return
     */
    public List<ReportColumn> getOrderedColumns();
    /**
     * Return an ordered list of labels for the ordering; this will include
     * any additional custom columns, and therefore does not
     * necessarily match the list of KbartTitle.Fields which are in the ordering.
     * @return a List of Strings representing the ordered labels for all columns
     */
    public List<String> getOrderedLabels();

    /**
     * Return and ordered list of ReportColumn descriptors for the ordering.
     * @return
     */
    public List<String> getOrderedDescriptors();

  }


  //////////////////////////////////////////////////////////////////////////////
  /**
   * A custom column ordering allows an arbitrary order of ReportColumns to be
   * specified at construction. This can include a limited number of
   * constant-valued columns. It will also allow duplicated Field columns.
   * 
   * @author Neil Mayo
   */
  public static class CustomColumnOrdering implements ColumnOrdering {
    /** A set of fields included in this ordering. */
    protected final EnumSet<Field> fields;
    /** An ordered list of ReportColumns in this ordering. */
    protected final List<ReportColumn> ordering;
    /** An ordered list of Fields in this ordering. */
    protected final List<Field> fieldOrdering;
    /** An ordered list of labels for the fields. */ 
    protected final List<String> orderedLabels;
    /**
     * A separator used to separate field names in the specification of a 
     * custom ordering. This may be any combination of whitespace. 
     */
    public static final String CUSTOM_ORDERING_FIELD_SEPARATOR = "\n";
    /**
     * A regular expression representing characters used to separate field names
     * in the specification of a custom ordering. Splits on line break and/or
     * carriage return.
     */
    private static final String CUSTOM_ORDERING_FIELD_SEPARATOR_REGEX = "[\n\r]+";
    /**
     * The maximum allowable number of columns from a user-defined list; when
     * splitting the list, any tokens beyond this number will be discarded.
     * Practically, this means it is possible to have up to MAX_COLUMNS-1
     * user-defined constant value columns (as there must be one id column).
     */
    private static final int MAX_COLUMNS = Field.values().length*2;

    public static ColumnOrdering getDefaultOrdering() {
      return PredefinedColumnOrdering.KBART;
    }
    // Getters
    public List<ReportColumn> getOrderedColumns() { return ordering; }
    public EnumSet<Field> getFields() { return this.fields; }
    public List<Field> getOrderedFields() { return this.fieldOrdering; }
    public List<String> getOrderedLabels() { return orderedLabels; }
    public List<String> getOrderedDescriptors() {
      return new LinkedList<String>() {{
        for (ReportColumn rc: getOrderedColumns()) add(rc.getColumnDescriptor());
      }};
    }

    /**
     * Convenience method to create a custom field ordering from an array of
     * Fields.
     * @param ordering an ordered array of Fields
     */
    public static CustomColumnOrdering create(final Field[] ordering) {
      return create(Arrays.asList(ordering));
    }
    /**
     * Convenience method to create a custom field ordering from a list of
     * Fields.
     * @param fieldOrdering an ordered list of Fields
     */
    public static CustomColumnOrdering create(final List<Field> fieldOrdering) {
      final List<ReportColumn> ordering = ReportColumn.fromFields(fieldOrdering);
      return new CustomColumnOrdering(ordering, fieldOrdering);
    }

    /**
     * Create a custom column ordering from the columns in another column
     * ordering.
     * @param other
     * @return
     */
    public static CustomColumnOrdering copy(ColumnOrdering other) {
      return createUnchecked(other.getOrderedDescriptors());
    }

    /**
     * Create an ordering from predefined lists of Fields and columns. This is
     * enough information from which to interpolate the other final members.
     * Because there is no String interpretation, there is no need to generate
     * or catch exceptions. This version includes the ordered list of labels,
     * to save processing for those cases where the caller has already generated
     * the list.
     * @param fieldOrdering
     * @param ordering
     * @param orderedDescriptors
     */
    private CustomColumnOrdering(final List<ReportColumn> ordering,
                                 final List<Field> fieldOrdering,
                                 final List<String> orderedDescriptors) {
      this.ordering = ordering;
      this.fieldOrdering = fieldOrdering;
      this.orderedLabels = new LinkedList<String>(){{
        for (ReportColumn rc : ordering) add(rc.getColumnHeader());
      }};
      this.fields = EnumSet.copyOf(fieldOrdering);
    }

    /**
     * Create an ordering from predefined lists of Fields and columns. This is
     * enough information from which to interpolate the other final members.
     * Because there is no String interpretation, there is no need to generate
     * or catch exceptions. It is possible to get the list of fields from the
     * list of ReportColumns, but there is already a constructor with a single
     * List argument.
     * @param fieldOrdering
     * @param ordering
     */
    protected CustomColumnOrdering(final List<ReportColumn> ordering,
                                 final List<Field> fieldOrdering) {
      this(ordering, fieldOrdering, new ArrayList<String>() {{
        for (ReportColumn rc : ordering) add(rc.getColumnDescriptor());
      }});
    }

    /**
     * Create a custom field ordering from a list of strings representing field
     * descriptors. There may also be one or more optionally-quoted strings, each
     * indicating a value to be inserted into a constant-valued column.
     * @param descriptors a list of field descriptors
     * @throws CustomColumnOrderingException if there is an unrecognised field name or no id fields
     */
    public CustomColumnOrdering(List<String> descriptors) throws CustomColumnOrderingException {
      this.ordering = ReportColumn.fromStrings(descriptors);
      // Note we regenerate the descriptors from the ReportColumns so constant-valued
      // columns are consistently unquoted.
      this.orderedLabels = ReportColumn.getLabels(ordering);

      // Create the Field ordering
      this.fieldOrdering = extractFieldOrderingFromReportColumns(ordering);

      // Sanity checks
      // Are there any fields included?
      if (this.fieldOrdering.isEmpty())
        throw new CustomColumnOrderingException("", "No valid fields specified.");
      // Are there id fields included?
      if (!Field.idFields.clone().removeAll(this.fieldOrdering)) {
        String idFieldStr = String.format("(%s)",
            StringUtils.join(Field.getLabels(Field.idFields), ", ")
        );
        throw new CustomColumnOrderingException("", String.format(
            "No identifying fields specified. You must include one of %s.",
            idFieldStr
        ));
      }
      // Set the field set if all is okay
      this.fields = EnumSet.copyOf(this.fieldOrdering);
    }


    /**
     * Create a custom field ordering from a string containing a list of field
     * labels separated by whitespace (specifically the
     * <code>CUSTOM_ORDERING_FIELD_SEPARATOR_REGEX</code>).
     * <p>
     * The ordering string must contain only valid field names separated by line
     * returns ("\n" and/or "\r"), except for one exception; if it contains a
     * quoted string on its own line, an extra column will be created in that
     * position, containing only that string in every position including the
     * header row. The string can include escaped characters.
     * @param orderStr a string of field labels separated by whitespace
     */
    public CustomColumnOrdering(String orderStr) throws CustomColumnOrderingException {
      this(splitCustomOrderingString(orderStr));
    }

    /**
     * Create a custom ordering without generating exceptions on invalid
     * specification. This method will create a custom ordering as per the list
     * of descriptors. Labels that do not match fields will be included as
     * constant-valued columns, and there are no restrictions on whether id
     * fields are included (allowing for the creation of orderings leading to
     * useless reports); nor are there limits on the number of columns which
     * can be included. This method should therefore only be used in creating
     * enumerated PredefinedFieldOrderings, or user-defined orderings defined
     * through the LOCKSS user interface.
     * @param descriptors
     * @return
     */
    protected static CustomColumnOrdering createUnchecked(List<String> descriptors) {
      final List<ReportColumn> ordering = ReportColumn.fromStrings(descriptors);
      List<Field> fieldOrdering = extractFieldOrderingFromReportColumns(ordering);
      return new CustomColumnOrdering(ordering, fieldOrdering, descriptors);
    }

    private static List<Field> extractFieldOrderingFromReportColumns(final List<ReportColumn> ordering) {
      return new ArrayList<Field>() {{
        for (ReportColumn c : ordering) {
          if (c.isField()) add(c.getField());
        }
      }};
    }

    /**
     * Convenience method delegating to createUnchecked(). Takes a list of
     * objects to make it easier to specify predefined lists with a mix of
     * string and field columns.
     * @param columns a list of objects whose string representation will define the ordering
     * @return
     */
    protected static CustomColumnOrdering createUnchecked(final Object... columns) {
      return createUnchecked(
          new ArrayList<String>() {{
            for (Object o : columns) add(o.toString());
          }}
      );
    }

    /**
     * Split a CustomOrdering string, like those supplied via the user interface,
     * into individual strings. This does not check whether those strings are
     * valid field names; it is just a convenience method. Individual tokens
     * are stripped of whitespace, and empty strings are omitted.
     * @param orderStr
     * @return
     */
    public static List<String> splitCustomOrderingString(String orderStr) {
      String[] arr = orderStr.split(CUSTOM_ORDERING_FIELD_SEPARATOR_REGEX, MAX_COLUMNS);
      List<String> list = new ArrayList<String>();
      // Trim whitespace, and omit empties
      for (String s : arr) {
        s = s.trim();
        if (s.length()!=0) list.add(s);
      }
      return list;
    }

    public String toString() {
      return "" + StringUtil.separatedString(orderedLabels, 
          "CustomColumnOrdering(", " | ", ")");
    }

    /**
     * Remove the named columns from the ordering. Returns the ordering for
     * convenience.
     *
     * @param columns a collection of labels
     * @return
     */
    protected CustomColumnOrdering remove(Collection<String> columns) {
      this.ordering.removeAll(ReportColumn.fromStrings(columns));
      this.orderedLabels.removeAll(columns);
      Collection<Field> flds = Field.getFields(columns);
      this.fieldOrdering.removeAll(flds);
      this.fields.removeAll(flds);
      return this;
    }

    /**
     * Remove the named Fields from the ordering. Returns the ordering for
     * convenience.
     *
     * @param fieldsToRemove a collection of fields
     * @return
     */
    protected CustomColumnOrdering removeFields(Collection<Field> fieldsToRemove) {
      List<ReportColumn> cols = ReportColumn.fromFields(fieldsToRemove);
      this.ordering.removeAll(cols);
      this.orderedLabels.removeAll(ReportColumn.getLabels(cols));
      this.fieldOrdering.removeAll(fieldsToRemove);
      this.fields.removeAll(fieldsToRemove);
      return this;
    }

    /**
     * A custom exception caused when an invalid custom ordering string is 
     * passed to the constructor of the CustomColumnOrdering.
     */
    public static class CustomColumnOrderingException extends Exception {
      /** A record of the string label for which a field could not be found. */
      private final String errorLabel;
      public CustomColumnOrderingException(String s) {
        super();
        this.errorLabel = s;
      }
      public CustomColumnOrderingException(String s, String msg) {
        super(msg);
        this.errorLabel = s;
      }
      public String getErrorLabel() { return errorLabel; }
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  /**
   * An enum for specifying a variety of <code>FieldOrdering</code>s. It is 
   * possible to specify an incomplete list of fields, and other fields will 
   * be omitted. 
   * 
   * @author Neil Mayo
   */
  public static enum PredefinedColumnOrdering implements ColumnOrdering {
        
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
        PRINT_IDENTIFIER
    ),

    // TITLE_ID only - should have an id for every record
//    TITLE_ID_ONLY("Title ID only", "Produce a list of unique identifiers",
//        TITLE_ID
//    ),

    // SFX fields only
    SFX("SFX Fields", "Produce a list of fields for SFX DataLoader",
        // ISSN or eISSN
        String.format("%s || %s", PRINT_IDENTIFIER, ONLINE_IDENTIFIER),
        "ACTIVE",
        COVERAGE_NOTES
    ),
    ;

    /** Store the field ordering internally as a CustomColumnOrdering. */
    private CustomColumnOrdering customColumnOrdering;

    // These are used in an interface to describe the ordering. */
    /** A name to identify the ordering. */
    public final String displayName;
    /** A description of the ordering. */
    public final String description;

    public EnumSet<Field> getFields() { return customColumnOrdering.getFields(); }
    public List<Field> getOrderedFields() { return customColumnOrdering.getOrderedFields(); }
    public List<String> getOrderedLabels() { return customColumnOrdering.getOrderedLabels(); }
    public List<ReportColumn> getOrderedColumns() { return customColumnOrdering.getOrderedColumns(); }
    public List<String> getOrderedDescriptors() {
      return new LinkedList<String>() {{
        for (ReportColumn rc: getOrderedColumns()) add(rc.getColumnDescriptor());
      }};
    }
    public String toString() {
      return "" + StringUtil.separatedString(getOrderedLabels(),
          "PredefinedColumnOrdering(", " | ", ")"
      );
    }

    /**
     * Return a list of the column labels which do not refer to a Field.
     * @return
     */
    public List<String> getNonFieldColumnLabels() {
      return new ArrayList<String>() {{
        for (ReportColumn rc : getOrderedColumns()) {
          if (!rc.isField()) add(rc.getColumnHeader());
        }
      }};
    }

    /**
     * Make a predefined ordering from a list of objects. This is a convenience
     * method to make it easier to specify predefined lists in this enum with
     * mixed string/field columns.
     * @param displayName the display name of the ordering
     * @param description a description for the ordering
     * @param columns a list of objects whose string representation will define the ordering
     * @throws org.lockss.exporter.kbart.KbartExportFilter.CustomColumnOrdering.CustomColumnOrderingException if there is a problem creating the internal ColumnOrdering
     */
    private PredefinedColumnOrdering(final String displayName, final String description,
                                     final Object... columns) {
      this(displayName, description, CustomColumnOrdering.createUnchecked(columns));
    }

    /**
     * Constructor for ordering based on aan array of Fields. This is the safest
     * constructor as it uses enums and therefore needs no checking.
     * @param displayName
     * @param description
     * @param fieldOrder
     */
    PredefinedColumnOrdering(final String displayName, final String description,
                             final Field[] fieldOrder) {
      this(displayName, description, CustomColumnOrdering.create(fieldOrder));
    }

    /**
     * Create an ordering with a name, description, and a pre-built
     * CustomColumnOrdering.
     * @param displayName the display name of the ordering
     * @param description a description for the ordering
     * @param customColumnOrdering the custom ordering
     */
    private PredefinedColumnOrdering(final String displayName, final String description,
                                     final CustomColumnOrdering customColumnOrdering) {
      this.displayName = displayName;
      this.description = description;
      this.customColumnOrdering = customColumnOrdering;
    }

  }

}

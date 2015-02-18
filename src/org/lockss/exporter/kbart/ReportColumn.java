/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.MetadataUtil;
import org.lockss.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A class to represent a column in a report. The column is represented either
 * by a Field column, or by an optionally-quoted String.
 * <p>
 * We may want to provide extensions of this class that allow it to define
 * for example where to get the value for each row in the column.
 * <p>
 * Implements the equals() method, which tests on the column descriptor.
 * It is possible to define functionally equivalent ReportColumns with a
 * variety of valid descriptors, but we do not consider these equal.
 */
public class ReportColumn {
  // ------------------------------------------------------------------------
  // Properties for exactly one of the following will be set; the others
  // will be null or empty:
  // FIELD
  /** The Field for the column, if applicable. */
  private KbartTitle.Field field;
  // CONSTANT VALUE COLUMN
  /** The header for a custom column, if applicable. */
  private String columnHeader;
  private String constantFieldValue = "";
  // RANKING OF FIELDS
  /** Fields to be used in a combined column, if applicable. */
  private final List<KbartTitle.Field> fieldRanking = new LinkedList<KbartTitle.Field>();
  private String defaultFieldValue = "";
  // ------------------------------------------------------------------------

  /** Token used to separate field alternatives in a custom column descriptor. */
  private static final String OR = "||";
  /** Token used at the end of a custom descriptor to indicate a column header. */
  private static final String HEAD = "@";
  /** The string used to define the ReportColumn; this can be used to
   * reconstruct the column. */
  private final String columnDescriptor;

  /**
   * Create a ReportColumn directly from a Field.
   * @param field
   */
  public ReportColumn(KbartTitle.Field field) {
    this.field = field;
    this.columnDescriptor = field.getLabel();
    this.columnHeader = field.getLabel();
  }

  /**
   * Create a column from the descriptor, treating it as a Field if the
   * descriptor matches a field descriptor, or a custom string otherwise.
   * @param descriptor the label of a Field, or a String with or without quotes
   */
  public ReportColumn(String descriptor) {
    this.columnDescriptor = descriptor;
    descriptor = descriptor.trim();

    // If the string has a column header value attached
    int h = descriptor.lastIndexOf(HEAD);
    if (h > -1) {
      this.columnHeader = unquote(descriptor.substring(h + HEAD.length()).trim());
      descriptor = descriptor.substring(0, h).trim();
    }

    try {
      this.field = KbartTitle.Field.valueOf(descriptor.toUpperCase());
      if (StringUtil.isNullString(this.columnHeader))
        this.columnHeader = field.getLabel();
    } catch (IllegalArgumentException e) {
      this.field = null;
      initCustom(descriptor);
    }
  }

  /**
   * Set up the ReportColumn as a custom; this is either a single repeated
   * value, or a combined value which indicates how to construct the value of
   * the column. A custom combined column descriptor is a token-separated
   * sequence of field names and custom strings.
   * <p>
   * Currently only the OR token is supported. Each token in the string is
   * considered in turn in generating the value for the column. The first
   * non-empty field value is returned; or an optional terminating constant
   * string token.
   * </p>
   * Custom strings are automatically unquoted.
   * @param descriptor the custom column descriptor
   */
  private void initCustom(String descriptor) {
    // Constant value report column
    if (descriptor.indexOf(OR) == -1) {
      if (StringUtil.isNullString(this.constantFieldValue))
        this.constantFieldValue = unquote(descriptor.trim());
      if (StringUtil.isNullString(this.columnHeader))
        this.columnHeader = constantFieldValue;
      return;
    } else {
      // Custom combination - split with token limit
      if (StringUtil.isNullString(this.columnHeader))
        this.columnHeader = descriptor;
      for (String tok : descriptor.split(StringUtil.escapeNonAlphaNum(OR), KbartTitle.Field.values().length)) {
        tok = tok.trim();
        try {
          this.fieldRanking.add(KbartTitle.Field.valueOf(tok.toUpperCase()));
        } catch (IllegalArgumentException e) {
          // If it's not a Field, use it as a final fallback value
          this.defaultFieldValue = tok;
          break;
          // TODO We could report a problem with the descriptor if there is a
          // non-field which is not last token in an ORed string
        }
      }
    }
  }


  /**
   * Remove quotes from the start and end of the string if they are both
   * present. Otherwise return the original string.
   * @param s the string to unquote
   * @return the string without surrounding quotes if both were present, or the original string
   */
  protected static String unquote(String s) {
    if (s.startsWith(quote) && s.endsWith(quote)) {
      return s.substring(quote.length(), s.length()-quote.length());
    }
    return s;
  }
  protected static final String quote = "\"";

  /**
   * Implements the equals() method, which compares the column descriptor.
   * It is possible to define functionally equivalent ReportColumns with a
   * variety of valid descriptors, but these are not considered equal.
   */
  @Override
  public boolean equals(Object o) {
    // Test equality based on the column descriptor
    return this.getColumnDescriptor().equals(((ReportColumn)o).getColumnDescriptor());
  }

  /** Whether the stored column is a Field. */
  public boolean isField() { return field!=null; }
  /** Whether the stored column is a constant. */
  public boolean isConstant() { return !StringUtil.isNullString(constantFieldValue); }
  /** Whether the stored column is a ranking of fields. */
  public boolean isRanking() { return !fieldRanking.isEmpty(); }

  /** Whether the column might hold ISSNs. */
  public boolean holdsIssns() {
    // The column is an ISSN field
    if (isField()) {
      return field == KbartTitle.Field.PRINT_IDENTIFIER ||
          field == KbartTitle.Field.ONLINE_IDENTIFIER;
    }
    // Or the column is a ranking which includes ISSN fields
    if (isRanking()) {
      return fieldRanking.contains(KbartTitle.Field.PRINT_IDENTIFIER) ||
          fieldRanking.contains(KbartTitle.Field.ONLINE_IDENTIFIER);
    }
    // Default
    return false;
  }

  /** Access to the Field for constructing FieldOrderings in the export filter. */
  public KbartTitle.Field getField() { return field; }

  /** Return the columnHeader of the column. */
  public String getColumnHeader() { return columnHeader;}
    /** Return the columnDescriptor of the column, so it can be reconstructed. */
  public String getColumnDescriptor() { return columnDescriptor; }

  /**
   * Get the value for a particular column, which is either the value of a
   * field of the title, or a constant column value.
   * @param title a KbartTitle providing values for the current row
   * @return the value of a field in the title, or the value of a constant column
   */
  public String getValue(KbartTitle title) {
    if (isField()) return getNormalisedFieldValue(title, field);
    else if (isConstant()) return constantFieldValue;
    else if (isRanking()) return getRankingValue(title);
    else return "UNKNOWN";
  }

  /**
   * Calculate which value to return based on the custom field ranking.
   * @return
   */
  private String getRankingValue(KbartTitle title) {
    for (KbartTitle.Field f : fieldRanking) {
      //String v = title.getField(f);
      String v = getNormalisedFieldValue(title, f);
      if (!v.isEmpty()) return v;
    }
    return defaultFieldValue;
  }

  /**
   * Get an appropriately normalised value from a field. Currently this just
   * normalises values from ISSN and ISBN fields; otherwise it returns the
   * original value. Invalid ISSN/ISBN will still get turned into null values.
   * @param title
   * @param f
   * @return
   */
  private String getNormalisedFieldValue(KbartTitle title, KbartTitle.Field f) {
    String v = title.getField(f);
    // Format and validate based on what we /expect/ the field to be.
    // If the field is an identifier, try to normalise it.
    if (f == KbartTitle.Field.PRINT_IDENTIFIER ||
        f == KbartTitle.Field.ONLINE_IDENTIFIER) {
      if (MetadataUtil.isIssn(v)) return MetadataUtil.normaliseIssn(v);
      if (MetadataUtil.isIsbn(v)) return MetadataUtil.normaliseIsbn(v);
    }
    return v;
  }


  /**
   * Generate a list of ReportColumns from a list of Fields.
   * Items in the returned list will be in the same order as provided by the
   * iterator of the argument list.
   * @param fieldOrdering
   * @return
   */
  public static List<ReportColumn> fromFields(final Collection<KbartTitle.Field> fieldOrdering) {
    return new ArrayList<ReportColumn>() {{
      for (KbartTitle.Field f : fieldOrdering) add(new ReportColumn(f));
    }};
  }

  /**
   * Generate a list of ReportColumns from a list of columns named by Strings.
   * This may include any number of Fields and constant-valued columns.
   * Items in the returned list will be in the same order as provided by the
   * iterator of the argument list.
   * @param columnOrdering a list of strings
   * @return a list of ReportColumns
   */
  public static List<ReportColumn> fromStrings(final Collection<String> columnOrdering)
  /*throws CustomColumnOrdering.CustomColumnOrderingException*/ {
    return new ArrayList<ReportColumn>() {{
      int constantCols = 0;
      for (String s: columnOrdering) {
        ReportColumn col = new ReportColumn(s);
        add(col);
      }
    }};
  }

  public static List<ReportColumn> fromObjects(final Object ... columnOrdering)
    /*throws CustomColumnOrdering.CustomColumnOrderingException*/ {
    return new ArrayList<ReportColumn>() {{
      for (Object s: columnOrdering) {
        ReportColumn col = new ReportColumn(s.toString());
        add(col);
      }
    }};
  }

  public static List<String> getLabels(final List<ReportColumn> columns) {
    return new ArrayList<String>() {{
      for (ReportColumn rc : columns) add(rc.getColumnHeader());
    }};
  }

  public static List<KbartTitle.Field> getFields(final List<ReportColumn> columns) {
    return new ArrayList<KbartTitle.Field>() {{
      for (ReportColumn rc : columns) {
        if (rc.isField()) add(rc.field);
      }
    }};
  }

}

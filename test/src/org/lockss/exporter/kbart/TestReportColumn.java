/*
 * $Id$
 */

/*

Copyright (c) 2013 EDINA, University of Edinburgh

*/
package org.lockss.exporter.kbart;

import org.lockss.test.LockssTestCase;
import org.lockss.exporter.kbart.KbartTitle.Field;

import java.util.*;

/**
 * @author Neil Mayo
 */
public class TestReportColumn extends LockssTestCase {

  // Empty ReportColumns should not get created, but if they.
  // are they will have an empty label and a value of "UNKNOWN".
  // They will not be field, constant or ranking types.
  String strEmpty             = "";
  String strEmptySpace        = "  ";
  Field  fldField              = KbartTitle.Field.TITLE_ID;
  String strFieldNameUpper    = fldField.toString().toUpperCase();
  String strFieldNameLower    = fldField.toString().toLowerCase();
  String strColHeadOnly       = "@HEAD";
  String strFieldHead         = "title_id@HEAD";
  String strOtherHead         = "other@HEAD";
  String strFieldRanking      = "online_identifier||print_identifier";
  String strFieldRankingSpace = " online_identifier || print_identifier ";
  String strFieldRankingHead  = "online_identifier||print_identifier@HEAD";

  // Note the list of all descriptors does not include the field
  String[] allDescriptors = {
      strEmpty,
      strEmptySpace,
      strFieldNameUpper,
      strFieldNameLower,
      strColHeadOnly,
      strFieldHead,
      strOtherHead,
      strFieldRanking,
      strFieldRankingSpace,
      strFieldRankingHead
  };

  ReportColumn rcEmpty             = new ReportColumn(strEmpty);
  ReportColumn rcEmptySpace        = new ReportColumn(strEmptySpace);
  ReportColumn rcField             = new ReportColumn(fldField);
  ReportColumn rcFieldNameUpper    = new ReportColumn(strFieldNameUpper);
  ReportColumn rcFieldNameLower    = new ReportColumn(strFieldNameLower);
  ReportColumn rcColHeadOnly       = new ReportColumn(strColHeadOnly);
  ReportColumn rcFieldHead         = new ReportColumn(strFieldHead);
  ReportColumn rcOtherHead         = new ReportColumn(strOtherHead);
  ReportColumn rcFieldRanking      = new ReportColumn(strFieldRanking);
  ReportColumn rcFieldRankingSpace = new ReportColumn(strFieldRankingSpace);
  ReportColumn rcFieldRankingHead  = new ReportColumn(strFieldRankingHead);

  ReportColumn[] allReportColumns = {
      rcEmpty,
      rcEmptySpace,
      rcField,
      rcFieldNameUpper,
      rcFieldNameLower,
      rcColHeadOnly,
      rcFieldHead,
      rcOtherHead,
      rcFieldRanking,
      rcFieldRankingSpace,
      rcFieldRankingHead
  };

  /**
   * Remove quotes from the start and end of the string if they are both
   * present. Otherwise return the original string.
   */
  public void testUnquote() throws Exception {
    // Expect the same value back
    for (String s : new String[] {
        " \"Quoted with space\"",
        "\"Quoted with space\" ",
        " \"Quoted with space\" ",
        "\"One quote",
        "One quote\"",
    }) assertEquals(s, ReportColumn.unquote(s));
    // Expect unquoted value back
    assertEquals("unquoted", ReportColumn.unquote("\"unquoted\""));
    assertEquals("un\"quoted", ReportColumn.unquote("\"un\"quoted\""));
    assertEquals("\"unquoted\"", ReportColumn.unquote("\"\"unquoted\"\""));
    assertEquals("Mid\"quote", ReportColumn.unquote("\"Mid\"quote\""));
  }

  /**
   * Test equality based on the column descriptor
   */
  public void testEquals() throws Exception {
    // Test descriptors against themselves
    for (ReportColumn rc : allReportColumns)
      assertEquals(rc, rc);
    // Test similar descriptors
    // Only ReportColumns with the same descriptor are considered equal
    assertNotEquals(rcEmpty, rcEmptySpace);
    assertNotEquals(rcFieldNameLower, rcFieldNameUpper);
    assertNotEquals(rcFieldHead, rcOtherHead);
    assertNotEquals(rcColHeadOnly, rcOtherHead);
    assertNotEquals(rcColHeadOnly, rcFieldHead);
    assertNotEquals(rcFieldNameLower, rcFieldHead);
    assertNotEquals(rcFieldNameUpper, rcFieldHead);
    assertNotEquals(rcFieldRanking, rcFieldRankingSpace);
    assertNotEquals(rcFieldRanking, rcFieldRankingHead);
    assertNotEquals(rcFieldRankingSpace, rcFieldRankingHead);
    // Specifying using a string is the same as a Field if it uses the Field's label
    assertEquals(rcFieldNameLower, rcField);
    assertNotEquals(rcFieldNameUpper, rcField);
  }

  public void testIsField() throws Exception {
    assertFalse(rcEmpty.isField());
    assertFalse(rcEmptySpace.isField());
    assertTrue(rcField.isField());
    assertTrue(rcFieldNameUpper.isField());
    assertTrue(rcFieldNameLower.isField());
    assertFalse(rcColHeadOnly.isField());
    assertTrue(rcFieldHead.isField());
    assertFalse(rcOtherHead.isField());
    assertFalse(rcFieldRanking.isField());
    assertFalse(rcFieldRankingSpace.isField());
    assertFalse(rcFieldRankingHead.isField());
  }

  public void testIsConstant() throws Exception {
    // Constant value columns cannot be empty, or field-based
    assertFalse(rcEmpty.isConstant());
    assertFalse(rcEmptySpace.isConstant());
    assertFalse(rcField.isConstant());
    assertFalse(rcFieldNameUpper.isConstant());
    assertFalse(rcFieldNameLower.isConstant());
    assertFalse(rcColHeadOnly.isConstant());
    assertFalse(rcFieldHead.isConstant());
    assertTrue(rcOtherHead.isConstant());
    assertFalse(rcFieldRanking.isConstant());
    assertFalse(rcFieldRankingSpace.isConstant());
    assertFalse(rcFieldRankingHead.isConstant());
  }

  public void testIsRanking() throws Exception {
    assertFalse(rcEmpty.isRanking());
    assertFalse(rcEmptySpace.isRanking());
    assertFalse(rcField.isRanking());
    assertFalse(rcFieldNameUpper.isRanking());
    assertFalse(rcFieldNameLower.isRanking());
    assertFalse(rcColHeadOnly.isRanking());
    assertFalse(rcFieldHead.isRanking());
    assertFalse(rcOtherHead.isRanking());
    assertTrue(rcFieldRanking.isRanking());
    assertTrue(rcFieldRankingSpace.isRanking());
    assertTrue(rcFieldRankingHead.isRanking());
  }

  public void testGetField() throws Exception {
    assertNull(rcEmpty.getField());
    assertNull(rcEmptySpace.getField());
    assertTrue(rcField.getField() instanceof KbartTitle.Field);
    assertTrue(rcFieldNameUpper.getField() instanceof KbartTitle.Field);
    assertTrue(rcFieldNameLower.getField() instanceof KbartTitle.Field);
    assertNull(rcColHeadOnly.getField());
    assertTrue(rcFieldHead.getField() instanceof KbartTitle.Field);
    assertNull(rcOtherHead.getField());
    assertNull(rcFieldRanking.getField());
    assertNull(rcFieldRankingSpace.getField());
    assertNull(rcFieldRankingHead.getField());
    // Non fields should not yield Field instance
    assertFalse(rcOtherHead.getField() instanceof KbartTitle.Field);
  }

  public void testGetColumnHeader() throws Exception {
    assertEquals("", rcEmpty.getColumnHeader());
    assertEquals("", rcEmptySpace.getColumnHeader());
    assertEquals(fldField.getLabel(), rcField.getColumnHeader());
    assertEquals(fldField.getLabel(), rcFieldNameUpper.getColumnHeader());
    assertEquals(fldField.getLabel(), rcFieldNameLower.getColumnHeader());
    assertEquals("HEAD", rcColHeadOnly.getColumnHeader());
    assertEquals("HEAD", rcFieldHead.getColumnHeader());
    assertEquals("HEAD", rcOtherHead.getColumnHeader());
    assertEquals(strFieldRanking, rcFieldRanking.getColumnHeader());
    // The ranking descriptor with spaces should only be trimmed
    assertEquals(strFieldRankingSpace.trim(), rcFieldRankingSpace.getColumnHeader());
    assertEquals("HEAD", rcFieldRankingHead.getColumnHeader());

  }

  public void testGetColumnDescriptor() throws Exception {
    assertEquals(strEmpty,             rcEmpty.getColumnDescriptor());
    assertEquals(strEmptySpace,        rcEmptySpace.getColumnDescriptor());
    assertEquals(fldField.getLabel(),  rcField.getColumnDescriptor());
    assertEquals(strFieldNameUpper,    rcFieldNameUpper.getColumnDescriptor());
    assertEquals(strFieldNameLower,    rcFieldNameLower.getColumnDescriptor());
    assertEquals(strColHeadOnly,       rcColHeadOnly.getColumnDescriptor());
    assertEquals(strFieldHead,         rcFieldHead.getColumnDescriptor());
    assertEquals(strOtherHead,         rcOtherHead.getColumnDescriptor());
    assertEquals(strFieldRanking,      rcFieldRanking.getColumnDescriptor());
    assertEquals(strFieldRankingSpace, rcFieldRankingSpace.getColumnDescriptor());
    assertEquals(strFieldRankingHead,  rcFieldRankingHead.getColumnDescriptor());

  }

  public void testGetValue() throws Exception {
    // Create a test KbartTitle
    String titleId = "titleId";
    String printId = "printId";
    String onlineId = "onlineId";
    KbartTitle title = new KbartTitle()
        .setField(Field.TITLE_ID, titleId)
        .setField(Field.PRINT_IDENTIFIER, printId)
        .setField(Field.ONLINE_IDENTIFIER, onlineId)
    ;
    KbartTitle titleNoOnline = new KbartTitle()
        .setField(Field.TITLE_ID, titleId)
        .setField(Field.PRINT_IDENTIFIER, printId)
        ;
    // Check expected values
    assertEquals("UNKNOWN", rcEmpty.getValue(title));
    assertEquals("UNKNOWN", rcEmptySpace.getValue(title));
    assertEquals(titleId, rcField.getValue(title));
    assertEquals(titleId, rcFieldNameUpper.getValue(title));
    assertEquals(titleId, rcFieldNameLower.getValue(title));
    assertEquals("UNKNOWN", rcColHeadOnly.getValue(title));
    assertEquals(titleId, rcFieldHead.getValue(title));
    assertEquals("other", rcOtherHead.getValue(title));

    // Test rankings
    assertEquals(onlineId, rcFieldRanking.getValue(title));
    assertEquals(printId, rcFieldRanking.getValue(titleNoOnline));
    assertEquals(onlineId, rcFieldRankingSpace.getValue(title));
    assertEquals(printId, rcFieldRankingSpace.getValue(titleNoOnline));
    assertEquals(onlineId, rcFieldRankingHead.getValue(title));
    assertEquals(printId, rcFieldRankingHead.getValue(titleNoOnline));
  }

  public void testFromFields() throws Exception {
    // Test creating from fields
    List<ReportColumn> fldCols = ReportColumn.fromFields(Arrays.asList(Field.values()));
    // Test creating from objects which hold strings
    List<ReportColumn> objCols = ReportColumn.fromObjects(
        new ArrayList<StringObject>() {{
          for (String s : allDescriptors) add(new StringObject(s));
        }}
    );
    // Test that ReportColumns created from Fields are recognised as fields
    // and match the Field's label in appropriate attributes.
    int n = 0;
    for (Field f : Field.values()) {
      ReportColumn rc = fldCols.get(n);
      ReportColumn rcFld = new ReportColumn(f);
      assertEquals(rc, rcFld);
      assertTrue(rc.isField());
      assertEquals(f, rc.getField());
      assertEquals(f.getLabel(), rc.getColumnDescriptor());
      assertEquals(f.getLabel(), rc.getColumnHeader());
      n++;
    }
  }

  public void testFromStrings() throws Exception {
    // Test creating from objects which are strings
    List<ReportColumn> strCols = ReportColumn.fromStrings(Arrays.asList(allDescriptors));
    // Test that ReportColumns created from strings match those created from
    // a list of string objects and directly from string.
    int n = 0;
    for (String s : allDescriptors) {
      ReportColumn rc = strCols.get(n);
      ReportColumn rcStr = new ReportColumn(s);
      assertEquals(rc, rcStr);
      assertEquals(rc.getColumnHeader(), rcStr.getColumnHeader());
      assertEquals(rc.getColumnDescriptor(), rcStr.getColumnDescriptor());
      n++;
    }
  }

  public void testFromObjects() throws Exception {
    // Test creating from objects which are strings
    List<ReportColumn> strCols = ReportColumn.fromObjects((Object[])allDescriptors);
    // Test creating from objects which hold strings
    List<StringObject> strObjs = new ArrayList<StringObject>() {{
      for (String s : allDescriptors) add(new StringObject(s));
    }};
    List<ReportColumn> objCols = ReportColumn.fromObjects(strObjs.toArray());
    // Test that ReportColumns created from objects match those created from
    // a list of string objects and directly from string.
    int n = 0;
    for (String s : allDescriptors) {
      ReportColumn rc = new ReportColumn(s);
      ReportColumn rcFromStr = strCols.get(n);
      ReportColumn rcFromObj = objCols.get(n);
      assertEquals(rc, rcFromStr);
      assertEquals(rc, rcFromObj);
      assertEquals(rcFromStr, rcFromObj);
      n++;
    }
  }

  public void testGetLabels() throws Exception {
    // Just re-exercises getColumnHeader()
  }

  public void testGetFields() throws Exception {
    // Just re-exercises isField()
  }


  /** Simple object holding a string, for testing creation with Objects. */
  class StringObject {
    String str;
    StringObject(String str) { this.str = str; }
    public String toString() {return str;}
  }

}

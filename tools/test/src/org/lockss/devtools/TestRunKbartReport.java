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
package org.lockss.devtools;

import org.apache.commons.lang3.StringUtils;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedColumnOrdering;
import org.lockss.exporter.kbart.KbartTitle;
import org.lockss.test.LockssTestCase;
import org.lockss.test.StringInputStream;
import org.lockss.util.MetadataUtil;
import org.lockss.util.StringUtil;
import static org.lockss.devtools.RunKbartReport.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Neil Mayo
 */
public class TestRunKbartReport extends LockssTestCase {

  // Set up some data with:
  // - mixed case field headings
  // - 1 duplicate title
  // - some quoted field values
  // - empty lines
  // - all final column empty
  static String emptyColumnLabel = "NUM_FIRST_VOL_ONLINE";

  static String header =
      "PUBLISHER_NAME,  publication_title, PRINT_IDENTIFIER, ONLINE_identifier, NUM_FIRST_VOL_ONLINE";

  // The expected output header is the list of col names we specify for the input,
  // plus any constant-valued cols specified in the Ordering.
  /*static EnumSet<KbartTitle.Field> headerFields = EnumSet.of(
      PUBLISHER_NAME,
      PUBLICATION_TITLE,
      PRINT_IDENTIFIER,
      ONLINE_IDENTIFIER,
      NUM_FIRST_VOL_ONLINE,
      TITLE_ID
  );*/

  // Lines forming the input document
  static String[] lines = new String[] {
      "",
      "",
      header,
      "Publisher 1,     Title 1,           1939-0343,                         , ",
      "",
      "Publisher 2,     Title 2,           0003-1615,        1533-6247        , ",
      "Publisher 3,     Title 3,           0097-3157,        1938-5293        , ",
      "\"Publisher 4\", Title 4,           1534-7311,                         , ",
      ""
  };

  /** There are 5 fields in the data lines. */
  int numberOfFields = 5;
  /** Settings */
  PredefinedColumnOrdering columnOrdering;
  boolean hideEmptyColumns = false;
  boolean showTdbStatus = false;
  File outputFile;


  /**
   * Test the main method.
   * @throws Exception
   */
  public void testMain() throws Exception {
    outputFile = getTempFile("TestRunKbartReport", ".csv");
    System.err.println("Saving report to temp file "+outputFile);
    for (boolean b : new boolean[]{true, false}) {
      hideEmptyColumns = b;
      showTdbStatus = b;
      for (PredefinedColumnOrdering fo : PredefinedColumnOrdering.values()) {
        System.err.println("Testing report with ordering "+fo.displayName);
        runTestWithFieldOrdering(fo);
      }
    }
  }

  private void runTestWithFieldOrdering(PredefinedColumnOrdering co)
      throws IOException {
    this.columnOrdering = co;
    // Set up the class
    new RunKbartReport(
        RunKbartReport.PubType.journal,
        hideEmptyColumns, showTdbStatus, columnOrdering,
        new StringInputStream(StringUtils.join(lines, "\n").toString()),
        new FileOutputStream(outputFile));

    // Test the file
    checkOutputFile(new FileReader(outputFile));
  }


  /**
   * Create iterator on InputStream; ensure it iterates over everything,
   * throws appropriate exceptions, and returns null when necessary.
   * @throws Exception
   */
  public void testKbartCsvIterator() throws Exception {
    KbartCsvIterator it = new KbartCsvIterator(
        new StringInputStream(StringUtils.join(lines, "\n").toString()),
        RunKbartReport.PubType.journal
    );
    // Check the properties of each item in the iterator
    // Items in the iterator should be in the same order as the lines,
    // omitting empty ones.
    int lineNum = 0;
    while (it.hasNext()) {
      BibliographicItem item = it.next();
      assertNotNull(item);
      lineNum = checkItemAgainstRecord(item, lineNum);
    }
    // Check next() failure
    try {
      it.next();
      fail("next() should throw NoSuchElementException.");
    } catch (NoSuchElementException e) {
      // Expected
    }
  }


  /**
   * Test iterator of titles consisting of BibliographicItems.
   * @throws Exception
   */
  public void testKbartCsvTitleIterator() throws Exception {
    KbartCsvTitleIterator it = new KbartCsvTitleIterator(
        new StringInputStream(StringUtils.join(lines, "\n").toString()),
        RunKbartReport.PubType.journal
    );

    int lineNum = 0;
    while (it.hasNext()) {
      List<BibliographicItem> title = it.next();
      assertNotNull(title);
      assertNotEmpty(title);
      // Omitting duplicate input rows is not part of the contract
      //assertNoDuplicates(title);
      // No null items
      for (BibliographicItem item : title) {
        assertNotNull(item);
      }
      // Items in the iterator should be in the same order as the lines,
      // omitting empty ones and the header.
      Iterator<BibliographicItem> bit = title.iterator();
      while (bit.hasNext()) {
        BibliographicItem item = bit.next();
        assertNotNull(item);
        lineNum = checkItemAgainstRecord(item, lineNum);
      }
    }
    // Check next() failure
    try {
      it.next();
      fail("next() should throw NoSuchElementException.");
    } catch (NoSuchElementException e) {
      // Expected
    }
  }

  private int checkItemAgainstRecord(BibliographicItem item, int recordNum) {
    // PUBLISHER_NAME,  publication_title, PRINT_IDENTIFIER, ONLINE_identifier, NUM_FIRST_VOL_ONLINE
    String line = "";
    try {
      while(StringUtil.isNullString(line) || line.equals(header)) {
        line = lines[recordNum++];
      }
    } catch (IndexOutOfBoundsException e) {
      fail("Should not run out of lines.", e);
    }
    // Split the line into trimmed tokens
    List<String> expectedProps = getExpectedProps(line);
    // The BibItem will return null for empty or invalid fields.
    assertEquals(expectedProps.get(0), item.getPublisherName());
    assertEquals(expectedProps.get(1), item.getPublicationTitle());
    assertEquals(asIssn(expectedProps.get(2)), item.getPrintIssn());
    assertEquals(asIssn(expectedProps.get(3)), item.getEissn());
    assertEquals(expectedProps.get(4), item.getStartVolume());
    // Return the record number that we have reached
    return recordNum;
  }
  
  /**
   * If the input string is a valid ISSN, returns the string,
   * othewise returns null.
   * @param s the string
   * @return s if it is a valid ISSN or null otherwise
   */
  private String asIssn(String s) {
    return MetadataUtil.isIssn(s) ? s : null;
  }

  /**
   * Split a record into a list of strings. If a field is empty, it is set to
   * null. Quotes and whitespace are trimmed from each token.
   * @param expectedProps
   * @param i
   * @return
   */
  private List<String> getExpectedProps(String line) {
    List<String> props = StringUtil.breakAt(line, ",", numberOfFields, false, true);
    Pattern ptn = Pattern.compile("^\"(.*)\"$");
    for (int i=0; i<props.size(); i++) {
      String prop = StringUtils.trim(props.get(i));
      Matcher m = ptn.matcher(prop);
      if (m.matches()) prop = m.group(1);
      props.set(i,  prop);
    }
    return props;
  }


  /**
   * A class that reads the output file, summarises it and runs various tests.
   */
  private void checkOutputFile(Reader r) {
    BufferedReader reader = new BufferedReader(r);
    int numberLines = 0;
    String outputHeader = null;
    try {
      outputHeader = reader.readLine();
      numberLines = 1;
      String s;
      while ((s = reader.readLine()) != null) {
        // Get the header from the first line
        //if (numberLines==0) outputHeader = s;
        numberLines++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Count empty and duplicate lines in the input
    int numEmptyLines = 0;
    int numDuplicateLines = 0;
    Vector<String> lineList = new Vector<String> (Arrays.asList(lines));
    for (String line : lines) {
      if (StringUtils.isEmpty(line)) numEmptyLines++;
      else {
        // If not empty, check ifthe line is a duplicate
        lineList.remove(line);
        if (lineList.contains(line)) numDuplicateLines++;
      }
    };

    // Number of entries
    assertEquals(lines.length - numEmptyLines - numDuplicateLines, numberLines);

    // Setup the expected output header line
    Vector<String> labels = new Vector<String>(columnOrdering.getOrderedLabels());
    if (hideEmptyColumns) {
      // The expected output header is the list of col names from input header,
      // plus any constant-valued cols specified in the Ordering.

      // Only keep the columns specified in the input, as the rest will be
      // empty. The list of input cols comes from splitting the original
      // header on commas and trimming each token.
      // Additionally, expect a missing column from the original inputs.
      labels.retainAll(
          new ArrayList<String>() {{
            // retain labels from the input header
            for (String s : Arrays.asList(StringUtils.split(header, ","))) {
              add(StringUtils.trim(s).toLowerCase());
            }
            // retain non-field labels from the ordering
            for (String s : columnOrdering.getNonFieldColumnLabels()) {
              add(s);
              //add(s.indexOf(" ")>=0 ? String.format("\"%s\"", s) : s);
            }
            // Also add title_url as it is filled in automatically by the exporter
            add(KbartTitle.Field.TITLE_URL.getLabel());
          }}
          //Arrays.asList(StringUtils.split(header.replace(" ", ""), ","))
      );
      labels.remove(emptyColumnLabel.toLowerCase());
    }
    if (showTdbStatus) {
      // expect an extra column
      // TODO this is not implemented yet, we need to add the extra column label
    }

    String expectedHeader = StringUtils.join(labels, ",");
    // Header should be equivalent to the defined field ordering
    //assertEquals(expectedHeader.toLowerCase(), outputHeader.toLowerCase());
    // NOTE the utf output includes a BOM, which screws up comparison
    // Also remove quotes added by CVS output rules (this is a quick kludge)
    outputHeader = outputHeader.replaceAll("\"", "");
    System.out.format("Output %s\nExpected %s\n", outputHeader, expectedHeader);
    assertTrue(outputHeader.toLowerCase().endsWith(expectedHeader.toLowerCase()));

    // TODO record ordering should be alphabetical by title, or first column

  }

}

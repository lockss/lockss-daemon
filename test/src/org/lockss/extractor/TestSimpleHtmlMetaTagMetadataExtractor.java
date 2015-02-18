/*
 * $Id$
 */

/*

  Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

public class TestSimpleHtmlMetaTagMetadataExtractor
  extends FileMetadataExtractorTestCase {

  public TestSimpleHtmlMetaTagMetadataExtractor() {
  }

  public FileMetadataExtractorFactory getFactory() {
    return new MyFileMetadataExtractorFactory();
  }

  public String getMimeType() {
    return Constants.MIME_TYPE_HTML;
  }

  public void testSingleTag() throws Exception {
    String text = "<meta name=\"FirstName\" content=\"FirstContent\">";
    assertRawEquals("firstname", "FirstContent", extractFrom(text));
  }

  public void testSingleTagReversed() throws Exception {
    String text = "<meta content=\"FirstContent\" name=\"FirstName\">";
    assertRawEquals("firstname", "FirstContent", extractFrom(text));
  }

  public void testSingleTagWithSpaces() throws Exception {
    String text = " \t <meta name=\"FirstName\" content=\"FirstContent\" >  ";
    assertRawEquals("firstname", "FirstContent", extractFrom(text));
  }

  public void testSingleTagNoContent() throws Exception {
    assertRawEmpty(extractFrom("<meta name=\"FirstName\">"));
  }

  public void testSingleTagNameUnterminated() throws Exception {
    assertRawEmpty(extractFrom("<meta name=FirstName\">"));
    assertRawEmpty(extractFrom("<meta name=\"FirstName>"));
    assertRawEmpty(extractFrom("<meta name=\"FirstName content=\"FirstContent\">"));
    assertRawEmpty(extractFrom("<meta name=FirstName\" content=\"FirstContent\">"));
    assertRawEmpty(extractFrom("<meta content=\"FirstContent\" name=\"FirstName>"));
    assertRawEmpty(extractFrom("<meta content=\"FirstContent\" name=FirstName\">"));
  }

  public void testSingleTagContentUnterminated() throws Exception {
    assertRawEmpty(extractFrom("<meta name=\"FirstName\">"));
    assertRawEmpty(extractFrom("<meta name=\"FirstName\" content=FirstContent\">"));
    assertRawEmpty(extractFrom("<meta content=\"FirstContent name=\"FirstName\">"));
    assertRawEmpty(extractFrom("<meta content=FirstContent\" name=\"FirstName\">"));
  }

  public void testSingleTagIgnoreCase() throws Exception {
    assertRawEquals("firstname", "FirstContent",
            extractFrom("<META NAME=\"FirstName\" CONTENT=\"FirstContent\">"));
    assertRawEquals("firstname", "SecondContent",
            extractFrom("<MeTa NaMe=\"FirstName\" CoNtEnT=\"SecondContent\">"));
  }

  public void testMultipleTag() throws Exception {
    String text =
      "<meta name=\"FirstName\" content=\"FirstContent\">" +
      "<meta name=\"SecondName\" content=\"SecondContent\">" +
      "<meta name=\"ThirdName\" content=\"ThirdContent\">\n" +
      "<meta name=\"FourthName\"\ncontent=\"FourthContent\">\n" +
      "<meta name=\"FifthName\" content=\"FifthContent\">\n";

    assertRawEquals(ListUtil.list("firstname", "FirstContent",
                  "secondname", "SecondContent",
                  "thirdname", "ThirdContent",
                  "fourthname", "FourthContent",
                  "fifthname", "FifthContent"),
            extractFrom(text));
  }

  public void testHtmlDecoding() throws Exception {
    String text =
      // line-feed character and multiple spaces
      "<meta name=\"jtitle\" content=\"foo\n&#xA;  \t   bar\">\n" + 
      "<meta name=\"title\" content=\"&#34;Quoted&#34; Title\">\n" +
      "<meta name=\"hex\" content=\"foo&#x22;bar&#x22; \">\n" +
      "<meta name=\"conjunct\" content=\"one&amp;two\">\n" +
      "<meta name=\"others\" content=\"l&lt;g&gt;a&amp;z\">\n";

    assertRawEquals(ListUtil.list(
                  "jtitle", "foo bar",
                  "title", "\"Quoted\" Title",
                  "hex", "foo\"bar\" ",
                  "conjunct", "one&two",
                  "others", "l<g>a&z"),
            extractFrom(text));
  }

  public void testMultipleTagWithNoise() throws Exception {
    String text =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" " +
      "\"http://www.w3.org/TR/html4/strict.dtd\">\n" +
      "<html>\n" +
      "<head>\n" +
      "<title>A Title</title>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"@@file/style.css\">\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
      "</head>\n" +
      "<body class=\"body\">" +
      "<meta name=\"FirstName\" content=\"FirstContent\"></meta>" +
      "<meta name=\"SecondName\" content=\"SecondContent\">" +
      "<p>\n" + 
      "<meta name=\"ThirdName\" content=\"ThirdContent\">\n" +
      "<meta name=\"FourthName\" content=\"FourthContent\">\n" +
      "<meta name=\n \t\"FifthName\" content=\"FifthContent\">\n" +
      "</body>\n";
    assertRawEquals(ListUtil.list("firstname", "FirstContent",
                  "secondname", "SecondContent",
                  "thirdname", "ThirdContent",
                  "fourthname", "FourthContent",
                  "fifthname", "FifthContent"),
            extractFrom(text));
  }

  private class MyFileMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    MyFileMetadataExtractorFactory() {
    }
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                 String mimeType)
        throws PluginException {
      return new SimpleHtmlMetaTagMetadataExtractor();
    }
  }
}

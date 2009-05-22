/*
 * $Id: TestSimpleMetaTagMetadataExtractor.java,v 1.1 2009-05-22 19:14:55 dshr Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

public class TestSimpleMetaTagMetadataExtractor
    extends MetadataExtractorTestCase {

  public TestSimpleMetaTagMetadataExtractor() {
  }

  public MetadataExtractorFactory getFactory() {
    return new MyMetadataExtractorFactory();
  }

  public String getMimeType() {
    return MIME_TYPE_HTML;
  }

  public void testSingleTag() throws Exception {
    String text = "<meta name=\"FirstName\" content=\"FirstContent\">";
    assertEquals(SetUtil.set("FirstName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testSingleTagReversed() throws Exception {
    String text = "<meta content=\"FirstContent\" name=\"FirstName\">";
    assertEquals(SetUtil.set("FirstName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testSingleTagWithSpaces() throws Exception {
    String text = " \t <meta name=\"FirstName\" content=\"FirstContent\" >  ";
    assertEquals(SetUtil.set("FirstName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testSingleTagNoContent() throws Exception {
    String text = "<meta name=\"FirstName\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagNameUnterminated() throws Exception {
    String text = "<meta name=FirstName\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta name=\"FirstName>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta name=\"FirstName content=\"FirstContent\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta name=FirstName\" content=\"FirstContent\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta content=\"FirstContent\" name=\"FirstName>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta content=\"FirstContent\" name=FirstName\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagContentUnterminated() throws Exception {
    String text = "<meta name=\"FirstName\" content=\"FirstContent>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta name=\"FirstName\" content=FirstContent\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta content=\"FirstContent name=\"FirstName\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<meta content=FirstContent\" name=\"FirstName\">";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagIgnoreCase() throws Exception {
    String text = "<META NAME=\"FirstName\" CONTENT=\"FirstContent\">";
    assertEquals(SetUtil.set("FirstName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent"),
		 SetUtil.theSet(extractFrom(text).values()));
    text = "<MeTa NaMe=\"FirstName\" CoNtEnT=\"FirstContent\">";
    assertEquals(SetUtil.set("FirstName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testMultipleTag() throws Exception {
    String text =
	"<meta name=\"FirstName\" content=\"FirstContent\">\n" +
	"<meta name=\"SecondName\" content=\"SecondContent\">\n" +
	"<meta name=\"ThirdName\" content=\"ThirdContent\">\n" +
	"<meta name=\"FourthName\" content=\"FourthContent\">\n" +
	"<meta name=\"FifthName\" content=\"FifthContent\">\n";
    assertEquals(SetUtil.set("FirstName", "SecondName", "ThirdName",
			     "FourthName", "FifthName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent", "SecondContent", "ThirdContent",
			     "FourthContent", "FifthContent"),
		 SetUtil.theSet(extractFrom(text).values()));
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
	"<body class=\"body\">\n" +

	"<meta name=\"FirstName\" content=\"FirstContent\">\n" +
	"<meta name=\"SecondName\" content=\"SecondContent\">\n" +
	"<meta name=\"ThirdName\" content=\"ThirdContent\">\n" +
	"<meta name=\"FourthName\" content=\"FourthContent\">\n" +
	"<meta name=\"FifthName\" content=\"FifthContent\">\n" +
	"</body>\n";
    assertEquals(SetUtil.set("FirstName", "SecondName", "ThirdName",
			     "FourthName", "FifthName"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstContent", "SecondContent", "ThirdContent",
			     "FourthContent", "FifthContent"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  private class MyMetadataExtractorFactory
      implements MetadataExtractorFactory {
    MyMetadataExtractorFactory() {
    }
    public MetadataExtractor createMetadataExtractor(String mimeType)
        throws PluginException {
      return new SimpleMetaTagMetadataExtractor();
    }
  }
}

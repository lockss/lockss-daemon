/*
 * $Id: TestSimpleXmlMetadataExtractor.java,v 1.5 2011-05-09 00:31:56 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

public class TestSimpleXmlMetadataExtractor
    extends FileMetadataExtractorTestCase {

  public TestSimpleXmlMetadataExtractor() {
  }

  public FileMetadataExtractorFactory getFactory() {
    return new MyFileMetadataExtractorFactory();
  }

  public String getMimeType() {
    return MIME_TYPE_XML;
  }

  public void testSingleTag() throws Exception {
    assertMdEquals("FirstTag", "FirstValue",
		   "<FirstTag>FirstValue</FirstTag>");
    assertMdEquals("firsttag", "FirstValue",
		   "<FirstTag>FirstValue</FirstTag>");
  }

  public void testSingleTagNoContent() throws Exception {
    assertMdEmpty("<FirstTag></FirstTag>");
  }

  public void testSingleTagUnmatched() throws Exception {
    assertMdEmpty("<FirstTag>FirstValue");
    assertMdEmpty("FirstValue</FirstTag>");
  }

  public void testSingleTagMalformed() throws Exception {
    assertMdEmpty("<FirstTag>FirstValue");
    assertMdEmpty("<FirstTag FirstValue</FirstTag>");
    assertMdEmpty("<FirstTag >FirstValue</FirstTag>");
    assertMdEmpty("<FirstTag>FirstValue</FirstTag");
    assertMdEmpty("<FirstTag>FirstValue</FirstTag >");
  }

  public void testSingleTagIgnoreCase() throws Exception {
    assertMdEquals("firsttag", "FirstValue",
		   "<fIRSTtAG>FirstValue</fIRSTtAG>");
  }

  public void testMultipleTag() throws Exception {
    String text =
	"<FirstTag>FirstValue</FirstTag>" +
	"<SecondTag>SecondValue</SecondTag>" +
	"<ThirdTag>ThirdValue</ThirdTag>" +
	"<FourthTag>FourthValue</FourthTag>" +
      "<FifthTag>FifthValue</FifthTag>";
    assertMdEquals(ListUtil.list("firsttag", "FirstValue",
				 "secondtag", "SecondValue",
				 "thirdtag", "ThirdValue",
				 "fourthtag", "FourthValue",
				 "fifthtag", "FifthValue"),
		   text);
  }

  public void testMultipleTagWithNoise() throws Exception {
    String text =
      "<OtherTag>OtherValue</OtherTag>" +
      "<SecondTag>SecondValue</SecondTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<FourthTag>FourthValue</FourthTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<FirstTag>FirstValue</FirstTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<FifthTag>FifthValue</FifthTag>" +
      "<OtherTag>OtherValue</OtherTag>" +
      "<ThirdTag>ThirdValue</ThirdTag>";
      
    assertMdEquals(ListUtil.list("firsttag", "FirstValue",
				 "secondtag", "SecondValue",
				 "thirdtag", "ThirdValue",
				 "fourthtag", "FourthValue",
				 "fifthtag", "FifthValue"),
		   text);
  }

  public void testHtmlDecoding() throws Exception {
    String text =
      "<FirstTag>&#34;Quoted&#34; Title</FirstTag>" +
      "<SecondTag>foo&#x22;bar&#x22; </SecondTag>" +
      "<ThirdTag>l&lt;g&gt;a&amp;q&quot;a&apos;z</ThirdTag>";

    assertMdEquals(ListUtil.list("FirstTag", "\"Quoted\" Title",
				 "SecondTag", "foo\"bar\" ",
				 "ThirdTag", "l<g>a&q\"a'z"),
		   text);
  }


  static final String[] TEST_TAGS = {
    "FirstTag",
    "SecondTag",
    "ThirdTag",
    "FourthTag",
    "FifthTag",
  };

  private class MyFileMetadataExtractorFactory
      implements FileMetadataExtractorFactory {
    MyFileMetadataExtractorFactory() {
    }
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							     String mimeType)
        throws PluginException {
      return new SimpleXmlMetadataExtractor(Arrays.asList(TEST_TAGS));
    }
  }
}

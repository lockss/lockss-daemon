/*
 * $Id: TestSimpleXmlMetadataExtractor.java,v 1.1.6.2 2009-11-03 23:52:01 edwardsb1 Exp $
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

public class TestSimpleXmlMetadataExtractor
    extends MetadataExtractorTestCase {

  public TestSimpleXmlMetadataExtractor() {
  }

  public MetadataExtractorFactory getFactory() {
    return new MyMetadataExtractorFactory();
  }

  public String getMimeType() {
    return MIME_TYPE_XML;
  }

  public void testSingleTag() throws Exception {
    String text = "<FirstTag>FirstValue</FirstTag>";
    assertEquals(SetUtil.set("firstname"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstValue"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testSingleTagNoContent() throws Exception {
    String text = "<FirstTag></FirstTag>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagUnmatched() throws Exception {
    String text = "<FirstTag>FirstValue";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "FirstValue</FirstTag>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagMalformed() throws Exception {
    String text = "<FirstTag FirstValue</FirstTag>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<FirstTag >FirstValue</FirstTag>";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<FirstTag>FirstValue</FirstTag";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
    text = "<FirstTag>FirstValue</FirstTag >";
    assertEquals(SetUtil.set(),
		 extractFrom(text).keySet());
  }

  public void testSingleTagIgnoreCase() throws Exception {
    String text = "<fIRSTtAG>FirstValue</fIRSTtAG>";
    assertEquals(SetUtil.set("firstname"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstValue"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  public void testMultipleTag() throws Exception {
    String text =
	"<FirstTag>FirstValue</FirstTag>" +
	"<SecondTag>SecondValue</SecondTag>" +
	"<ThirdTag>ThirdValue</ThirdTag>" +
	"<FourthTag>FourthValue</FourthTag>" +
      "<FifthTag>FifthValue</FifthTag>";
    assertEquals(SetUtil.set("firstname", "secondname", "thirdname",
			     "fourthname", "fifthname"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstValue", "SecondValue", "ThirdValue",
			     "FourthValue", "FifthValue"),
		 SetUtil.theSet(extractFrom(text).values()));
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
      
    assertEquals(SetUtil.set("firstname", "secondname", "thirdname",
			     "fourthname", "fifthname"),
		 extractFrom(text).keySet());
    assertEquals(SetUtil.set("FirstValue", "SecondValue", "ThirdValue",
			     "FourthValue", "FifthValue"),
		 SetUtil.theSet(extractFrom(text).values()));
  }

  static final String[] tags = {
    "FirstTag",
    "SecondTag",
    "ThirdTag",
    "FourthTag",
    "FifthTag",
  };
  static final String[] names = {
    "FirstName",
    "SecondName",
    "ThirdName",
    "FourthName",
    "FifthName",
  };

  private class MyMetadataExtractorFactory
      implements MetadataExtractorFactory {
    MyMetadataExtractorFactory() {
    }
    public MetadataExtractor createMetadataExtractor(String mimeType)
        throws PluginException {
      Map tagMap = new HashMap();
      for (int i = 0; i < tags.length; i++) {
	tagMap.put(tags[i], names[i]);
      }
      return new SimpleXmlMetadataExtractor(tagMap);
    }
  }
}

/*
 * $Id$
 */

/*

  Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

/** Tests for both SimpleXmlMetadataExtractor and SaxMetadataExtractor */
public abstract class TestXmlMetadataExtractor
  extends FileMetadataExtractorTestCase {
  static Logger log = Logger.getLogger("TestXmlMetadataExtractor");

  // Variant to test SaxMetadataExtractor
  public static class TestSax extends TestXmlMetadataExtractor {
    public FileMetadataExtractorFactory getFactory() {
      return new FileMetadataExtractorFactory() {
	public FileMetadataExtractor
	  createFileMetadataExtractor(MetadataTarget target, String mimeType)
	    throws PluginException {
          return new SaxMetadataExtractor(Arrays.asList(TEST_TAGS));
        }};
    }

    // SaxMetadataExtractor ignores unmatched tags
    public void testNestedTag() throws Exception {
      String text =
	"<root>" +
	"<FirstTag>FirstValue</FirstTag>" +
	"<SecondTag>SecondValue" +
	"<ThirdTag>ThirdValue</ThirdTag>" +
	"MoreValueSecond</SecondTag>" +
	"</root>";
      assertRawEquals(ListUtil.list("firsttag", "FirstValue",
				    "thirdtag", "ThirdValue"),
		      extractFrom(text));
    }
  }

  // Variant to test SimpleXmlMetadataExtractor
  public static class TestSimple extends TestXmlMetadataExtractor {
    public FileMetadataExtractorFactory getFactory() {
      return new FileMetadataExtractorFactory() {
	public FileMetadataExtractor
	  createFileMetadataExtractor(MetadataTarget target, String mimeType)
	    throws PluginException {
          return new SimpleXmlMetadataExtractor(Arrays.asList(TEST_TAGS));
        }};
    }

    // SimpleXmlMetadataExtractor handles nested tags inelegantly
    public void testNestedTag() throws Exception {
      String text =
	"<root>" +
	"<FirstTag>FirstValue</FirstTag>" +
	"<SecondTag>SecondValue" +
	"<ThirdTag>ThirdValue</ThirdTag>" +
	"MoreValueSecond</SecondTag>" +
	"</root>";
      assertRawEquals(ListUtil.list("firsttag", "FirstValue",
				    "secondtag", "SecondValue<ThirdTag>ThirdValue</ThirdTag>MoreValueSecond",
				    "thirdtag", "ThirdValue"),
				   
		      extractFrom(text));
    }
  }

  /** 
   * This test class transforms the old-style raw tags into XPath equivalents
   * that locate a node anywhere in the DOM tree.
   * 
   * @author phil
   */
  public static class MyXmlMetadataExtractor extends XmlDomMetadataExtractor {
    public MyXmlMetadataExtractor(Collection<String> keys) 
        throws XPathExpressionException {
      super(keys.size());
      
      int i = 0;
      XPath xpath = XPathFactory.newInstance().newXPath();
      for (String key : keys) {
        xpathKeys[i] = key;
        xpathExprs[i] = xpath.compile("//" + key);
        nodeValues[i] = TEXT_VALUE;
        i++;
      }
    }
  }
  
  // Variant to test XmlMetadataExtractor which uses XPath expressions
  public static class TestXPath extends TestXmlMetadataExtractor {
    public FileMetadataExtractorFactory getFactory() {
      return new FileMetadataExtractorFactory() {
        public FileMetadataExtractor
          createFileMetadataExtractor(MetadataTarget target, String mimeType)
          throws PluginException {
          try {
            return new MyXmlMetadataExtractor(Arrays.asList(TEST_TAGS));
          } catch (XPathExpressionException ex) {
            PluginException ex2 = 
                new PluginException("Error parsing XPath expressions");
            ex2.initCause(ex);
            throw ex2;
          }
        }
      };
    }

    public void testNestedTag() throws Exception {
      String text =
        "<root>" +
        "<FirstTag>FirstValue</FirstTag>" +
        "<SecondTag>SecondValue" +
        "<ThirdTag>ThirdValue</ThirdTag>" +
        "MoreValueSecond</SecondTag>" +
        "</root>";
      // The XML Document parser resolves this differently
      assertRawEquals(ListUtil.list("FirstTag", "FirstValue",
          "SecondTag", "SecondValueThirdValueMoreValueSecond",
          "ThirdTag", "ThirdValue"),
         
        extractFrom(text));
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
	TestSax.class,
	TestSimple.class,
	TestXPath.class
      });
  }

  public String getMimeType() {
    return MIME_TYPE_XML;
  }

  public void testSingleTag() throws Exception {
    assertRawEquals("FirstTag", "FirstValue",
		    extractFrom("<FirstTag>FirstValue</FirstTag>"));
    assertRawEquals("SecondTag", "SecondValue",
		    extractFrom("<SecondTag>SecondValue</SecondTag>"));
  }

  public void testSingleTagNoContent() throws Exception {
    assertRawEmpty(extractFrom("<FirstTag></FirstTag>"));
  }

  public void testSingleTagUnmatched() throws Exception {
    assertRawEmpty(extractFrom("<FirstTag>FirstValue"));
    assertRawEmpty(extractFrom("FirstValue</FirstTag>"));
  }

  public void testSingleTagMalformed() throws Exception {
    assertRawEmpty(extractFrom("<FirstTag>FirstValue"));
    assertRawEmpty(extractFrom("<FirstTag FirstValue</FirstTag>"));
    // SAX parses this although there is the trailing space
    // in the opening tag:
    // assertRawEmpty("<FirstTag >FirstValue</FirstTag>");
    assertRawEmpty(extractFrom("<FirstTag>FirstValue</FirstTag"));
    // SAX parses this although there is the trailing space
    // in the closing tag:
    // assertRawEmpty(extractFrom("<FirstTag>FirstValue</FirstTag >"));
  }

  public void testSingleTagIgnoreCase() throws Exception {
    assertRawEquals("FirstTag", "FirstValue",
		    extractFrom("<FirstTag>FirstValue</FirstTag>"));
  }

  public void testMultipleTag() throws Exception {
    String text =
      "<root>" +
      "<FirstTag>FirstValue</FirstTag>" +
      "<SecondTag>SecondValue</SecondTag>" +
      "<ThirdTag>ThirdValue</ThirdTag>" +
      "<FourthTag>FourthValue</FourthTag>" +
      "<FifthTag>FifthValue</FifthTag>" +
      "</root>";
    assertRawEquals(ListUtil.list("FirstTag", "FirstValue",
          "SecondTag", "SecondValue",
          "ThirdTag", "ThirdValue",
          "FourthTag", "FourthValue",
          "FifthTag", "FifthValue"),
        extractFrom(text));
  }

  public void testMultipleTagWithNoise() throws Exception {
    String text =
      "<root>" +
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
      "<ThirdTag>ThirdValue</ThirdTag>" +
      "</root>";

    assertRawEquals(ListUtil.list("firsttag", "FirstValue",
				  "secondtag", "SecondValue",
				  "thirdtag", "ThirdValue",
				  "fourthtag", "FourthValue",
				  "fifthtag", "FifthValue"),
		    extractFrom(text));
  }

  public void testXmlDecoding() throws Exception {
    String text =
      "<root>" +
      "<FirstTag>&#34;Quoted&#34; Title</FirstTag>" +
      "<SecondTag>foo&#x22;bar&#x22; </SecondTag>" +
      "<ThirdTag>l&lt;g&gt;a&amp;q&quot;a&apos;z</ThirdTag>" +
      "</root>";

    assertRawEquals(ListUtil.list("FirstTag", "\"Quoted\" Title",
				  "SecondTag", "foo\"bar\" ",
				  "ThirdTag", "l<g>a&q\"a'z"),
		    extractFrom(text));
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
    public FileMetadataExtractor
      createFileMetadataExtractor(MetadataTarget target, String mimeType)
        throws PluginException {
      return new SaxMetadataExtractor(Arrays.asList(TEST_TAGS));
    }
  }
}

package org.lockss.extractor;

import org.lockss.daemon.PluginException;
import org.lockss.test.FileMetadataExtractorTestCase;
import org.lockss.util.ListUtil;

import java.util.Arrays;

public class TestJsoupXmlMetadataExtractor extends
    FileMetadataExtractorTestCase {
  static final String[] TEST_TAGS = {
      "FirstTag",
      "SecondTag",
      "ThirdTag",
      "FourthTag",
      "FifthTag",
  };


  public void testNestedTag() throws Exception {
    String text =
        "<root>" +
        "<FirstTag>FirstValue</FirstTag>" +
        "<SecondTag>SecondValue" +
        "<ThirdTag>ThirdValue</ThirdTag>" +
        "MoreValueSecond</SecondTag>" +
        "</root>";
        ArticleMetadata result = extractFrom(text);
    // This parses exactly like the xpath expression parser
    assertRawEquals(ListUtil.list("FirstTag", "FirstValue",
        "SecondTag", "SecondValueThirdValueMoreValueSecond",
        "ThirdTag", "ThirdValue"),
        extractFrom(text));
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
    // jsoup will add the missing close tag on this one
    //assertRawEmpty(extractFrom("<FirstTag>FirstValue"));
    assertRawEmpty(extractFrom("FirstValue</FirstTag>"));
  }

  public void testSingleTagMalformed() throws Exception {
    // jsoup will add the missing close tag on this one
    //assertRawEmpty(extractFrom("<FirstTag>FirstValue"));
    //assertRawEmpty(extractFrom("<FirstTag FirstValue</FirstTag>"));
    // jsoup parses this
    //assertRawEmpty(extractFrom("<FirstTag >FirstValue</FirstTag>"));
    // jsoup parses this
    // assertRawEmpty(extractFrom("<FirstTag>FirstValue</FirstTag"));
    // jsoup parses this
    //assertRawEmpty(extractFrom("<FirstTag>FirstValue</FirstTag >"));
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
     // jsoup parses this but strips trailing space
    assertRawEquals(ListUtil.list("FirstTag", "\"Quoted\" Title",
        "SecondTag", "foo\"bar\"",
        "ThirdTag", "l<g>a&q\"a'z"),
        extractFrom(text));
  }

  @Override
  protected String getMimeType() {
    return MIME_TYPE_XML;
  }

  @Override
  protected FileMetadataExtractorFactory getFactory() {
    return new MyFileMetadataExtractorFactory();
  }

  private class MyFileMetadataExtractorFactory implements
      FileMetadataExtractorFactory {

    MyFileMetadataExtractorFactory() {
    }

    public FileMetadataExtractor createFileMetadataExtractor(
                                  MetadataTarget target, String mimeType)
        throws PluginException {
      JsoupTagExtractor extractor = new JsoupTagExtractor(MIME_TYPE_XML);
      extractor.setSelectors(Arrays.asList(TEST_TAGS));
      return extractor;
    }
  }

}

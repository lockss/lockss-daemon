package org.lockss.extractor;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA. User: claire Date: 03/09/2013 Time: 15:32 To
 * change this template use File | Settings | File Templates.
 */
public class TestJsoupHtmlMetaTagExtractor extends
    FileMetadataExtractorTestCase {
  public void setUp() throws Exception {
    super.setUp();
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
    //jsoup normalizes this
//assertRawEmpty(extractFrom("<meta name=FirstName\" content=\"FirstContent\">"));
    assertRawEmpty(extractFrom("<meta content=\"FirstContent\" name=\"FirstName>"));
    //jsoup normalizes this
//assertRawEmpty(extractFrom("<meta content=\"FirstContent\" name=FirstName\">"));
  }

  public void testSingleTagContentUnterminated() throws Exception {
    assertRawEmpty(extractFrom("<meta name=\"FirstName\">"));
//assertRawEmpty(extractFrom("<meta name=\"FirstName\" content=FirstContent\">"));
    assertRawEmpty(extractFrom("<meta content=\"FirstContent name=\"FirstName\">"));
//assertRawEmpty(extractFrom("<meta content=FirstContent\" name=\"FirstName\">"));
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
        "others", "la&z"), // this strips the tag g
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
        "<meta name=\"FifthName\" content=\"FifthContent\">\n" +
        "</body>\n";
      assertRawEquals(ListUtil.list("firstname", "FirstContent",
        "secondname", "SecondContent",
        "thirdname", "ThirdContent",
        "fourthname", "FourthContent",
        "fifthname", "FifthContent"),
        extractFrom(text));
  }

  public void testProblemFile() throws Exception {
    InputStream istr = this.getClass().getResourceAsStream("utf8-meta.xhtml");
    //NB. StringUtil.fromInputStream() encodes as ENCODING_ISO_8859_1
    // This is incorrect for xhtml files which defaults to utf-8/16 or the
    // value in the content encoding statement.
    String html = StringUtil.fromReader(new InputStreamReader(istr,
        Constants.ENCODING_UTF_8));
    ArticleMetadata am = extractFrom(html);
    String utf8=new String("L\u221E structures on mapping cones".getBytes(),
        Constants.ENCODING_UTF_8);
    assertEquals(utf8,
                  am.getRawList("citation_title").get(0));
  }

  private class MyFileMetadataExtractorFactory
      implements FileMetadataExtractorFactory {
    MyFileMetadataExtractorFactory() {
    }
    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                             String mimeType)
        throws PluginException {
      return new JsoupTagExtractor(mimeType);
    }
  }
}

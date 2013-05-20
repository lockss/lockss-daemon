package org.lockss.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.lockss.extractor.HtmlFormExtractor.FormElementLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.SetUtil;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created with IntelliJ IDEA. User: claire Date: 2013-01-02 Time: 10:58 AM To
 * change this template use File | Settings | File Templates.
 */


public class TestHtmlFormExtractor extends LockssTestCase {
  private JsoupHtmlLinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private Map<String, HtmlFormExtractor.FieldIterator> generators;
  private Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictions;
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    generators = new HashMap<String, HtmlFormExtractor.FieldIterator>();
    restrictions =
        new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();
    m_extractor =
        new JsoupHtmlLinkExtractor(false, true,restrictions,generators);
  }

  public void testInitProcessor() throws Exception {

    HtmlFormExtractor formExtractor = new HtmlFormExtractor(m_mau,m_callback,
                                                            ENC, null, null);
    formExtractor.initProcessor(m_extractor, null);

    // check that the extractor for form tag extractor
    assertEquals(m_extractor.unregisterTagExtractor(HtmlFormExtractor.FORM_TAG),
                 formExtractor.getFormLinkExtractor());


    FormElementLinkExtractor tag_extractor = formExtractor.getTagsLinkExtractor();
    // check it for all known form elements
    for(String el_name : HtmlFormExtractor.FORM_TAG_ELEMENTS)
    {
      assertEquals(m_extractor.unregisterTagExtractor(el_name), tag_extractor);
    }

  }

  public void testInitProcessorWithForms() throws Exception {
    StringBuilder builder = openDoc();
    StringBuilder f1 = new StringBuilder();
    StringBuilder f2 = new StringBuilder();
    openForm(f1,"http://www.example.com/bioone/cgi/;F2","form1");
    openForm(f2,"http://www.example.com/bioone/cgi/;F2", null);
    HashMap<String, String> inmap = new HashMap<String, String>(5);
    inmap.put("type", "text");
    inmap.put("name", "fname");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    inmap.put("name", "lname");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    inmap.clear();
    inmap.put("type", "submit");
    inmap.put("value", "Submit");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    closeForm(f1);
    closeForm(f2);
    builder.append(f1);
    builder.append("\n<br>");
    builder.append(f2);
    String html = closeDoc(builder);
    Document doc = Jsoup.parse(html);
    Elements forms = doc.select("form[id]");
    assertEquals(1, forms.size());
    HtmlFormExtractor formExtractor = new HtmlFormExtractor(m_mau,m_callback,
                                                            ENC, null, null);
    formExtractor.initProcessor(m_extractor, forms);
    assertNotNull(formExtractor.getForm("form1"));
    assertNull(formExtractor.getForm("form2"));
  }

  public void testAddForm() throws Exception {
    StringBuilder builder = openDoc();
    StringBuilder f1 = new StringBuilder();
    StringBuilder f2 = new StringBuilder();
    openForm(f1,"http://www.example.com/bioone/cgi/;F2","form1");
    openForm(f2,"http://www.example.com/bioone/cgi/;F2", null);
    HashMap<String, String> inmap = new HashMap<String, String>(5);
    inmap.put("type", "text");
    inmap.put("name", "fname");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    inmap.put("name", "lname");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    inmap.clear();
    inmap.put("type", "submit");
    inmap.put("value","Submit");
    addInputTag(f1, inmap);
    addInputTag(f2, inmap);
    closeForm(f1);
    closeForm(f2);
    builder.append(f1);
    builder.append("\n<br>");
    builder.append(f2);
    addTag(builder,"a",
           "href=\"http://www.example.com\" id= \"foo\">Foo","</a>");
    String html = closeDoc(builder);
    // parse the document to generate elements
    Document doc = Jsoup.parse(html);
    Elements forms = doc.select("form");

    HtmlFormExtractor formExtractor = new HtmlFormExtractor(m_mau,m_callback,
                                                            ENC, null, null);
    formExtractor.addForm(forms.first());
    assertNotNull(formExtractor.getForm("form1"));
    try {
      formExtractor.addForm(forms.last());
      fail("attempt to add form without id should fail");
    }
    catch(Exception e) {
    }
    try {
      formExtractor.addForm(forms.last());
      fail("attempt to add form without id should fail");
    }
    catch(Exception e) {
    }
    try {
      formExtractor.addForm(doc.getElementById("foo"));
      fail("attempt to non form element should fail");
    }
    catch(Exception e) {
    }
  }

  public void testElementsOutsideForm() throws Exception
  {
    String url1 = "http://www.example.com/bioone/cgi/;F2?filename=blah1.ppt&odd=world";
    String url2 = "http://www.example.com/biotwo/cgi/;F2?filename=blah2.ppt";
    String source = "<html><head><title>Test</title></head><body>"
      + "<form action=\"http://www.example.com/bioone/cgi/;F2\" id =\"form1\" method=\"get\">"
      + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah1.ppt\"></form>"
      + "<input type=\"hidden\" name=\"odd\" form=\"form1\" value=\"world\">"
      + "<form action=\"http://www.example.com/biotwo/cgi/;F2\" method=\"get\">"
      + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah2.ppt\"></form>";

    assertEquals(SetUtil.set(url1, url2), parseSingleSource(source));

  }

  public void testNonFormElementsInsideForm() throws Exception
  {
    String url1 = "http://www.example.com/bioone/cgi/;F2?filename=blah1.ppt&odd=world";
    String url2 = "http://www.example.com/test.html";
    String source = "<html><head><title>Test</title></head><body>"
      + "<form action=\"http://www.example.com/bioone/cgi/;F2\" id =\"form1\" method=\"get\">"
      + "<img src=\""+ url2 +"\">"
      + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah1.ppt\"></form>"
      + "<input type=\"hidden\" name=\"odd\" form=\"form1\" value=\"world\">";

    assertIsomorphic(SetUtil.set(url1, url2), parseSingleSource(source));
  }

  public void testElementInFormWithDiffId() throws Exception
  {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/form1?vehicle=Bike");
    expected.add("http://www.example.com/form1?vehicle=Car");
    expected.add("http://www.example.com/form1?vehicle=Train");
    expected.add("http://www.example.com/form2?vehicle=Motorcycle");
    expected.add("http://www.example.com/form2?vehicle=Bus");

    String source = "<html><head><title>Test</title></head><body>"
      +"<form id=\"form1\" name=\"input\" "
      +"action=\"http://www.example.com/form1\" method=\"get\">"
      +"<input type=\"radio\" name=\"vehicle\" value=\"Bike\" form=\"form1\">bike<br>"
      +"<input type=\"radio\" name=\"vehicle\" value=\"Car\" form=\"form1\">bar<br><br>"
      +"<input type=\"submit\" value=\"Submit1\">"
      +"</form>"
      +"<form id=\"form2\" name=\"input\" "
      +"action=\"http://www.example.com/form2\" method=\"get\">"
      +"<input type=\"radio\" name=\"vehicle\" value=\"Motorcycle\" form=\"form2\">motorcycle<br>"
      +"<input type=\"radio\" name=\"vehicle\" value=\"Train\" form=\"form1\">train<br><br>"
      +"<input type=\"radio\" name=\"vehicle\" value=\"Bus\">bus<br><br>"
      +"<input type=\"submit\" value=\"Submit2\">"
      +"</form>";

    assertIsomorphic(expected, parseSingleSource(source));
  }

 public void testNestedForms() throws Exception
 {
   Set<String> expected = new java.util.HashSet<String>();
   expected.add("http://www.example.com/form1?vehicle=Bike");
   expected.add("http://www.example.com/form1?vehicle=Car");
   expected.add("http://www.example.com/form1?vehicle=Train");
   expected.add("http://www.example.com/form1?vehicle=Bus");

   String source = "<html><head><title>Test</title></head><body>"
     +"<form id=\"form1\" name=\"input\" "
     +"action=\"http://www.example.com/form1\" method=\"get\">"
     +"<input type=\"radio\" name=\"vehicle\" value=\"Bike\" form=\"form1\">bike<br>"
     +"<input type=\"radio\" name=\"vehicle\" value=\"Car\" form=\"form1\">bar<br><br>"
     +"<input type=\"submit\" value=\"Submit1\">"
     +"<form id=\"form2\" name=\"input\" "
     +"action=\"http://www.example.com/form2\" method=\"get\">"
     +"<input type=\"radio\" name=\"vehicle\" value=\"Motorcycle\" form=\"form2\">motorcycle<br>"
     +"<input type=\"radio\" name=\"vehicle\" value=\"Train\" form=\"form1\">train<br><br>"
     +"<input type=\"radio\" name=\"vehicle\" value=\"Bus\">bus<br><br>"
     +"<input type=\"submit\" value=\"Submit2\">"
     +"</form>"
     +"</form>";

   assertIsomorphic(expected, parseSingleSource(source));

 }
  // based upon highwire test case
 public void testFormOneHiddenAttribute() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2?filename=jci116136F2.ppt";

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"jci116136F2.ppt\"></form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  public void testFormOneHiddenAttributeWithoutName() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2";

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" VALUE=\"jci116136F2.ppt\"></form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  public void testFormOneHiddenAttributeWithBlankName() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2";

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"\" VALUE=\"jci116136F2.ppt\"></form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // based upon highwire test case
  public void testFormTwoHiddenAttribute() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2?filename=jci116136F2.ppt&gender=male";

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"jci116136F2.ppt\">"
                        + "<InpUt name=\"gender\" tYpe=\"hidden\" value=\"male\"> </form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // based upon highwire test case
  public void testFormManyHiddenAttribute() throws Exception {
  String url1="http://www.example.com/cgi/powerpoint/pediatrics;" +
  "103/1/SE1/203/F4?image_path=%252Fcontent%252Fpediatrics%252Fvol103%252Fissue1%252Fimages%252Flarge%252Fpe01t0183004.jpeg&caption=No%2BCaption%2BFound&citation=Plsek%252C%2BP.%2BE.%2BPediatrics%2B1999%253B103%253Ae203&copyright=1999%2BAmerican%2BAcademy%2Bof%2BPediatrics&filename=pediatricsv103i1pe203F4.ppt&ppt_download=true&id=103%2F1%2FSE1%2F203%2FF4&redirect_url=http%253A%252F%252Fpediatrics.aappublications.org%252Fcgi%252Fcontent%252Ffull%252F103%252F1%252FSE1%252F203%252FF4&site_name=pediatrics&generate_file=Download+Image+to+PowerPoint";
    String source = "<html><head><title>Test</title></head><body>"
                        + "	  <FORM METHOD=GET ACTION=\"/cgi/powerpoint/pediatrics;103/1/SE1/203/F4\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"image_path\" VALUE=\"%2Fcontent%2Fpediatrics%2Fvol103%2Fissue1%2Fimages%2Flarge%2Fpe01t0183004.jpeg\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"caption\" VALUE=\"No+Caption+Found\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"citation\" VALUE=\"Plsek%2C+P.+E.+Pediatrics+1999%3B103%3Ae203\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"copyright\" VALUE=\"1999+American+Academy+of+Pediatrics\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"pediatricsv103i1pe203F4.ppt\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"ppt_download\" VALUE=\"true\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"id\" VALUE=\"103/1/SE1/203/F4\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"redirect_url\" VALUE=\"http%3A%2F%2Fpediatrics.aappublications.org%2Fcgi%2Fcontent%2Ffull%2F103%2F1%2FSE1%2F203%2FF4\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"site_name\" VALUE=\"pediatrics\">"
                        + "<INPUT TYPE=\"hidden\" NAME=\"notes_text\" VALUE=\"\">"
                        + "<INPUT TYPE=\"submit\" NAME=\"generate_file\" VALUE=\"Download Image to PowerPoint\">"
                        + "</FORM>";
    assertIsomorphic(SetUtil.set(url1), parseSingleSource(source));

  }

  public void testTwoFormsOneHiddenAttribute() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2?filename=blah1.ppt";
    String url2 = "http://www.example.com/biotwo/cgi/;F2?filename=blah2.ppt";

    String source = "<html><head><title>Test</title></head><body>"
    + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
    + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah1.ppt\"></form>"
    + "<form action=\"http://www.example.com/biotwo/cgi/;F2\" method=\"get\">"
    + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"blah2.ppt\"></form>";

    assertIsomorphic(SetUtil.set(url1, url2), parseSingleSource(source));

  }

  public void testPOSTForm() throws Exception {
    // For a POST form, we treat it the same way as a GET form but normalize (alpha sorted parameters) the url
    // before storing. There is a corresponding logic in the proxy handler where a post request is assembled into
    // a normalized get request before serving.
    String url = "http://www.example.com/form/post?aArg=aVal&bArg=bVal";
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"/form/post\" method=\"post\">"
                        + "<input type=\"submit\" value=\"Blah\" />"
                        + "<input type=\"hidden\" name=\"bArg\" value=\"bVal\" />"
                        + "<input type=\"hidden\" name=\"aArg\" value=\"aVal\" />"
                        + "</form>";

    assertEquals(SetUtil.set(url), parseSingleSource(source));
  }

  // a submit form of type get should return a single url
  public void testSubmitOnlyForm() throws Exception {
    String url1 = "http://www.example.com/bioone/cgi/;F2?submitName=Blah";

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input name=\"submitName\" type=\"submit\" value=\"Blah\"></form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));

  }

  // url > 256 characters after the slash succeeds (as long as long URLrepository is active)
  public void testTooLongFormUrl() throws Exception {
    StringBuilder long_value_builder = new StringBuilder();
    String prefix = ";F2?filename=j";
    for(int i = 0; i < (256 - prefix.length()); i++) {
      long_value_builder.append("a");
    }
    String long_value = long_value_builder.toString();
    String url1 = "http://www.example.com/bioone/cgi/" + prefix
                      + long_value;

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"j"
                        + long_value + "\"></form>";
    assertEquals(SetUtil.set(url1), parseSingleSource(source));
  }

  // url = 255 characters after the slash passes
  public void testMaxLengthFormUrl() throws Exception {
    StringBuilder long_value_builder = new StringBuilder();
    String prefix = ";F2?filename=j";
    for(int i = 0; i < (255 - prefix.length()); i++) {
      long_value_builder.append("a");
    }
    String long_value = long_value_builder.toString();
    String url1 = "http://www.example.com/bioone/cgi/" + prefix
                      + long_value;

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/;F2\" method=\"get\">"
                        + "<input type=\"submit\" value=\"Blah\"><INPUT TYPE=\"hidden\" NAME=\"filename\" VALUE=\"j"
                        + long_value + "\"></form>";
    assertIsomorphic(SetUtil.set(url1), parseSingleSource(source));
  }

  // no submit button => no URL returned
  public void testEmptyForm() throws Exception {

    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "</form>";
    assertIsomorphic(SetUtil.set(), parseSingleSource(source));

  }

  public void testEmptySelect() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\"></select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testSelectWithoutName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select><option value=\"hello_val\" />hello</option></select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testSelectWithBlankName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"\"><option value=\"hello_val\" />hello</option></select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneOption() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\"><option value=\"hello_val\" />hello</option></select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testTwoOptions() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>" + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testThreeOptions() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
    expected
        .add(
                "http://www.example.com/bioone/cgi/?hello_name=goodbye_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>"
                        + "<option value=\"goodbye_val\" />goodbye</option>"
                        + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testTwoSelect() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&numbers_name=one_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&numbers_name=two_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=world_val&numbers_name=one_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=world_val&numbers_name=two_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?numbers_name=one_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?numbers_name=two_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>" + "</select>"
                        + "<select name=\"numbers_name\">"
                        + "<option value=\"one_val\" />one</option>"
                        + "<option value=\"two_val\" />two</option>" + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testTwoSelectWithOneSelectUnnamed() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>" + "</select>"
                        + "<select>" + "<option value=\"one_val\" />one</option>"
                        + "<option value=\"two_val\" />two</option>" + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testTwoSelectWithOneSelectUnnamedReversedOrder()
      throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world");
    expected
        .add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world");
    expected
      .add("http://www.example.com/bioone/cgi/?odd=world");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select>" + "<option value=\"one_val\" />one</option>"
                        + "<option value=\"two_val\" />two</option>" + "</select>"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>" + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneRadioOneValue() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/?arg=val");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" name=\"arg\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneRadioOneValueWithoutName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneRadioOneValueWithBlankName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" name=\"\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneRadioMultipleValues() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/?arg=val1");
    expected.add("http://www.example.com/bioone/cgi/?arg=val2");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" name=\"arg\" value=\"val1\" />"
                        + "<input type=\"radio\" name=\"arg\" value=\"val2\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testTwoRadioMultipleValues() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?arg1=val11&arg2=val21");
    expected
        .add("http://www.example.com/bioone/cgi/?arg1=val11&arg2=val22");
    expected
        .add("http://www.example.com/bioone/cgi/?arg1=val12&arg2=val21");
    expected
        .add("http://www.example.com/bioone/cgi/?arg1=val12&arg2=val22");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" name=\"arg1\" value=\"val11\" />"
                        + "<input type=\"radio\" name=\"arg1\" value=\"val12\" />"
                        + "<input type=\"radio\" name=\"arg2\" value=\"val21\" />"
                        + "<input type=\"radio\" name=\"arg2\" value=\"val22\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testRadioWithoutName() throws Exception {
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"radio\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(SetUtil.set("http://www.example.com/bioone/cgi/"),
                 parseSingleSource(source));
  }

  public void testOneCheckboxOneValue() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/?arg=val");
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"checkbox\" name=\"arg\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneCheckboxOneValueWithoutName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"checkbox\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneCheckboxOneValueWithBlankName() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"checkbox\" name=\"\" value=\"val\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testOneCheckboxMultipleValues() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?arg=val1&arg=val2");
    expected.add("http://www.example.com/bioone/cgi/?arg=val2");
    expected.add("http://www.example.com/bioone/cgi/?arg=val1");
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"checkbox\" name=\"arg\" value=\"val1\" />"
                        + "<input type=\"checkbox\" name=\"arg\" value=\"val2\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testMultipleCheckboxesWithDefaultValues() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected
        .add("http://www.example.com/bioone/cgi/?arg1=on&arg2=on");
    expected.add("http://www.example.com/bioone/cgi/?arg2=on");
    expected.add("http://www.example.com/bioone/cgi/?arg1=on");
    expected.add("http://www.example.com/bioone/cgi/");
    String source = "<html><head><title>Test</title></head><body>"
      + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
      + "<input type=\"checkbox\" name=\"arg1\"/>"
      + "<input type=\"checkbox\" name=\"arg2\"/>"
      + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  // Add any new input types supported to this test case as well.
  public void testAllFormInputs() throws Exception {
    Set<String> expected = new java.util.HashSet<String>();
    expected.add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval1");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval1&checkbox=on");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval2");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=hello_val&odd=world&radio=rval2&checkbox=on");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval1");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval1&checkbox=on");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval2");
    expected.add("http://www.example.com/bioone/cgi/?hello_name=world_val&odd=world&radio=rval2&checkbox=on");
    expected.add("http://www.example.com/bioone/cgi/?odd=world&radio=rval1");
    expected.add("http://www.example.com/bioone/cgi/?odd=world&radio=rval1&checkbox=on");
    expected.add("http://www.example.com/bioone/cgi/?odd=world&radio=rval2");
    expected.add("http://www.example.com/bioone/cgi/?odd=world&radio=rval2&checkbox=on");
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<select name=\"hello_name\">"
                        + "<option value=\"hello_val\" />hello</option>"
                        + "<option value=\"world_val\" />world</option>" + "</select>"
                        + "<input type=\"hidden\" name=\"odd\" value=\"world\" />"
                        + "<input type=\"radio\" name=\"radio\" value=\"rval1\" />"
                        + "<input type=\"radio\" name=\"radio\" value=\"rval2\" />"
                        + "<input type=\"checkbox\" name=\"checkbox\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(expected, parseSingleSource(source));
  }

  public void testFormInsideForm() throws Exception {
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"submit\"/>"
                        + "<input type=\"hidden\" name=\"arg1\" value=\"value1\" />"
                        + "<form action=\"http://www.example.com/biotwo/cgi/\" method=\"get\">"
                        + "<input type=\"hidden\" name=\"arg2\" value=\"value2\"/>"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertIsomorphic(
                    SetUtil.set("http://www.example.com/bioone/cgi/?arg1=value1&arg2=value2"),
                    parseSingleSource(source));
  }
// NOTE: The test below is supposed to test the max num url restriction of 1000000 but it takes about 16-17s for the
// Form iterator to iterate through 1000000 urls.
//
//	public void testTooManyCheckBoxes() throws Exception {
//		String source = "<html><head><title>Test</title></head><body>"
//			+ "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
//			+ "<input type=\"checkbox\" name=\"cb1\" />"
//			+ "<input type=\"checkbox\" name=\"cb2\" />"
//			+ "<input type=\"checkbox\" name=\"cb3\" />"
//			+ "<input type=\"checkbox\" name=\"cb4\" />"
//			+ "<input type=\"checkbox\" name=\"cb5\" />"
//			+ "<input type=\"checkbox\" name=\"cb6\" />"
//			+ "<input type=\"checkbox\" name=\"cb7\" />"
//			+ "<input type=\"checkbox\" name=\"cb8\" />"
//			+ "<input type=\"checkbox\" name=\"cb9\" />"
//			+ "<input type=\"checkbox\" name=\"cb10\" />"
//			+ "<input type=\"checkbox\" name=\"cb11\" />"
//			+ "<input type=\"checkbox\" name=\"cb12\" />"
//			+ "<input type=\"checkbox\" name=\"cb13\" />"
//			+ "<input type=\"checkbox\" name=\"cb14\" />"
//			+ "<input type=\"checkbox\" name=\"cb15\" />"
//			+ "<input type=\"checkbox\" name=\"cb16\" />"
//			+ "<input type=\"checkbox\" name=\"cb17\" />"
//			+ "<input type=\"checkbox\" name=\"cb18\" />"
//			+ "<input type=\"checkbox\" name=\"cb19\" />"
//			+ "<input type=\"checkbox\" name=\"cb20\" />"
//			+ "<input type=\"submit\"/>" + "</form></html>";
//		assertEquals(1000000, parseSingleSource(source).size());
//      System.gc();
//      System.gc();
//	}

  public void testLargeNumberOfCheckBoxes() throws Exception {
    String source = "<html><head><title>Test</title></head><body>"
                        + "<form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"checkbox\" name=\"cb1\" />"
                        + "<input type=\"checkbox\" name=\"cb2\" />"
                        + "<input type=\"checkbox\" name=\"cb3\" />"
                        + "<input type=\"checkbox\" name=\"cb4\" />"
                        + "<input type=\"checkbox\" name=\"cb5\" />"
                        + "<input type=\"checkbox\" name=\"cb6\" />"
                        + "<input type=\"checkbox\" name=\"cb7\" />"
                        + "<input type=\"checkbox\" name=\"cb8\" />"
                        + "<input type=\"checkbox\" name=\"cb9\" />"
                        + "<input type=\"checkbox\" name=\"cb10\" />"
                        + "<input type=\"checkbox\" name=\"cb11\" />"
                        + "<input type=\"checkbox\" name=\"cb12\" />"
                        + "<input type=\"checkbox\" name=\"cb13\" />"
                        + "<input type=\"checkbox\" name=\"cb14\" />"
                        + "<input type=\"checkbox\" name=\"cb15\" />"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertEquals(32768, parseSingleSource(source).size());
    System.gc();
    System.gc();
  }

  public void testFormWithNoStartTag() throws Exception {
    String source = "<html><head><title>Test</title></head><body>"
                        + "</form><form action=\"http://www.example.com/bioone/cgi/\" method=\"get\">"
                        + "<input type=\"hidden\" name=\"arg1\" value=\"value1\" />"
                        + "<input type=\"hidden\" name=\"arg2\" value=\"value2\"/>"
                        + "<input type=\"submit\"/>" + "</form></html>";
    assertEquals(
                    SetUtil.set("http://www.example.com/bioone/cgi/?arg1=value1&arg2=value2"),
                    parseSingleSource(source));
  }
  public void testFailingForm() throws Exception {
    String source =  "<html><head><title>Test</title></head><body>"+
                     "<base href=http://www.example.com> " +
        "<form action=\"action/doSearch\" method=\"get\"><input " +
        "type=\"text\" name=\"searchText\" value=\"\" size=\"17\"" +
        " />\n" +
        "                <input type=\"hidden\" name=\"issue\" " +
        "value=\"1\" />\n" +
        "                <input type=\"hidden\" " +
        "name=\"journalCode\" value=\"jaeied\" />\n" +
        "                <input type=\"hidden\" name=\"volume\" " +
        "value=\"18\" />\n" +
        "                <input type=\"hidden\" name=\"filter\" value=\"issue\" />\n" +
        "                <input type=\"submit\" value=\"Search " +
        "Issue\" /></form>";
   parseSingleSource(source);
  }

  public void testFormRestrictions() throws Exception {
    Set<String> incl = new HashSet<String>();
    Set<String> excl = new HashSet<String>();
    HtmlFormExtractor.FormFieldRestrictions restricted;
    String base = "http://www.example.com/bioone/cgi/?";
    Set<String> expected = new java.util.HashSet<String>();
    // test simple select element
    List<String> elements = ListUtil.fromCSV("one,two,three,four");
    String form = formWithSelect("restrict", elements);
    for(String val : elements) {
      expected.add(base+ "restrict="+val);
    }
    expected.add("http://www.example.com/bioone/cgi/");
    // both null - no change.
    restricted = new HtmlFormExtractor.FormFieldRestrictions(null,null);
    restrictions.put("restrict", restricted);
    for(String val : elements) {
      expected.add(base+ "restrict="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
    // includes only
    incl = SetUtil.fromCSV("one,two,three");
    restricted = new HtmlFormExtractor.FormFieldRestrictions(incl,null);
    restrictions.put("restrict", restricted);
    expected.clear();
    expected.add("http://www.example.com/bioone/cgi/");
    for(String val : incl) {
      expected.add(base+ "restrict="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));

    excl = SetUtil.fromCSV("one,two,three");
    restricted = new HtmlFormExtractor.FormFieldRestrictions(null,excl);
    restrictions.put("restrict", restricted);
    expected.clear();
    expected.add("http://www.example.com/bioone/cgi/");
    for(String val : elements) {
      if(!incl.contains(val))
        expected.add(base+ "restrict="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));

    // one of each - no overlap
    incl = SetUtil.fromCSV("one,two");
    excl = SetUtil.fromCSV("three");
    restricted = new HtmlFormExtractor.FormFieldRestrictions(incl,excl);
    restrictions.put("restrict", restricted);
    expected.clear();
    expected.add("http://www.example.com/bioone/cgi/");
    for(String val : elements) {
      if(!excl.contains(val)  && incl.contains(val))
        expected.add(base+ "restrict="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
    // one of each - with overlap so exclude take precedence
    incl = SetUtil.fromCSV("one,two,three");
    excl = SetUtil.fromCSV("three");
    restricted = new HtmlFormExtractor.FormFieldRestrictions(incl,excl);
    restrictions.put("restrict", restricted);
    expected.clear();
    expected.add("http://www.example.com/bioone/cgi/");
    for(String val : elements) {
      if(!excl.contains(val)  && incl.contains(val))
        expected.add(base+ "restrict="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
  }

  public void testFixedListGenerator() throws Exception {
    HtmlFormExtractor.FieldIterator iter;
    String base = "http://www.example.com/bioone/cgi/?";
    // test fixed list generator
    List<String> elements = ListUtil.fromCSV("one,two,three,four");
    iter = new HtmlFormExtractor.FixedListFieldGenerator("genfixed", elements);
    String form = formWithInput("text", "genfixed", null);
    generators.put("genfixed", iter);
    Set<String> expected = new java.util.HashSet<String>();
    for(String val : elements) {
      expected.add(base+ "genfixed="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
  }

  public void testIntegerGenerator() throws Exception {
    HtmlFormExtractor.FieldIterator iter;
    String base = "http://www.example.com/bioone/cgi/?";

    iter = new HtmlFormExtractor.IntegerFieldIterator("genints",1,5,1);
    String form = formWithInput("text", "genints", null);
    generators.put("genints", iter);
    Set<String> expected = new java.util.HashSet<String>();
    for(int val=1; val<=5; val++) {
      expected.add(base+ "genints="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
    // make sure negative ints work
    iter = new HtmlFormExtractor.IntegerFieldIterator("genints",-1, 5, 2);
    generators.put("genints", iter);
    expected.clear();
    for(int val=-1; val<=5; val+=2) {
      expected.add(base+ "genints="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
  }
  public void testFloatGenerator() throws Exception {
    HtmlFormExtractor.FieldIterator iter;
    String base = "http://www.example.com/bioone/cgi/?";

    iter = new HtmlFormExtractor.FloatFieldIterator("genfloats", 0.0f,1.0f,0.2f);
    String form = formWithInput("text", "genfloats", null);
    generators.put("genfloats", iter);
    Set<String> expected = new java.util.HashSet<String>();
    for(float val=0.0f; val<=1.0f; val+=0.2f) {
      expected.add(base+ "genfloats="+val);
    }
    assertIsomorphic(expected, parseSingleSource(form));
  }

  public void testCalendarGenerator() throws Exception {
    HtmlFormExtractor.FieldIterator iter;
    String base = "http://www.example.com/bioone/cgi/?";
    // test calendar generator
    Calendar start = new GregorianCalendar(2012,Calendar.JANUARY,1);
    Calendar end = new GregorianCalendar(2012, Calendar.JUNE, 30);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    System.out.println("start:"+sdf.format(start.getTime())+
                       " end:"+sdf.format(end.getTime()));
    iter = new HtmlFormExtractor.CalendarFieldGenerator("gencal", start, end,
        Calendar.MONTH,1, "yyyy-MM-dd");
    String form = formWithInput("text", "gencal", null);
    generators.put("gencal", iter);
    Set<String> expected = new java.util.HashSet<String>();
    for(int val=1; val<=6; val++) {
      expected.add(base+ "gencal=2012-0"+val+"-01");
    }
    assertIsomorphic(expected, parseSingleSource(form));
  }

  public void testFormRestrictionsAndGenerators() throws Exception {
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictions
        = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();
    Map<String, HtmlFormExtractor.FieldIterator> generators
        = new HashMap<String, HtmlFormExtractor.FieldIterator>();

  }


  private StringBuilder openDoc() {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head><title>Test</title></head><body>");
    return sb;
  }

  private String closeDoc(StringBuilder sb)  {
    sb.append("</body></html>");
    return sb.toString();
  }

  private void openForm(StringBuilder sb, String action, String id) {
    sb.append("<form action=");
    sb.append(action);
    if(id != null)
    {
      sb.append(" id=");
      sb.append(id);
    }
    sb.append(">");
  }

  private void closeForm(StringBuilder sb) {
    sb.append("</form>");
  }

  private void addInputTag(StringBuilder sb, HashMap<String, String> attrs)
  {
    sb.append("<input");
    Iterator<Entry<String,String>> it = attrs.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String,String> attr = it.next();
      sb.append(" ").append(attr.getKey());
      sb.append("=").append(attr.getValue());
    }
    sb.append(">");
  }

  private void addOptionTags(StringBuilder sb, String openTag,
                           String closeTag, String[] values)
  {
    sb.append(openTag);
    for(String val : values)
    {
      sb.append("<option value=").append(val).append(">");
    }
    sb.append(closeTag);
  }

  private void addTag(StringBuilder sb, String openTag,
                              String tagData, String closeTag)
  {
    sb.append(openTag);
    sb.append(tagData);
    sb.append(closeTag);
  }

  private Set<String> parseSingleSource(String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
      new org.lockss.test.MockCachedUrl("http://www.example.com", m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
                            new org.lockss.test.StringInputStream(source), ENC,
                            "http://www.example.com", m_callback);
    return m_callback.getFoundUrls();
  }

  private String formWithSelect(String selName, List<String> options)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head><title>Test Title</title></head><body>");
    sb.append("<form action=\"http://www.example.com/bioone/cgi/\"");
    sb.append(" method=\"get\">\n");
    sb.append("<select name=\"").append(selName).append("\">\n");
    for(String value : options){
      sb.append("<option value=\"").append(value).append("\"/>").append
          (value).append("</option>\n");
    }
    sb.append("</select>\n");
    sb.append("\n<input type=\"submit\"/>").append("</form></html>");
    return sb.toString();
  }

  private String formWithInput(String inType, String inName, String inValue)
  {

    StringBuilder sb = new StringBuilder();
    String val = inValue == null ? "" : inValue;
    sb.append("<html><head><title>Test Title</title></head><body>");
    sb.append("<form action=\"http://www.example.com/bioone/cgi/\"");
    sb.append(" method=\"get\">\n");
    sb.append("<input type=\"").append(inType).append("\"");
    sb.append(" name=\"").append(inName).append("\"");
    sb.append(" value=\"").append(val).append("\"/>");
    sb.append("\n<input type=\"submit\"/>").append("</form></html>");
   return sb.toString();
  }

  private static class MyLinkExtractorCallback implements
                                               LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
  }
}

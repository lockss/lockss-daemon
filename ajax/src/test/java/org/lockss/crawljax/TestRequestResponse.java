/*
 * $Id: TestRequestResponse.java,v 1.3 2014/06/02 00:46:07 tlipkis Exp $
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

package org.lockss.crawljax;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.lockss.crawljax.AjaxRequestResponse.Header;
import org.lockss.crawljax.AjaxRequestResponse.Request;
import org.lockss.crawljax.AjaxRequestResponse.Response;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class TestRequestResponse  extends TestCase {
  static final String DEF_METHOD = "GET";
  static final String DEF_URL = "http://www.example.com";
  static final String DEF_VERSION = "HTTP/1.0";
  static final String DEF_STATUS = "200";
  static final String DEF_MESSAGE = "OK";
  static final String DEF_TEST_CONTENT = "org/lockss/crawljax/TestHtmlDocument.html";

  AjaxRequestResponse mReqResp;
  private JsonFactory factory = new JsonFactory();
  private String tmpDirPath;

  public void setUp() throws Exception {
    tmpDirPath = FileUtils.getTempDirectoryPath();

    mReqResp = new AjaxRequestResponse();
    Request req = new Request();
    req.setUrl(DEF_URL);
    req.setMethod(DEF_METHOD);
    req.setVersion(DEF_VERSION);
    List<Header> headers = new ArrayList<Header>();
    for (int i = 0; i < 5; i++) {
      Header header = new Header();
      header.setName("req-h" + i);
      header.setValue("val-" + i);
      headers.add(header);
    }
    req.setHeaders(headers);
    Response resp = new Response();
    resp.setMessage(DEF_MESSAGE);
    resp.setStatus(DEF_STATUS);
    headers = new ArrayList<Header>();
    for (int i = 0; i < 5; i++) {
      Header header = new Header();
      header.setName("resp-h" + i);
      header.setValue("val-" + i);
      headers.add(header);
    }
    resp.setHeaders(headers);
    resp.setContent(makeMockFile().getBytes(Charset.forName("UTF8")));


    mReqResp.setRequest(req);
    mReqResp.setResponse(resp);
  }


  public void tearDown() throws Exception
  {
    super.tearDown();
  }

  public void testToJson() throws Exception {
    File outFile = new File("test.json");
    outFile.deleteOnExit();
    JsonGenerator generator =
        factory.createGenerator(outFile,JsonEncoding.UTF8);

    AjaxRequestResponse.toJson(generator, mReqResp);
    generator.flush();
    generator.close();

    Request req = mReqResp.getRequest();
    Response resp = mReqResp.getResponse();
    // load it in and check
    ObjectMapper mapper = new ObjectMapper();
    AjaxRequestResponse rr = mapper.readValue(outFile, AjaxRequestResponse.class);
    // same request data
    AjaxRequestResponse.Request rr_req = rr.getRequest();
    AjaxRequestResponse.Response rr_resp = rr.getResponse();

    assertEquals(rr_req.getMethod(), req.getMethod());
    assertEquals(rr_req.getUrl(), req.getUrl());
    assertEquals(rr_req.getVersion(), req.getVersion());

    // same response data
    assertEquals(rr_resp.getMessage(), resp.getMessage());
    assertEquals(rr_resp.getStatus(), resp.getStatus());
    assertEquals(new String(rr_resp.getContent()),
                 new String(resp.getContent()));

  }

  public void testFromJson() throws Exception {
    File outFile = new File("test.json");
    outFile.deleteOnExit();
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(outFile, mReqResp);
    Request req = mReqResp.getRequest();
    Response resp = mReqResp.getResponse();

    // now test our streaming reader...
    JsonParser parser = factory.createParser(outFile);
    AjaxRequestResponse rr = AjaxRequestResponse.fromJson(parser);
    // same request data
    AjaxRequestResponse.Request rr_req = rr.getRequest();
    AjaxRequestResponse.Response rr_resp = rr.getResponse();

    assertEquals(rr_req.getMethod(), req.getMethod());
    assertEquals(rr_req.getUrl(), req.getUrl());
    assertEquals(rr_req.getVersion(), req.getVersion());

    // same response data
    assertEquals(rr_resp.getMessage(), resp.getMessage());
    assertEquals(rr_resp.getStatus(), resp.getStatus());
    assertEquals(new String(rr_resp.getContent()),
                 new String(resp.getContent()));

  }

  private String makeMockFile() {
    StringBuilder sb = new StringBuilder();
    sb.append("<HTML>\n");
    sb.append("<HEAD>\n");
    sb.append("<TITLE>Your Title Here</TITLE>\n");
    sb.append("</HEAD>\n");
    sb.append("<BODY BGCOLOR=\"FFFFFF\">\n");
    sb.append("<CENTER><IMG SRC=\"clouds.jpg\" ALIGN=\"BOTTOM\"> </CENTER>\n");
    sb.append("<HR>\n");
    sb.append("<a href=\"http://somegreatsite.com\">Link Name</a>\n");
    sb.append("is a link to another nifty site\n");
    sb.append("<H1>This is a Header</H1>\n");
    sb.append("<H2>This is a Medium Header</H2>\n");
    sb.append("Send me mail at <a href=\"mailto:support@yourcompany.com\">\n");
    sb.append("support@yourcompany.com</a>.\n");
    sb.append("<P> This is a new paragraph!\n");
    sb.append("<P> <B>This is a new paragraph!</B>\n");
    sb.append("<BR> <B><I>This is a new sentence without a paragraph break, in bold italics.</I></B>\n");
    sb.append("<HR>\n");
    sb.append("</BODY>\n");
    sb.append("</HTML>\n");
    return sb.toString();
  }

  // unused, name conflicts with LockssTestCase
  private URL getResource0(String resource){

    URL url ;

    //Try with the Thread Context Loader.
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if(classLoader != null){
      url = classLoader.getResource(resource);
      if(url != null){
        return url;
      }
    }

    //Let's now try with the classloader that loaded this class.
    classLoader = this.getClass().getClassLoader();
    if(classLoader != null){
      url = classLoader.getResource(resource);
      if(url != null){
        return url;
      }
    }

    //Last ditch attempt. Get the resource from the classpath.
    return ClassLoader.getSystemResource(resource);
  }


}
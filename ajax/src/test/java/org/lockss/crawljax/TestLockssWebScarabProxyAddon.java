/*
 * $Id: TestLockssWebScarabProxyAddon.java,v 1.2 2014/06/02 00:46:07 tlipkis Exp $
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import org.lockss.crawljax.AjaxRequestResponse.IndexEntry;

import org.owasp.webscarab.httpclient.HTTPClient;
import org.owasp.webscarab.model.HttpUrl;
import org.owasp.webscarab.model.NamedValue;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.model.Response;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for LockssWebScarabProxyAddon
 * Created by claire on 3/18/14.
 */
public class TestLockssWebScarabProxyAddon extends TestCase {
  static final String DEF_METHOD = "GET";
  static final String DEF_URL = "http://www.example.com";
  static final String DEF_VERSION = "HTTP/1.0";
  static final String DEF_STATUS = "200";
  static final String DEF_MESSAGE = "OK";

  LockssWebScarabProxyAddon m_scarabProxyAddon;
  File m_cacheDir;

  public void setUp() throws Exception {
    super.setUp();

    m_cacheDir = new File(FileUtils.getTempDirectory(),"cjax_cache");
    m_cacheDir.mkdirs();
    m_scarabProxyAddon = new LockssWebScarabProxyAddon(m_cacheDir.getAbsolutePath());
  }

  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(m_cacheDir);
    super.tearDown();
  }

  public void testFlush() throws Exception {
    List<IndexEntry> idxList = new ArrayList<IndexEntry>();
    for(int i=1; i <= 5; i++) {
      IndexEntry entry = new IndexEntry();
      entry.setUrl("url" +i);
      entry.setFile("file" + i);
      idxList.add(entry);
    }
    m_scarabProxyAddon.setIndex(idxList);
    List<IndexEntry> idx = m_scarabProxyAddon.getIndex();
    assertEquals(5, idx.size());
    m_scarabProxyAddon.clearCache();
    assertEquals(0, idx.size());

  }

  public void testWriteIndex() throws Exception {
    List<IndexEntry> idxList = new ArrayList<IndexEntry>();
    for(int i=1; i <= 5; i++) {
      IndexEntry entry = new IndexEntry();
      entry.setUrl("url" +i);
      entry.setFile("file" + i);
      idxList.add(entry);
    }
    m_scarabProxyAddon.setIndex(idxList);
    m_scarabProxyAddon.writeIndex();

    File ifile = new File(m_cacheDir, AjaxRequestResponse.INDEX_FILE_NAME);
    ObjectMapper mapper = new ObjectMapper();
    List<IndexEntry> resList = mapper.readValue(ifile,
                              new TypeReference<List<IndexEntry>>(){});
    assertEquals(idxList.size(), resList.size());
    for(int i=0; i < 5; i++) {
      assertEquals(idxList.get(i).getUrl(), resList.get(i).getUrl());
      assertEquals(idxList.get(i).getFile(), resList.get(i).getFile());
    }
  }

  public void testWriteJson() throws Exception {
    File file = new File(m_cacheDir, "1.json");
    Request req = makeMockRequest();
    Response resp = makeMockResponse();
    resp.setContent("This is test content".getBytes("UTF8"));
    m_scarabProxyAddon.writeJson(req, resp, file);
    // read in the objects as written and make sure what we wrote is valid
    ObjectMapper mapper = new ObjectMapper();
    AjaxRequestResponse rr = mapper.readValue(file,AjaxRequestResponse.class);
    // same request data
    AjaxRequestResponse.Request rr_req = rr.getRequest();
    AjaxRequestResponse.Response rr_resp = rr.getResponse();

    assertEquals(rr_req.getMethod(), req.getMethod());
    assertEquals(rr_req.getUrl(), req.getURL().toString());
    assertEquals(rr_req.getVersion(), req.getVersion());

    // same response data
    assertEquals(rr_resp.getMessage(), resp.getMessage());
    assertEquals(rr_resp.getStatus(), resp.getStatus());
    assertEquals(new String(rr_resp.getContent()), new String(resp.getContent()));
  }

  public void testGetPluginName() throws Exception {
    assertEquals("LOCKSS Request/Response Cache Proxy Addon",
                 m_scarabProxyAddon.getPluginName());
  }

  Request makeMockRequest() throws MalformedURLException {
    NamedValue[] headers = new NamedValue[5];
    for (int i = 0; i < 5; i++) {
      headers[i] = new NamedValue("req-h" + i, "v" + i);
    }

    return makeMockRequest(DEF_METHOD, DEF_URL, DEF_VERSION, headers);
  }

  private Request makeMockRequest(String method, String url,
                                  String version, NamedValue[] headers)
  throws MalformedURLException {
    Request request = new Request();
    if (method != null) {
      request.setMethod(method);
    }
    if (url != null) {
      request.setURL(new HttpUrl(url));
    }
    if (version != null) {
      request.setVersion(version);
    }
    if (headers != null) {
      request.setHeaders(headers);
    }
    return request;
  }

  Response makeMockResponse() {
    NamedValue[] headers = new NamedValue[5];
    for (int i = 0; i < 5; i++) {
      headers[i] = new NamedValue("resp-h" + i, "v" + i);
    }
    return makeMockResponse(DEF_STATUS, DEF_MESSAGE, headers);
  }

  private Response makeMockResponse(String status, String message,
                                    NamedValue[] headers) {
    Response response = new Response();
    response.setStatus(status);
    response.setMessage(message);
    response.setHeaders(headers);
    return response;
  }

  private class MockHttpClient implements HTTPClient {

    @Override
    public Response fetchResponse(final Request request) throws IOException {
      return null;
    }
  }
}

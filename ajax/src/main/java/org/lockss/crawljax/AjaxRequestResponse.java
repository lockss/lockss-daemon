/*
 * $Id: AjaxRequestResponse.java,v 1.1 2014/04/14 23:08:24 clairegriffin Exp $
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


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for providing structure for the Request - Response sequence
 *
 * Request
 *  method
 *  url
 *  version
 *  headers
 * Response
 *  status
 *  message
 *  headers
 *  content
 *
 */
public class AjaxRequestResponse {
  public static final String INDEX_FILE_NAME = "index.json";
  private Request request;
  private Response response;

  public Request getRequest() {
    return request;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  /**
   * An Entry in the index table that maps a url to a file.
   */
  public static class IndexEntry {
    private String url;
    private String file;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getFile() {
      return file;
    }

    public void setFile(String file) {
      this.file = file;
    }
  }

  /**
   * A wrapper around Name:Value pairs found in an http header.
   */
  public static class Header {
    private String name;
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  /**
   * A wrapper around an HTTP Request.
   */
  public static class Request {
    private String method;
    private String url;
    private String version;
    private List<Header> headers;

    public String getMethod() {
      return method;
    }

    public void setMethod(String method) {
      this.method = method;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public List<Header> getHeaders() {
      return headers;
    }

    public void setHeaders(List<Header> headers) {
      this.headers = headers;
    }
  }

  /**
   * A wrapper around an HTTP Response
   */
  public static class Response {
    private String status;
    private String message;
    private List<Header> headers;
    private byte[] content;

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public List<Header> getHeaders() {
      return headers;
    }

    public void setHeaders(List<Header> headers) {
      this.headers = headers;
    }

    public byte[] getContent() {
      return content;
    }

    public void setContent(byte[] content) {
      this.content = content;
    }

  }

  /**
   * Write out LOCKSS Ajax File using the streaming (fast) Jackson API
   * @param generator The Json Generator to use for writing
   * @param ajaxFile The AjaxFile object to write out.
   * @throws IOException thrown if the write fails
   */
  public static void toJson(JsonGenerator generator,
                            AjaxRequestResponse ajaxFile) throws IOException {

    Request req = ajaxFile.getRequest();
    Response resp = ajaxFile.getResponse();

    generator.writeStartObject(); // start req-resp object
    // the Request Object
    generator.writeObjectFieldStart("request"); // start request object
    generator.writeStringField("method", req.getMethod());
    generator.writeStringField("url", req.getUrl());
    generator.writeStringField("version", req.getVersion());
    generator.writeArrayFieldStart("headers"); //start headers
    List<Header> headers = req.getHeaders();
    for (Header header : headers) {
      generator.writeStartObject(); //start header object
      generator.writeStringField("name", header.getName());
      generator.writeStringField("value", header.getValue());
      generator.writeEndObject(); // end header object
    }
    generator.writeEndArray();  // end headers
    generator.writeEndObject(); // end request
    // the Response Object
    generator.writeObjectFieldStart("response"); //start response object
    generator.writeStringField("status", resp.getStatus());
    generator.writeStringField("message", resp.getMessage());
    headers = resp.getHeaders();
    generator.writeArrayFieldStart("headers"); //start headers array
    for (Header header : headers) {
      generator.writeStartObject(); //start header object
      generator.writeStringField("name", header.getName());
      generator.writeStringField("value", header.getValue());
      generator.writeEndObject(); // end header object
    }
    generator.writeEndArray(); // end headers
    generator.writeBinaryField("content", resp.getContent());
    generator.writeEndObject(); //end response
    generator.writeEndObject(); //end req-resp object
    generator.flush();
    generator.close();
  }

  /**
   * Read a LOCKSS Ajax File using the Jackson Streaming API
   * @param parser the Json parser which contains the parsed data to create
   * a new AjaxRequestResponse
   * @return a newly created and filled in AjaxRequestResponse
   * @throws IOException thrown if the parser fails to parse
   */
  public static AjaxRequestResponse fromJson(JsonParser parser)
                                          throws IOException {
    AjaxRequestResponse ajaxFile = new AjaxRequestResponse();
    Request req = new Request();
    Response resp = new Response();
    ajaxFile.setRequest(req);
    ajaxFile.setResponse(resp);
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String obj_name = parser.getCurrentName();
      if ("request".equals(obj_name)) {
        parser.nextToken();// begin request object
        while (parser.nextToken() != JsonToken.END_OBJECT) {
          String field_name = parser.getCurrentName();
          parser.nextToken(); // move to the value
          if ("method".equals(field_name)) {
            req.setMethod(parser.getText());
          } else if ("url".equals(field_name)) {
            req.setUrl(parser.getText());
          } else if ("version".equals(field_name)) {
            req.setVersion(parser.getText());
          } else if ("headers".equals(field_name)) {
            req.setHeaders(getHeaders(parser));
          }
        } // end request
      } else if ("response".equals(obj_name)) {
        parser.nextToken(); // begin response object
        while (parser.nextToken() != JsonToken.END_OBJECT) {
          String field_name = parser.getCurrentName();
          parser.nextToken(); // move to the value
          if ("status".equals(field_name)) {
            resp.setStatus(parser.getText());
          } else if ("message".equals(field_name)) {
            resp.setMessage(parser.getText());
          } else if ("content".equals(field_name)) {
            resp.setContent(parser.getBinaryValue());
          } else if ("headers".equals(field_name)) {
            resp.setHeaders(getHeaders(parser));
          }
        } // end response
      }
    }
    parser.close();
    return ajaxFile;
  }

  /**
   * Build a List of Header from a parser
   * @param parser the Json parser which contains the parsed data to create a new List
   * @return the List of Header objects
   * @throws IOException thrown if the parse fails.
   */
  private static List<Header> getHeaders(JsonParser parser) throws IOException {
    parser.nextToken(); // begin headers array
    List<Header> headerList = new ArrayList<Header>();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      parser.nextToken();
      Header header = new Header();
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String name = parser.getCurrentName();
        if ("name".equals(name)) {
          header.setName(parser.getText());
        } else if ("value".equals(name)) {
          header.setValue(parser.getText());
        }
      } // end header
      headerList.add(header);
    } // end array
    return headerList;
  }
}

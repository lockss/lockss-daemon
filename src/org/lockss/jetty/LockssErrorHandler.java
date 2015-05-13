/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
// Some portions of this code are:
// ========================================================================
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// $Id$
// ------------------------------------------------------------------------

package org.lockss.jetty;

import java.io.*;
import java.net.URLDecoder;

import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.lockss.util.*;
import org.lockss.util.StringUtil;

/** Handler to generate error pages for servlets and proxy
 */
public class LockssErrorHandler extends AbstractHttpHandler {

  private String name;

  /** Create an error handler that identifies itself as <b>LOCKSS
   * <i>name</i>, powered by Jetty</b> */
  public LockssErrorHandler(String name) {
    this.name = name;
  }

  public void handle(String pathInContext,
		     String pathParams,
		     HttpRequest request,
		     HttpResponse response)
      throws HttpException, IOException {
    String msg = response.getReason();
    Object eobj = request.getAttribute(org.mortbay.jetty.servlet.ServletHandler.__J_S_ERROR_EXCEPTION);
    if (eobj instanceof Error) {
      String cls = StringUtil.shortName(eobj.getClass());
      msg = cls + ": " + msg;
    }
    response.setContentType(HttpFields.__TextHtml);
    ByteArrayISO8859Writer writer= new ByteArrayISO8859Writer(2048);
    writeErrorPage(request, writer,
		   response.getStatus(), msg);
    writer.flush();
    response.setContentLength(writer.size());
    writer.writeTo(response.getOutputStream());
    writer.destroy();
  }
    
  protected void writeErrorPage(HttpRequest request, Writer writer,
				int code, String message)
      throws IOException {
    Integer codeInt = new Integer(code);
    String respMsg = (String)HttpResponse.__statusMsg.get(codeInt);
    if (message != null) {
      message = URLDecoder.decode(message,"UTF-8");
    }

    String errstr;
    if (respMsg != null) {
      errstr = Integer.toString(code) + " " + respMsg;
    } else {
      errstr = Integer.toString(code);
    }      

    writer.write("<html>\n<head>\n<title>Error ");
    writer.write(errstr);
    writer.write("</title>\n</head>\n<body>\n<h2>ERROR: ");
    writer.write(errstr);
    writer.write("</h2>\n");
    if (message != null) {
      writer.write("<pre>");
      writer.write(HtmlUtil.encode(message, HtmlUtil.ENCODE_TEXT));
      writer.write("</pre>\n");
    }
    writer.write("<p>RequestURI=");
    writer.write(HtmlUtil.encode(request.getURI().toString(),
				 HtmlUtil.ENCODE_TEXT));
//     writer.write(HtmlUtil.encode(request.getPath(), HtmlUtil.ENCODE_TEXT));
    writer.write("</p>\n");
    writer.write("<p><i><small>" +
		 "<a href=\"" + Constants.LOCKSS_HOME_URL +
		 "\">LOCKSS " + name + "</a>, " +
		 "<a href=\"http://jetty.mortbay.org\">powered by Jetty</a>" +
		 "</small></i></p>");

//     for (int i= 0; i < 20; i++)
//       writer.write("\n                                                ");
    writer.write("\n</body>\n</html>\n");
  }
}

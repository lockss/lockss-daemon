/*
 * $Id: ProxyHandler.java,v 1.18 2003-09-12 20:47:47 eaalto Exp $
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
// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: ProxyHandler.java,v 1.18 2003-09-12 20:47:47 eaalto Exp $
// ========================================================================

package org.lockss.proxy;

import java.io.*;
import java.net.Socket;
import org.mortbay.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.lockss.plugin.*;
import org.lockss.daemon.CuUrl;
import org.lockss.util.Logger;
import org.lockss.app.LockssDaemon;

/** LOCKSS proxy handler.
 */
public class ProxyHandler extends AbstractHttpHandler {
  private static Logger log = Logger.getLogger("Proxy");
  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;

  ProxyHandler(LockssDaemon daemon) {
    theDaemon = daemon;
    pluginMgr = theDaemon.getPluginManager();
  }

  /* ------------------------------------------------------------ */
  public void handle(String pathInContext,
		     String pathParams,
		     HttpRequest request,
		     HttpResponse response)
      throws HttpException, IOException {

    // XXX we should proxy IF
    //  1) we have a configured path that we send to a configured
    //     destination (URL rewritting forbidden by rfc???)
    //  2) We have a http and the host is not what is configured
    //     as us
    //  3) We have a ftp scheme and the FTP client classes are in
    //     our classpath (should delegate to another class to
    //     avoid linking hassles).
    org.mortbay.util.URI uri = request.getURI();
    if (!"http".equals(uri.getScheme())) {
      return;
    }

    if (log.isDebug3()) {
      log.debug3("\nPROXY:");
      log.debug3("pathInContext="+pathInContext);
      log.debug3("URI="+uri);
    }

    String urlString = uri.toString();
    CachedUrl cu = pluginMgr.findMostRecentCachedUrl(urlString);
    if (log.isDebug2()) {
      log.debug2("cu: " + cu);
    }
    if (cu != null && cu.hasContent()) {
      serveFromCache(pathInContext, pathParams, request, response, cu);
      return;
    }

    Socket socket = null;
    try {
      String host= uri.getHost();
      int port = uri.getPort();
      if (port<=0) {
        port = 80;
      }
      String path = uri.getPath();
      if (path==null || path.length()==0) {
        path = "/";
      }

      if (Code.debug()) {
        Code.debug("host=",host);
        Code.debug("port="+port);
        Code.debug("uri=",uri.toString());
      }

      // XXX associate this socket with the connection so
      // that it may be persistent.

      socket = new Socket(host,port);
      socket.setSoTimeout(5000); // XXX configure this
      OutputStream sout = socket.getOutputStream();

      request.setState(HttpMessage.__MSG_EDITABLE);
      HttpFields header = request.getHeader();

      // XXX Lets reject range requests at this point!!!!?

      // XXX need to process connection header????

      // XXX Handle Max forwards - and maybe OPTIONS/TRACE???

      // XXX need to encode the path

      header.put("Connection","close");
      header.add("Via","1.0 host (LOCKSS: Jetty/4.x)");

      // Convert the header to byte array.
      // Should assoc writer with connection for recycling!
      ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
      writer.write(request.getMethod());
      writer.write(' ');
      writer.write(uri.toString());
      writer.write(' ');
      // tk - for now always use 1.0 to avoid chunked responses, which we
      // don't handle right
      writer.write(request.getDotVersion()==0  || true
		   ? HttpMessage.__HTTP_1_0
		   : HttpMessage.__HTTP_1_1);
      writer.write('\015');
      writer.write('\012');
      header.write(writer);

      // Send the request to the next hop.
      Code.debug("\nreq=\n"+new String(writer.getBuf(), 0, writer.length()));
      writer.writeTo(sout);
      writer.reset();

      // XXX If expect 100-continue flush or no body the header now!
      sout.flush();

      // XXX cache http versions and do 417

      // XXX To to copy content with content length or chunked.


      // get ready to read the results back
      LineInput lin = new LineInput(socket.getInputStream());
      Code.debug("lin="+lin);

      // XXX need to do something about timeouts here
      String resLine = lin.readLine();
      Code.debug("First resLine = " + resLine);
      if (resLine==null) {
        return; // XXX what should we do?
      }

      // At this point we are committed to sending a response!!!!
      header = response.getHeader();
      header.clear();
      OutputStream out=response.getOutputStream();

      // Forward 100 responses
      while (resLine.startsWith("100")) {
        Code.debug("resLine = " + resLine);
        writer.write(resLine);
        writer.writeTo(out);
        out.flush();
        writer.reset();

        resLine = lin.readLine();
        if (resLine==null) {
          return; // XXX what should we do?
        }
      }

      // Read Response lne
      header.read(lin);
      // Add VIA
      header.add("Via","1.0 host (LOCKSS: Jetty/4.x)");
      // XXX do the connection based stuff here!

      // return the header
      writer.write(resLine);
      writer.write('\015');
      writer.write('\012');
      header.write(writer);
      writer.writeTo(out);
      // must set this after writing the header, and before writing the data,
      // to prevent header from being output twice
      response.setState(HttpMessage.__MSG_SENDING);

      // return the body
      // XXX need more content length options here
      // XXX need to handle prechunked
      IO.copy(lin,out);
    } catch (Exception e) {
      Code.warning(e);
    } finally {
      request.setHandled(true);
      request.setState(HttpMessage.__MSG_RECEIVED);
      response.setState(HttpMessage.__MSG_SENT);
      if (socket!=null) {
        try { socket.close(); }
        catch (Exception e) { Code.warning(e); }
      }
    }
  }

  /* XXXXXX */


  /**
   * Add a Lockss-Cu: field to the request with the locksscu: url to serve
   * from the cache, then allow request to be passed on to a
   * LockssResourceHandler.
   * @param pathInContext the path
   * @param pathParams params
   * @param request the HttpRequest
   * @param response the HttpResponse
   * @param cu the CachedUrl
   * @throws HttpException
   * @throws IOException
   */
  private void serveFromCache(String pathInContext,
			      String pathParams,
			      HttpRequest request,
			      HttpResponse response,
			      CachedUrl cu)
      throws HttpException, IOException {

    // Save current state then make request editable
    int oldState = request.getState();
    request.setState(HttpMessage.__MSG_EDITABLE);
    request.setField("Lockss-Cu", CuUrl.fromCu(cu).toString());
    request.setState(oldState);
    // Add a header to the response to identify content from LOCKSS cache
    response.setField("X-LOCKSS", "from-cache");
    if (log.isDebug2()) {
      log.debug2("serveFromCache(" + request + ")");
    }
  }
}

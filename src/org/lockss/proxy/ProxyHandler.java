/*
 * $Id: ProxyHandler.java,v 1.4 2002-10-08 01:08:31 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy;

import java.io.*;
import java.util.*;
import java.net.*;
import org.mortbay.util.*;
import org.mortbay.http.*;
//import org.mortbay.jetty.servlet.*;
import org.mortbay.http.handler.*;
//import org.mortbay.servlet.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/* ------------------------------------------------------------ */
/** LOCKSS proxy handler.
 *
 */
public class ProxyHandler extends NullHandler {

  public static void startProxy() {
    try {
      // Create the server
      HttpServer server = new HttpServer();

      // Create a port listener
      HttpListener listener = server.addListener(new InetAddrPort (9090));

      // Create a context
      HttpContext context = server.getContext(null, "/");

      // Create a servlet container
      HttpHandler handler = new ProxyHandler();

      context.addHandler(handler);

      // Start the http server
      server.start ();
    } catch (Exception e) {
      e.printStackTrace();
    }
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
    if (!"http".equals(uri.getScheme()))
      return;

    System.err.println("\nPROXY:");
    System.err.println("pathInContext="+pathInContext);
    System.err.println("URI="+uri);

    String urlString = uri.toString();
    CachedUrlSet cus = Plugin.findArchivalUnit(urlString);
    if (cus != null) {
      CachedUrl cu = cus.makeCachedUrl(urlString);
      System.err.println("proxy: cu = " + cu);
      if (cu.exists()) {
	serveFromCache(pathInContext, pathParams, request, response, cu);
	return;
      }
    }

    Socket socket=null;
    try {
      String host=uri.getHost();
      int port =uri.getPort();
      if (port<=0)
	port=80;
      String path=uri.getPath();
      if (path==null || path.length()==0)
	path="/";

      System.err.println("host="+host);
      System.err.println("port="+port);
      System.err.println("path="+path);

      // XXX associate this socket with the connection so
      // that it may be persistent.

      socket = new Socket(host,port);
      socket.setSoTimeout(5000); // XXX configure this
      System.err.println("socket="+socket);

      OutputStream sout=socket.getOutputStream();
      System.err.println("sout="+sout);

      request.setState(HttpMessage.__MSG_EDITABLE);
      HttpFields header=request.getHeader();

      // XXX Lets reject range requests at this point!!!!?

      // XXX need to process connection header????

      // XXX Handle Max forwards - and maybe OPTIONS/TRACE???

      // XXX need to encode the path

      header.put("Connection","close");
      header.add("Via","Via: 1.1 host (Jetty/4.x)");

      // Convert the header to byte array.
      // Should assoc writer with connection for recycling!
      ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
      writer.write(request.getMethod());
      writer.write(' ');
      writer.write(path);
      writer.write(' ');
      writer.write(request.getDotVersion()==0
		   ?HttpMessage.__HTTP_1_0
		   :HttpMessage.__HTTP_1_1);
      writer.write('\015');
      writer.write('\012');
      header.write(writer);

      // Send the request to the next hop.
      System.err.println("\nreq=\n"+new String(writer.getBuf(),0,writer.length()));
      writer.writeTo(sout);
      writer.reset();

      // XXX If expect 100-continue flush or no body the header now!
      sout.flush();

      // XXX cache http versions and do 417

      // XXX To to copy content with content length or chunked.


      // get ready to read the results back
      LineInput lin = new LineInput(socket.getInputStream());
      System.err.println("lin="+lin);

      // XXX need to do something about timeouts here
      String resLine = lin.readLine();
      if (resLine==null)
	return; // XXX what should we do?

      // At this point we are committed to sending a response!!!!
//        request.setHandled(true);
//        response.setState(HttpMessage.__MSG_SENT);
      OutputStream out=response.getOutputStream();

      // Forward 100 responses
      while (resLine.startsWith("100")) {
	writer.write(resLine);
	writer.writeTo(out);
	out.flush();
	writer.reset();

	resLine = lin.readLine();
	if (resLine==null)
	  return; // XXX what should we do?
      }

      System.err.println("resLine="+resLine);

      // Receive the response headers
      HttpFields resHeader = response.getHeader();
      resHeader.clear();

      // Modify them
      resHeader.read(lin);
      resHeader.add("Via","Via: 1.1 host (Jetty/4.x)"); // XXX
      // XXX do the connection based stuff here!

      // return the header
      // XXX this should really be set in the
      writer.write(resLine);
      writer.write('\015');
      writer.write('\012');
      resHeader.write(writer);
      System.err.println("\nres=\n"+resLine+"\015\012"+resHeader);
      writer.writeTo(out);

      // return the body
      // XXX need more content length options here
      // XXX need to handle prechunked
      IO.copy(lin,out);
    }
    catch (Exception e) {
      Code.warning(e);
    } finally {
      response.setState(HttpMessage.__MSG_SENT);
      request.setHandled(true);
      request.setState(HttpMessage.__MSG_RECEIVED);
      if (socket!=null) {
	try{socket.close();}
	catch(Exception e){Code.warning(e);}
      }
    }
  }

  /* XXXXXX */


  /* ------------------------------------------------------------ */
  /** Handler to serve files and resources.
   * Serves files from a given resource URL base and implements
   * the GET, HEAD, DELETE, OPTIONS, PUT, MOVE methods and the
   * IfModifiedSince and IfUnmodifiedSince header fields.
   * A simple memory cache is also provided to reduce file I/O.
   * HTTP/1.1 ranges are supported.
   */
  private String _allowHeader = null;
  private boolean _handleGeneralOptionsQuery=true;
  private boolean _acceptRanges=true;

  public boolean isAcceptRanges() {
    return _acceptRanges;
  }

  /** Set if the handler accepts range requests.
   * Default is false;
   * @param ar True if the handler should accept ranges
   */
  public void setAcceptRanges(boolean ar) {
    _acceptRanges=ar;
  }

  public boolean getHandleGeneralOptionsQuery() {
    return _handleGeneralOptionsQuery;
  }

  public void setHandleGeneralOptionsQuery(boolean b) {
    _handleGeneralOptionsQuery=b;
  }

    /* ------------------------------------------------------------ */
  private void serveFromCache(String pathInContext,
                       String pathParams,
			      HttpRequest request,
			      HttpResponse response,
			      CachedUrl cu)
      throws HttpException, IOException {
    Properties prop = cu.getProperties();

    pathInContext=Resource.canonicalPath(pathInContext);
    if (false && pathInContext==null) {
      throw new HttpException(HttpResponse.__403_Forbidden);
    }

    String method=request.getMethod();
    if (method.equals(HttpRequest.__GET) ||
	method.equals(HttpRequest.__POST) ||
	method.equals(HttpRequest.__HEAD)) {
      handleGet(request, response, pathInContext, pathParams, cu);
    } else if (method.equals(HttpRequest.__OPTIONS)) {
      handleOptions(response, pathInContext);
    } else {
      Code.debug("Unknown action:"+method);
      // anything else...
      try {
	response.sendError(response.__501_Not_Implemented);
      } catch(Exception e) {
	Code.ignore(e);
      }
    }
  }

  public void handleGet(HttpRequest request,
			HttpResponse response,
			String pathInContext,
			String pathParams,
			CachedUrl cu)
      throws IOException {

    // Check modified dates
    if (!passConditionalHeaders(request, response, cu)) {
      return;
    }

    sendCachedUrl(request, response, cu, true);
  }


  /* ------------------------------------------------------------ */
  /* Check modification date headers.
   */
  private boolean passConditionalHeaders(HttpRequest request,
					 HttpResponse response,
					 CachedUrl cu)
      throws IOException {
    if (!request.getMethod().equals(HttpRequest.__HEAD)) {
      // check any modified headers.
      HttpMessage msg = request.getHttpMessage();
      Properties props = cu.getProperties();
      long lastModified = getCuLastModified(cu);
      long date;

      if (lastModified != -1) {
	if ((date = msg.getDateField(HttpFields.__IfUnmodifiedSince)) > 0) {
	  if (lastModified > date) {
	    response.sendError(response.__412_Precondition_Failed);
	    return false;
	  }
	}

	if ((date = msg.getDateField(HttpFields.__IfModifiedSince)) > 0) {
	  if (lastModified <= date) {
	    response.sendError(response.__304_Not_Modified);
	    return false;
	  }
	}
      }
    }
    return true;
  }

  /* ------------------------------------------------------------ */
  void handleOptions(HttpResponse response, String pathInContext)
      throws IOException {
    if (!_handleGeneralOptionsQuery && pathInContext.equals("*")) {
      return;
    }

    setAllowHeader(response);
    response.commit();
  }

  /* ------------------------------------------------------------ */
  void setAllowHeader(HttpResponse response) {
    if (_allowHeader == null) {
      StringBuffer sb = new StringBuffer(128);
      sb.append(HttpRequest.__GET);
      sb.append(", ");
      sb.append(HttpRequest.__HEAD);
      sb.append(", ");
      sb.append(HttpRequest.__OPTIONS);
      _allowHeader = sb.toString();
    }
    response.getHttpMessage().setField(HttpFields.__Allow, _allowHeader);
  }


    /* ------------------------------------------------------------ */
    void sendCachedUrl(HttpRequest request,
		       HttpResponse response,
		       CachedUrl cu,
		       boolean writeHeaders)
        throws IOException {

      SendableResource data = new CachedUrlFile(cu);
      long resLength = data.getLength();

      //  see if there are any range headers
      Enumeration reqRanges =
	(request.getDotVersion() > 0
	 ? request.getHttpMessage().getFieldValues(HttpFields.__Range)
	 : null);

      if (!writeHeaders || reqRanges == null ||
	  !reqRanges.hasMoreElements()) {
	//  if there were no ranges, send entire entity
	if (writeHeaders) {
	  data.writeHeaders(request, response, resLength);
	}
	System.err.println("writing " + resLength + " bytes");
	data.writeBytes(response.getOutputStream(),
			0, resLength);
	data.requestDone();
	request.setHandled(true);
	response.setState(HttpMessage.__MSG_SENT);
	request.setState(HttpMessage.__MSG_RECEIVED);
	return;
      }

      // Parse the ranges
      List validRanges =InclusiveByteRange.parseRangeHeaders(reqRanges);
      Code.debug("requested ranges: " + reqRanges + "=" + validRanges);

      //  run through the ranges and count satisfiable ranges;
      ListIterator rit = validRanges.listIterator();
      InclusiveByteRange singleSatisfiableRange = null;
      while (rit.hasNext()) {
	InclusiveByteRange ibr = (InclusiveByteRange) rit.next();

	if (ibr.getFirst()>=resLength) {
	  Code.debug("not satisfiable: ",ibr);
	  rit.remove();
	  continue;
	}

	if (singleSatisfiableRange == null) {
	  singleSatisfiableRange = ibr;
	}
      }

      //  if there are no satisfiable ranges, send 416 response
      if (singleSatisfiableRange == null) {
	Code.debug("no satisfiable ranges");
	data.writeHeaders(request,response, resLength);
	response.setStatus(response.__416_Requested_Range_Not_Satisfiable);
	response.setReason((String)response.__statusMsg
			   .get(new Integer(response.__416_Requested_Range_Not_Satisfiable)));
	response.getHttpMessage().setField(
					   HttpFields.__ContentRange,
					   InclusiveByteRange.to416HeaderRangeString(resLength));
	data.writeBytes(response.getOutputStream(),
			0, resLength);
	request.setHandled(true);
	return;
      }

      //  if there is only a single valid range (must be satisfiable
      //  since were here now), send that range with a 216 response
      if ( validRanges.size()== 1) {
	Code.debug("single satisfiable range: " + singleSatisfiableRange);
	long singleLength = singleSatisfiableRange.getSize(resLength);
	data.writeHeaders(request,response, singleLength);
	response.setStatus(response.__206_Partial_Content);
	response.setReason((String)response.__statusMsg
			   .get(new Integer(response.__206_Partial_Content)));
	response.getHttpMessage().setField(HttpFields.__ContentRange,
					   singleSatisfiableRange.toHeaderRangeString(resLength));
	data.writeBytes(response.getOutputStream(),
			singleSatisfiableRange.getFirst(resLength),
			singleLength);
	request.setHandled(true);
	return;
      }

      //  multiple non-overlapping valid ranges cause a multipart
      //  216 response which does not require an overall
      //  content-length header
      //
      String encoding = data.getEncoding();
      MultiPartResponse multi = new MultiPartResponse(request, response);
      response.setStatus(response.__206_Partial_Content);
      response.setReason((String)response.__statusMsg
			 .get(new Integer(response.__206_Partial_Content)));

      // If the request has a "Request-Range" header then we need to
      // send an old style multipart/x-byteranges Content-Type. This
      // keeps Netscape and acrobat happy. This is what Apache does.
      String ctp;
      if (request.getHttpMessage().containsField(HttpFields.__RequestRange)) {
	ctp = "multipart/x-byteranges; boundary=";
      } else {
	ctp = "multipart/byteranges; boundary=";
      }
      response.getHttpMessage().setContentType(ctp+multi.getBoundary());

      rit = validRanges.listIterator();
      while (rit.hasNext()) {
	InclusiveByteRange ibr = (InclusiveByteRange) rit.next();
	String header=HttpFields.__ContentRange+": "+
	  ibr.toHeaderRangeString(resLength);
	Code.debug("multi range: ",encoding," ",header);
	multi.startPart(encoding,new String[]{header});
	data.writeBytes(multi.getOut(), ibr.getFirst(resLength), ibr.getSize(resLength));
      }
      multi.close();

      request.setHandled(true);

      return;
    }

  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */

  private interface SendableResource {
    long getLength();
    String getEncoding();
    void writeHeaders(HttpRequest request,HttpResponse response, long count)
	throws IOException;
    void writeBytes(OutputStream os, long startByte, long count)
	throws IOException;
    void requestDone();
  }

  static long getCuLastModified(CachedUrl cu) {
    Properties props = cu.getProperties();
    return Long.parseLong(props.getProperty(HttpFields.__LastModified, "-1"));
  }

  static long getCuLength(CachedUrl cu) {
    Properties props = cu.getProperties();
    return Long.parseLong(props.getProperty(HttpFields.__ContentLength, "-1"));
  }

  static String getCuContentType(CachedUrl cu) {
    Properties props = cu.getProperties();
    return props.getProperty(HttpFields.__ContentType, "text/plain");
  }

  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  /** Holds a CachedUrl file.
   */
  private class CachedUrlFile implements SendableResource {

    CachedUrl cu;
    InputStream ris = null;
    String encoding;
    long length = 0;
    long pos = 0;

    /* ------------------------------------------------------------ */
    public String getEncoding() {
      return encoding;
    }

    /* ------------------------------------------------------------ */
    public long getLength() {
      return length;
    }

    /* ------------------------------------------------------------ */
    public CachedUrlFile(CachedUrl cu) {
      this.cu = cu;
      encoding = ProxyHandler.getCuContentType(cu);
      length = getCuLength(cu);
    }

    /* ------------------------------------------------------------ */
    public void writeBytes(OutputStream os, long start, long count)
	throws IOException {
      if (ris == null || pos > start) {
	if (ris != null) {
	  ris.close();
	}
	ris = cu.openForReading();
	pos = 0;
      }

      if (pos < start) {
	ris.skip(start - pos);
	pos = start;
      }
      IO.copy(ris,os,(int)count);
      pos+=count;
    }

    /* ------------------------------------------------------------ */
    public void writeHeaders(HttpRequest request, HttpResponse response,
			     long count) {
      HttpMessage facade = response.getHttpMessage();
      facade.setContentType(encoding);
      if (length != -1) {
	facade.setContentLength((int)count);
      }
      facade.setDateField(HttpFields.__LastModified, getCuLastModified(cu));
      if (_acceptRanges && request.getDotVersion()>0) {
	facade.setField(HttpFields.__AcceptRanges,"bytes");
      }
    }

    /* ------------------------------------------------------------ */
    public void requestDone() {
      try {
	if (ris != null) {
	  ris.close();
	}
      } catch (IOException ioe) {
	Code.ignore(ioe);
      }
    }
  }
}

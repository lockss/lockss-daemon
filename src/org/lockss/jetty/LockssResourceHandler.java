/*
 * $Id: LockssResourceHandler.java,v 1.15 2005-12-01 23:28:04 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
// Portions of this code are:
// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: LockssResourceHandler.java,v 1.15 2005-12-01 23:28:04 troberts Exp $
// ---------------------------------------------------------------------------

package org.lockss.jetty;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.log.LogFactory;
import org.mortbay.util.*;

import com.sun.jimi.core.*;
import com.sun.jimi.core.raster.JimiRasterImage;

import org.lockss.app.LockssDaemon;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.CachedUrl;
import org.lockss.proxy.ProxyManager;

/** Extension of ResourceHandler that allows flexibility in finding the
 * Resource.  Mostly copied here because some things in ResourceHandler
 * aren't public or protected. */
public class LockssResourceHandler extends AbstractHttpHandler {
    private static Log log = LogFactory.getLog(ResourceHandler.class);

    /* ----------------------------------------------------------------- */
    private LockssDaemon theDaemon = null;
    private ProxyManager proxyMgr = null;
    private boolean _acceptRanges=true;
    private boolean _redirectWelcomeFiles ;
    private String _redirectRootTo ;
    private String[] _methods=null;
    private String _allowed;
    private boolean _dirAllowed=true;
    private int _minGzipLength =-1;
    private StringMap _methodMap = new StringMap();
    {
        setAllowedMethods(new String[]
            {
                HttpRequest.__GET,
                HttpRequest.__MOVE,
                HttpRequest.__POST,
                HttpRequest.__HEAD,
                HttpRequest.__OPTIONS,
                HttpRequest.__TRACE
            });
    }

    /* ----------------------------------------------------------------- */
    /** Construct a ResourceHandler.
     */
    public LockssResourceHandler(LockssDaemon daemon)
    {
      theDaemon = daemon;
      proxyMgr = theDaemon.getProxyManager();
    }


    /* ----------------------------------------------------------------- */
    public synchronized void start()
        throws Exception
    {
        super.start();
    }

    /* ----------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        super.stop();
    }

    public void setRedirectRootTo(String target) {
      _redirectRootTo = target;
    }

    /* ------------------------------------------------------------ */
    public String[] getAllowedMethods()
    {
        return _methods;
    }

    /* ------------------------------------------------------------ */
    public void setAllowedMethods(String[] methods)
    {
        StringBuffer b = new StringBuffer();
        _methods=methods;
        _methodMap.clear();
        for (int i=0;i<methods.length;i++)
        {
            _methodMap.put(methods[i],methods[i]);
            if (i>0)
                b.append(',');
            b.append(methods[i]);
        }
        _allowed=b.toString();
    }

    /* ------------------------------------------------------------ */
    public boolean isMethodAllowed(String method)
    {
        return _methodMap.get(method)!=null;
    }

    /* ------------------------------------------------------------ */
    public String getAllowedString()
    {
        return _allowed;
    }

    /* ------------------------------------------------------------ */
    public boolean isDirAllowed()
    {
        return _dirAllowed;
    }

    /* ------------------------------------------------------------ */
    public void setDirAllowed(boolean dirAllowed)
    {
        _dirAllowed = dirAllowed;
    }

    /* ------------------------------------------------------------ */
    public boolean isAcceptRanges()
    {
        return _acceptRanges;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return True if welcome files are redirected to. False if forward is used.
     */
    public boolean getRedirectWelcome()
    {
        return _redirectWelcomeFiles;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param redirectWelcome True if welcome files are redirected to. False
     * if forward is used.
     */
    public void setRedirectWelcome(boolean redirectWelcome)
    {
        _redirectWelcomeFiles = redirectWelcome;
    }

    /* ------------------------------------------------------------ */
    /** Set if the handler accepts range requests.
     * Default is false;
     * @param ar True if the handler should accept ranges
     */
    public void setAcceptRanges(boolean ar)
    {
        _acceptRanges=ar;
    }

    /* ------------------------------------------------------------ */
    /** Get minimum content length for GZIP encoding.
     * @return Minimum length of content for gzip encoding or -1 if disabled.
     */
    public int getMinGzipLength()
    {
        return _minGzipLength;
    }

    /* ------------------------------------------------------------ */
    /** Set minimum content length for GZIP encoding.
     * @param minGzipLength If set to a positive integer, then static content
     * larger than this will be served as gzip content encoded
     * if a matching resource is found ending with ".gz"
     */
    public void setMinGzipLength(int minGzipLength)
    {
        _minGzipLength = minGzipLength;
    }


    /* ------------------------------------------------------------ */
    /** get Resource to serve.
     * Map a path to a resource. The default implementation calls
     * HttpContext.getResource but derived handers may provide
     * their own mapping.
     * @param pathInContext The path to find a resource for.
     * @return The resource to serve.
     */
    protected Resource getResource(HttpRequest request, String pathInContext)
        throws IOException
    {
        return getHttpContext().getResource(pathInContext);
    }

    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        Resource resource = getResource(request, pathInContext);
        if (resource==null)
            return;

        // Is the method allowed?
        if (!isMethodAllowed(request.getMethod()))
        {
            if(log.isDebugEnabled())log.debug("Method not allowed: "+request.getMethod());
            if (resource.exists())
            {
                setAllowHeader(response);
                response.sendError(HttpResponse.__405_Method_Not_Allowed);
            }
            return;
        }

        // Handle the request
        try
        {
            if(log.isDebugEnabled())log.debug("PATH="+pathInContext+" RESOURCE="+resource);

            // check filename
            String method=request.getMethod();
            if (method.equals(HttpRequest.__GET) ||
                method.equals(HttpRequest.__POST) ||
                method.equals(HttpRequest.__HEAD))
                handleGet(request, response, pathInContext, pathParams, resource);
            else if (method.equals(HttpRequest.__PUT))
                handlePut(request, response, pathInContext, resource);
            else if (method.equals(HttpRequest.__DELETE))
                handleDelete(request, response, pathInContext, resource);
            else if (method.equals(HttpRequest.__OPTIONS))
                handleOptions(response, pathInContext);
            else if (method.equals(HttpRequest.__MOVE))
                handleMove(request, response, pathInContext, resource);
            else if (method.equals(HttpRequest.__TRACE))
                handleTrace(request, response);
            else
            {
                if(log.isDebugEnabled())log.debug("Unknown action:"+method);
                // anything else...
                try{
                    if (resource.exists())
                        response.sendError(HttpResponse.__501_Not_Implemented);
                }
                catch(Exception e) {LogSupport.ignore(log,e);}
            }
        }
        catch(IllegalArgumentException e)
        {
            LogSupport.ignore(log,e);
        }
        finally
        {
            if (resource!=null && !(resource instanceof CachedResource))
                resource.release();
        }
    }

    /* ------------------------------------------------------------------- */
    public void handleGet(HttpRequest request,
                          HttpResponse response,
                          String pathInContext,
                          String pathParams,
                          Resource resource)
        throws IOException
    {
        if(log.isDebugEnabled())log.debug("Looking for "+resource);

        if (resource!=null && resource.exists())
        {
            // check if directory
            if (resource.isDirectory())
            {
                if (!pathInContext.endsWith("/") && !pathInContext.equals("/"))
                {
                    log.debug("Redirect to directory/");

                    String q=request.getQuery();
                    StringBuffer buf=request.getRequestURL();
                    if (q!=null&&q.length()!=0)
                    {
                        buf.append('?');
                        buf.append(q);
                    }
                    response.setField(HttpFields.__Location, URI.addPaths(buf.toString(),"/"));
                    response.setStatus(302);
                    request.setHandled(true);
                    return;
                }

		if (_redirectRootTo != null && pathInContext.equals("/")) {
		  log.debug("Redirect root to " + _redirectRootTo);

		  String q=request.getQuery();
		  StringBuffer buf=request.getRequestURL();
		  if (q!=null&&q.length()!=0) {
		    buf.append('?');
		    buf.append(q);
		  }
		  response.setField(HttpFields.__Location,
				    URI.addPaths(buf.toString(),
						 _redirectRootTo));
		  response.setStatus(302);
		  request.setHandled(true);
		  return;
                }

                // See if index file exists
                String welcome=getHttpContext().getWelcomeFile(resource);
                if (welcome!=null)
                {
                    // Forward to the index
                    String ipath=URI.addPaths(pathInContext,welcome);
                    if (_redirectWelcomeFiles)
                    {
                        // Redirect to the index
                        ipath=URI.addPaths(getHttpContext().getContextPath(),ipath);
                        response.setContentLength(0);
                        response.sendRedirect(ipath);
                    }
                    else
                    {
                        URI uri=request.getURI();
                        uri.setPath(URI.addPaths(uri.getPath(),welcome));
                        getHttpContext().handle(ipath,pathParams,request,response);
                    }
                    return;
                }

                // Check modified dates
                if (!passConditionalHeaders(request,response,resource))
                    return;
                // If we got here, no forward to index took place
                sendDirectory(request,response,resource,pathInContext.length()>1);
            }
            else if (handleLockssRedirect(request, response, pathInContext,
					  pathParams, resource))
            {
	      return;
	    }
            // check if it is a file
            else if (resource.exists())
            {
                // Check modified dates
                if (!passConditionalHeaders(request,response,resource))
                    return;
                sendData(request,response,pathInContext,resource,true);
            }
            else
                // don't know what it is
                log.warn("Unknown file type");
        }
    }

  // CachedUrls may have content, yet specify a redirect elsewhere.  The
  // redirect must be returned to the requestor.
  boolean handleLockssRedirect(HttpRequest request,
			       HttpResponse response,
			       String pathInContext,
			       String pathParams,
			       Resource resource) {
    if (!(resource instanceof CuUrlResource)) {
      return false;
    }
    CuUrlResource cur = (CuUrlResource)resource;
    String nodeUrl = cur.getProperty(CachedUrl.PROPERTY_NODE_URL);
    String rTo = cur.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
    String reqUrl = request.getRequestURL().toString();
    // follow any redirect property, unless it points at current URL.  (Can
    // happen on "directory" nodes, which have two names, with and without
    // slash.)
    if (rTo != null) {
      if (rTo.equals(reqUrl)) {
	return false;
      } else {
	sendLockssRedirect(request, response, pathInContext,
			   pathParams, resource, rTo);
	return true;
      }
    } else
      // Can't count on directory node having a redirected-to property,
      // bacause it might have been with a final slash, hence no redirect.
      // If request path doesn't end with slash but node's path does,
      // then this is a request for a node that's actually a "directory",
      // so issue the redirect.
      if (!pathInContext.endsWith("/")) {
	URI nodeUri = new URI(nodeUrl);
	if (nodeUri.getPath().endsWith("/")) {
	  sendLockssRedirect(request, response, pathInContext,
			     pathParams, resource, nodeUrl);
	  return true;
	}
      }
    return false;
  }

  void sendLockssRedirect(HttpRequest request,
			  HttpResponse response,
                          String pathInContext,
                          String pathParams,
			  Resource resource,
			  String to) {
    response.setField(HttpFields.__Location, to);
    response.setStatus(HttpResponse.__301_Moved_Permanently);
    request.setHandled(true);
  }

    /* ------------------------------------------------------------ */
    /* Check modification date headers.
     */
    private boolean passConditionalHeaders(HttpRequest request,
                                           HttpResponse response,
                                           Resource resource)
        throws IOException
    {
        if (!request.getMethod().equals(HttpRequest.__HEAD))
        {
            // If we have meta data for the file
            // Try a direct match for most common requests. Avoids
            // parsing the date.
            ResourceCache.ResourceMetaData metaData =
                (ResourceCache.ResourceMetaData)resource.getAssociate();
            if (metaData!=null)
            {
                String ifms=request.getField(HttpFields.__IfModifiedSince);
                String mdlm=metaData.getLastModified();
                if (ifms!=null && mdlm!=null && ifms.equals(mdlm))
                {
                    response.setStatus(HttpResponse.__304_Not_Modified);
                    request.setHandled(true);
                    return false;
                }
            }


            long date=0;
            // Parse the if[un]modified dates and compare to resource

            if ((date=request.getDateField(HttpFields.__IfUnmodifiedSince))>0)
            {
                if (resource.lastModified()/1000 > date/1000)
                {
                    response.sendError(HttpResponse.__412_Precondition_Failed);
                    return false;
                }
            }

            if ((date=request.getDateField(HttpFields.__IfModifiedSince))>0)
            {

                if (resource.lastModified()/1000 <= date/1000)
                {
                    response.setStatus(HttpResponse.__304_Not_Modified);
                    request.setHandled(true);
                    return false;
                }
            }

        }
        return true;
    }


    /* ------------------------------------------------------------ */
    void handlePut(HttpRequest request,
                   HttpResponse response,
                   String pathInContext,
                   Resource resource)
        throws IOException
    {
        if(log.isDebugEnabled())log.debug("PUT "+pathInContext+" in "+resource);

        boolean exists=resource!=null && resource.exists();
        if (exists &&
            !passConditionalHeaders(request,response,resource))
            return;

        if (pathInContext.endsWith("/"))
        {
            if (!exists)
            {
                if (!resource.getFile().mkdirs())
                    response.sendError(HttpResponse.__403_Forbidden, "Directories could not be created");
                else
                {
                    request.setHandled(true);
                    response.setStatus(HttpResponse.__201_Created);
                    response.commit();
                }
            }
            else
            {
                request.setHandled(true);
                response.setStatus(HttpResponse.__200_OK);
                response.commit();
            }
        }
        else
        {
            try
            {
                int toRead = request.getContentLength();
                InputStream in = request.getInputStream();
                OutputStream out = resource.getOutputStream();
                if (toRead>=0)
                    IO.copy(in,out,toRead);
                else
                    IO.copy(in,out);
                out.close();
                request.setHandled(true);
                response.setStatus(exists
                                   ?HttpResponse.__200_OK
                                   :HttpResponse.__201_Created);
                response.commit();
            }
            catch (Exception ex)
            {
                log.warn(LogSupport.EXCEPTION,ex);
                response.sendError(HttpResponse.__403_Forbidden,
                                   ex.getMessage());
            }
        }
    }

    /* ------------------------------------------------------------ */
    void handleDelete(HttpRequest request,
                      HttpResponse response,
                      String pathInContext,
                      Resource resource)
        throws IOException
    {
        if(log.isDebugEnabled())log.debug("DELETE "+pathInContext+" from "+resource);

        if (!resource.exists() ||
            !passConditionalHeaders(request,response,resource))
            return;

        try
        {
            // delete the file
            if (resource.delete())
                response.setStatus(HttpResponse.__204_No_Content);
            else
                response.sendError(HttpResponse.__403_Forbidden);

            // Send response
            request.setHandled(true);
        }
        catch (SecurityException sex)
        {
            log.warn(LogSupport.EXCEPTION,sex);
            response.sendError(HttpResponse.__403_Forbidden, sex.getMessage());
        }
    }


    /* ------------------------------------------------------------ */
    void handleMove(HttpRequest request,
                    HttpResponse response,
                    String pathInContext,
                    Resource resource)
        throws IOException
    {
        if (!resource.exists() || !passConditionalHeaders(request,response,resource))
            return;


        String newPath = URI.canonicalPath(request.getField("New-uri"));
        if (newPath==null)
        {
            response.sendError(HttpResponse.__405_Method_Not_Allowed,
                               "Bad new uri");
            return;
        }

        String contextPath = getHttpContext().getContextPath();
        if (contextPath!=null && !newPath.startsWith(contextPath))
        {
            response.sendError(HttpResponse.__405_Method_Not_Allowed,
                               "Not in context");
            return;
        }


        // Find path
        try
        {
            // XXX - Check this
            String newInfo=newPath;
            if (contextPath!=null)
                newInfo=newInfo.substring(contextPath.length());
            Resource newFile = getHttpContext().getBaseResource().addPath(newInfo);

            if(log.isDebugEnabled())log.debug("Moving "+resource+" to "+newFile);
            resource.renameTo(newFile);

            response.setStatus(HttpResponse.__204_No_Content);
            request.setHandled(true);
        }
        catch (Exception ex)
        {
            log.warn(LogSupport.EXCEPTION,ex);
            setAllowHeader(response);
            response.sendError(HttpResponse.__405_Method_Not_Allowed,
                               "Error:"+ex);
            return;
        }
    }

    /* ------------------------------------------------------------ */
    void handleOptions(HttpResponse response, String pathInContext)
        throws IOException
    {
        if ("*".equals(pathInContext))
            return;
        setAllowHeader(response);
        response.commit();
    }

    /* ------------------------------------------------------------ */
    void setAllowHeader(HttpResponse response)
    {
        response.setField(HttpFields.__Allow, getAllowedString());
    }

    /* ------------------------------------------------------------ */
    public void writeHeaders(HttpResponse response,Resource resource, long count)
        throws IOException
    {
        ResourceCache.ResourceMetaData metaData =
            (ResourceCache.ResourceMetaData)resource.getAssociate();

	String ctype = null;

	// XXX should we copy more of the properties here?
	if (resource instanceof CuUrlResource) {
	  CuUrlResource cur = (CuUrlResource)resource;
	  ctype = cur.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
	}
	if (ctype == null) {
	  ctype = metaData.getMimeType();
	}
        response.setContentType(ctype);
        if (count != -1)
        {
            if (count==resource.length())
                response.setField(HttpFields.__ContentLength,metaData.getLength());
            else
                response.setContentLength((int)count);
        }

        response.setField(HttpFields.__LastModified,metaData.getLastModified());

        if (_acceptRanges && response.getHttpRequest().getDotVersion()>0)
            response.setField(HttpFields.__AcceptRanges,"bytes");
    }

    /* ------------------------------------------------------------ */
    public void sendData(HttpRequest request,
                         HttpResponse response,
                         String pathInContext,
                         Resource resource,
                         boolean writeHeaders)
        throws IOException
    {
        long resLength=resource.length();

        //  see if there are any range headers
        Enumeration reqRanges =
            request.getDotVersion()>0
            ?request.getFieldValues(HttpFields.__Range)
            :null;

        if (!writeHeaders || reqRanges == null || !reqRanges.hasMoreElements())
        {
            // look for a gziped content.
            Resource data=resource;
            if (_minGzipLength>0)
            {
                String accept=request.getField(HttpFields.__AcceptEncoding);
                if (accept!=null && resLength>_minGzipLength &&
                    !pathInContext.endsWith(".gz"))
                {
                    Resource gz = getHttpContext().getResource(pathInContext+".gz");
                    if (gz.exists() && accept.indexOf("gzip")>=0)
                    {
                        if(log.isDebugEnabled())log.debug("gzip="+gz);
                        response.setField(HttpFields.__ContentEncoding,"gzip");
                        data=gz;
                        resLength=data.length();
                    }
                }
            }
            writeHeaders(response,resource,resLength);

            request.setHandled(true);

	    // (sethm) Begin content rewrite hack.
	    //
	    // Content Rewriting: If the PARAM_REWRITE_GIF_PNG
	    // config parameter is set, and this is GIF content, use
	    // JIMI to rewrite as PNG

	    InputStream in = data.getInputStream();
	    OutputStream out = null;
	    boolean enableRewrite =
              CurrentConfig.getCurrentConfig().getBoolean(ProxyManager.PARAM_REWRITE_GIF_PNG,
                                                          ProxyManager.DEFAULT_REWRITE_GIF_PNG);
	    if (!proxyMgr.isRepairRequest(request) &&
		enableRewrite &&
		"image/gif".equals(response.getContentType()) &&
		"from-cache".equals(response.getField("X-Lockss"))) {
	      try {
		JimiRasterImage img =
		  Jimi.getRasterImage(in, Jimi.SYNCHRONOUS);
		// Content length cannot be known before the data is
		// written.  Remove the Content-Length header.
		response.removeField("Content-Length");
		response.setContentType("image/png");
		out = response.getOutputStream();
		Jimi.putImage("image/png", img, out);
		out.flush();
	      } catch (JimiException ex) {
		throw new IOException(ex.getMessage());
	      }
	    } else {
	      out = response.getOutputStream();
	      IO.copy(in, out, resLength);
	    }

	    if (in != null) {
	      in.close();
	    }
	    if (out != null) {
	      out.close();
	    }

	    // End hack.
	    //
            // OutputStream out = response.getOutputStream();
            // data.writeTo(out,0,resLength);
	    //

            return;
        }

        // Parse the satisfiable ranges
        List ranges =InclusiveByteRange.satisfiableRanges(reqRanges,resLength);
        if(log.isDebugEnabled())log.debug("ranges: " + reqRanges + " == " + ranges);

        //  if there are no satisfiable ranges, send 416 response
        if (ranges==null || ranges.size()==0)
        {
            log.debug("no satisfiable ranges");
            writeHeaders(response, resource, resLength);
            response.setStatus(HttpResponse.__416_Requested_Range_Not_Satisfiable);
            response.setReason((String)HttpResponse.__statusMsg
                               .get(TypeUtil.newInteger(HttpResponse.__416_Requested_Range_Not_Satisfiable)));

            response.setField(HttpFields.__ContentRange,
                              InclusiveByteRange.to416HeaderRangeString(resLength));

            OutputStream out = response.getOutputStream();
            resource.writeTo(out,0,resLength);
            request.setHandled(true);
            return;
        }


        //  if there is only a single valid range (must be satisfiable
        //  since were here now), send that range with a 216 response
        if ( ranges.size()== 1)
        {
            InclusiveByteRange singleSatisfiableRange =
                (InclusiveByteRange)ranges.get(0);
            if(log.isDebugEnabled())log.debug("single satisfiable range: " + singleSatisfiableRange);
            long singleLength = singleSatisfiableRange.getSize(resLength);
            writeHeaders(response,resource,singleLength);
            response.setStatus(HttpResponse.__206_Partial_Content);
            response.setReason((String)HttpResponse.__statusMsg
                               .get(TypeUtil.newInteger(HttpResponse.__206_Partial_Content)));
            response.setField(HttpFields.__ContentRange,
                              singleSatisfiableRange.toHeaderRangeString(resLength));
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,
                             singleSatisfiableRange.getFirst(resLength),
                             singleLength);
            request.setHandled(true);
            return;
        }


        //  multiple non-overlapping valid ranges cause a multipart
        //  216 response which does not require an overall
        //  content-length header
        //
        ResourceCache.ResourceMetaData metaData =
            (ResourceCache.ResourceMetaData)resource.getAssociate();
        String encoding = metaData.getMimeType();
        MultiPartResponse multi = new MultiPartResponse(response);
        response.setStatus(HttpResponse.__206_Partial_Content);
        response.setReason((String)HttpResponse.__statusMsg
                           .get(TypeUtil.newInteger(HttpResponse.__206_Partial_Content)));

	// If the request has a "Request-Range" header then we need to
	// send an old style multipart/x-byteranges Content-Type. This
	// keeps Netscape and acrobat happy. This is what Apache does.
	String ctp;
	if (request.containsField(HttpFields.__RequestRange))
	    ctp = "multipart/x-byteranges; boundary=";
	else
	    ctp = "multipart/byteranges; boundary=";
	response.setContentType(ctp+multi.getBoundary());

        InputStream in=(resource instanceof CachedResource)
            ?null:resource.getInputStream();
        OutputStream out = response.getOutputStream();
        long pos=0;

        for (int i=0;i<ranges.size();i++)
        {
            InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
            String header=HttpFields.__ContentRange+": "+
                ibr.toHeaderRangeString(resLength);
            if(log.isDebugEnabled())log.debug("multi range: "+encoding+" "+header);
            multi.startPart(encoding,new String[]{header});

            long start=ibr.getFirst(resLength);
            long size=ibr.getSize(resLength);
            if (in!=null)
            {
                // Handle non cached resource
                if (start<pos)
                {
                    in.close();
                    in=resource.getInputStream();
                    pos=0;
                }
                while (pos<start)
                {
                    pos += in.skip(start-pos);
                }
                IO.copy(in,out,size);
                pos+=size;
            }
            else
                // Handle cached resource
                resource.writeTo(out,start,size);

        }
        if (in!=null)
            in.close();
        multi.close();

        request.setHandled(true);

        return;
    }


    /* ------------------------------------------------------------------- */
    void sendDirectory(HttpRequest request,
                       HttpResponse response,
                       Resource resource,
                       boolean parent)
        throws IOException
    {
        if (!_dirAllowed)
        {
            response.sendError(HttpResponse.__403_Forbidden);
            return;
        }

        request.setHandled(true);

        if(log.isDebugEnabled())log.debug("sendDirectory: "+resource);
        byte[] data=null;
        if (resource instanceof CachedResource)
            data=((CachedResource)resource).getCachedData();

        if (data==null)
        {
            String base = URI.addPaths(request.getPath(),"/");
            String dir = resource.getListHTML(base,parent);
            if (dir==null)
            {
                response.sendError(HttpResponse.__403_Forbidden,
                                   "No directory");
                return;
            }
            data=dir.getBytes("UTF8");
            if (resource instanceof CachedResource)
                ((CachedResource)resource).setCachedData(data);
        }

        response.setContentType("text/html; charset=UTF8");
        response.setContentLength(data.length);

        if (request.getMethod().equals(HttpRequest.__HEAD))
        {
            response.commit();
            return;
        }

        response.getOutputStream().write(data,0,data.length);
        response.commit();
    }
}

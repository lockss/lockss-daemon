/*
 * $Id: FilteredDirFileResource.java,v 1.4 2009-06-01 07:32:33 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

// This a mostly a copy of org.mortbay.util.FileResource.java.  It should
// be just a simple extension, but FileResource isn't extendable because
// its constructors aren't public or protected

// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.

package org.lockss.jetty;

import org.lockss.util.PlatformUtil;
import org.mortbay.util.*;
import java.io.*;
import java.net.*;
import java.net.URI;
import java.security.Permission;

import org.apache.commons.logging.Log;
import org.mortbay.log.LogFactory;


/* ------------------------------------------------------------ */
/** File Resource.
 *
 * Handle resources of implied or explicit file type.
 * This class can check for aliasing in the filesystem (eg case
 * insensitivity).  By default this is turned on if the platform does
 * not have the "/" path separator, or it can be controlled with the
 * "org.mortbay.util.FileResource.checkAliases" system parameter.
 *
 * If alias checking is turned on, then aliased resources are
 * treated as if they do not exist, nor can they be created.
 *
 * @version $Revision: 1.4 $
 * @author Greg Wilkins (gregw)
 */
public class FilteredDirFileResource extends URLResource
{
    private static Log log = LogFactory.getLog(FilteredDirFileResource.class);
    private static boolean __checkAliases;
    static
    {
        __checkAliases=
            "true".equalsIgnoreCase
            (System.getProperty("org.mortbay.util.FileResource.checkAliases","true"));
 
       if (__checkAliases)
            log.info("Checking Resource aliases");
    }
    
    /* ------------------------------------------------------------ */
    private File _file;
    private FilenameFilter _filter;
    private transient URL _alias=null;
    private transient boolean _aliasChecked=false;

    /* ------------------------------------------------------------------------------- */
    /** setCheckAliases.
     * @param checkAliases True of resource aliases are to be checked for (eg case insensitivity or 8.3 short names) and treated as not found.
     */
    public static void setCheckAliases(boolean checkAliases)
    {
        __checkAliases=checkAliases;
    }

    /* ------------------------------------------------------------------------------- */
    /** getCheckAliases.
     * @return True of resource aliases are to be checked for (eg case insensitivity or 8.3 short names) and treated as not found.
     */
    public static boolean getCheckAliases()
    {
        return __checkAliases;
    }
    
    /* -------------------------------------------------------- */
    public FilteredDirFileResource(URL url, FilenameFilter filter)
        throws IOException, URISyntaxException
    {
        super(url,null);
	_filter = filter;
	
        try
        {
            // Try standard API to convert URL to file.
            _file = new File(new URI(url.toString()));
        }
        catch (Exception e)
        {
            LogSupport.ignore(log,e);
            try
            {
                // Assume that File.toURL produced unencoded chars. So try
                // encoding them.
                String urls=
                    "file:"+org.mortbay.util.URI.encodePath(url.toString().substring(5));
                _file =new File(new URI(urls));
            }
            catch (Exception e2)
            {
                LogSupport.ignore(log,e2);

                // Still can't get the file.  Doh! try good old hack!
                checkConnection();
                Permission perm = _connection.getPermission();
                _file = new File(perm==null?url.getFile():perm.getName());
            }
        }
    }
    
    /* -------------------------------------------------------- */
    FilteredDirFileResource(URL url, URLConnection connection, File file,
			    FilenameFilter filter)
    {
        super(url,connection);
        _file=file;
	_filter = filter;
    }
    
    /* -------------------------------------------------------- */
    public Resource addPath(String path)
        throws IOException,MalformedURLException
    {
        FilteredDirFileResource r=null;

        if (!isDirectory())
        {
            r=(FilteredDirFileResource)super.addPath(path);
        }
        else
        {
            path = org.mortbay.util.URI.canonicalPath(path);
            
            // treat all paths being added as relative
            if (path.startsWith("/"))
                path = path.substring(1);
            
            File newFile = new File(_file,path);
            
            r=new FilteredDirFileResource(newFile.toURI().toURL(),null,newFile,
					  _filter);
        }
                                  
        return r;
    }
   
    
    /* ------------------------------------------------------------ */
    public URL getAlias()
    {
        if (__checkAliases && !_aliasChecked)
        {
            try
            {    
                String abs=_file.getAbsolutePath();
                String can=_file.getCanonicalPath();
                
                if (abs.length()!=can.length() || !abs.equals(can))
                    _alias=new File(can).toURI().toURL();
                
                _aliasChecked=true;
                
                if (_alias!=null && log.isDebugEnabled())
                {
                    log.debug("ALIAS abs="+abs);
                    log.debug("ALIAS can="+can);
                }
            }
            catch(Exception e)
            {
                log.warn(LogSupport.EXCEPTION,e);
                return getURL();
            }                
        }
        return _alias;
    }
    
    /* -------------------------------------------------------- */
    /**
     * Returns true if the resource exists.
     */
    public boolean exists()
    {
        return _file.exists();
    }
        
    /* -------------------------------------------------------- */
    /**
     * Returns the last modified time
     */
    public long lastModified()
    {
        return _file.lastModified();
    }

    /* -------------------------------------------------------- */
    /**
     * Returns true if the respresenetd resource is a container/directory.
     */
    public boolean isDirectory()
    {
        return _file.isDirectory();
    }

    /* --------------------------------------------------------- */
    /**
     * Return the length of the resource
     */
    public long length()
    {
        return _file.length();
    }
        

    /* --------------------------------------------------------- */
    /**
     * Returns the name of the resource
     */
    public String getName()
    {
        return _file.getAbsolutePath();
    }
        
    /* ------------------------------------------------------------ */
    /**
     * Returns an File representing the given resource or NULL if this
     * is not possible.
     */
    public File getFile()
    {
        return _file;
    }
        
    /* --------------------------------------------------------- */
    /**
     * Returns an input stream to the resource
     */
    public InputStream getInputStream() throws IOException
    {
        return new FileInputStream(_file);
    }
        
    /* --------------------------------------------------------- */
    /**
     * Returns an output stream to the resource
     */
    public OutputStream getOutputStream()
        throws java.io.IOException, SecurityException
    {
        return new FileOutputStream(_file);
    }
        
    /* --------------------------------------------------------- */
    /**
     * Deletes the given resource
     */
    public boolean delete()
        throws SecurityException
    {
        return _file.delete();
    }

    /* --------------------------------------------------------- */
    /**
     * Rename the given resource
     */
    public boolean renameTo( Resource dest)
        throws SecurityException
    {
        if( dest instanceof FilteredDirFileResource)
            return PlatformUtil.updateAtomically(_file,
                                                 ((FilteredDirFileResource)dest)._file);
        else
            return false;
    }

    /* --------------------------------------------------------- */
    /**
     * Returns a list of resources contained in the given resource
     */
    public String[] list()
    {
        String[] list =_file.list(_filter);
        if (list==null)
            return null;
        for (int i=list.length;i-->0;)
        {
            if (new File(_file,list[i]).isDirectory() &&
                !list[i].endsWith("/"))
                list[i]+="/";
        }
        return list;
    }
         
    /* ------------------------------------------------------------ */
    /** Encode according to this resource type.
     * File URIs are encoded.
     * @param uri URI to encode.
     * @return The uri unchanged.
     */
    public String encode(String uri)
    {
        return uri;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param o Object to test for equality
     * @return true if the object is equal to this
     */
    public boolean equals( Object o)
    {
        if (this == o)
            return true;

        if (null == o || ! (o instanceof FilteredDirFileResource))
            return false;

        FilteredDirFileResource f=(FilteredDirFileResource)o;
        return f._file == _file || (null != _file && _file.equals(f._file));
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the hashcode.
     */
    public int hashCode()
    {
       return null == _file ? super.hashCode() : _file.hashCode();
    }
}

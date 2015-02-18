/*
 * $Id$
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

package org.lockss.filter;

import java.io.*;
import java.util.List;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Convenience methods for filters and filter factories */
public class FilterUtil {
  static Logger log = Logger.getLogger("FilterUtil");

  // no instances
  private FilterUtil() {
  }

  /** Return a Reader that reads from the InputStream.  If the specified
   * encoding is not found, tries {@link Constants#DEFAULT_ENCODING}.  If
   * the supplied InputStream is a ReaderInputStream, returns the
   * underlying Reader.
   * @param in the InputStream to be wrapped
   * @param encoding the charset
   * deprecated plugins should call StreamUtil.getReader() directly after
   * 1.22 released
   */
  public static Reader getReader(InputStream in, String encoding) {
    return StreamUtil.getReader(in, encoding);
  }

  /** Wrap a stream in a crawl filter, if one exists for this mime type,
   * else return the orginal stream.
   * @param au the AU
   * @param in InputStream to filter
   * @param encoding charset whith which to interpret byte stream
   * @param mimeType look for a filter of this MIME type
   * @return a filtered stream, or the original stream if AU has no filter
   * for this MIME type
   */
  public static InputStream getCrawlFilteredStream(ArchivalUnit au,
						   InputStream in,
						   String encoding,
						   String mimeType) {
    FilterFactory fact = au.getCrawlFilterFactory(mimeType);
    if (fact == null) {
      return in;
    }
    if (log.isDebug3()) {
      log.debug3("Filtering " + mimeType +
		 " with " + fact.getClass().getName());
    }
    try {
      return fact.createFilteredInputStream(au, in, encoding);
    } catch (PluginException e) {
      IOUtil.safeClose(in);
      throw new RuntimeException(e);
    }
  }    
}

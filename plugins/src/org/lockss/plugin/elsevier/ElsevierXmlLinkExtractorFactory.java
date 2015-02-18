/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.elsevier;
import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

import org.lockss.extractor.*;

/**
 * Creates instances of a LinkExtractor that parses Elsevier
 * <code>datasetinfo.xml</code> files to extract "links"
 * to the .tar and .toc files to be collected.
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public class ElsevierXmlLinkExtractorFactory
  implements LinkExtractorFactory {

  private static Logger logger =
    Logger.getLogger("ElsevierXmlLinkExtractorFactory");

  public ElsevierXmlLinkExtractorFactory() {
  }

  public LinkExtractor createLinkExtractor(String mimeType) {
    return new ElsevierXmlLinkExtractor();
  }

  private class ElsevierXmlLinkExtractor implements LinkExtractor {
    private static final String START_TAG = "file name=\"";
    private static final String END_TAG = "\"";
    ElsevierXmlLinkExtractor() {
    }

    /**
     * This is a very primitive implementation. The datasetinfo.xml
     * file could be parsed using a real XML parser.  It contains
     * size and MD5 values for each file,  which should be checked.
     */
    public void extractUrls(ArchivalUnit au, InputStream in, String encoding,
			    String srcUrl, LinkExtractor.Callback cb)
	throws IOException {

      if (in == null) {
	throw new IllegalArgumentException("Called with null InputStream");
      }
      if (cb == null) {
	throw new IllegalArgumentException("Called with null callback");
      }
      int ix = srcUrl.lastIndexOf("/");
      if (ix <= 0) {
	throw new IllegalArgumentException("Malformed URL: " + srcUrl);
      }
      String stem = srcUrl.substring(0, ix + 1);
      BufferedReader bReader =
	new BufferedReader(StreamUtil.getReader(in, encoding));
      for (String line = bReader.readLine();
	   line != null;
	   line = bReader.readLine()) {
	line = line.trim();
	logger.debug3("Line: " + line);
	// Look for file name="123456.tar"
	int startOfName = line.indexOf(START_TAG) + START_TAG.length();
	if (startOfName > START_TAG.length()) {
	  int endOfName = line.indexOf(END_TAG, startOfName) +
	    END_TAG.length() - 1;
	  logger.debug3("Found [" + startOfName + "," + endOfName + "]");
	  if (endOfName - startOfName > 0) {
	    String fileName = line.substring(startOfName, endOfName);
	    logger.debug3("Found: " + fileName);
	    cb.foundLink(stem + fileName);
	  }
	}
      }
      IOUtil.safeClose(bReader);
    }
  }
}


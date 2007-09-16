/*
 * $Id: ElsevierXmlParser.java,v 1.1.2.1 2007-09-16 20:47:20 dshr Exp $
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

package org.lockss.plugin.elsevier;
import java.io.*;

import org.lockss.crawler.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class ElsevierXmlParser implements ContentParser {
  private static Logger logger =
    Logger.getLogger("ElsevierXmlParser");

  private static final String START_TAG = "file name=\"";
  private static final String END_TAG = "\"";

  public ElsevierXmlParser() {
  }

  public void parseForUrls(Reader reader, String srcUrl,
			   ArchivalUnit au, ContentParser.FoundUrlCallback cb)
      throws IOException {
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    BufferedReader bReader = new BufferedReader(reader);
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
	  cb.foundUrl(srcUrl + fileName);
	}
      }
    }
  }
}

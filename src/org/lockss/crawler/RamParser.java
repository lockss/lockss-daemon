/*
 * $Id: RamParser.java,v 1.3 2004-03-06 00:38:34 troberts Exp $
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

package org.lockss.crawler;
import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public class RamParser implements ContentParser {
  BufferedReader reader = null;
  String source = null;
  String dest = null;


  public RamParser() {
  }

  /**
   * Creates a RamParser which will find rtsp:// urls starting with
   * source and replace that string with dest
   */
  public RamParser(String source, String dest) {
    this.source = source;
    this.dest = dest;
  }

  public void parseForUrls(CachedUrl cu, FoundUrlCallback cb)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("Called with null cu");
    } else if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
//     reader = new BufferedReader(cu.getReader());
    reader = new BufferedReader(cu.openForReading());
    for (String line = reader.readLine();
	 line != null;
	 line = reader.readLine()) {
      line = line.trim().toLowerCase();
      if (line.startsWith("http://")) {
	cb.foundUrl(line);
      } else if (source != null && dest != null && line.startsWith(source)) {
	cb.foundUrl(StringUtil.replaceFirst(line, source, dest));
      }
    }
  }
}

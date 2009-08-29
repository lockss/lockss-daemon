/*
 * $Id: SimpleXmlMetadataExtractor.java,v 1.1 2009-08-29 04:26:27 dshr Exp $
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

package org.lockss.extractor;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public class SimpleXmlMetadataExtractor implements MetadataExtractor {
  static Logger log = Logger.getLogger("SimpleXmlMetadataExtractor");
  private Map tagMap;

  /**
   * @param tagMap a map from XML tags to the property name in the
   * extracted Metadata object
   */
  public SimpleXmlMetadataExtractor(Map tagMap) {
    this.tagMap = tagMap;
  }

  /*
   * XXX this should really do an XML parse and get all the metadata
   */
  public Metadata extract(CachedUrl cu)
      throws IOException {

    if (cu == null) {
      throw new IllegalArgumentException("extract(null)");
    }
    Metadata ret = new Metadata();
    BufferedReader bReader =
	new BufferedReader(cu.openForReading());
    for (String line = bReader.readLine();
	 line != null;
	 line = bReader.readLine()) {
      line = line.trim();
      log.debug2("Line: " + line);
      for (Iterator it = tagMap.keySet().iterator(); it.hasNext(); ) {
	String tag = (String)it.next();
	scanForTag(line, tag, ret);
      }
    }
    IOUtil.safeClose(bReader);
    return ret;
  }

  private void scanForTag(String line, String tag, Metadata ret) {
    int i = 0;
    String begin = "<" + tag + ">";
    String end = "</" + tag + ">";
    log.debug2("Scan: " + tag);
    while ((i = StringUtil.indexOfIgnoreCase(line, begin, i)) >= 0) {
      i += begin.length();
      int j = StringUtil.indexOfIgnoreCase(line, end, i);
      if (j > i) {
	String value = line.substring(i, j);
	log.debug2(tag + " = " + value);
	ret.setProperty((String)tagMap.get(tag), value);
      }
    }
  }
}

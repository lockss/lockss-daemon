/*
 * $Id: SimpleXmlMetadataExtractor.java,v 1.6 2011-01-22 08:22:30 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

public class SimpleXmlMetadataExtractor extends SimpleFileMetadataExtractor {
  static Logger log = Logger.getLogger("SimpleXmlMetadataExtractor");
  private Collection<String> tags;

  /**
   * Create an extractor what will extract the value(s) of the xml tags in
   * <code>tags</code>
   * @param tags the list of XML tags whose value to extract
   */
  public SimpleXmlMetadataExtractor(Collection<String> tags) {
    this.tags = tags;
  }

  /**
   * Create an extractor that will extract the value(s) of the xml tags in
   * <code>tagMap.keySet()</code>
   * @param tagMap a map from XML tags to cooked keys.  (Only the set of
   * tags is used by this object.)
   */
  public SimpleXmlMetadataExtractor(Map tagMap) {
    this.tags = tagMap.keySet();
  }

  /*
   * XXX this should really do an XML parse and get all the metadata
   */
  public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("extract() called with null CachedUrl");
    }
    ArticleMetadata ret = new ArticleMetadata();
    BufferedReader bReader =
	new BufferedReader(cu.openForReading());
    try {
      for (String line = bReader.readLine();
	   line != null;
	   line = bReader.readLine()) {
	line = line.trim();
	if (log.isDebug3()) {
	  log.debug3("Line: " + line);
	}
	for (String tag : tags) {
	  scanForTag(line, tag, ret);
	}
      }
    } finally {
      IOUtil.safeClose(bReader);
    }
    return ret;
  }

  private void scanForTag(String line, String tag, ArticleMetadata ret) {
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
	ret.putRaw(tag, value);
      }
    }
  }
}

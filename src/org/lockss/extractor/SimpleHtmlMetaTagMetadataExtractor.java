/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.plugin.*;

public class SimpleHtmlMetaTagMetadataExtractor
  extends SimpleFileMetadataExtractor {

  private static final Logger log = Logger.getLogger(SimpleHtmlMetaTagMetadataExtractor.class);
  
  private Pattern whiteSpacePat = Pattern.compile("\\s+");

  public SimpleHtmlMetaTagMetadataExtractor() {
  }

  public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("extract() called with null CachedUrl");
    }
    ArticleMetadata ret = new ArticleMetadata();
    BufferedReader bReader = null;
    try {
      bReader = new BufferedReader(cu.openForReading());
      for (String line = bReader.readLine();
	   line != null;
	   line = bReader.readLine()) {
	int i = StringUtil.indexOfIgnoreCase(line, "<meta ");
	while (i >= 0) {
	  // recognize end of tag character preceded by optional '/', 
	  // preceded by a double-quote that is separated by zero or more 
	  // whitespace characters
	  int j = i+1;
	  while (true) {
	    j = StringUtil.indexOfIgnoreCase(line, ">", j);
	    if (j < 0) break;
	    String s = line.substring(i,j);
	    if (s.endsWith("/")) {
	      s = s.substring(0,s.length()-1);
	    }
	    if (s.trim().endsWith("\"")) {
	      break;
	    }
	    j++;
	  }
	  if (j < 0) {
	    // join next line with tag end
	    String nextLine = bReader.readLine();
	    if (nextLine == null) {
	      break;
	    }
	    if (line.endsWith("=") && nextLine.startsWith(" ")) {
	      // here we trim leading spaces from nextLine
	      Matcher m = whiteSpacePat.matcher(nextLine);
	      nextLine = m.replaceFirst("");
	    }
	    line += nextLine;
	    continue;
	  }
	  String meta = line.substring(i, j+1);
	  if (log.isDebug3()) log.debug3("meta: " + meta);
	  addTag(meta, ret);
	  i = StringUtil.indexOfIgnoreCase(line, "<meta ", j+1);
	}
      }
    } finally {
      IOUtil.safeClose(bReader);
    }
    return ret;
  }
  private void addTag(String line, ArticleMetadata ret) {
    String nameFlag = "name=\"";
    int nameBegin = StringUtil.indexOfIgnoreCase(line, nameFlag);
    if (nameBegin <= 0) {
      if (log.isDebug3()) log.debug3(line + " : no " + nameFlag);
      return;
    }
    nameBegin += nameFlag.length();
    int nameEnd = line.indexOf('"', nameBegin + 1);
    if (nameEnd <= nameBegin) {
      log.debug2(line + " : " + nameFlag + " unterminated");
      return;
    }
    String name = line.substring(nameBegin, nameEnd);
    String contentFlag = "content=\"";
    int contentBegin = StringUtil.indexOfIgnoreCase(line, contentFlag);
    if (contentBegin <= 0) {
      if (log.isDebug3()) log.debug3(line + " : no " + contentFlag);
      return;
    }
    if (nameBegin <= contentBegin && nameEnd >= contentBegin) {
      log.debug2(line + " : " + contentFlag + " overlaps " + nameFlag);
      return;
    }
    contentBegin += contentFlag.length();
    int contentEnd = line.indexOf('"', contentBegin + 1);
    if (log.isDebug3()) {
      log.debug3(line + " name [" + nameBegin + "," + nameEnd + "] cont [" +
		 contentBegin + "," + contentEnd + "]");
    }
    if (contentEnd <= contentBegin) {
      log.debug2(line + " : " + contentFlag + " unterminated");
      return;
    }
    if (contentBegin <= (nameBegin - nameFlag.length())
	&& contentEnd >= (nameBegin - nameFlag.length())) {
      log.debug2(line + " : " + nameFlag + " overlaps " + contentFlag);
      return;
    }
      
    String content = line.substring(contentBegin, contentEnd);
    putValue(ret, name, content);
  }
  
  protected void putValue(ArticleMetadata ret, String name, String content) {
    // filter raw HTML tags embedded within content -- publishers get sloppy
    content = HtmlUtil.stripHtmlTags(content);
    // remove character entities from content
    content = StringEscapeUtils.unescapeHtml4(content);
    // normalize multiple whitespace characters to a single space character
    Matcher m = whiteSpacePat.matcher(content);
    content = m.replaceAll(" ");
    
    if (log.isDebug3()) log.debug3("Add: " + name + " = " + content);
    ret.putRaw(name, content);
  }
}

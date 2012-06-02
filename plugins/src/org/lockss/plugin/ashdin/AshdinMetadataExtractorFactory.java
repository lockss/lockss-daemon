/*
 * $Id: AshdinMetadataExtractorFactory.java,v 1.1 2012-06-02 00:22:20 akanshab01 Exp $
 */

/*

 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ashdin;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.FileMetadataExtractor.Emitter;
import org.lockss.plugin.*;

public class AshdinMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AshdinMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new AshdinHtmlMetadataExtractor();
  }

  public static class AshdinHtmlMetadataExtractor implements
      FileMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
     
    }

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(
          target, cu);
      am.cook(tagMap);

      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine();

        line != null; line = bReader.readLine()) {
          line = line.trim();
          log.debug3("Line: " + line);
          if (StringUtil.startsWithIgnoreCase(line,
              "<pre xmlns =\"http://www.w3.org/1999/xhtml\">")) {
            log.debug3("Line: " + line);
            addDoi(line, am);

          } else if (StringUtil.startsWithIgnoreCase(line, "<h3 xmlns")) {

            log.debug3("Line: " + line);
            addAuthors(line, am);

          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, am);
    }

    protected void addAuthors(String line, ArticleMetadata ret) {
      String authorBFlag = "absauthor";
      String authorEFlag = "</h3>";
      String auth = "";
      String compAuthStr = "";
      int authorEnd = StringUtil.indexOfIgnoreCase(line, authorEFlag);
      int authorBegin = StringUtil.indexOfIgnoreCase(line, authorBFlag);
      authorBegin += authorBFlag.length();
      authorEnd += authorEFlag.length();
      auth = line.substring(authorBegin + 1, authorEnd);
     if (auth.matches("<sup>")|| auth.matches("</sup>")){
        log.warning("inside the loop");
         auth.replaceAll("<sup>","");
         auth.replaceAll("</sup>", "");
      }
      String[] authArr = auth.split(",");
      for (int i = 0; i < authArr.length; i++) {
        compAuthStr = compAuthStr + authArr[i];
      }
      ret.put(MetadataField.FIELD_AUTHOR, compAuthStr);
    }

    protected void addDoi(String line, ArticleMetadata ret) {
      String doiFlag = "doi: ";
      String artFlag = "ID";
      int doiBegin = StringUtil.indexOfIgnoreCase(line, doiFlag);
      int artBegin = StringUtil.indexOfIgnoreCase(line, artFlag);
      if (doiBegin <= 0) {
        log.debug3(line + " : no " + doiFlag);
        return;
      }
      doiBegin += doiFlag.length();
      artBegin += artFlag.length();
      String art = line.substring(artBegin, artBegin + 7);
      String doi = line.substring(doiBegin, doiBegin + 20);
      if (doi.length() < 20) {
        log.debug3(line + " : too short");
        return;
      }
      ret.put(MetadataField.FIELD_DOI, doi);
      ret.put(MetadataField.FIELD_ARTICLE_TITLE, art);
    }
  }
}

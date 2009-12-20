/*
 * $Id: BioOneMetadataExtractorFactory.java,v 1.1 2009-12-20 00:04:59 dshr Exp $
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

package org.lockss.plugin.bioone;

import java.io.*;
import java.net.URL;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.apache.commons.lang.StringEscapeUtils;

import java.net.MalformedURLException;
import java.net.URLDecoder;


public class BioOneMetadataExtractorFactory implements MetadataExtractorFactory {
  static Logger log = Logger.getLogger("BioOneMetadataExtractorFactory");

  /**
   * Create a MetadataExtractor
   *
   * @param contentType the content type type from which to extract URLs
   */
  public MetadataExtractor createMetadataExtractor(String contentType) throws PluginException {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if ("text/html".equalsIgnoreCase(mimeType)) {
      return new BioOneMetadataExtractor();
    }
    return null;
  }

  public class BioOneMetadataExtractor extends SimpleMetaTagMetadataExtractor {

    public BioOneMetadataExtractor() {
    }

    public Metadata extract(CachedUrl cu) throws IOException {
      if (cu == null) {
        throw new IllegalArgumentException("extract(null)");
      }

      Metadata ret = super.extract(cu);

      // extract DOI from URL
      addDOI(cu.getUrl(), ret);

      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
	for (String line = bReader.readLine(); line != null;
	     line = bReader.readLine()) {
	  line = line.trim();
	  boolean hasOnlineISSN = false;

	  // if online issn is available then store that as the issn
	  if (StringUtil.startsWithIgnoreCase(line, "<p><strong>Online ISSN:")) {
	    log.debug2("Line: " + line);
	    addISSN(line, ret);
	    hasOnlineISSN = true;
	  }

	  if (!hasOnlineISSN && StringUtil.startsWithIgnoreCase(line, "<p><strong>Print ISSN:")) {
	    log.debug2("Line: " + line);
	    addISSN(line, ret);
	  }

	  if (StringUtil.startsWithIgnoreCase(line, "<p><strong>Current: </strong>")) {
	    addVolume(line, ret);
	    addIssue(line, ret);
	  }

	  if (StringUtil.startsWithIgnoreCase(line, "pg(s)")) {
	    addFirstPage(line, ret);
	  }
	}
      } finally {
	IOUtil.safeClose(bReader);
      }
      return ret;
    }

    protected void addISSN(String line, Metadata ret) {
      String issnFlag = " </strong>";
      int issnBegin = StringUtil.indexOfIgnoreCase(line, issnFlag);
      if (issnBegin <= 0) {
        log.debug(line + " : no ISSN ");
        return;
      }
      issnBegin += issnFlag.length();
      String issn = line.substring(issnBegin, issnBegin + 9);
      if (issn.length() < 9) {
        log.debug(line + " : too short");
        return;
      }
      ret.putISSN(issn);
    }

    protected void addVolume(String line, Metadata ret) {
      String volumeFlag = "volume ";

      int start = StringUtil.indexOfIgnoreCase(line, volumeFlag);
      int end = StringUtil.indexOfIgnoreCase(line, "issue");

      if (start <= 0) {
        log.debug(line + " : no " + volumeFlag);
        return;
      }

      start += volumeFlag.length();
      String volume = line.substring(start, end).trim();

      ret.putVolume(volume);
    }

    protected void addIssue(String line, Metadata ret) {
      String issueFlag = "issue ";

      int start = StringUtil.indexOfIgnoreCase(line, issueFlag);
      int end = StringUtil.indexOfIgnoreCase(line, "</p>");

      if (start <= 0) {
        log.debug(line + " : no " + issueFlag);
        return;
      }

      start += issueFlag.length();
      String issue = line.substring(start, end).trim();

      ret.putIssue(issue);
    }

    protected void addFirstPage(String line, Metadata ret) {
      String fPageFlag = "g(s) ";
      int fPageBegin = StringUtil.indexOfIgnoreCase(line, fPageFlag);
      int fPageEnd = StringUtil.indexOfIgnoreCase(line, "-");

      if (fPageBegin <= 0) {
        log.debug(line + " : no " + fPageFlag);
        return;
      }

      fPageBegin += fPageFlag.length();
      String fPage = line.substring(fPageBegin, fPageEnd).trim();

      ret.putStartPage(fPage);
    }

    protected void addDOI(String url, Metadata ret) {
      // The URL is of the following form:
      // http://www.bioone.org/doi/abs/10.3377/004.044.0201      
      try {
        URL bioUrl = new URL(url);
        String path = bioUrl.getPath();
	log.debug2("path: " + path);
        String parameters[] = path.split("/");
	if (parameters.length > 4) {
	  // unescape URL codes that might appear in DOI as it's extracted
	  // from the URL
	  String doi = URLDecoder.decode(parameters[3] + "/" + parameters[4]);
	  ret.putDOI(doi);
	} else {
	  log.debug("too few path components: " + path);
	}
      } catch (MalformedURLException e) {
        log.debug(url + " : Malformed URL");
      }
    }

  }
}

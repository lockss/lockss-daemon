/*
 * $Id$
 */

/*

 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.codeocean;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */

public class CodeOceanUrlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(CodeOceanUrlMetadataExtractorFactory.class);
  private static final String CODE_OCEAN = "Code Ocean";
  /// http://foo.com/sourcefiles/code-ocean/2018/test-publisher/333b30ee-8262-4210-9ec7-f6462048f893.zip
  private static final Pattern  ZIP_PAT = Pattern.compile("/code-ocean-released/([^/]+)/([^/]+)/([^/]+)\\.zip", Pattern.CASE_INSENSITIVE);  


  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new CodeOceanUrlMetadataExtractor();
  }

  public static class CodeOceanUrlMetadataExtractor 
    implements FileMetadataExtractor {

	@Override
	public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

		// base_url/2018/pubfoo/111blah222.zip
		// EITHER - we'll have a plugin with base_url and directory of "2018/pubfoo" per publisher
		// OR - we'll collect all 2018 stuff and then map each "pubfoo" to a specific publisher
		// not sure which yet - this is just a prototype/placeholder while we experiment
		if(cu != null) {
			String zip_url = cu.getUrl();
			Matcher urlMat = ZIP_PAT.matcher(zip_url);
			String year = null;
			String publisher = null;
			String file_id = null;
			if (urlMat.find()) {
				year = urlMat.group(1);
				publisher = urlMat.group(2);
				file_id = urlMat.group(3);
			}

			ArticleMetadata UrlMd = new ArticleMetadata();
			UrlMd.put(MetadataField.FIELD_ACCESS_URL, zip_url);
			UrlMd.put(MetadataField.FIELD_PROVIDER, CODE_OCEAN);
			// for now use the publisher from the path - might need to get a clean version 
			UrlMd.put(MetadataField.FIELD_PUBLISHER, publisher);
			// these two are new to support the archiving of arbitrary blobs of content
			UrlMd.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_FILE);
			// This isn't explicitly necessary - it would be created based on the ARTICLE_TYPE_FILE and
			// it is used only to attach the publisher to the "article"
			UrlMd.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_FILE);
			// tempting though it is FIELD_PROPRIETARY_IDENTIFIER is publication level only
			Map<String, String> ID_MAP = new HashMap<String,String>();
			ID_MAP.put("FileIdentifier", file_id);
			UrlMd.putRaw(MetadataField.FIELD_MD_MAP.getKey(), ID_MAP);
			// for now use the ingest year - might need to pull a real year from the metadata?
			if (year != null) UrlMd.put(MetadataField.FIELD_DATE, year);
			emitter.emitMetadata(cu, UrlMd);	
		}
	}
  }    

}

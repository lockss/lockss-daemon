/*
 * $Id$
 */

/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.crossref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.BaseFileArticleMetadataExtractor;

/*
 * Customize the ArticleMetadata class for generic file objects for Crossref
 * CrossRef doesn't extract from any file, it just uses the information found in the url
 * to fill in values.
 */

public class CrossrefArticleMetadataExtractor extends BaseFileArticleMetadataExtractor{
	
	private static final String CROSSREF_FILE_TYPE = "archive";
	private static final String DEFAULT_CROSSREF_PUB = "Crossref";
	private static final Pattern  TGZ_PAT = 
			Pattern.compile("/crossref-released/([^/]+)/([^/]+)\\.tar\\.gz", Pattern.CASE_INSENSITIVE);
	private static final int YEAR_GROUP = 1;
	private static final int FNAME_GROUP = 2;
	


	public CrossrefArticleMetadataExtractor(String role) {
		super(role);
	}

	//am has nothing in it because we don't extract information from any file
	@Override
	protected String getContentYear(CachedUrl cu, ArticleMetadata am) {
		// Get limited information from the TDB file
		String defYr = super.getContentYear(cu,am);
		if (defYr == null) {
			Matcher umat = TGZ_PAT.matcher(cu.getUrl());
			if (umat.find()) {
				return umat.group(YEAR_GROUP);
			}
		}
		return defYr;
	}

	/* remove .tar.gz and use just the remaining filename
	 */
	@Override
	protected String getFileIdentifier(CachedUrl cu) {
		// we know cu isn't null
		Matcher umat = TGZ_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			return umat.group(FNAME_GROUP);
		}
		// if that failed just go with the default - basefilename
		return super.getFileIdentifier(cu);
	}	
	
	@Override
	protected String getDefaultPublisherName() {
		return DEFAULT_CROSSREF_PUB;
	}

	@Override
	protected String getFileObjectType(CachedUrl cu) {
		return CROSSREF_FILE_TYPE;
	}	

}
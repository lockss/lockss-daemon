/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
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

package org.lockss.plugin.clockss.codeocean;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.TdbAu;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.BaseFileArticleMetadataExtractor;

/*
 * Customize the ArticleMetadata class for generic file objects for Code Ocean
 * code modules
 * 
 * The files inside a specific 
 * code-ocean-released/2019/nature/LONGUUID/v1.0/
 *     capsule.zip  - code capsule, data, etc - including metadata
 *     results.zip - 
 *     image.tar.xz - compressed docker image
 *     extract.sh - extraction script should anything be needed
 */

public class CodeOceanArticleMetadataExtractor extends BaseFileArticleMetadataExtractor{
	
	/*
	 * code ocean delivers capsules under publisher subdirectories - map the publisher path to a specifc publisher
	 */
	static private final Map<String, String> PublisherIDMap =
			new HashMap<String,String>();
	static {
		PublisherIDMap.put("nature", "Springer");
		PublisherIDMap.put("bmc", "Springer");
		PublisherIDMap.put("taylorandfrancis", "Taylor & Francis");
		PublisherIDMap.put("cambridge", "Cambridge University Press");
	}

	private static final String CODE_OCEAN_FILE_TYPE = "code capsule";
	private static final String CC_ARTICLE_DOI = "CodeCapsule_article_doi";
	private static final String CODE_OCEAN = "Code Ocean";
	private static final Pattern  ZIP_PAT = Pattern.compile("/code-ocean-released/([^/]+)/([^/]+)/([^/]+)/[vV]([^/]+)/capsule\\.zip", Pattern.CASE_INSENSITIVE);  
	private static final int YEAR_GROUP = 1;
	private static final int PUB_GROUP = 2;
	private static final int UUID_GROUP = 3;
	private static final int VERSION_GROUP = 4;
	
	public CodeOceanArticleMetadataExtractor(String role) {
		super(role);
	}

	@Override
	protected String getContentYear(CachedUrl cu, TdbAu tdbau) {
		// Get limited information from the TDB file
		String defYr = super.getContentYear(cu,tdbau);
		if (defYr == null) {
			Matcher umat = ZIP_PAT.matcher(cu.getUrl());
			if (umat.find()) {
				return umat.group(YEAR_GROUP);
			}
		}
		return defYr;
	}

	/*
	 * Code Ocean is actually the provider, serving code modules
	 * for multiple publishers. 
	 * If for some reason not set, use the publisher name in the url
	 */
	@Override
	protected String getContentPublisher(CachedUrl cu, TdbAu tdbau) {
		String defPub = super.getContentPublisher(cu, tdbau);
		if (defPub == null) {
			Matcher umat = ZIP_PAT.matcher(cu.getUrl());
			if (umat.find()) {
				String pubkey = umat.group(PUB_GROUP);
				if (PublisherIDMap.containsKey(pubkey)) {
						return PublisherIDMap.get(pubkey);
				}
				return pubkey;
			}
		}
		return defPub;
	}
	
	@Override
	protected String getFileIdentifier(CachedUrl cu) {
		String defId = super.getFileIdentifier(cu);
		Matcher umat = ZIP_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			return umat.group(UUID_GROUP);
			}
	    return defId; // might not be unique but we shouldn't get here.
	}


	@Override
	protected String getContentProvider(CachedUrl cu, TdbAu tdbau, String publisher) {
		return (tdbau != null) ? tdbau.getProviderName() : CODE_OCEAN;
	}

	/*
	 * Code Ocean is actually the provider and provides
	 * code modules for multliple publishers
	 */
	

	@Override
	protected String getFileObjectType(CachedUrl cu) {
		return CODE_OCEAN_FILE_TYPE;
	}
	
	
	/*
	 * In place in case we get the iterator to see metadata.yml instead of the zip
	 * If the capsule.zip is the metadata cu then just use defaults
	 */
	@Override
	protected String getFileSize(CachedUrl cu) {
		String defSize = super.getFileSize(cu);
		if (cu.getUrl().endsWith(".yml")) {
  		  String zip_url = StringUtils.substringBefore(cu.getUrl(), "!");
		  ArchivalUnit au = cu.getArchivalUnit();
		  if (au != null) {
			CachedUrl zipCu = au.makeCachedUrl(zip_url);
			if(zipCu != null && (zipCu.hasContent())) {
				defSize = Long.toString(zipCu.getContentSize());
			}
		  }
		}
	    return defSize;
	}
	
	/*
	 * In place in case we get the iterator to see metadata.yml instead of the zip
	 * If the capsule.zip is the metadata cu then just use defaults
	 */
	protected String getFileMime(CachedUrl cu) {
		String defMime = super.getFileMime(cu);
		if (cu.getUrl().endsWith(".yml")) {
		  String zip_url = StringUtils.substringBefore(cu.getUrl(), "!");
		  ArchivalUnit au = cu.getArchivalUnit();
		  if (au != null) {
			 CachedUrl zipCu = au.makeCachedUrl(zip_url);
  			  if(zipCu != null && (zipCu.hasContent())) {
			  	defMime = zipCu.getContentType();
			  }
		  }
		}
		return defMime;
		
	}	
	
	
	/*
	 * The following are unique to Code Ocean and should only be queries on a FILE object
	 * that has type of CODE_OCEAN_FILE_TYPE and provider = CODE_OCEAN
	 * 
	 * CODE OCEAN provides information about the DOI of the article the code capsule
	 * is associated with if it's associated with a published article. This might be null
	 * if it is associated with unpublished graduate research
	 */

	@Override
	protected void setAdditionalFileData(CachedUrl cu, Map<String, String> FILE_MAP) {
		String defV = "1.0";
		Matcher umat = ZIP_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			defV = umat.group(VERSION_GROUP);
		}
		FILE_MAP.put("CapsuleVersion", defV);
		//get associated article doi
		//FILE_MAP.put("CapsuleArticleDoi, "");
	}
	
	
	/*
	 * CODE OCEAN provides DOIs for each version of each code capsule
	 * so the same UUID might have 1+ DOIs
	 * currently our metadata cu is the capsule.zip, but we might switch 
	 * to metadata.yml - in which case, need to change the access_url as well.
	 * 
	 */
	protected void setAdditionalArticleMetadata(CachedUrl metadataCu, ArticleMetadata fileAM) {
		if(metadataCu.getUrl().endsWith(".yml")) {
			//fileAM.put(MetadataField.FIELD_DOI,thedoi);
			// and because the metadata is pulled from the internal metadata file, 
			// modify the access_url to be just the capsule.zip
			String thedoi = null;
			String zip_url = StringUtils.substringBefore(metadataCu.getUrl(), "!");
			fileAM.replace(MetadataField.FIELD_ACCESS_URL, zip_url);
		}
	}

	

}
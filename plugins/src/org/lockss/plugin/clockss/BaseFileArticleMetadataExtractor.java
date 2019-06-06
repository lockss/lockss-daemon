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

package org.lockss.plugin.clockss;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

// Make a custom AF object so that we can pass the already scraped DOI to the extractor
// then just fill in the metadata from what is known by the landing url
// there is nothing more to get

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;


/*
 *  A shared ArticleMetadataExtractor for "file" objects.
 *  File objects (as opposed to article, book, or proceeding require 
 *  only an access_url and a publisher
 *  Additional data is stored in a map in the MD table for the item.
 *  This class adds standardized support to add the size, and mimetype of the item
 *  The sub class extends this to provide a file identifier and the file type (what it is),
 *  and any additional custom key/value pairs for the specific type of file.
 */
public class BaseFileArticleMetadataExtractor extends BaseArticleMetadataExtractor{

	private static Logger log = 
			Logger.getLogger(BaseFileArticleMetadataExtractor.class);
	private static final String DEFAULT_FILE_TYPE = "file";
	private static final String DEFAULT_PUBLICATION_TITLE = "File Publication";
	

	public BaseFileArticleMetadataExtractor(String role) {
		super(role);
	}


	@Override
	public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
			throws IOException, PluginException {

		BaseFileEmitter emit = new BaseFileEmitter(af, emitter);

		CachedUrl metadataCu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
		ArticleMetadata fileAM = new ArticleMetadata();
		setFileMetadata(metadataCu, fileAM);
		emit.emitMetadata(metadataCu, fileAM);
	}


	/**
	 * For a FILE item (as opposed to article, book, or proceeding)
	 * the metadata may be very limited and does not require parsing the
	 * contents of the object.
	 * Set consistent basic information before emitting.
	 * A child plugin can override as necessary.
	 * @param cu - The CU for which we are generating metadata
	 * @param am - the AM in to which to put the generated metadata 
	 */
	protected void setFileMetadata(CachedUrl cu, ArticleMetadata am) {
		String url = cu.getUrl();
		log.debug3("generate MD for generic file object url " + url);
		
		
		ArchivalUnit au = cu.getArchivalUnit();
		TdbAu tdbau = au.getTdbAu();
		// use getters so a child plugin can override
		// default values come from tdbau if it's available
		String year = getContentYear(cu, tdbau);
		String publisher = getContentPublisher(cu, tdbau);
		String provider = getContentProvider(cu,tdbau, publisher);
		String pTitle = getPublicationTitle(cu,tdbau);


		am.put(MetadataField.FIELD_ACCESS_URL, url);
		am.put(MetadataField.FIELD_PROVIDER, provider);
		am.put(MetadataField.FIELD_PUBLISHER, publisher);
		am.put(MetadataField.FIELD_DATE, year);
		// Neither an article, book, nor proceeding - "other"
		am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_FILE);
		// Not explicitly necessary, would be inferred
		am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_FILE);
		am.put(MetadataField.FIELD_PUBLICATION_TITLE, pTitle);

		// Add a custom map to the generic am table 
		// Allow a child to override FileType
		Map<String, String> FILE_MAP = new HashMap<String,String>();

		//default is "file"
		FILE_MAP.put("FileType", getFileObjectType(cu));
		// default is base filename
		FILE_MAP.put("FileIdentifier", getFileIdentifier(cu));
		FILE_MAP.put("FileSizeBytes", getFileSize(cu));
		FILE_MAP.put("FileMime", getFileMime(cu));
		// default is no additional k-v pairs
		setAdditionalFileData(cu,FILE_MAP);

		am.putRaw(MetadataField.FIELD_MD_MAP.getKey(), FILE_MAP);
		
		// in case there are any other am items that can be set
		setAdditionalArticleMetadata(cu,am);

		
	}

	/**
	 * Set an identifier value to associate with this object
	 * The default is the filename (not the full path) of the url.
	 * The cu is available for pattern matching
	 * @param cu  
	 * @return
	 */
	protected String getFileIdentifier(CachedUrl cu) {
		// we know cu isn't null
		return FilenameUtils.getBaseName(cu.getUrl());
	}


	/**
	 * Return the "publication" year for the file object
	 * By default it is the year associated with the AU in the TDB
	 * The child plugin can override this method to set a different
	 * way to identify the plugin, for example from information in the url pattern
	 * @param cu
	 * @param tdbau
	 * @return
	 */
	protected String getContentYear(CachedUrl cu, TdbAu tdbau) {
		// Get limited information from the TDB file
		return (tdbau != null) ? tdbau.getEndYear() : null;
	}
	
	/**
	 * Return the "publication" title for the file object
	 * By default it is the title associated with the AU in the TDB
	 * The child plugin can override this method to set a different
	 * way to identify the content.
	 * @param cu
	 * @param tdbau
	 * @return
	 */
	protected String getPublicationTitle(CachedUrl cu, TdbAu tdbau) {
		// Get limited information from the TDB file
		return (tdbau != null) ? tdbau.getPublicationTitle() : DEFAULT_PUBLICATION_TITLE;
	}	
	
	/**
	 * Return the publisher associated with this content
	 * Take the value defined in the tdb file if available.
	 * If not, use a default value which can be set by the sub class
	 * @param cu
	 * @param tdbau
	 * @return
	 */
	protected String getContentPublisher(CachedUrl cu, TdbAu tdbau) {
		return (tdbau != null) ? tdbau.getPublisherName() : getDefaultPublisherName();
	}
	
	/**
	 *  When the publisher isn't available from the TDB
	 *  use this value
	 *  A subclass can override this method to set an appropriate value
	 *  though the TDB should be available and should have publisher set
	 */
	protected String getDefaultPublisherName() {
		return null;
	}


	protected String getContentProvider(CachedUrl cu, TdbAu tdbau, String publisher) {
		return (tdbau != null) ? tdbau.getProviderName() : publisher;
	}
	
	/**
	 * Define the specific type of file object being preserved
	 * which could be a publisher specific description, like "code capsule"
	 * or something more generic like "image" or "tar archive"
	 * A sub class can override this method and has the cu if
	 * the url pattern holds identifying information.
	 * @param cu
	 * @return the string to associate with the FileType key in the MD map
	 */
	protected String getFileObjectType(CachedUrl cu) {
		return DEFAULT_FILE_TYPE;
	}	
	
	/*
	 * A child can override this
	 * By default use the size of the metadata cu
	 * but this might not be true if the object is an archive
	 * from which the metadata file is accessed
	 */
	protected String getFileSize(CachedUrl cu) {
		long content_size = cu.getContentSize();
		return Long.toString(content_size);
		
	}
	
	/*
	 * A child can override this
	 * By default use the mime type of the metadata cu
	 * but this might not be true if the object is an archive
	 * from which the metadata file is accessed
	 */
	protected String getFileMime(CachedUrl cu) {
		String content_mime = cu.getContentType();
		return content_mime;
		
	}

	/**
	 * A child plugin might extend this class to add more information appropriate to
	 * the file object being preserved.  
	 * @param cu
	 * @param fILE_MAP
	 */
	protected void setAdditionalFileData(CachedUrl cu, Map<String, String> fILE_MAP) {
		log.debug3("In empty default setAdditionalFileData");
	}
	
	/*
	 * Most FILE objects won't need more than what is set in the setFileMetadata method
	 * but in case there is some other metadata that maps to standard article metadata fields
	 * a child can override this
	 */
	protected void setAdditionalArticleMetadata(CachedUrl metadataCu, ArticleMetadata fileAM) {
		log.debug3("In empty default setAdditionalArticleMetadata");
	}

	


	/*
	 * Do not use the emitter from BaseArticleMetadata because
	 * some of the default behaviors do not make sense for File objects
	 */
	static class BaseFileEmitter implements FileMetadataExtractor.Emitter {
		private Emitter parent;
		private ArticleFiles af;

		BaseFileEmitter(ArticleFiles af, Emitter parent) {
			this.af = af;
			this.parent = parent;
		}

		public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
			parent.emitMetadata(af, am);
		}  

		void setParentEmitter(Emitter parent) {
			this.parent = parent;
		}
	}




}
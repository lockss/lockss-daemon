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

package org.lockss.plugin.atypon.futurescience;

import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponNullMetadataExtractorFactory.BaseAtyponNullMetadataExtractor;


/*
 * FutureScience overcrawled badly at one point and while we can limit the iterator Pattern to 
 * require the journal_id in the doi (which FS and FM use consistently) it doesn't address
 * the overcrawling to other volumes of the same journal.
 * HOWEVER - it would appear that the overcrawled articles do not hae the RIS citation information
 * and are extracting from the dc.* information which doesn't have publication level info to 
 * use to compare.
 * For FutureScience - simply reject any html sources of metadata. Only the RIS is assocaited with
 * valid articles.
 * 
 * This could probably just be BaseAtyponNullMetadataExtractorFactory but I want to leave the option
 * to special case depending on the cu...
 * 
 */
public class FSLimitedHtmlMetadataExtractorFactory 
implements FileMetadataExtractorFactory {

	public FileMetadataExtractor 
	createFileMetadataExtractor(MetadataTarget target, String contentType)
			throws PluginException {
		return new FSNullMetadataExtractor();
	}

	public static class FSNullMetadataExtractor
	implements FileMetadataExtractor {
		static Logger log = Logger.getLogger(BaseAtyponNullMetadataExtractor.class);


		@Override
		public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
				throws IOException {
			log.debug3("Suppressing emit of metadata in Null extractor: " + cu.getUrl());
			// do nothing, do not allow TDB info to get used as default
			// by not emitting
		}

	}
}
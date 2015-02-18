/*
 * $Id$
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

package org.lockss.plugin.springer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.springerlink.com/content/978-3-642-14308-3/#section=723602&page=8&locus=0
 */
public class SpringerLinkBookMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SpringerLinkBookMetadataExtractorFactory");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new SpringerLinkBookMetadataExtractor();
  }

  public static class SpringerLinkBookMetadataExtractor
    implements FileMetadataExtractor {

	private Pattern[] patterns  = {
			Pattern.compile("(<h1.*>)"),
			Pattern.compile("(.*Volume [\\d]+(,|/) ?)([\\d]+)(, )?(<span class=\".*)"),
			Pattern.compile("(.*Volume )([\\d]+)((,|/) ?[\\d]+(, )?<span class=\".*)"),
			Pattern.compile("(.*DOI:</span> <span class=\"value\">)([^<]+)(</span>.*)"),
			Pattern.compile("(.*DOI:</span> <span class=\"value\">[^/]+/)([0-9-Xx]+)(.*</span>.*)"),
                        Pattern.compile("(.*DOI:</span> <span class=\"value\">[^/]+/[0-9-Xx]+)_([0-9]+)(</span>.*)"),
			Pattern.compile("(.*<span class=\"subtitle\">)([^<]+)(</span>.*)"),
			Pattern.compile("(</h1><p class=\"authors\">)(<a[^>]+>)([^<]+)(</a>([^<]+)?)"),
			Pattern.compile("(.*title=\"Link to the Book of this Chapter\">)([^<]+)(</a>.*)"),
			Pattern.compile("(.*<span class=\"pagination\">)([\\d]+)((-[\\d]+)?<.*)")
	};
	private MetadataField[] metadataFields = {
			MetadataField.FIELD_ARTICLE_TITLE,
			MetadataField.FIELD_DATE,
			MetadataField.FIELD_VOLUME,
			MetadataField.FIELD_DOI,
			MetadataField.FIELD_EISBN,
                        MetadataField.FIELD_ITEM_NUMBER,
			MetadataField.FIELD_ARTICLE_TITLE,
			MetadataField.FIELD_AUTHOR,
			MetadataField.FIELD_JOURNAL_TITLE,
			MetadataField.FIELD_START_PAGE
	};
	private String[] regex = {
			"gap",
			"$3",
			"$2",
			"$2",
                        "$2",
			"$2",
			"$2",
			"multiples",
			"$2",
			"$2"
	};
	private String[] metadataValues = new String[patterns.length];

    /**
     * Extracts metadata directly by reading the html file
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
      if (log.isDebug3()) {
        log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);
      }
      ArticleMetadata am = extractFrom(cu);

      // publisher name does not appear anywhere on the page in this form
      am.put(MetadataField.FIELD_PUBLISHER, "Springer Science+Business Media");
      emitter.emitMetadata(cu, am);
    }

    private ArticleMetadata extractFrom(CachedUrl cu) throws IOException {

    	ArticleMetadata am = new ArticleMetadata();

    	if(cu.hasContent()) {
    	  BufferedReader bReader = null;
    	  try {
    	        String startTag = "heading enumeration";
    		bReader = new BufferedReader(cu.openForReading());

    		String line = bReader.readLine();

    		while(line != null) {
    			if(line.contains(startTag)) {
    				fill(am, bReader);
    				return am;
    			}
    			else
    				line = bReader.readLine();
    		}
    	    } finally {
    	        IOUtil.safeClose(bReader);
    	    }
    	}

    	return am;
    }

    private void fill(ArticleMetadata am, BufferedReader bReader) throws IOException {
    	String line = bReader.readLine(), endTag = "ContentSecondary";

    	while(line != null && !line.contains(endTag)) {
    		for(int i = 0; i < patterns.length; ++i) {
    			if(patterns[i].matcher(line).find()) {
    				if(regex[i].contains("$"))
    					metadataValues[i] = patterns[i].matcher(line).replaceAll(regex[i]);
    				else
    					processSpecialCase(am, line, i, bReader);
       			}
    		}

    		line = bReader.readLine();
    	}

    	for(int i = 0; i < metadataFields.length; ++i) {
    	  if(metadataValues[i] != null) {
    	    if ((MetadataField.FIELD_EISBN).equals(metadataFields[i])) {
    	      /* special case for ISBN which is not always the 2nd half of DOI
    	       * and if a bad ISBN goes through it will cause an exception to get thrown
    	       */
    	      if (MetadataUtil.isIsbn(metadataValues[i], false)) //don't need strict validation, hence the false
    	        am.put(metadataFields[i], metadataValues[i]);
    	    } else if ((MetadataField.FIELD_AUTHOR).equals(metadataFields[i])) {
              // store individual values of a multi-valued field
              String[] values = metadataValues[i].split(";");
              for (String value : values) {
                am.put(metadataFields[i], value.trim());
              }
    	    } else {
    	      am.put(metadataFields[i], metadataValues[i]);
    	    }
    	  }
    	}
    }

    private void processSpecialCase(ArticleMetadata am, String line, int index, BufferedReader bReader) throws IOException {
      if(patterns[index].pattern().contains("<h1")) {
        /*
         * The title might not be a standalone title, it might look like this:
         *   <a href="/content/7263447022121139/" title="Link to Chapter">Generality Is Predictive of Prediction Accuracy</a>
         *   but perhaps there are examples where this isn't the case, so make it optional
         */
        String titleWithHref = "(<a href=.*\">)([^<]+)(</a>.*)";
        Pattern hrefTitle = Pattern.compile(titleWithHref);

        line = bReader.readLine();
        Matcher mat = hrefTitle.matcher(line);
        if (mat.find()) {
          metadataValues[index] = mat.group(2);
        } else {
          metadataValues[index] = line.trim();
        }
      } else if(patterns[index].pattern().contains("authors")) {
        Matcher mat = patterns[index].matcher(line);

        while(mat.find()) {
          if(metadataValues[index] == null)
            metadataValues[index] = mat.group(3);
          else
            metadataValues[index] += "; " + mat.group(3);

          mat.reset(mat.replaceFirst("$1"));
        }
      }
    }
  }
}
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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataField.Validator;
import org.lockss.plugin.*;

/**
 * Files from Elsevier archive used to write this class:
 * dataset.toc
 *
 */
public class ElsevierTocMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("ElsevierTocMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ElsevierTocMetadataExtractor();
  }

  public static class ElsevierTocMetadataExtractor 
  implements FileMetadataExtractor {

    private final int DOI_LEN = 6;

    private final int ISSN_INDEX = 0;
    private final int DATE_INDEX = 5;
    private final int FILE_NAME_INDEX = 6;
    private final int DOI_INDEX = 7;
    private final int AUTHOR_INDEX = 10;
    private final int PAGE_INDEX = 13;
    private final int KEYWORD_INDEX = 12;

    private final int INVALID_TAG = -1;
    private final int REPEATED_TAG = -3;
    private final int ARTICLE_COMPLETE = -4;

    private List<String> articleTags = Arrays.asList(new String[]{
        "_t1",
        "_vl",
        "_jn", 
        "_cr", 
        "_is",
        "_dt",
        "_t3", 
        "_ii",
        "_la",
        "_ti",
        "_au",
        "_ab", 
        "_kw", 
        "_pg",
        "_pg",
    });

    private final String END_ARTICLE_METADATA = "_mf";

    private String[] articleValues = new String[articleTags.size()];

    // Overriding MetadataField.FIELD_AUTHOR and MetadataField.FIELD_KEYWORDS
    // local version sets the "splitter", which is necessary to trigger
    // ArticleMetadata.put() [really, putMulti()],  to separate the
    // list using "splitCh" (DB expects fields to be < 128 chars)
    private static  Validator authorvalid = new Validator(){
      public String validate(ArticleMetadata am,MetadataField field,String val)
          throws MetadataException.ValidationException {
        // normalize author entries especially with no names .
        // For example : <meta name="citation_authors" content=", "/>
          if(!MetadataUtil.isAuthor(val)) {
            throw new MetadataException.ValidationException("Illegal Author: " 
          + val);
          }
          return val;
          }
     };
    private final static String splitCh = "; ";
    private static final MetadataField ELSEVIER_FIELD_AUTHOR = new MetadataField(
        MetadataField.KEY_AUTHOR, Cardinality.Multi, authorvalid, MetadataField.splitAt(splitCh));
    private static final MetadataField ELSEVIER_FIELD_KEYWORDS = new MetadataField(
        MetadataField.KEY_KEYWORDS, Cardinality.Multi, MetadataField.splitAt(splitCh));      

    private MetadataField[] metadataFields = {
        MetadataField.FIELD_ISSN, 
        MetadataField.FIELD_VOLUME, 
        MetadataField.FIELD_JOURNAL_TITLE, 
        MetadataField.DC_FIELD_RIGHTS,
        MetadataField.FIELD_ISSUE,
        MetadataField.FIELD_DATE,
        MetadataField.FIELD_ACCESS_URL, 
        MetadataField.FIELD_DOI, 
        MetadataField.DC_FIELD_LANGUAGE,
        MetadataField.FIELD_ARTICLE_TITLE, 
        ELSEVIER_FIELD_AUTHOR, 
        MetadataField.DC_FIELD_DESCRIPTION,
        ELSEVIER_FIELD_KEYWORDS, 
        MetadataField.FIELD_START_PAGE,
        MetadataField.FIELD_END_PAGE,
    };

    private int lastTag = INVALID_TAG;
    private String base_url, year;
    
    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      CachedUrl metadata = cu.getArchivalUnit().makeCachedUrl(getToc(cu.getUrl()));

      if (metadata.getUrl().equals(cu.getUrl())) {
        metadata = cu;
      }
      if (metadata == null || !metadata.hasContent()) {
        log.error("The metadata file does not exist or is not readable.");
        return;
      }

      base_url = cu.getArchivalUnit().getConfiguration().get("base_url");
      year = cu.getArchivalUnit().getConfiguration().get("year");

      BufferedReader bReader = new BufferedReader(metadata.openForReading());
      try {
        for (String line = bReader.readLine(); line != null; line = bReader.readLine()) { 
          line = line.trim();

          if (extractFrom(line) && articleValues[FILE_NAME_INDEX] != null) {
            CachedUrl container = cu.getArchivalUnit().makeCachedUrl(getUrlFrom(articleValues[FILE_NAME_INDEX]));
            log.debug3("Emitting metadata for url: " + container.getUrl());
            articleValues[FILE_NAME_INDEX] = container.getUrl();

            ArticleMetadata am = new ArticleMetadata();
            putMetadataIn(am);
            emitter.emitMetadata(container, am);

            clear(articleValues);
          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
    }

    private String getIssnFrom(String line) {
      if (line.length() < 4) {
        return "";
      }
      int i = line.lastIndexOf(' ');
      if (i > 0) {
        return MetadataUtil.formatIssn(line.substring(i+1));
      }
      return line;
    }

    private String getStartPageFrom(String line) {
      // skip past "_pg " prefix
      if (line.length() < 4) {
        return "";
      }
      int i = line.indexOf(' ');
      if (i > 0) {
        // "6A" -> "6A"
        // "18A-19A" -> "18A"
        // "6A,8A,10A,12A,14A,16A,18A-19A" -> "6A
        String[] pages = line.substring(i+1).split("[-,]");
        return pages[0].trim();
      }
      return line;
    }

    private String getEndPageFrom(String line) {
      if (line.length() < 4) {
        return "";
      }
      int i = line.indexOf(' ');
      if (i > 0) {
        // examples:
        // "6A" -> "6A"
        // "18A-19A" -> "19A"
        // "6A,8A,10A,12A,14A,16A,18A-19A" -> "19A
        String[] pages = line.substring(i+1).split("[-,]");
        return pages[pages.length-1].trim();
      }
      return line;
    }


    Pattern datepat = Pattern.compile("([\\d]{4})([\\d]{2})?([\\d]{2})?");

    private String getDateFrom(String line) {
      if (line.length() < 4) {
        return "";
      }

      int i = line.indexOf(' ');
      if (i >= 3) {
        Matcher m = datepat.matcher(line.substring(i+1));
        if (m.matches()) {
          String date = m.group(1);
          if (m.group(2) != null) {
            date += "-" + m.group(2);
            if (m.group(3) != null) {
              date += "-" + m.group(3);
            }
          }
          return date;
        }
      }
      return line;
    }

    private String getDoiFrom(String line)
    {
      if (line.contains("[DOI] ")) {
        return line.substring(line.indexOf("[DOI] ")+DOI_LEN);
      }
      return line;
    }

    private void clear(String[] values)
    {
      for(int i = FILE_NAME_INDEX; i < values.length; ++i) {
        values[i] = null;
      }
    }

    private String getMetadataFrom(String line)
    {
      if (line.length() < 4) {
        return "";
      }
      return line.substring(4); //substring after '_xx' tag
    }

    /**
     * Returns the index of the current line's tag in the tag List or
     * some other value if the current tag is special in some way
     * @param line
     * @return
     */
    private int getTagIndex(String line)
    {
      if (line.indexOf("_") < 0) {
        return REPEATED_TAG;
      }
      String tag = line.substring(0,3);

      if (tag.equals(END_ARTICLE_METADATA)) {
        return ARTICLE_COMPLETE;
      } else if (articleTags.contains(tag)) {
        return articleTags.indexOf(tag);
      } else {
        return INVALID_TAG;
      }
    }

    private boolean extractFrom(String line)
    {
      int tag = getTagIndex(line);

      if (tag == ARTICLE_COMPLETE) {
        return true;
      }
      if (tag == INVALID_TAG) {
        lastTag = tag;
      } else if (tag == REPEATED_TAG) {
        if (lastTag != INVALID_TAG) {
          articleValues[lastTag] += " "+line;
        }
      } else {
        // build up the single AUTHORS and KEYWORDS into a '; ' separated string
        if (tag == AUTHOR_INDEX || tag == KEYWORD_INDEX) {
          if (articleValues[tag] == null) {
            articleValues[tag] = getMetadataFrom(line);
          } else {
            articleValues[tag] +=  splitCh + getMetadataFrom(line);
          }
    
        } else if (tag == DOI_INDEX) {
          articleValues[tag] = getDoiFrom(line);
        } else if (tag == ISSN_INDEX) {
          articleValues[tag] = getIssnFrom(line);
        } else if (tag == PAGE_INDEX) {
          articleValues[tag] = getStartPageFrom(line);
          articleValues[tag+1] = getEndPageFrom(line);
        } else if (tag == DATE_INDEX) {
          articleValues[tag] = getDateFrom(line);
        }
        else {
          articleValues[tag] = getMetadataFrom(line);
        }

        lastTag = tag;
      }

      return false;
    }

    /**
     * Stores the gathered MetadataField values in the ArticleMetadata
     * so it can be emitted
     * @param am the ArticleMetadata to populate
     */
    private void putMetadataIn(ArticleMetadata am)
    {   
      // metadata does not include publisher name
      am.put(MetadataField.FIELD_PUBLISHER, "Elsevier");
      
      for (int i = 0; i < articleTags.size(); ++i) {
        if (articleValues[i] != null) {
           // Both AUTHORs and KEYWORDs are ';' separated lists of values.
           // the DB can only take entries of < 128 chars, but don't split
           // the list here - that's done in ArticleMetadata.putMulti()
          am.put(metadataFields[i],articleValues[i]);                    
          if (log.isDebug3()) {
            log.debug(metadataFields[i].getKey() + ": " +  articleValues[i]);
          }
        }
      }
    }

    /**
     * Uses a url of an article to construct its metadata file's url
     * @param url - address of an article file
     * @return the metadata file's pathname
     */
    protected String getToc(String url)
    {
      Pattern pattern = Pattern.compile("(.*/[^/]+/)[\\d]+[^/]+/[\\d]+/[\\d]+/main.pdf$",Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(url);
      return matcher.replaceFirst("$1dataset.toc");
    }

    protected String getUrlFrom(String identifier)
    {
      Pattern pattern = Pattern.compile("([^/]+)( )([^/]+)( )([^/]+)( )([^/]+)");
      Matcher matcher = pattern.matcher(identifier);
      return matcher.replaceFirst(base_url+year+"/$1/$3.tar!/$5/$7/main.pdf");
    }
  }
}
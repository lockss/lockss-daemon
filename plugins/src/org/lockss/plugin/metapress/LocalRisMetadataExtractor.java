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

package org.lockss.plugin.metapress;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import java.io.*;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
//TODO 1.70 remove this class in place of daemon RisMetadataExtractor class

/**
 * A local version of RisMetadataExtractor that brings the 1.70 functionality 
 * in to the plugin until 1.70 is available. This version can handle
 * multi-line values
 * 
 */
public class LocalRisMetadataExtractor implements FileMetadataExtractor {
  
  private static Logger log = Logger.getLogger(LocalRisMetadataExtractor.class);
  
  /**
   * <p>
   * The default regular expression for tagged lines, from the RIS specification
   * (/^([A-Z][A-Z0-9])  - (.*)$/).
   * </p>
   * 
   * @since 1.70
   * @see http://refman.com/sites/rm/files/m/direct_export_ris.pdf
   */
  public static final Pattern DEFAULT_RIS_PATTERN = Pattern.compile("^([A-Z][A-Z0-9])  - (.*)$");
  
  protected static final String REFTYPE_JOURNAL = "Journal";
  protected static final String REFTYPE_BOOK = "Book";
  protected static final String REFTYPE_OTHER = "Other";
  
  private MultiMap risTagToMetadataField;
  
  /**
   * <p>
   * The regular expression for tagged lines, by default
   * {@link #DEFAULT_RIS_PATTERN}.
   * </p>
   * 
   * @since 1.70
   */
  private Pattern risPattern;
  
  /**
   * <p>
   * The tag currently being parsed.
   * </p>
   * 
   * @since 1.70
   */
  private String currentTag;
  
  /**
   * <p>
   * The value currently being accumulated.
   * </p>
   * 
   * @since 1.70
   */
  private StringBuilder currentValue;
  
  /**
   * <p>
   * The {@link ArticleMetadata} instance currently being filled.
   * </p>
   * 
   * @since 1.70
   */
  private ArticleMetadata am;
  
  /**
   * <p>
   * The type of reference currently being parsed.
   * </p>
   * 
   * @since 1.70
   * @see #REFTYPE_BOOK
   * @see #REFTYPE_JOURNAL
   * @see #REFTYPE_OTHER
   */
  private String refType;
  
  /**
   * <p>
   * Creates a RIS metadata extractor with a default pattern and field map.
   * </p>
   * 
   * @see #LocalRisMetadataExtractor(Pattern, MultiValueMap)
   * @see #DEFAULT_RIS_PATTERN
   * @see #getDefaultFieldMap()
   */
  public LocalRisMetadataExtractor() {
    this(DEFAULT_RIS_PATTERN, getDefaultFieldMap());
  }
  
  /**
   * <p>
   * Creates a RIS metadata extractor with a default pattern and the given field
   * map.
   * </p>
   * 
   * @param fieldMap
   *          A map from RIS tags to metadata fields.
   * @see #LocalRisMetadataExtractor(Pattern, MultiValueMap)
   * @see #DEFAULT_RIS_PATTERN
   */
  public LocalRisMetadataExtractor(MultiValueMap fieldMap) {
    this(DEFAULT_RIS_PATTERN, fieldMap);
  }
  
  /**
   * <p>
   * Creates a RIS metadata extractor with the given pattern and a default field
   * map.
   * </p>
   * 
   * @param risPattern
   *          A regular expression for tagged lines
   * @since 1.70
   * @see #LocalRisMetadataExtractor(Pattern, MultiValueMap)
   * @see #getDefaultFieldMap()
   */
  public LocalRisMetadataExtractor(Pattern risPattern) {
    this(risPattern, getDefaultFieldMap());
  }
  
  /**
   * <p>
   * Creates a RIS metadata extractor with the given pattern and field map.
   * </p>
   * 
   * @param risPattern
   *          A regular expression for tagged lines
   * @param fieldMap
   *          A map from RIS tags to metadata fields.
   * @since 1.70
   */
  public LocalRisMetadataExtractor(Pattern risPattern, MultiValueMap fieldMap) {
    setRisPattern(risPattern);
    setFieldMap(fieldMap);
  }
  
  /**
   * Create a RisMetadataExtractor with a RIS tag To MetadataField map of
   * default fieldMap and adding the specified metadata field Ris tag pair
   * 
   * @param risTag
   * @param field
   * @deprecated as of 1.70; use {@link #LocalRisMetadataExtractor()} then call
   *             {@link #addRisTag(String, MetadataField)} instead.
   */
  @Deprecated
  public LocalRisMetadataExtractor(String risTag, MetadataField field) {
    this();
    addRisTag(risTag, field);
  }
  
  /**
   * <p>
   * Sets the field map to the given field map.
   * </p>
   *  
   * @param fieldMap
   *          A map from RIS tags to metadata fields.
   * @since 1.70
   */
  public void setFieldMap(MultiValueMap fieldMap) {
    this.risTagToMetadataField = fieldMap;
  }
  
  
  /**
   * <p>
   * Add the given metadata field to the given RIS tag in the field map.
   * </p>
   * 
   * 
   * @param risTag
   *          A RIS tag
   * @param field
   *          A metadata field
   */
  public void addRisTag(String risTag, MetadataField field) {
    risTagToMetadataField.put(risTag, field);
  }
  
  /**
   * <p>
   * Removes the given RIS tag from the field map.
   * <p>
   * 
   * @param risTag
   *          A RIS tag
   */
  public void removeRisTag(String risTag) {
    risTagToMetadataField.remove(risTag);
  }
  
  /**
   * <p>
   * Checks that the given RIS tag is in the field map.
   * </p>
   * 
   * @param risTag
   *          A RIS tag
   */
  public boolean containsRisTag(String risTag) {
    return risTagToMetadataField.containsKey(risTag);
  }
  
  /**
   * <p>
   * Sets the regular expression for tagged line.
   * </p>
   * 
   * @param risPattern
   *          A regular expression for tagged lines.
   * @since 1.70
   */
  public void setRisPattern(Pattern risPattern) {
    this.risPattern = risPattern;
  }
  
  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException, PluginException {
    ArticleMetadata md = extract(target, cu);
    if (md != null) {
      emitter.emitMetadata(cu, md);
    }
  }  

  /**
   * Extract metadata from the content of the cu, which should be an RIS file.
   * Reads line by line inserting the 2 character code and value into the raw map.
   * The first line should be a material type witch if it is book or journal will 
   * determine if we interpret the SN tag as IS beltSN or ISBN.
   * @param target
   * @param cu
   */
  public final ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException, PluginException {
    if (cu == null) {
      throw new IllegalArgumentException();
    }
    
    log.debug2("Parsing " + cu.getUrl());

    am = new ArticleMetadata();
    refType = null;
    currentTag = null;
    currentValue = new StringBuilder();
    boolean hasBegun = false;
    
    LineNumberReader reader = new LineNumberReader(cu.openForReading());
    try {
      for (String line = reader.readLine() ; line != null ; line = reader.readLine()) {
        // Skip empty lines at beginning of file
        if (!hasBegun && line.trim().isEmpty()) {
          continue;
        }
        hasBegun = true;
        
        Matcher mat = risPattern.matcher(line);
        if (mat.find()) {
          // process old tag
          processTag();
          // set up new tag
          currentTag = mat.group(1);
          currentValue = new StringBuilder(mat.group(2).trim());
        }
        else {
          if (currentTag == null) {
            log.debug(String.format("%s line %d: ignoring continuation line in preamble", cu.getUrl(), reader.getLineNumber()));
            continue;
          }
          // process continuation of current tag
          String continuation = line.trim();
          if (!continuation.isEmpty()) {
            currentValue.append(' ');
            currentValue.append(continuation);
          }
        }
      }
      processTag();
    }
    finally {
      IOUtil.safeClose(reader);
    }
    
    am.cook(risTagToMetadataField);
    return am;
  }
  
  private void processTag() {
    if (currentTag == null) {
      return; // first tag of the file
    }
    
    String value = currentValue.toString();
    
    if (currentTag.equals("TY")) {
      switch (value) {
        case "JOUR":
          refType = REFTYPE_JOURNAL;
          break;
        case "BOOK": case "CHAP": case "EBOOK": case "ECHAP": case "EDBOOK":
          refType = REFTYPE_BOOK;
          break;
        default:
          refType = REFTYPE_OTHER;
          break;
      }
    }
    else if (currentTag.equals("SN")) {
      if (!containsRisTag("SN")) {
        if (refType == null) {
          log.debug("SN tag without prior TY tag");
        }
        else if (refType == REFTYPE_BOOK) {
          addRisTag("SN", MetadataField.FIELD_ISBN);
        }
        else {
          addRisTag("SN", MetadataField.FIELD_ISSN);
        }
      }
    }
    
    am.putRaw(currentTag, value);
  }

  public static final MultiValueMap getDefaultFieldMap() {
    MultiValueMap mvmap = new MultiValueMap();
    mvmap.put("T1", MetadataField.FIELD_ARTICLE_TITLE);
    mvmap.put("AU", MetadataField.FIELD_AUTHOR);
    mvmap.put("JF", MetadataField.FIELD_PUBLICATION_TITLE);
    mvmap.put("DO", MetadataField.FIELD_DOI);
    mvmap.put("PB", MetadataField.FIELD_PUBLISHER);
    mvmap.put("VL", MetadataField.FIELD_VOLUME);
    mvmap.put("IS", MetadataField.FIELD_ISSUE);
    mvmap.put("SP", MetadataField.FIELD_START_PAGE);
    mvmap.put("EP", MetadataField.FIELD_END_PAGE);
    mvmap.put("DA", MetadataField.FIELD_DATE);
    return mvmap;
  }

}

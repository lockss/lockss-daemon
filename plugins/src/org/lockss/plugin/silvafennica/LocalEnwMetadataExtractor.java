/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silvafennica;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import java.io.*;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
// TODO remove this class in place of daemon EnwMetadataExtractor class

/**
 * A local version of EnwMetadataExtractor 
 */
public class LocalEnwMetadataExtractor implements FileMetadataExtractorFactory, FileMetadataExtractor {
  
  private static Logger log = Logger.getLogger(LocalEnwMetadataExtractor.class);
  
  public static final Pattern DEFAULT_ENW_PATTERN = Pattern.compile("^(%[A-Z0-9]) (.*)$");
  
  protected static final String REFTYPE_JOURNAL = "Journal";
  
  private MultiMap enwTagToMetadataField;
  
  /**
   * <p>
   * The regular expression for tagged lines, by default
   * {@link #DEFAULT_ENW_PATTERN}.
   * </p>
   */
  private Pattern enwPattern;
  
  /**
   * <p>
   * The tag currently being parsed.
   * </p>
   */
  private String currentTag;
  
  /**
   * <p>
   * The value currently being accumulated.
   * </p>
   */
  private StringBuilder currentValue;
  
  /**
   * <p>
   * The {@link ArticleMetadata} instance currently being filled.
   * </p>
   */
  private ArticleMetadata am;
  
  /**
   */
  private String refType;
  
  /**
   * <p>
   * Creates a ENW metadata extractor with a default pattern and field map.
   * </p>
   * 
   * @see #LocalEnwMetadataExtractor(Pattern, MultiValueMap)
   * @see #DEFAULT_ENW_PATTERN
   * @see #getDefaultFieldMap()
   */
  public LocalEnwMetadataExtractor() {
    this(DEFAULT_ENW_PATTERN, getDefaultFieldMap());
  }
  
  /**
   * <p>
   * Creates a ENW metadata extractor with a default pattern and the given field
   * map.
   * </p>
   * 
   * @param fieldMap
   *          A map from ENW tags to metadata fields.
   * @see #LocalEnwMetadataExtractor(Pattern, MultiValueMap)
   * @see #DEFAULT_ENW_PATTERN
   */
  public LocalEnwMetadataExtractor(MultiValueMap fieldMap) {
    this(DEFAULT_ENW_PATTERN, fieldMap);
  }
  
  /**
   * <p>
   * Creates a ENW metadata extractor with the given pattern and a default field
   * map.
   * </p>
   * 
   * @param enwPattern
   *          A regular expression for tagged lines
   * @since 1.70
   * @see #LocalEnwMetadataExtractor(Pattern, MultiValueMap)
   * @see #getDefaultFieldMap()
   */
  public LocalEnwMetadataExtractor(Pattern enwPattern) {
    this(enwPattern, getDefaultFieldMap());
  }
  
  /**
   * <p>
   * Creates a ENW metadata extractor with the given pattern and field map.
   * </p>
   * 
   * @param enwPattern
   *          A regular expression for tagged lines
   * @param fieldMap
   *          A map from ENW tags to metadata fields.
   * @since 1.70
   */
  public LocalEnwMetadataExtractor(Pattern enwPattern, MultiValueMap fieldMap) {
    setEnwPattern(enwPattern);
    setFieldMap(fieldMap);
  }
  
  /**
   * Create a EnwMetadataExtractor with a ENW tag To MetadataField map of
   * default fieldMap and adding the specified metadata field Enw tag pair
   * 
   * @param enwTag
   * @param field
   */
  public LocalEnwMetadataExtractor(String enwTag, MetadataField field) {
    this();
    addEnwTag(enwTag, field);
  }
  
  /**
   * <p>
   * Sets the field map to the given field map.
   * </p>
   *  
   * @param fieldMap
   *          A map from ENW tags to metadata fields.
   * @since 1.70
   */
  public void setFieldMap(MultiValueMap fieldMap) {
    this.enwTagToMetadataField = fieldMap;
  }
  
  
  /**
   * <p>
   * Add the given metadata field to the given ENW tag in the field map.
   * </p>
   * 
   * 
   * @param enwTag
   *          A ENW tag
   * @param field
   *          A metadata field
   */
  public void addEnwTag(String enwTag, MetadataField field) {
    enwTagToMetadataField.put(enwTag, field);
  }
  
  /**
   * <p>
   * Removes the given ENW tag from the field map.
   * <p>
   * 
   * @param enwTag
   *          A ENW tag
   */
  public void removeEnwTag(String enwTag) {
    enwTagToMetadataField.remove(enwTag);
  }
  
  /**
   * <p>
   * Checks that the given ENW tag is in the field map.
   * </p>
   * 
   * @param enwTag
   *          A ENW tag
   */
  public boolean containsEnwTag(String enwTag) {
    return enwTagToMetadataField.containsKey(enwTag);
  }
  
  /**
   * <p>
   * Sets the regular expression for tagged line.
   * </p>
   * 
   * @param enwPattern
   *          A regular expression for tagged lines.
   * @since 1.70
   */
  public void setEnwPattern(Pattern enwPattern) {
    this.enwPattern = enwPattern;
  }
  
  /**
   * Extract metadata from the content of the cu, which should be an ENW file.
   * Reads line by line inserting the 2 character code and value into the raw map.
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
        
        Matcher mat = enwPattern.matcher(line);
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
    
    am.cook(enwTagToMetadataField);
    return am;
  }
  
  private void processTag() {
    if (currentTag == null) {
      return; // first tag of the file
    }
    
    String value = currentValue.toString();
    
    am.putRaw(currentTag, value);
  }

  public static final MultiValueMap getDefaultFieldMap() {
    MultiValueMap mvmap = new MultiValueMap();
    mvmap.put("%T", MetadataField.FIELD_ARTICLE_TITLE);
    mvmap.put("%A", MetadataField.FIELD_AUTHOR);
    mvmap.put("%J", MetadataField.FIELD_PUBLICATION_TITLE);
    mvmap.put("%R", MetadataField.FIELD_DOI);
    //mvmap.put("PB", MetadataField.FIELD_PUBLISHER);
    mvmap.put("%V", MetadataField.FIELD_VOLUME);
    mvmap.put("%N", MetadataField.FIELD_ISSUE);
    mvmap.put("%D", MetadataField.FIELD_DATE);
    return mvmap;
  }

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target, String contentType)
      throws PluginException {
    return new LocalEnwMetadataExtractor();
  }

  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
    ArticleMetadata am = extract(target, cu);
    am.putRaw("extractor.type", "ENW");
    emitter.emitMetadata(cu, am);
    
  }

}

/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.clockss;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BibtexMetadataExtractor implements FileMetadataExtractor {

  private static final Logger log = Logger.getLogger( BibtexMetadataExtractor.class);

  /**
   * <p>
   * The default regular expression for tagged lines, from the BibTeX specification
   * ^\s*(.+)=(.*)$.
   * </p>
   *
   * @see <a href="https://www.bibtex.com/format/">https://www.bibtex.com/format</a>
   */
  public static final Pattern DEFAULT_ENTRY_TYPE_PATTERN = Pattern.compile("^@(.+)\\{.*$");
  public static final Pattern DEFAULT_FIELD_PATTERN = Pattern.compile("^\\s*(.+)=(.*),\\s*$");

  public static final Pattern BIBTEX_PAGE_PATTERN = Pattern.compile("(\\d+)--(\\d+)");

  private MultiMap bibTeXTagToMetadataField;

  /**
   * <p>
   * The regular expression for tagged lines, by default
   * {@link #DEFAULT_FIELD_PATTERN}.
   * </p>
   *
   * @since 1.70
   */
  private Pattern bibTeXFieldPattern;

  /**
   * <p>
   * The regular expression for entry lines, by default
   * {@link #DEFAULT_ENTRY_TYPE_PATTERN}.
   * </p>
   *
   * @since 1.70
   */
  private Pattern bibTeXEntryPattern;

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
   * Creates a BibTeX metadata extractor with a default pattern and field map.
   * </p>
   *
   * @see #BibtexMetadataExtractor(Pattern, MultiValueMap)
   */
  public BibtexMetadataExtractor() {this(DEFAULT_FIELD_PATTERN, getDefaultFieldMap());
  }

  /**
   * <p>
   * Creates a BibTeX metadata extractor with a default pattern and the given field
   * map.
   * </p>
   *
   * @param fieldMap
   *         A map from BibTeX fields to MetadataFields.
   * @see #BibtexMetadataExtractor(Pattern, MultiValueMap)
   */
  public  BibtexMetadataExtractor(MultiValueMap fieldMap) {
    this(DEFAULT_FIELD_PATTERN, fieldMap);
  }

  /**
   * <p>
   * Creates a BibTeX metadata extractor with the given pattern and a default field
   * map.
   * </p>
   *
   * @param bibTeXPattern
   *         A Regex Pattern to identify field:value pairs on BibTeX file lines
   * @since 1.70
   * @see #BibtexMetadataExtractor(Pattern, MultiValueMap)
   */
  public BibtexMetadataExtractor(Pattern bibTeXPattern) {
    this(bibTeXPattern, getDefaultFieldMap());
  }

  /**
   * <p>
   * Creates a BibTeX metadata extractor with the given pattern and field map.
   * </p>
   *
   * @param bibTeXPattern
   *          A regular expression for tagged lines
   * @param fieldMap
   *          A map from BibTeX tags to metadata fields.
   * @since 1.70
   */
  public BibtexMetadataExtractor(Pattern bibTeXPattern, MultiValueMap fieldMap) {
    setFieldPattern(bibTeXPattern);
    setFieldMap(fieldMap);
  }

  /**
   * <p>
   * Sets the field map to the given field map.
   * </p>
   *
   * @param fieldMap
   *          A map from BibTeX tags to metadata fields.
   * @since 1.70
   */
  public void setFieldMap(MultiValueMap fieldMap) {
    this.bibTeXTagToMetadataField = fieldMap;
  }


  /**
   * <p>
   * Add the given metadata field to the given BibTeX tag in the field map.
   * </p>
   *
   *
   * @param bibTeXTag
   *          A BibTeX tag
   * @param field
   *          A metadata field
   */
  public void addBibTeXTag(String bibTeXTag, MetadataField field) {
    bibTeXTagToMetadataField.put(bibTeXTag, field);
  }

  /**
   * <p>
   * Removes the given BibTeX tag from the field map.
   * <p>
   *
   * @param bibTeXTag
   *          A bibTeXTag tag
   */
  public void removeBibTeXTag(String bibTeXTag) {
    bibTeXTagToMetadataField.remove(bibTeXTag);
  }

  /**
   * <p>
   * Checks that the given BibTeX tag is in the field map.
   * </p>
   *
   * @param bibTeXTag
   *          A BibTeX tag
   */
  public boolean containsBibTeXTag(String bibTeXTag) {
    return bibTeXTagToMetadataField.containsKey(bibTeXTag);
  }

  /**
   * <p>
   * Sets the regular expression for tagged line.
   * </p>
   *
   * @param fieldPattern
   *          A regular expression for tagged lines.
   * @since 1.70
   */
  public void setFieldPattern(Pattern fieldPattern) {
    this.bibTeXFieldPattern = fieldPattern;
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
   * Extract metadata from the content of the cu, which should be an BibTeX file.
   * Reads line by line inserting the 2 character code and value into the raw map.
   * The first line should be a material type witch if it is book or journal will
   * determine if we interpret the SN tag as IS beltSN or ISBN.
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
        Matcher mat = bibTeXFieldPattern.matcher(line);

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
            // add the continuation, unless it is a closing brace, which is the final line.
            if (!continuation.equals("}")) {
              currentValue.append(' ');
              currentValue.append(continuation);
            }
          }
        }
      }
      processTag();
    }
    finally {
      IOUtil.safeClose(reader);
    }

    am.cook(bibTeXTagToMetadataField);
    return am;
  }

  private void processTag() {
    if (currentTag == null) {
      return; // first tag of the file
    }
    String value = currentValue.toString();
    if (value.startsWith("{") || value.startsWith("\"")) {
      // many BibTeX values are enclosed in quotes, or braces,
      // strip them
      value = value.substring(1,value.length()-1);
      // further process if it is a page numbers field
      if (currentTag.equals("pages")) {
        Matcher pageMatcher = BIBTEX_PAGE_PATTERN.matcher(value);
        if (pageMatcher.find()) {
          am.put(MetadataField.FIELD_START_PAGE, pageMatcher.group(1));
          am.put(MetadataField.FIELD_END_PAGE, pageMatcher.group(2));
        }
        return;
      }
      // the other cases only needed removal of braces
      // "author" "doi" "date" "title" "publisher" "journal"
    }
    am.putRaw(currentTag, value);
  }

  public static MultiValueMap getDefaultFieldMap() {
    MultiValueMap mvmap = new MultiValueMap();
    // per BibTeX standard, multiple authors are seperated by "and"
    mvmap.put("author", new MetadataField(
        MetadataField.FIELD_AUTHOR, MetadataField.splitAt(" and ")));
    mvmap.put("title", MetadataField.FIELD_ARTICLE_TITLE);
    mvmap.put("publisher", MetadataField.FIELD_PUBLISHER);
    mvmap.put("journal", MetadataField.FIELD_PUBLICATION_TITLE);
    mvmap.put("date", MetadataField.FIELD_DATE);
    mvmap.put("volume", MetadataField.FIELD_VOLUME);
    mvmap.put("number", MetadataField.FIELD_ITEM_NUMBER);
    mvmap.put("note", MetadataField.FIELD_ARTICLE_TITLE);
    mvmap.put("doi", MetadataField.FIELD_DOI);
    mvmap.put("issn", MetadataField.FIELD_ISSN);
    mvmap.put("eprint", MetadataField.FIELD_ARTICLE_TITLE);
    mvmap.put("url", MetadataField.FIELD_ACCESS_URL);
    mvmap.put("isbn", MetadataField.FIELD_ISBN);
    // other fields that may be useful.
    // "year", "month", "day", "editor", "edition"
    return mvmap;
  }

}

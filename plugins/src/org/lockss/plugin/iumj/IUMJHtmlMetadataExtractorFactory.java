/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.iumj;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IUMJHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {

  static Logger log = Logger.getLogger(IUMJHtmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new IUMJHtmlMetadataExtractor();
  }

  public static class IUMJHtmlMetadataExtractor
      implements FileMetadataExtractor {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "<span[^>]*>\\s*([A-Z]+)\\s*</span>\\s*=\\s*" +
        "<span[^>]*>\\{</span>(.*?)<span[^>]*>\\}</span>",
        Pattern.CASE_INSENSITIVE);


    private static final Pattern PAGES_PATTERN = Pattern.compile(
        "([^&\\-\u2013]+)(?:&ndash;|&amp;ndash;|[\\-\u2013])(.+)");

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {

      ArticleMetadata am = new ArticleMetadata();

      BufferedReader bReader = null;
      try {
        // Read entire file into one string — the BibTeX block is all on one line
        bReader = new BufferedReader(cu.openForReading());
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bReader.readLine()) != null) {
          sb.append(line);
        }
        String content = sb.toString();

        Matcher m = FIELD_PATTERN.matcher(content);
        while (m.find()) {
          String key   = m.group(1).toUpperCase().trim();
          String value = m.group(2).trim();

          if (value.isEmpty()) {
            continue;
          }

          am.putRaw(key, value);

          switch (key) {
            case "TITLE":
              am.put(MetadataField.FIELD_ARTICLE_TITLE, value);
              break;
            case "AUTHOR":
              String[] byAnd = value.split("(?i)\\s+and\\s+");
              if (byAnd.length > 1) {
                for (String a : byAnd) {
                  String trimmed = a.trim();
                  if (!trimmed.isEmpty()) am.put(MetadataField.FIELD_AUTHOR, trimmed);
                }
              } else {
                for (String a : value.split(",")) {
                  String trimmed = a.trim();
                  if (!trimmed.isEmpty()) am.put(MetadataField.FIELD_AUTHOR, trimmed);
                }
              }
              break;
            case "YEAR":
              am.put(MetadataField.FIELD_DATE, value);
              break;
            case "VOLUME":
              am.put(MetadataField.FIELD_VOLUME, value);
              break;
            case "ISSUE":
              am.put(MetadataField.FIELD_ISSUE, value);
              break;
            case "PAGES":
              Matcher pm = PAGES_PATTERN.matcher(value);
              if (pm.find()) {
                am.put(MetadataField.FIELD_START_PAGE, pm.group(1).trim());
                am.put(MetadataField.FIELD_END_PAGE,   pm.group(2).trim());
              }
              break;
            case "ISSN":
              am.put(MetadataField.FIELD_ISSN, value);
              break;
            case "FJOURNAL":
              am.put(MetadataField.FIELD_PUBLICATION_TITLE, value);
              break;
            default:
              break;
          }
        }
      } catch (Exception e) {
        log.debug(e + " : Error reading/parsing OAI HTML citation file: "
            + cu.getUrl());
      } finally {
        if (bReader != null) {
          try { bReader.close(); } catch (IOException ignored) {}
        }
        AuUtil.safeRelease(cu);
      }

      emitter.emitMetadata(cu, am);
    }
  }
}

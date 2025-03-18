/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.stockholmup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class StockholmUniversityPressJsonMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(StockholmUniversityPressJsonMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new StockholmUniversityPressJsonMetadataExtractor();
  }

  public static class StockholmUniversityPressJsonMetadataExtractor implements FileMetadataExtractor {

    private ArticleMetadata am;

    static MultiMap jsonPathToMetadataField = new MultiValueMap();

    static {
      jsonPathToMetadataField.put("message.title", MetadataField.FIELD_ARTICLE_TITLE);
      jsonPathToMetadataField.put("bibjson.keywords", MetadataField.FIELD_KEYWORDS);
      jsonPathToMetadataField.put("message.volume", MetadataField.FIELD_VOLUME);
      jsonPathToMetadataField.put("message.journal-issue.issue", MetadataField.FIELD_ISSUE);
      jsonPathToMetadataField.put("message.language", MetadataField.FIELD_LANGUAGE);
      jsonPathToMetadataField.put("message.ISSN", MetadataField.FIELD_ISSN);
      jsonPathToMetadataField.put("message.publisher", MetadataField.FIELD_PUBLISHER);
      jsonPathToMetadataField.put("message.link.url", MetadataField.FIELD_ACCESS_URL);
      jsonPathToMetadataField.put("message.doi", MetadataField.FIELD_DOI);
      jsonPathToMetadataField.put("message.container-title", MetadataField.FIELD_PUBLICATION_TITLE);

    }

    static String stripBrackets(String value) {
      if (value.matches("\\[.*\\]")) {
        return value.substring(1, value.length() - 2);
      }
      return value;
    }

    // Function to extract date from "[2021, 4, 23]" format
    public static String extractDateParts(String rawDateParts) {
      // Remove brackets and whitespace, then split by comma
      String[] parts = rawDateParts.replaceAll("[\\[\\]\\s]", "").split(",");

      // Ensure we have at least a year (avoid errors on malformed data)
      if (parts.length >= 3) {
        return String.join("-", parts);  // Returns "YYYY-MM-DD"
      }
      return ""; // Return empty string if data is invalid
    }

    static String zipAuthors(String familiesRaw, String givensRaw) {
      // Remove the enclosing square brackets if present
      String familiesStr = stripBrackets(familiesRaw);
      String givensStr = stripBrackets(givensRaw);

      // Split the strings into arrays using comma as the separator
      String[] families = familiesStr.split(",\\s*");
      String[] givens = givensStr.split(",\\s*");

      // Check for a mismatch in array lengths, if needed
      if (families.length != givens.length) {
        throw new IllegalArgumentException("The number of family names and given names do not match.");
      }

      // Build the combined author list in the format "Family, Given"
      List<String> authors = new ArrayList<>();
      for (int i = 0; i < families.length; i++) {
        authors.add(families[i] + ", " + givens[i]);
      }

      // Join with " ; " separator and enclose in square brackets
      return "[" + String.join(" ; ", authors) + "]";
    }

    static void processKeys (String currentPath,
            JsonNode jsonNode,
            Map < String, String > map,
            List < Integer > suffix){
      if (jsonNode.isObject()) {
        ObjectNode objectNode = (ObjectNode) jsonNode;
        Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
        String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

        while (iter.hasNext()) {
          Map.Entry<String, JsonNode> entry = iter.next();
          processKeys(pathPrefix + entry.getKey(), entry.getValue(), map, suffix);
        }
      } else if (jsonNode.isArray()) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;

        for (int i = 0; i < arrayNode.size(); i++) {
          processKeys(currentPath, arrayNode.get(i), map, suffix);
        }

      } else if (jsonNode.isValueNode()) {
        ValueNode valueNode = (ValueNode) jsonNode;
        if (map.containsKey(currentPath)) {
          String oldValues = stripBrackets(map.get(currentPath));
          map.put(
                  currentPath,
                  "[" + oldValues + ", " + valueNode.asText() + "]"
          );
        } else {
          map.put(currentPath, valueNode.asText());
        }
      }
    }

      @Override
      public void extract (MetadataTarget target,
              CachedUrl cu,
              Emitter emitter)
        throws IOException, PluginException {

        am = new ArticleMetadata();

        String eissn = null;
        String authorFamilyNames = null;
        String authorGiveNames = null;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try (Reader reader = cu.openForReading()) {
          rootNode = objectMapper.readTree(reader);
        }

        Map<String, String> map = new HashMap<>();
        // add all json keys to a map
        processKeys("", rootNode, map, new ArrayList<>());

        for (Map.Entry<String, String> entry : map.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();

          if (entry.getKey().startsWith("message.issn-type") && entry.getValue().equals("electronic")) {
            String issnValueKey = entry.getKey().replace(".type", ".value");
            eissn = map.get(issnValueKey);
            am.put(MetadataField.FIELD_EISSN, eissn);
          }

          if (entry.getKey().startsWith("message.issn-type") && entry.getValue().equals("electronic")) {
            String issnValueKey = entry.getKey().replace(".type", ".value");
            eissn = map.get(issnValueKey);
            am.put(MetadataField.FIELD_EISSN, eissn);
          }

          if (entry.getKey().equals("message.published-online.date-parts") || entry.getKey().equals("message.published-print.date-parts")) {
            String dateParts = extractDateParts(value);
            if (!dateParts.isEmpty()) {
              am.put(MetadataField.FIELD_DATE, dateParts);
            }
          }

          am.putRaw(key, value);
        }
        
        String authorsList = zipAuthors(am.getRaw("message.author.given"), am.getRaw("message.author.family"));

        am.put(MetadataField.FIELD_AUTHOR, authorsList);

        am.cook(jsonPathToMetadataField);

        emitter.emitMetadata(cu, am);
    }
  }
}

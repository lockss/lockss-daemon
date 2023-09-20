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

package org.lockss.plugin.jasper;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.apache.commons.collections.MultiMap;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/*
{
  "last_updated": "2020-11-24T21:55:20Z",
  "created_date": "2018-11-04T12:37:46Z",
  "id": "00003741594643f4996e2555a01e03c7",
  "bibjson": {
    "title": "Variation in TMEM106B in chronic traumatic encephalopathy",
    "year": "2018",
    "month": "11",
    "start_page": "1",
    "end_page": "9",
    "abstract": "Abstract The genetic basis of chronic traumatic encephalopathy (CTE) is poorly understood. Variation in transmembrane protein 106B (TMEM106B) has been associated with enhanced neuroinflammation during aging and with TDP-43-related neurodegenerative disease, and rs3173615, a missense coding SNP in TMEM106B, has been implicated as a functional variant in these processes. Neuroinflammation and TDP-43 pathology are prominent features in CTE. The purpose of this study was to determine whether genetic variation in TMEM106B is associated with CTE risk, pathological features, and ante-mortem dementia. Eighty-six deceased male athletes with a history of participation in American football, informant-reported Caucasian, and a positive postmortem diagnosis of CTE without comorbid neurodegenerative disease were genotyped for rs3173615. The minor allele frequency (MAF = 0.42) in participants with CTE did not differ from previously reported neurologically normal controls (MAF = 0.43). However, in a case-only analysis among CTE cases, the minor allele was associated with reduced phosphorylated tau (ptau) pathology in the dorsolateral frontal cortex (DLFC) (AT8 density, odds ratio [OR] of increasing one quartile = 0.42, 95% confidence interval [CI] 0.22–0.79, p = 0.008), reduced neuroinflammation in the DLFC (CD68 density, OR of increasing one quartile = 0.53, 95% CI 0.29–0.98, p = 0.043), and increased synaptic protein density (β = 0.306, 95% CI 0.065–0.546, p = 0.014). Among CTE cases, TMEM106B minor allele was also associated with reduced ante-mortem dementia (OR = 0.40, 95% CI 0.16–0.99, p = 0.048), but was not associated with TDP-43 pathology. All case-only models were adjusted for age at death and duration of football play. Taken together, variation in TMEM106B may have a protective effect on CTE-related outcomes.",
    "journal": {
      "volume": "6",
      "number": "1",
      "publisher": "BMC",
      "title": "Acta Neuropathologica Communications",
      "country": "GB",
      "license": [
        {
          "title": "CC BY",
          "type": "CC BY",
          "url": "https://actaneurocomms.biomedcentral.com/submission-guidelines/copyright",
          "open_access": true
        }
      ],
      "language": [
        "EN"
      ],
      "issns": [
        "2051-5960"
      ]
    },
    "identifier": [
      {
        "type": "doi",
        "id": "10.1186/s40478-018-0619-9"
      },
      {
        "type": "eissn",
        "id": "2051-5960"
      }
    ],
    "keywords": [
      "Keyword",
    ],
    "link": [
      {
        "type": "fulltext",
        "url": "http://link.springer.com/article/10.1186/s40478-018-0619-9",
        "content_type": "HTML"
      }
    ],
    "subject": [
      {
        "scheme": "LCC",
        "term": "Neurology. Diseases of the nervous system",
        "code": "RC346-429"
      }
    ],
    "author": [
      {
        "name": "Jonathan D. Cherry",
        "affiliation": "Boston University Alzheimer’s Disease and CTE Center, Boston University School of Medicine"
      }, ...
    ]
  }
}
*/

public class JasperJsonMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(JasperJsonMetadataExtractorFactory.class);


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new JasperJsonMetadataExtractor();
  }

  public static class JasperJsonMetadataExtractor implements FileMetadataExtractor {

    private ArticleMetadata am;
    private String currentTag;
    private StringBuilder currentValue;

    static MultiMap jsonPathToMetadataField = new MultiValueMap();

    static {
      jsonPathToMetadataField.put("created_date", MetadataField.FIELD_DATE);
      jsonPathToMetadataField.put("bibjson.abstract", MetadataField.FIELD_ABSTRACT);
      jsonPathToMetadataField.put("bibjson.start_page", MetadataField.FIELD_START_PAGE);
      jsonPathToMetadataField.put("bibjson.end_page", MetadataField.FIELD_END_PAGE);
      jsonPathToMetadataField.put("bibjson.title", MetadataField.FIELD_ARTICLE_TITLE);
      jsonPathToMetadataField.put("bibjson.keywords", MetadataField.FIELD_KEYWORDS);
      jsonPathToMetadataField.put("bibjson.journal.volume", MetadataField.FIELD_VOLUME);
      jsonPathToMetadataField.put("bibjson.journal.title", MetadataField.FIELD_PUBLICATION_TITLE);
      jsonPathToMetadataField.put("bibjson.journal.language", MetadataField.FIELD_LANGUAGE);
      jsonPathToMetadataField.put("bibjson.journal.issns", MetadataField.FIELD_ISSN);
      jsonPathToMetadataField.put("bibjson.journal.publisher", MetadataField.FIELD_PUBLISHER);
      jsonPathToMetadataField.put("bibjson.link.url", MetadataField.FIELD_ACCESS_URL);
      jsonPathToMetadataField.put("bibjson.author.name", MetadataField.FIELD_AUTHOR);
    }

    static String stripBrackets(String value) {
      if (value.matches("\\[.*\\]")) {
        return value.substring(1, value.length() - 2);
      }
      return value;
    }

    /*
     * For particular "Item", an issn/issns will come from three resources:
     * 1. tdb file (1 or more), name it tdb_issn
     * 2. publisher ".json" file (1 or more), name it json_issn
     * 3. "url" inside tar file (only 1 for any givne directory). issn appended to directory name itself, is not reliable, name it url_issn
     *
     */


    static Boolean compareTdbInfoWithJsonInfo(String tdbIssn, List<String> jsonIssns) {

      Boolean issnInfoMatch = false;

      if (tdbIssn != null) {
        for (String issn : jsonIssns) {
          if (issn.equals(tdbIssn)) {
            issnInfoMatch = true;
            break;
          }
        }
      }

      return issnInfoMatch;
    }

    static Boolean compareSrcUrlInfoWithJsonInfo(String issnFromSrcUrl, List<String> jsonIssns) {

      Boolean issnInfoMatch = false;

      if (issnFromSrcUrl != null ) {
        for (String issn : jsonIssns) {
          if (issn.equals(issnFromSrcUrl)) {
            issnInfoMatch = true;
            break;
          }
        }
      }

      return issnInfoMatch;
    }

    static String getIssnFromSrcUrl(String srcUrl) {

      //https://archive.org/download/Daysona_Life_Science_2708-6291/27086283-2022-10-01-05-17-46.tar.gz!/27086283-2022-10-01-05-17-46/2708-6283/99ecd4071a21457e90ac4fedf7c75978/data/metadata/metadata.json
      //Expected "2708-6283" inside the tar file
      Pattern issnPattern = Pattern.compile(".*/(\\d{4}-\\d{3}[0-9X])/.*");

      Matcher issnMatcher = issnPattern.matcher(srcUrl);
      String issnFromSrcUrl = null;

      if (issnMatcher.find()) {
        log.debug3("Handle issns: single issns matching pattern,  srcUrl = " + srcUrl);
        int groupCount = issnMatcher.groupCount();

        if (groupCount >= 1) {
          issnFromSrcUrl = issnMatcher.group(1);
        } else {
          log.debug3("Handle issns: single issns NOTs matching pattern, srcUrl = " + srcUrl);
        }
      } else {
        log.debug3("Handle issns: single issns NOTs matching pattern, srcUrl = " + srcUrl);
      }
      return issnFromSrcUrl;
    }


    static String getIdentifierIdFromJsonFile(JsonNode jsonNode, String identifierType) {
      try {
        JsonNode identifierArray = jsonNode.at("/bibjson/identifier");

        for (JsonNode identifier : identifierArray) {
          String type = identifier.at("/type").asText();
          if (type.equals(identifierType)) {
            return identifier.at("/id").asText();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      return null; // Return null if identifier with given type is not found
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
        //
        log.debug2("Parsing " + cu.getUrl());

        am = new ArticleMetadata();
        currentTag = null;
        currentValue = new StringBuilder();
        boolean hasBegun = false;

        String tdbItemName = null;
        String tdbIssn = null;
        String tdbEissn = null;
        String issnFromSrcUrl = null;
        String issnFromJsonPissn = null;
        String eissnFromJsonEissn = null;
        String doiFromJson = null;

        Boolean tdbIssnMatchJson = false;
        Boolean tdbEissnMatchJson = false;
        Boolean issnFromSrcUrlMatchJson = false;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try (Reader reader = cu.openForReading()) {
          rootNode = objectMapper.readTree(reader);
        }
        Map<String, String> map = new HashMap<>();
        // add all json keys to a map
        processKeys("", rootNode, map, new ArrayList<>());

        String srcUrl = cu.getUrl();
        issnFromSrcUrl = getIssnFromSrcUrl(srcUrl);

        issnFromJsonPissn = getIdentifierIdFromJsonFile(rootNode, "pissn");
        eissnFromJsonEissn = getIdentifierIdFromJsonFile(rootNode, "eissn");
        doiFromJson = getIdentifierIdFromJsonFile(rootNode, "doi").replace("https://doi.org/", "");

        if (doiFromJson != null) {
          am.put(MetadataField.FIELD_DOI, doiFromJson);
        }

        log.debug3("Handle issns: getIdentifierIdFromJsonFile, issnFromJsonPissn = " + issnFromJsonPissn + ", eissnFromJsonEissn = " + eissnFromJsonEissn);


        TdbAu tdbau = cu.getArchivalUnit().getTdbAu();

        if (tdbau != null) {
          tdbItemName = tdbau.getParam("item");
          tdbIssn = tdbau.getIssn();
          tdbEissn = tdbau.getEissn();

          log.debug3("Handle issns: tdbAu is not null, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn);
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();

          //Colombo_Business_Journal_-_2579-2210/8bfc2597-2023-01-05-02-45-00.tar/8bfc2597-2023-01-05-02-45-00/1800-363X/d65087ccb4e043c299cb5e8e72c40e98/data/metadata/metadata.json
          //            "issns": [
          //                "1800-363X",
          //                "2579-2210"
          //            ]
          //santander_estudios_de_patrimonio_2605-5317/26054450-2023-03-24-08-22-25.tar.gz!/26054450-2023-03-24-08-22-25/2605-4450/5bcea626423f42f596a7dfe5c67ce620/data/metadata/metadata.json
          //            "issns": [
          //                "2605-4450",
          //                "2605-5317"
          //            ]
          //Daysona_Life_Science_2708-6291/27086283-2022-10-08-19-03-38.tar/27086283-2022-10-08-19-03-38/2708-6291/23fdd6ebec514db79b687d3da502a6ba/data/metadata/metadata.json
          //            "issns": [
          //                "2708-6291",
          //                "2708-6283"
          //            ]

          if (key.contains("issn")) {

            List<String> jsonIssns = new ArrayList<>();

            // "issns" is array, has more than 1 issn
            if (value.contains("[") && value.contains("]") && value.contains(",")) {
              Pattern pattern = Pattern.compile("\\[(.*?)\\]");

              // Match the pattern against the input string
              Matcher matcher = pattern.matcher(value);

              if (matcher.find()) {
                String matchedString = matcher.group(1);

                // Split the matched string by commas
                jsonIssns = Arrays.asList(matchedString.split(", "));

                tdbIssnMatchJson = compareTdbInfoWithJsonInfo(tdbIssn, jsonIssns);
                tdbEissnMatchJson = compareTdbInfoWithJsonInfo(tdbEissn, jsonIssns);
                issnFromSrcUrlMatchJson = compareSrcUrlInfoWithJsonInfo(issnFromSrcUrl , jsonIssns);


                if (tdbIssnMatchJson && tdbEissnMatchJson && issnFromSrcUrlMatchJson) {
                  // All three resources matches, follow normal metadata flow
                  log.debug3("Handle issns: : Multi value, tdbIssnMatchJson && tdbEissnMatchJson && issnFromSrcUrlMatchJson: tdbIssnEissnMatchJson && issnFromSrcUrlMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);

                } else if (tdbIssnMatchJson && tdbEissnMatchJson) {
                  // Two resources matches, follow normal metadata flow
                  log.debug3("Handle issns: :  Multi value, tdbIssnMatchJson && tdbEissnMatchJson: tdbIssnEissnMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);
                } else {
                  // Two resources matches, ONLY USE JSON DATA FOR METADATA
                  log.debug3("Handle issns: :  Multi value, issnFromSrcUrlMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);
                }

              } else {
                log.debug3("Handle issns: Multile issn found, NOT match pattern, key =  " + key + ", val = " + value + ", srcUrl = " + srcUrl);
              }
            } else {
              // "issns" is single value, still need to check if it is the same with the directory
              jsonIssns.add(value);

              tdbIssnMatchJson = compareTdbInfoWithJsonInfo(tdbIssn, jsonIssns);
              tdbEissnMatchJson = compareTdbInfoWithJsonInfo(tdbEissn, jsonIssns);
              issnFromSrcUrlMatchJson = compareSrcUrlInfoWithJsonInfo(issnFromSrcUrl, jsonIssns);


              if (tdbIssnMatchJson && issnFromSrcUrlMatchJson) {
                // All three resources matches, follow normal metadata flow
                log.debug3("Handle issns: Single value, tdbIssnMatchJson && issnFromSrcUrlMatchJson: tdbIssnEissnMatchJson && issnFromSrcUrlMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);
              } else if (tdbEissnMatchJson && issnFromSrcUrlMatchJson) {
                // Two resources matches, follow normal metadata flow
                log.debug3("Handle issns: Single value, tdbEissnMatchJson && issnFromSrcUrlMatchJson: tdbIssnEissnMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);
              } else {
                // Two resources matches, ONLY USE JSON DATA FOR METADATA
                log.debug3("Handle issns: Single value, issnFromSrcUrlMatchJson, tdbItemName = " + tdbItemName + ", tdbIssn = " + tdbIssn + ", tdbEissn = " + tdbEissn + ", issnFromSrcUrl = " + issnFromSrcUrl);
              }
            }

            if (tdbIssnMatchJson) {
              log.debug3("=====Handle issns: setting Issn value from TDB Issn to " + tdbIssn);
              am.put(MetadataField.FIELD_ISSN, tdbIssn);
            }
            if (tdbEissnMatchJson) {
              log.debug3("====Handle issns: setting Eissn value to TDB Eissn to " + tdbEissnMatchJson);
              am.put(MetadataField.FIELD_EISSN, tdbEissn);
            }
            if (issnFromSrcUrlMatchJson && tdbIssnMatchJson == false && tdbEissnMatchJson == false ){
              // ONLY USE JSON DATA FOR METADATA
              log.debug3("=====Handle issns: setting value from JSON") ;
              if  (issnFromJsonPissn != null) {
                log.debug3("=====Handle issns: setting ISSN value from JSON issnFromJsonPissn = " +  issnFromSrcUrl);
                am.put(MetadataField.FIELD_ISSN, issnFromJsonPissn);

                if ( eissnFromJsonEissn == null) {
                  log.debug3("=====Handle issns: setting ESSIN value from JSON issnFromJsonPissn = " +  eissnFromJsonEissn);
                  am.put(MetadataField.FIELD_EISSN, issnFromJsonPissn);
                }
              }
              if ( eissnFromJsonEissn != null) {
                log.debug3("=====Handle issns: setting EISSN value from JSON eissnFromJsonEissn = " +  eissnFromJsonEissn);
                am.put(MetadataField.FIELD_EISSN, eissnFromJsonEissn);

                if  (issnFromJsonPissn == null) {
                  log.debug3("=====Handle issns: setting ISSN value from JSON eissnFromJsonEissn = = " +  issnFromSrcUrl);
                  am.put(MetadataField.FIELD_ISSN, eissnFromJsonEissn);

                }
              }
            }
            // if issnFromSrcUrlMatchJson is false, which means the issn/eissn of the folder is does not match any pissn/eissn in JSON
            // Take it as mis-package from the publisher?
          } else {

            am.putRaw(key, value);

            log.debug3("Handle issns: Putraw key =  " + key + ", val = " + value + ", srcUrl = " + srcUrl);
          }
        }
        am.cook(jsonPathToMetadataField);

        emitter.emitMetadata(cu, am);
    }
  }
}

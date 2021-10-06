package org.lockss.plugin.jasper;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.apache.commons.collections.MultiMap;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

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

  static MultiMap jsonPathToMetadataField = new MultiValueMap();

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    JasperJsonMetadataExtractor jjme = new JasperJsonMetadataExtractor();
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
    return jjme;
  }

  public static class JasperJsonMetadataExtractor implements FileMetadataExtractor {

    private ArticleMetadata am;
    private String currentTag;
    private StringBuilder currentValue;


    static String stripBrackets(String value) {
      if (value.matches("\\[.*\\]") ) {
        return value.substring(1,value.length()-2);
      }
      return value;
    }

    static void processKeys(String currentPath,
                            JsonNode jsonNode,
                            Map<String, String> map,
                            List<Integer> suffix) {
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
    public void extract(MetadataTarget target,
                        CachedUrl cu,
                        Emitter emitter)
        throws IOException, PluginException {
      //
      log.debug2("Parsing " + cu.getUrl());

      am = new ArticleMetadata();
      currentTag = null;
      currentValue = new StringBuilder();
      boolean hasBegun = false;

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
        am.putRaw(key, value);
      }
      am.cook(jsonPathToMetadataField);
      emitter.emitMetadata(cu, am);
    }
  }

}

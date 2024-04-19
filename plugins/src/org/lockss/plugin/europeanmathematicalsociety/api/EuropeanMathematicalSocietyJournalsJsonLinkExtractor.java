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

package org.lockss.plugin.europeanmathematicalsociety.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/*
https://content.ems.press/serial-issues?filter[serial]=11&filter[volume]=25

{
    "data": [
        {
            "type": "serial-issues",
            "id": "2053",
            "attributes": {
                "createdAt": "2023-03-04T04:30:45.013Z",
                "updatedAt": "2023-12-11T08:47:52.360Z",
                "serialType": "journal",
                "name": "Journal of the European Mathematical Society",
                "nameAbbreviation": "J. Eur. Math. Soc.",
                "doiPrefix": "10.4171/jems",
                "issn": "1435-9855",
                "eIssn": "1435-9863",
                "mscs": [],
                "keywords": [],
                "owner": "European Mathematical Society",
                "ownerUrl": "https://euromathsoc.org/",
                "publishingModel": "s2o",
                "s2oStatus": "open-access",
                "s2oStatusStart": "2023-01-01T00:00:00.000Z",
                "s2oStatusEnd": "2023-12-31T00:00:00.000Z",
                "publisher": "EMS Press",
                "publisherOrganization": "European Mathematical Society - EMS - Publishing House GmbH",
                "publisherAddress": "Straße des 17. Juni 136, 10623 Berlin",
                "publisherUrl": "https://ems.press",
                "copyright": "© European Mathematical Society",
                "copyrightUrl": "https://euro-math-soc.eu/",
                "codeOfPractice": "EMS Code of Practice",
                "codeOfPracticeUrl": "https://euromathsoc.org/code-of-practice",
                "volumesPerYear": 1,
                "issuesPerVolume": 12,
                "pagesPerYear": 4000,
                "printWidthCm": 17,
                "printHeightCm": 24,
                "year": 2023,
                "volume": 25,
                "issue": 1,
                "issueCount": 1,
                "pageStart": 1,
                "pageEnd": 368,
                "publishedAt": "2023-03-04T00:00:00.000Z",
                "publishedAtPrecision": "day"
            },
            "relationships": {
                "articles": {
                    "links": {
                        "self": "https://content.ems.press/serial-issues/2053/relationships/articles"
                    },
                    "data": [
                        {
                            "type": "serial-articles",
                            "id": "3476684"
                        },
                        {
                            "type": "serial-articles",
                            "id": "4103563"
                        },
                        {
                            "type": "serial-articles",
                            "id": "3324288"
                        },
                        {
                            "type": "serial-articles",
                            "id": "3731346"
                        },
                        {
                            "type": "serial-articles",
                            "id": "4732564"
                        },
                        {
                            "type": "serial-articles",
                            "id": "3744107"
                        }
                    ]
                },
                "coverImage": {
                    "links": {
                        "self": "https://content.ems.press/serial-issues/2053/relationships/coverImage"
                    },
                    "data": {
                        "type": "images",
                        "id": "31366"
                    }
                },
                "personGroups": {
                    "links": {
                        "self": "https://content.ems.press/serial-issues/2053/relationships/personGroups"
                    },
                    "data": []
                },
                "serial": {
                    "links": {
                        "self": "https://content.ems.press/serial-issues/2053/relationships/serial"
                    },
                    "data": {
                        "type": "serials",
                        "id": "11"
                    }
                },
                "serialIssueFiles": {
                    "links": {
                        "self": "https://content.ems.press/serial-issues/2053/relationships/serialIssueFiles"
                    },
                    "data": []
                }
            },
            "links": {
                "self": "https://content.ems.press/serial-issues/2053"
            }
        }
    ]

}



 */
public class EuropeanMathematicalSocietyJournalsJsonLinkExtractor implements LinkExtractor {


  public static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyJournalsJsonLinkExtractor.class);

  public EuropeanMathematicalSocietyJournalsJsonLinkExtractor() {
    log.debug3("EuropeanMathematicalSocietyJournalsJsonLinkExtractor2 is called");
  }


  @Override
  public void extractUrls(ArchivalUnit au, InputStream in, String encoding, String srcUrl, Callback cb) throws IOException, PluginException {

    log.debug3("Parsing " + srcUrl);



    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    Scanner scanner = new Scanner(in);

    try {
      scanner.useDelimiter("\\A");
      String inputString = scanner.hasNext() ? scanner.next() : "";

      log.debug3("srcUrl = " + srcUrl + ", encoding = " + encoding + ", input = " + inputString);

      rootNode = objectMapper.readTree(inputString);

      // Check if the JSON response matches the criteria
      if (rootNode.isObject() && !rootNode.fields().hasNext()) {
        log.debug3("JSON response is an empty JSON object, srcUrl = " + srcUrl);
      } else if (rootNode.has("error") && rootNode.get("error").isTextual() && rootNode.get("error").asText().equals("Not found")) {
        log.debug3("JSON response Contains error: Not found , srcUrl = " + srcUrl);
      } else {

        JsonNode dataNode = rootNode.get("data");
        int i = 0;
        for (JsonNode node : dataNode) {
          i = i + 1;
          String type = node.get("type").asText();
          if (type.equals("serial-issues")) {
            String serialID = node.get("id").asText();

            //https://content.ems.press/serial-articles?filter[serialIssue]=2054

            String queryString = "serial-articles?filter[serialIssue]=";


            String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
            String APIUrl = au.getConfiguration().get("api_url");
            String journalID = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
            String serialQueryString = APIUrl + queryString + serialID;

            String webIssueUrl = baseUrl + "journals/" + journalID + "/issues/" + serialID;

            log.debug3("JSON response found serial-issues, issue_id = " + serialID
                            + ", serialQueryString = " + serialQueryString
                            + ", webIssueUrl = " + webIssueUrl);

            cb.foundLink(serialQueryString);
            cb.foundLink(webIssueUrl);
          }
        }

      }
    } catch (IOException e) {
      log.error("IOException occurred while reading input stream: " + e.getMessage());
    } finally {
      // Close the scanner
      scanner.close();
    }
  }
}

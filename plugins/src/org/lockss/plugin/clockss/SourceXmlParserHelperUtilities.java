/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class SourceXmlParserHelperUtilities {

    /**
     * This function return string representation of publication date from particular xpath selected xml node
     * @param node
     *      <PubDate>
     *         <Year>2019</Year>
     *         <Month>01</Month>
     *         <Day>01</Day>
     *     </PubDate>
     * @return a string representation of pubdate "mm-dd-Year"
     */
    public static String getPubDateFromPubDateXpathNodeValue(Node node) {

       NodeList nameChildren = node.getChildNodes();
        if (nameChildren == null) return null;

        String year = null;
        String month = null;
        String day = null;

        if (nameChildren == null) return null;
        for (int p = 0; p < nameChildren.getLength(); p++) {
            Node partNode = nameChildren.item(p);
            String partName = partNode.getNodeName();
            if ("Year".equalsIgnoreCase(partName)) {
                year  = partNode.getTextContent();
            } else if ("Month".equalsIgnoreCase(partName)) {
                month = partNode.getTextContent();
            } else if ("Day".equalsIgnoreCase(partName)) {
                day = partNode.getTextContent();
            }
        }
        StringBuilder valbuilder = new StringBuilder();
        if (!StringUtils.isBlank(month)) {
            valbuilder.append(month);
            if (!StringUtils.isBlank(day)) {
                valbuilder.append("-" + day);
            }
            if (!StringUtils.isBlank(year)) {
                valbuilder.append("-" + year);
            }
            return valbuilder.toString();
        }
        return null;
    }

    /**
     * This function return string representation of author name from particular xpath selected xml node
     * @param node
     *         <Author ValidYN="Y">
     *             <LastName>Artzi</LastName>
     *             <ForeName>Ofir</ForeName>
     *             <Initials>O</Initials>
     *             <AffiliationInfo>
     *                 <Affiliation>Department of Dermatology, Tel Aviv Medical Center, Tel Aviv, 6423906, Israel. benofir@gmail.com.</Affiliation>
     *             </AffiliationInfo>
     *         </Author>
     * @return a string representation of author's name "firstname lastname"
     */
    public static String getAuthorNameFromAuthorNameXpathNodeValue(Node node) {
        NodeList nameChildren = node.getChildNodes();
        if (nameChildren == null) return null;

        String surname = null;
        String firstname = null;

        if (nameChildren == null) return null;
        for (int p = 0; p < nameChildren.getLength(); p++) {
            Node partNode = nameChildren.item(p);
            String partName = partNode.getNodeName();
            if ("LastName".equalsIgnoreCase(partName)) {
                surname  = partNode.getTextContent();
            } else if ("SurName".equalsIgnoreCase(partName)) {
                surname = partNode.getTextContent();
            } else if ("SurName".equalsIgnoreCase(partName)) {
                surname = partNode.getTextContent();
            } else if ("ForeName".equalsIgnoreCase(partName)) {
                firstname = partNode.getTextContent();
            } else if ("FirstName".equalsIgnoreCase(partName)) {
                firstname = partNode.getTextContent();
            } else if ("given_name".equalsIgnoreCase(partName)) {
                firstname = partNode.getTextContent();
            }
        }
        StringBuilder valbuilder = new StringBuilder();
        if (!StringUtils.isBlank(firstname)) {
            valbuilder.append(firstname);
            if (!StringUtils.isBlank(surname)) {
                valbuilder.append(" " + surname);
            }
            return valbuilder.toString();
        }
        return null;
    }
}

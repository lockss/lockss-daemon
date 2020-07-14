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

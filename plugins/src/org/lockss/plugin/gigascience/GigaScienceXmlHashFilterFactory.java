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
package org.lockss.plugin.gigascience;
import java.io.IOException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.DeferredTempFileOutputStream;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GigaScienceXmlHashFilterFactory implements FilterFactory{

    private static final Logger logger = Logger.getLogger(GigaScienceXmlHashFilterFactory.class); 

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in, String encoding) throws PluginException{

        try{
            Document doc = createDoc(in);

            NodeList filesNodeList = doc.getElementsByTagName("files");

            NodeList sampleAttributesList = doc.getElementsByTagName("sample_attributes");

            if(filesNodeList != null && filesNodeList.getLength() != 0){

                //there should be only one files tag
                if(filesNodeList.getLength() > 1){
                    throw new PluginException("There should only be one files tag in document.");
                }
                
                Node filesNode = filesNodeList.item(0); 
                NodeList fileNodes = doc.getElementsByTagName("file");
                List<Element> sortedFileNodes = sortFileNodes(fileNodes); 

                for(int i=0; i < fileNodes.getLength(); i++){
                    //logger.debug3("You are in the files node. You are about to remove: " + fileNodes.item(i));
                    filesNode.removeChild(fileNodes.item(i));
                }
                
                for(Element sortedFileNode: sortedFileNodes){
                    sortedFileNode.removeAttribute("download_count");
                    filesNode.appendChild(sortedFileNode);
                }
            }

            if(sampleAttributesList != null && sampleAttributesList.getLength() != 0){
                for(int i=0; i < sampleAttributesList.getLength(); i++){
                    Node sampleAttributesNode = sampleAttributesList.item(i);
                    //logger.debug3("You are currently in the sample_attributes tag." + sampleAttributesNode.getTextContent());
                    NodeList attributeNodes = sampleAttributesNode.getChildNodes();
                    List<Element> sortedAttributeNodes = sortAttributeNodes(attributeNodes);
                    for(int j=0; j < attributeNodes.getLength(); j++){
                        sampleAttributesNode.removeChild(attributeNodes.item(j));
                    }

                    for(Element sortedAttributeNode:sortedAttributeNodes){
                        sampleAttributesNode.appendChild(sortedAttributeNode);
                    }
                    //logger.debug3("You are currently in the sample_attributes tag, after sorting." + sampleAttributesNode.getTextContent());
                }

            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            DeferredTempFileOutputStream out = new DeferredTempFileOutputStream(1024*1024);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            return out.getDeleteOnCloseInputStream();
        }
        catch (PluginException | ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.getStackTrace();
            throw new PluginException(e);
        }
    }

    private static List<Element> sortFileNodes(NodeList nodes) {
        List<Element> sortedNodes = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            sortedNodes.add((Element)nodes.item(i));
        }
        
        Collections.sort(sortedNodes, new Comparator<Element>() {
            public int compare(Element o1, Element o2) {
                return o1.getAttribute("id").compareTo(
                        o2.getAttribute("id"));
            }
        });
        return sortedNodes;
    }
    private static List<Element> sortAttributeNodes(NodeList nodes) {
        List<Element> sortedNodes = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            //make sure that nodes have content, otherwise the Element case will throw an error
            if(nodes.item(i).hasChildNodes()){
                //logger.debug("You are adding nodes to the sortedNodes list. You are adding: " + nodes.item(i).getTextContent());
                sortedNodes.add((Element)nodes.item(i));
            }
        } 
        
        Collections.sort(sortedNodes, new Comparator<Element>() {
            public int compare(Element o1, Element o2) {
                return o1.getTextContent().compareTo(o2.getTextContent());
            }
        });
        return sortedNodes;
    }

    //create document object from input stream
    public Document createDoc(InputStream in) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document doc = builder.parse(in);
        return doc;
    }
    
}
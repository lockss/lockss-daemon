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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.DeferredTempFileOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GigaScienceXmlHashFilterFactory implements FilterFactory{

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in, String encoding)
            throws PluginException {
        
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(in);
            NodeList filesNodeList = doc.getElementsByTagName("files");

            //there should be only one files tag
            if(filesNodeList.getLength() != 1){
                //handle exception
            }

            Node filesNode = filesNodeList.item(0); 
            NodeList nodes = doc.getElementsByTagName("file");
            System.out.println("Node is" + nodes.item(0).getTextContent());
            List<Element> sortedElements = sort(nodes); 
            System.out.println("Node is" + sortedElements.get(0).getAttribute("id"));

            for(int i=0; i < nodes.getLength(); i++){
                filesNode.removeChild(nodes.item(i));
            }

            for(Element sortedElement: sortedElements){
                filesNode.appendChild(sortedElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            DeferredTempFileOutputStream out = new DeferredTempFileOutputStream(1024*1024);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
            return out.getDeleteOnCloseInputStream();

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
        return in;
    }

    private static List<Element> sort(NodeList nodes) {
        List<Element> sortedNodes = new ArrayList<Element>();
        for (int i = 0; i < nodes.getLength(); i++) {
            sortedNodes.add((Element) nodes.item(i));
        }
        
        Collections.sort(sortedNodes, new Comparator<Element>() {
            public int compare(Element o1, Element o2) {
                //System.out.println(o1.getAttribute("id"));
                return o1.getAttribute("id").compareTo(
                        o2.getAttribute("id"));
            }
        });

        return sortedNodes;
    }
    
}
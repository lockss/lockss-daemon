/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.impl.rest;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class DefaultXmlHandler extends DefaultHandler {

    private StringBuffer currText = null;

        public void startDocument() {}

        public void endDocument() {}

        public void startElement(String uri, String name, String qName, Attributes attrs) {
            this.currText = new StringBuffer();
            this.startElement(name, attrs);
        }

        public void startElement(String name, Attributes attrs) {
             this.startElement(name);
         }

        public void startElement(String name) { }

        public void endElement(String uri, String name, String qName) {
            String elementText = this.currText.toString();
            this.endElement(name, elementText);
        }

        public void endElement(String name, String content) { }

        public void characters(char ch[], int start, int length) {
            this.currText.append(ch, start, length);
        }
}

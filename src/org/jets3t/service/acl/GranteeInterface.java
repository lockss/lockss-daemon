/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
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
package org.jets3t.service.acl;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents a grantee (entity) who can be assigned access permissions in an {@link AccessControlList}.
 * All grantees have an ID of some kind (though the format of the ID can differ depending on the kind
 * of grantee) and can be represented as an XML fragment suitable for use by the S3 REST implementation.
 *
 * @author James Murty
 */
public interface GranteeInterface {

    /**
     * @return
     * the grantee represented in an XML fragment compatible with the S3 REST interface.
     */
    public String toXml() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError;

    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError;

    public void setIdentifier(String id);

    public String getIdentifier();

}

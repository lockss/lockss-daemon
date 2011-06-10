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
package org.jets3t.service.acl.gs;

import com.jamesmurty.utils.XMLBuilder;

import org.jets3t.service.acl.GroupGrantee;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Represents a grant to all authenticated users.
 *
 * @author Google Developers
 *
 */
public class AllAuthenticatedUsersGrantee extends GroupGrantee {
    private final String id = "AllAuthenticatedUsers";

    @Override
    public String toXml() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return toXMLBuilder().asString();
    }

    @Override
    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return XMLBuilder.create("Scope").attr("type", "AllAuthenticatedUsers");
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AllAuthenticatedUsersGrantee);
    }

    @Override
    public String toString() {
        return id;
    }
}

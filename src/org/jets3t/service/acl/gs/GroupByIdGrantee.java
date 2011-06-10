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

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jets3t.service.acl.GroupGrantee;

/**
 * Represents a Group grantee.
 *
 * @author Google Developers
 *
 */
public class GroupByIdGrantee extends GroupGrantee {
    private String name;

    public GroupByIdGrantee() {
      super();
    }

    /**
     * Constructs a group grantee object using the given group id as an identifier.
     *
     * @param id
     */
    public GroupByIdGrantee(String id) {
        super(id);
    }

    public GroupByIdGrantee(String identifier, String name) {
        super(identifier);
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        XMLBuilder builder =
            XMLBuilder.create("Scope")
                .attr("type", "GroupById")
                .element("ID").text(getIdentifier()).up();
        if (getName() != null) {
            builder.element("Name").text(getName());
        }
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof GroupByIdGrantee
                && ((GroupByIdGrantee)obj).getIdentifier() == this.getIdentifier());
    }

    @Override
    public String toString() {
        return "GroupById [" + id
            + (name != null ? ", name=" + getName() : "")
            + "]";
    }
}

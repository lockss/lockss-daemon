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

import org.jets3t.service.acl.CanonicalGrantee;

/**
 * Represents a grantee identified by their canonical Google ID, which is something along the lines
 * of a Google-internal ID specific to a user. For example, Google can map a grantee identified
 * by an email address to a canonical ID.
 * <p>
 * Canonical grantees may have an associated Display Name, which is a human-friendly name that
 * Google has linked to the canonical ID (eg the user's login name).
 *
 * @author Google Developers
 *
 */
public class UserByIdGrantee extends CanonicalGrantee {

    /**
     * Default constructor.
     * <p>
     * <b>Warning!</b> If created with this constructor this class will not
     * represent a valid grantee until the identifier is set.
     */
    public UserByIdGrantee() {
      super();
    }

    /**
     * Constructs a grantee with the given canonical ID.
     * @param identifier
     */
    public UserByIdGrantee(String identifier) {
        super(identifier);
    }

    public UserByIdGrantee(String identifier, String name) {
        super(identifier);
        setName(name);
    }

    public String getName() {
        return getDisplayName();
    }

    public void setName(String name) {
        setDisplayName(name);
    }

    @Override
    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return (XMLBuilder.create("Scope")
            .attr("type", "UserById")
            .element("ID").text(id)
            );
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof UserByIdGrantee
                && ((UserByIdGrantee)obj).getIdentifier() == this.getIdentifier());
    }

    @Override
    public String toString() {
        return "UserById [id=" + id
            + (displayName != null ? ", Name=" + displayName : "")
            + "]";
    }
}

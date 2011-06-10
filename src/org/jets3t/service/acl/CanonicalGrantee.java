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

import com.jamesmurty.utils.XMLBuilder;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Represents a grantee identified by their canonical Amazon ID, which is something along the lines
 * of an Amazon-internal ID specific to a user. For example, Amazon can map a grantee identified
 * by an email address to a canonical ID.
 * <p>
 * Canonical grantees may have an associated Display Name, which is a human-friendly name that
 * Amazon has linked to the canonical ID (eg the user's login name).
 *
 * @author James Murty
 *
 */
public class CanonicalGrantee implements GranteeInterface {
    protected String id = null;
    protected String displayName = null;

    /**
     * Default constructor.
     * <p>
     * <b>Warning!</b> If created with this constructor this class will not
     * represent a valid grantee until the identifier is set.
     */
    public CanonicalGrantee() {
    }

    /**
     * Constructs a grantee with the given canonical ID.
     * @param identifier
     */
    public CanonicalGrantee(String identifier) {
        this.setIdentifier(identifier);
    }

    public String toXml() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return toXMLBuilder().asString();
    }

    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return (XMLBuilder.create("Grantee")
            .attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            .attr("xsi:type", "CanonicalUser")
            .element("ID").text(id)
            );
    }

    public void setIdentifier(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof CanonicalGrantee) {
            CanonicalGrantee canonicalGrantee = (CanonicalGrantee) obj;
            return id.equals(canonicalGrantee.id);
        }
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public String toString() {
        return "CanonicalGrantee [id=" + id
            + (displayName != null ? ", displayName=" + displayName : "")
            + "]";
    }

}

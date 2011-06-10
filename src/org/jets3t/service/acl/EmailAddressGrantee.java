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
 * Represents an Email Grantee, that is a grantee identified by their email address and
 * authenticated by an Amazon system.
 *
 * @author James Murty
 *
 */
public class EmailAddressGrantee implements GranteeInterface {
    private String emailAddress = null;

    /**
     * Default construtor.
     * <p>
     * <b>Warning!</b> If this constructor is used the class will not represent
     * a valid email grantee until the identifier has been set.
     */
    public EmailAddressGrantee() {
    }

    /**
     * Constructs an email grantee with the given email address.
     * @param emailAddress
     */
    public EmailAddressGrantee(String emailAddress) {
        this.setIdentifier(emailAddress);
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
            .attr("xsi:type", "AmazonCustomerByEmail")
            .element("EmailAddress").text(emailAddress)
            );
    }

    /**
     * Set the email address as the grantee's ID.
     */
    public void setIdentifier(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Returns the grantee's email address (ID).
     */
    public String getIdentifier() {
        return emailAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EmailAddressGrantee) {
            return emailAddress.equals(((EmailAddressGrantee)obj).emailAddress);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return emailAddress.hashCode();
    }

}

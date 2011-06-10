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

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jets3t.service.acl.EmailAddressGrantee;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents an User By Email Grantee, that is a grantee identified by their email address.
 *
 * @author Google Developers
 *
 */
public class UserByEmailAddressGrantee extends EmailAddressGrantee {
    private String name;

    /**
     * Default constructor.
     * <p>
     * <b>Warning!</b> If created with this constructor this class will not
     * represent a valid grantee until the identifier is set.
     */
    public UserByEmailAddressGrantee() {
      super();
    }

    /**
     * Constructs a grantee with the given email.
     * @param emailAddress
     * user's service-recognizable email address.
     */
    public UserByEmailAddressGrantee(String emailAddress) {
        super(emailAddress);
    }

    public UserByEmailAddressGrantee(String emailAddress, String name) {
        super(emailAddress);
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
                .attr("type", "UserByEmail")
                .element("EmailAddress").text(getIdentifier()).up();
        if (getName() != null) {
            builder.element("Name").text(getName());
        }
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof UserByEmailAddressGrantee
                && ((UserByEmailAddressGrantee)obj).getIdentifier()
                    .equals(getIdentifier()));
    }

    @Override
    public String toString() {
        return "UserByEmail [email=" + getIdentifier()
            + (name != null ? ", name=" + getName() : "")
            + "]";
    }

}

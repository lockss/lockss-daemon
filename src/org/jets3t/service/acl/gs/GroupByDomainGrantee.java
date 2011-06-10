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
 * Represents an Group By Domain Grantee, that is a group identified by their domain address.
 *
 * @author Google Developers
 *
 */
public class GroupByDomainGrantee extends GroupGrantee {
    public GroupByDomainGrantee() {
      super();
    }

    /**
     * Constructs a group grantee object using the given domain address as an identifier.
     *
     * @param domain
     */
    public GroupByDomainGrantee(String domain) {
      super(domain);
    }

    @Override
    public XMLBuilder toXMLBuilder() throws TransformerException,
        ParserConfigurationException, FactoryConfigurationError
    {
        return (XMLBuilder.create("Scope")
            .attr("type", "GroupByDomain")
            .element("Domain").text(id)
            );
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof GroupByDomainGrantee
                && ((GroupByDomainGrantee)obj).getIdentifier() == this.getIdentifier());
    }

    @Override
    public String toString() {
        return "GroupByDomain [" + id + "]";
    }
}

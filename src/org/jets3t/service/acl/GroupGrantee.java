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
 * Represents a Group grantee.
 * <p>
 * Only three groups are available in S3:<br>
 * <tt>ALL_USERS</tt>: The general public<br>
 * <tt>AUTHENTICATED_USERS</tt>: Authenticated Amazon S3 users<br>
 * <tt>LOG_DELIVERY</tt>: Amazon's S3 Log Delivery group, who deliver bucket log files<br>
 *
 * @author James Murty
 *
 */
public class GroupGrantee implements GranteeInterface {
    /**
     * The group of all users, represented in S3 by the URI:
     * http://acs.amazonaws.com/groups/global/AllUsers
     */
    public static final GroupGrantee ALL_USERS = new GroupGrantee("http://acs.amazonaws.com/groups/global/AllUsers");

    /**
     * The group of authenticated users, represented in S3 by the URI:
     * http://acs.amazonaws.com/groups/global/AuthenticatedUsers
     */
    public static final GroupGrantee AUTHENTICATED_USERS = new GroupGrantee("http://acs.amazonaws.com/groups/global/AuthenticatedUsers");

    /**
     * The group of Bucket Log delivery users, represented in S3 by the URI:
     * http://acs.amazonaws.com/groups/s3/LogDelivery
     */
    public static final GroupGrantee LOG_DELIVERY = new GroupGrantee("http://acs.amazonaws.com/groups/s3/LogDelivery");

    protected String id = null;

    public GroupGrantee() {
    }

    /**
     * Constructs a group grantee object using the given group URI as an identifier.
     * <p>
     * <b>Note</b>: All possible group types are available as public static variables from this class,
     * so this constructor should rarely be necessary.
     *
     * @param groupUri
     */
    public GroupGrantee(String groupUri) {
        this.id = groupUri;
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
            .attr("xsi:type", "Group")
            .element("URI").text(id)
            );
    }

    /**
     * Set the group grantee's URI.
     */
    public void setIdentifier(String uri) {
        this.id = uri;
    }

    /**
     * Returns the group grantee's URI.
     */
    public String getIdentifier() {
        return id;
    }

    public String toString() {
        return "GroupGrantee [" + id + "]";
    }

    public boolean equals(Object obj) {
        if (obj instanceof GroupGrantee) {
            return id.equals(((GroupGrantee)obj).id);
        }
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

}

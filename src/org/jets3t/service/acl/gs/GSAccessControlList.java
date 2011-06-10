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

import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.Permission;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents a Google Storage Access Control List (ACL), including the ACL's set of grantees and the
 * permissions assigned to each grantee.
 * <p>
 *
 * </p>
 *
 * @author Google Developers
 *
 */
public class GSAccessControlList extends AccessControlList {

    private static final long serialVersionUID = -3170938665076811564L;

    /*
     * Predefined ACLs that can be applied on creation of an object or bucket,
     * topic "Applying ACLs with an extension request header" at
     * http://code.google.com/apis/storage/docs/developer-guide.html#authentication
     */
    public static final GSAccessControlList REST_CANNED_PRIVATE = new GSAccessControlList();
    public static final GSAccessControlList REST_CANNED_PUBLIC_READ = new GSAccessControlList();
    public static final GSAccessControlList REST_CANNED_PUBLIC_READ_WRITE = new GSAccessControlList();
    public static final GSAccessControlList REST_CANNED_AUTHENTICATED_READ = new GSAccessControlList();
    public static final GSAccessControlList REST_CANNED_BUCKET_OWNER_READ = new GSAccessControlList();
    public static final GSAccessControlList REST_CANNED_BUCKET_OWNER_FULL_CONTROL = new GSAccessControlList();

    /**
     * Returns a string representation of the ACL contents, useful for debugging.
     */
    @Override
    public String toString() {
        return "GSAccessControlList [owner=" + owner + ", grants=" + getGrantAndPermissions() + "]";
    }

    @Override
    public XMLBuilder toXMLBuilder() throws ServiceException, ParserConfigurationException,
        FactoryConfigurationError, TransformerException
    {
        if (owner == null) {
            throw new ServiceException("Invalid AccessControlList: missing an Owner");
        }
        XMLBuilder builder = XMLBuilder.create("AccessControlList");

        // Owner
        XMLBuilder ownerBuilder = builder.elem("Owner");
        ownerBuilder.elem("ID").text(owner.getId()).up();
        if (owner.getDisplayName() != null) {
            ownerBuilder.elem("Name").text(owner.getDisplayName());
        }

        XMLBuilder accessControlList = builder.elem("Entries");
        for (GrantAndPermission gap: grants) {
            GranteeInterface grantee = gap.getGrantee();
            Permission permission = gap.getPermission();
            accessControlList
                .elem("Entry")
                    .importXMLBuilder(grantee.toXMLBuilder())
                    .elem("Permission").text(permission.toString());
        }
        return builder;
    }

    /**
     * @return the header value string for this ACL if it is a predefined ACL, otherwise return null;
     */
    @Override
    public String getValueForRESTHeaderACL() {
        if (GSAccessControlList.REST_CANNED_PRIVATE.equals(this)) {
            return "private";
        } else if (GSAccessControlList.REST_CANNED_PUBLIC_READ.equals(this)) {
            return "public-read";
        } else if (GSAccessControlList.REST_CANNED_PUBLIC_READ_WRITE.equals(this)) {
            return "public-read-write";
        } else if (GSAccessControlList.REST_CANNED_AUTHENTICATED_READ.equals(this)) {
            return "authenticated-read";
        } else if (GSAccessControlList.REST_CANNED_BUCKET_OWNER_READ.equals(this)) {
            return "bucket-owner-read";
        } else if (GSAccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL.equals(this)) {
            return "bucket-owner-full-control";
        }
        return null;
    }

}

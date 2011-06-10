/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2011 James Murty
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
package org.jets3t.service.impl.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.EmailAddressGrantee;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.BaseVersionOrDeleteMarker;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.model.GSOwner;
import org.jets3t.service.model.MultipartCompleted;
import org.jets3t.service.model.MultipartPart;
import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.NotificationConfig;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3BucketLoggingStatus;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3DeleteMarker;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.S3Owner;
import org.jets3t.service.model.S3Version;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.utils.ServiceUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Sax parser to read XML documents returned by S3 via the REST interface, converting these
 * documents into JetS3t objects.
 *
 * @author James Murty
 */
public class XmlResponsesSaxParser {
    private static final Log log = LogFactory.getLog(XmlResponsesSaxParser.class);

    private XMLReader xr = null;
    private Jets3tProperties properties = null;
    private boolean isGoogleStorageMode = false;

    /**
     * Constructs the XML SAX parser.
     *
     * @param properties
     * the JetS3t properties that will be applied when parsing XML documents.
     *
     * @throws S3ServiceException
     */
    public XmlResponsesSaxParser(Jets3tProperties properties, boolean returnGoogleStorageObjects)
        throws ServiceException
    {
        this.properties = properties;
        this.isGoogleStorageMode = returnGoogleStorageObjects;
        this.xr = ServiceUtils.loadXMLReader();
    }

    protected StorageBucket newBucket() {
        if (isGoogleStorageMode) {
            return new GSBucket();
        } else {
            return new S3Bucket();
        }
    }

    protected StorageObject newObject() {
        if (isGoogleStorageMode) {
            return new GSObject();
        } else {
            return new S3Object();
        }
    }

    protected StorageOwner newOwner() {
        if (isGoogleStorageMode) {
            return new GSOwner();
        } else {
            return new S3Owner();
        }
    }

    /**
     * Parses an XML document from an input stream using a document handler.
     * @param handler
     *        the handler for the XML document
     * @param inputStream
     *        an input stream containing the XML document to parse
     * @throws ServiceException
     *        any parsing, IO or other exceptions are wrapped in an ServiceException.
     */
    protected void parseXmlInputStream(DefaultHandler handler, InputStream inputStream)
        throws ServiceException
    {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Parsing XML response document with handler: " + handler.getClass());
            }
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                Constants.DEFAULT_ENCODING));
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(new InputSource(breader));
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after XML parse failure", e);
                }
            }
            throw new ServiceException("Failed to parse XML document with handler "
                + handler.getClass(), t);
        }
    }

    protected InputStream sanitizeXmlDocument(DefaultHandler handler, InputStream inputStream)
        throws ServiceException
    {
        if (!properties.getBoolProperty("xmlparser.sanitize-listings", true)) {
            // No sanitizing will be performed, return the original input stream unchanged.
            return inputStream;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sanitizing XML document destined for handler " + handler.getClass());
            }

            InputStream sanitizedInputStream = null;

            try {
                /* Read object listing XML document from input stream provided into a
                 * string buffer, so we can replace troublesome characters before
                 * sending the document to the XML parser.
                 */
                StringBuffer listingDocBuffer = new StringBuffer();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, Constants.DEFAULT_ENCODING));

                char[] buf = new char[8192];
                int read = -1;
                while ((read = br.read(buf)) != -1) {
                    listingDocBuffer.append(buf, 0, read);
                }
                br.close();

                // Replace any carriage return (\r) characters with explicit XML
                // character entities, to prevent the SAX parser from
                // misinterpreting 0x0D characters as 0x0A.
                String listingDoc = listingDocBuffer.toString().replaceAll("\r", "&#013;");

                sanitizedInputStream = new ByteArrayInputStream(
                    listingDoc.getBytes(Constants.DEFAULT_ENCODING));
            } catch (Throwable t) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    if (log.isErrorEnabled()) {
                        log.error("Unable to close response InputStream after failure sanitizing XML document", e);
                    }
                }
                throw new ServiceException("Failed to sanitize XML document destined for handler "
                    + handler.getClass(), t);
            }
            return sanitizedInputStream;
        }
    }

    /**
     * Parses a ListBucket response XML document from an input stream.
     * @param inputStream
     * XML data input stream.
     * @return
     * the XML handler object populated with data parsed from the XML stream.
     * @throws ServiceException
     */
    public ListBucketHandler parseListBucketResponse(InputStream inputStream)
        throws ServiceException
    {
        ListBucketHandler handler = new ListBucketHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    /**
     * Parses a ListAllMyBuckets response XML document from an input stream.
     * @param inputStream
     * XML data input stream.
     * @return
     * the XML handler object populated with data parsed from the XML stream.
     * @throws ServiceException
     */
    public ListAllMyBucketsHandler parseListMyBucketsResponse(InputStream inputStream)
        throws ServiceException
    {
        ListAllMyBucketsHandler handler = new ListAllMyBucketsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    /**
     * Parses an AccessControlListHandler response XML document from an input stream.
     *
     * @param inputStream
     * XML data input stream.
     * @return
     * the XML handler object populated with data parsed from the XML stream.
     *
     * @throws ServiceException
     */
    public AccessControlListHandler parseAccessControlListResponse(InputStream inputStream)
        throws ServiceException
    {
        AccessControlListHandler handler = null;
        if (this.isGoogleStorageMode) {
            handler = new GSAccessControlListHandler();
        } else {
            handler = new AccessControlListHandler();
        }

        return parseAccessControlListResponse(inputStream, handler);
    }

    /**
     * Parses an AccessControlListHandler response XML document from an input stream.
     *
     * @param inputStream
     * XML data input stream.
     * @param handler
     * the instance of AccessControlListHandler to be used.
     * @return
     * the XML handler object populated with data parsed from the XML stream.
     *
     * @throws ServiceException
     */
    public AccessControlListHandler parseAccessControlListResponse(InputStream inputStream,
        AccessControlListHandler handler)
        throws ServiceException
    {
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    /**
     * Parses a LoggingStatus response XML document for a bucket from an input stream.
     *
     * @param inputStream
     * XML data input stream.
     * @return
     * the XML handler object populated with data parsed from the XML stream.
     *
     * @throws ServiceException
     */
    public BucketLoggingStatusHandler parseLoggingStatusResponse(InputStream inputStream)
        throws ServiceException
    {
        BucketLoggingStatusHandler handler = new BucketLoggingStatusHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public String parseBucketLocationResponse(InputStream inputStream)
        throws ServiceException
    {
        BucketLocationHandler handler = new BucketLocationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.getLocation();
    }

    public CopyObjectResultHandler parseCopyObjectResponse(InputStream inputStream)
        throws ServiceException
    {
        CopyObjectResultHandler handler = new CopyObjectResultHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    /**
     * @param inputStream
     *
     * @return
     * true if the bucket is configured as Requester Pays, false if it is
     * configured as Owner pays.
     *
     * @throws ServiceException
     */
    public boolean parseRequestPaymentConfigurationResponse(InputStream inputStream)
        throws ServiceException
    {
        RequestPaymentConfigurationHandler handler = new RequestPaymentConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.isRequesterPays();
    }

    /**
     * @param inputStream
     *
     * @return
     * true if the bucket has versioning enabled, false otherwise.
     *
     * @throws ServiceException
     */
    public S3BucketVersioningStatus parseVersioningConfigurationResponse(
        InputStream inputStream) throws ServiceException
    {
        VersioningConfigurationHandler handler = new VersioningConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.getVersioningStatus();
    }

    public ListVersionsResultsHandler parseListVersionsResponse(InputStream inputStream)
        throws ServiceException
    {
        ListVersionsResultsHandler handler = new ListVersionsResultsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public MultipartUpload parseInitiateMultipartUploadResult(InputStream inputStream)
        throws ServiceException
    {
        MultipartUploadResultHandler handler = new MultipartUploadResultHandler(xr);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler.getMultipartUpload();
    }

    public ListMultipartUploadsResultHandler parseListMultipartUploadsResult(
        InputStream inputStream) throws ServiceException
    {
        ListMultipartUploadsResultHandler handler = new ListMultipartUploadsResultHandler(xr);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ListMultipartPartsResultHandler parseListMultipartPartsResult(
        InputStream inputStream) throws ServiceException
    {
        ListMultipartPartsResultHandler handler = new ListMultipartPartsResultHandler(xr);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public CompleteMultipartUploadResultHandler parseCompleteMultipartUploadResult(
        InputStream inputStream) throws ServiceException
    {
        CompleteMultipartUploadResultHandler handler = new CompleteMultipartUploadResultHandler(xr);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WebsiteConfig parseWebsiteConfigurationResponse(
        InputStream inputStream) throws ServiceException
    {
        WebsiteConfigurationHandler handler = new WebsiteConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.getWebsiteConfig();
    }

    public NotificationConfig parseNotificationConfigurationResponse(
        InputStream inputStream) throws ServiceException
    {
        NotificationConfigurationHandler handler = new NotificationConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.getNotificationConfig();
    }

    //////////////
    // Handlers //
    //////////////

    /**
     * Handler for ListBucket response XML documents.
     * The document is parsed into {@link S3Object}s available via the {@link #getObjects()} method.
     */
    public class ListBucketHandler extends DefaultXmlHandler {
        private StorageObject currentObject = null;
        private StorageOwner currentOwner = null;
        private boolean insideCommonPrefixes = false;

        private final List<StorageObject> objects = new ArrayList<StorageObject>();
        private final List<String> commonPrefixes = new ArrayList<String>();

        // Listing properties.
        private String bucketName = null;
        private String requestPrefix = null;
        private String requestMarker = null;
        private long requestMaxKeys = 0;
        private boolean listingTruncated = false;
        private String lastKey = null;
        private String nextMarker = null;

        /**
         * If the listing is truncated this method will return the marker that should be used
         * in subsequent bucket list calls to complete the listing.
         *
         * @return
         * null if the listing is not truncated, otherwise the next marker if it's available or
         * the last object key seen if the next marker isn't available.
         */
        public String getMarkerForNextListing() {
            if (listingTruncated) {
                if (nextMarker != null) {
                    return nextMarker;
                } else if (lastKey != null) {
                    return lastKey;
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to find Next Marker or Last Key for truncated listing");
                    }
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * @return
         * true if the listing document was truncated, and therefore only contained a subset of the
         * available S3 objects.
         */
        public boolean isListingTruncated() {
            return listingTruncated;
        }

        /**
         * @return
         * the S3 objects contained in the listing.
         */
        public StorageObject[] getObjects() {
            return objects.toArray(new StorageObject[objects.size()]);
        }

        public String[] getCommonPrefixes() {
            return commonPrefixes.toArray(new String[commonPrefixes.size()]);
        }

        public String getRequestPrefix() {
            return requestPrefix;
        }

        public String getRequestMarker() {
            return requestMarker;
        }

        public String getNextMarker() {
            return nextMarker;
        }

        public long getRequestMaxKeys() {
            return requestMaxKeys;
        }

        @Override
        public void startElement(String name) {
            if (name.equals("Contents")) {
                currentObject = newObject();
                if (currentObject instanceof S3Object) {
                    ((S3Object)currentObject).setBucketName(bucketName);
                }
            } else if (name.equals("Owner")) {
                currentOwner = newOwner();
                currentObject.setOwner(currentOwner);
            } else if (name.equals("CommonPrefixes")) {
                insideCommonPrefixes = true;
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            // Listing details
            if (name.equals("Name")) {
                bucketName = elementText;
                if (log.isDebugEnabled()) {
                    log.debug("Examining listing for bucket: " + bucketName);
                }
            } else if (!insideCommonPrefixes && name.equals("Prefix")) {
                requestPrefix = elementText;
            } else if (name.equals("Marker")) {
                requestMarker = elementText;
            } else if (name.equals("NextMarker")) {
                nextMarker = elementText;
            } else if (name.equals("MaxKeys")) {
                requestMaxKeys = Long.parseLong(elementText);
            } else if (name.equals("IsTruncated")) {
                String isTruncatedStr = elementText.toLowerCase(Locale.getDefault());
                if (isTruncatedStr.startsWith("false")) {
                    listingTruncated = false;
                } else if (isTruncatedStr.startsWith("true")) {
                    listingTruncated = true;
                } else {
                    throw new RuntimeException("Invalid value for IsTruncated field: "
                        + isTruncatedStr);
                }
            }
            // Object details.
            else if (name.equals("Contents")) {
                objects.add(currentObject);
                if (log.isDebugEnabled()) {
                    log.debug("Created new object from listing: " + currentObject);
                }
            } else if (name.equals("Key")) {
                currentObject.setKey(elementText);
                lastKey = elementText;
            } else if (name.equals("LastModified")) {
                try {
                    currentObject.setLastModifiedDate(ServiceUtils.parseIso8601Date(elementText));
                } catch (ParseException e) {
                    throw new RuntimeException(
                        "Non-ISO8601 date for LastModified in bucket's object listing output: "
                        + elementText, e);
                }
            } else if (name.equals("ETag")) {
                currentObject.setETag(elementText);
            } else if (name.equals("Size")) {
                currentObject.setContentLength(Long.parseLong(elementText));
            } else if (name.equals("StorageClass")) {
                currentObject.setStorageClass(elementText);
            }
            // Owner details.
            else if (name.equals("ID")) {
                // Work-around to support Eucalyptus responses, which do not
                // contain Owner elements.
                if (currentOwner == null) {
                    currentOwner = newOwner();
                    currentObject.setOwner(currentOwner);
                }

                currentOwner.setId(elementText);
            } else if (name.equals("DisplayName")) {
                currentOwner.setDisplayName(elementText);
            }
            // Common prefixes.
            else if (insideCommonPrefixes && name.equals("Prefix")) {
                commonPrefixes.add(elementText);
            } else if (name.equals("CommonPrefixes")) {
                insideCommonPrefixes = false;
            }
        }
    }

    /**
     * Handler for ListAllMyBuckets response XML documents. The document is parsed into
     * {@link StorageBucket}s available via the {@link #getBuckets()} method.
     *
     * @author James Murty
     *
     */
    public class ListAllMyBucketsHandler extends DefaultXmlHandler {
        private StorageOwner bucketsOwner = null;
        private StorageBucket currentBucket = null;

        private final List<StorageBucket> buckets = new ArrayList<StorageBucket>();

        /**
         * @return
         * the buckets listed in the document.
         */
        public StorageBucket[] getBuckets() {
            return buckets.toArray(new StorageBucket[buckets.size()]);
        }

        /**
         * @return
         * the owner of the buckets.
         */
        public StorageOwner getOwner() {
            return bucketsOwner;
        }

        @Override
        public void startElement(String name) {
            if (name.equals("Bucket")) {
                currentBucket = newBucket();
            } else if (name.equals("Owner")) {
                bucketsOwner = newOwner();
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            // Listing details.
            if (name.equals("ID")) {
                bucketsOwner.setId(elementText);
            } else if (name.equals("DisplayName")) {
                bucketsOwner.setDisplayName(elementText);
            }
            // Bucket item details.
            else if (name.equals("Bucket")) {
                if (log.isDebugEnabled()) {
                    log.debug("Created new bucket from listing: " + currentBucket);
                }
                currentBucket.setOwner(bucketsOwner);
                buckets.add(currentBucket);
            } else if (name.equals("Name")) {
                currentBucket.setName(elementText);
            } else if (name.equals("CreationDate")) {
                elementText += ".000Z";
                try {
                    currentBucket.setCreationDate(ServiceUtils.parseIso8601Date(elementText));
                } catch (ParseException e) {
                    throw new RuntimeException(
                        "Non-ISO8601 date for CreationDate in list buckets output: "
                        + elementText, e);
                }
            }
        }
    }

    /**
     * Handler for LoggingStatus response XML documents for a bucket.
     * The document is parsed into an {@link S3BucketLoggingStatus} object available via the
     * {@link #getBucketLoggingStatus()} method.
     *
     * @author James Murty
     *
     */
    public class BucketLoggingStatusHandler extends DefaultXmlHandler {
        private S3BucketLoggingStatus bucketLoggingStatus = null;

        private String targetBucket = null;
        private String targetPrefix = null;
        private GranteeInterface currentGrantee = null;
        private Permission currentPermission = null;

        /**
         * @return
         * an object representing the bucket's LoggingStatus document.
         */
        public S3BucketLoggingStatus getBucketLoggingStatus() {
            return bucketLoggingStatus;
        }

        @Override
        public void startElement(String name) {
            if (name.equals("BucketLoggingStatus")) {
                bucketLoggingStatus = new S3BucketLoggingStatus();
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("TargetBucket")) {
                targetBucket = elementText;
            } else if (name.equals("TargetPrefix")) {
                targetPrefix = elementText;
            } else if (name.equals("LoggingEnabled")) {
                bucketLoggingStatus.setTargetBucketName(targetBucket);
                bucketLoggingStatus.setLogfilePrefix(targetPrefix);
            }
            // Handle TargetGrants ACLs
            else if (name.equals("ID")) {
                currentGrantee = new CanonicalGrantee();
                currentGrantee.setIdentifier(elementText);
            } else if (name.equals("EmailAddress")) {
                currentGrantee = new EmailAddressGrantee();
                currentGrantee.setIdentifier(elementText);
            } else if (name.equals("URI")) {
                currentGrantee = new GroupGrantee();
                currentGrantee.setIdentifier(elementText);
            } else if (name.equals("DisplayName")) {
                ((CanonicalGrantee) currentGrantee).setDisplayName(elementText);
            } else if (name.equals("Permission")) {
                currentPermission = Permission.parsePermission(elementText);
            } else if (name.equals("Grant")) {
                GrantAndPermission grantAndPermission = new GrantAndPermission(
                    currentGrantee, currentPermission);
                bucketLoggingStatus.addTargetGrant(grantAndPermission);
            }
        }
    }

    /**
     * Handler for CreateBucketConfiguration response XML documents for a bucket.
     * The document is parsed into a String representing the bucket's location,
     * available via the {@link #getLocation()} method.
     *
     * @author James Murty
     *
     */
    public class BucketLocationHandler extends DefaultXmlHandler {
        private String location = null;

        /**
         * @return
         * the bucket's location.
         */
        public String getLocation() {
            return location;
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("LocationConstraint")) {
                if (elementText.length() == 0) {
                    location = null;
                } else {
                    location = elementText;
                }
            }
        }
    }


    public class CopyObjectResultHandler extends DefaultXmlHandler {
        // Data items for successful copy
        private String etag = null;
        private Date lastModified = null;

        // Data items for failed copy
        private String errorCode = null;
        private String errorMessage = null;
        private String errorRequestId = null;
        private String errorHostId = null;
        private boolean receivedErrorResponse = false;

        public Date getLastModified() {
            return lastModified;
        }

        public String getETag() {
            return etag;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorHostId() {
            return errorHostId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorRequestId() {
            return errorRequestId;
        }

        public boolean isErrorResponse() {
            return receivedErrorResponse;
        }

        @Override
        public void startElement(String name) {
            if (name.equals("CopyObjectResult")) {
                receivedErrorResponse = false;
            } else if (name.equals("Error")) {
                receivedErrorResponse = true;
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("LastModified")) {
                try {
                    lastModified = ServiceUtils.parseIso8601Date(elementText);
                } catch (ParseException e) {
                    throw new RuntimeException(
                        "Non-ISO8601 date for LastModified in copy object output: "
                        + elementText, e);
                }
            } else if (name.equals("ETag")) {
                etag = elementText;
            } else if (name.equals("Code")) {
                errorCode = elementText;
            } else if (name.equals("Message")) {
                errorMessage = elementText;
            } else if (name.equals("RequestId")) {
                errorRequestId = elementText;
            } else if (name.equals("HostId")) {
                errorHostId = elementText;
            }
        }
    }

    /**
     * Handler for RequestPaymentConfiguration response XML documents for a bucket.
     * The document is parsed into a boolean value: true if the bucket is configured
     * as Requester Pays, false if it is configured as Owner pays. This boolean value
     * is available via the {@link #isRequesterPays()} method.
     *
     * @author James Murty
     */
    public class RequestPaymentConfigurationHandler extends DefaultXmlHandler {
        private String payer = null;

        /**
         * @return
         * true if the bucket is configured as Requester Pays, false if it is
         * configured as Owner pays.
         */
        public boolean isRequesterPays() {
            return "Requester".equals(payer);
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("Payer")) {
                payer = elementText;
            }
        }
    }

    public class VersioningConfigurationHandler extends DefaultXmlHandler {
        private S3BucketVersioningStatus versioningStatus = null;
        private String status = null;
        private String mfaStatus = null;

        public S3BucketVersioningStatus getVersioningStatus() {
            return this.versioningStatus;
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("Status")) {
                this.status = elementText;
            } else if (name.equals("MfaDelete")) {
                this.mfaStatus = elementText;
            } else if (name.equals("VersioningConfiguration")) {
                this.versioningStatus = new S3BucketVersioningStatus(
                    "Enabled".equals(status),
                    "Enabled".equals(mfaStatus));
            }
        }
    }

    public class ListVersionsResultsHandler extends DefaultXmlHandler {
        private final List<BaseVersionOrDeleteMarker> items =
            new ArrayList<BaseVersionOrDeleteMarker>();
        private final List<String> commonPrefixes = new ArrayList<String>();

        private String key = null;
        private String versionId = null;
        private boolean isLatest = false;
        private Date lastModified = null;
        private StorageOwner owner = null;

        private String etag = null;
        private long size = 0;
        private String storageClass = null;

        private boolean insideCommonPrefixes = false;

        // Listing properties.
        private String bucketName = null;
        private String requestPrefix = null;
        private String keyMarker = null;
        private String versionIdMarker = null;
        private long requestMaxKeys = 0;
        private boolean listingTruncated = false;
        private String nextMarker = null;
        private String nextVersionIdMarker = null;

        /**
         * @return
         * true if the listing document was truncated, and therefore only contained a subset of the
         * available S3 objects.
         */
        public boolean isListingTruncated() {
            return listingTruncated;
        }

        /**
         * @return
         * the S3 objects contained in the listing.
         */
        public BaseVersionOrDeleteMarker[] getItems() {
            return items.toArray(new BaseVersionOrDeleteMarker[items.size()]);
        }

        public String[] getCommonPrefixes() {
            return commonPrefixes.toArray(new String[commonPrefixes.size()]);
        }

        public String getRequestPrefix() {
            return requestPrefix;
        }

        public String getKeyMarker() {
            return keyMarker;
        }

        public String getVersionIdMarker() {
            return versionIdMarker;
        }

        public String getNextKeyMarker() {
            return nextMarker;
        }

        public String getNextVersionIdMarker() {
            return nextVersionIdMarker;
        }

        public long getRequestMaxKeys() {
            return requestMaxKeys;
        }

        @Override
        public void startElement(String name) {
            if (name.equals("Owner")) {
                owner = null;
            } else if (name.equals("CommonPrefixes")) {
                insideCommonPrefixes = true;
            }
        }

        @Override
        public void endElement(String name, String elementText) {
            // Listing details
            if (name.equals("Name")) {
                bucketName = elementText;
                if (log.isDebugEnabled()) {
                    log.debug("Examining listing for bucket: " + bucketName);
                }
            } else if (!insideCommonPrefixes && name.equals("Prefix")) {
                requestPrefix = elementText;
            } else if (name.equals("KeyMarker")) {
                keyMarker = elementText;
            } else if (name.equals("NextKeyMarker")) {
                nextMarker = elementText;
            } else if (name.equals("VersionIdMarker")) {
                versionIdMarker = elementText;
            } else if (name.equals("NextVersionIdMarker")) {
                nextVersionIdMarker = elementText;
            } else if (name.equals("MaxKeys")) {
                requestMaxKeys = Long.parseLong(elementText);
            } else if (name.equals("IsTruncated")) {
                String isTruncatedStr = elementText.toLowerCase(Locale.getDefault());
                if (isTruncatedStr.startsWith("false")) {
                    listingTruncated = false;
                } else if (isTruncatedStr.startsWith("true")) {
                    listingTruncated = true;
                } else {
                    throw new RuntimeException("Invalid value for IsTruncated field: "
                        + isTruncatedStr);
                }
            }
            // Version/DeleteMarker finished.
            else if (name.equals("Version")) {
                BaseVersionOrDeleteMarker item = new S3Version(key, versionId,
                    isLatest, lastModified, (S3Owner)owner, etag, size, storageClass);
                items.add(item);
            } else if (name.equals("DeleteMarker")) {
                BaseVersionOrDeleteMarker item = new S3DeleteMarker(key, versionId,
                        isLatest, lastModified, (S3Owner)owner);
                items.add(item);

            // Version/DeleteMarker details
            } else if (name.equals("Key")) {
                key = elementText;
            } else if (name.equals("VersionId")) {
                versionId = elementText;
            } else if (name.equals("IsLatest")) {
                isLatest = "true".equals(elementText);
            } else if (name.equals("LastModified")) {
                try {
                    lastModified = ServiceUtils.parseIso8601Date(elementText);
                } catch (ParseException e) {
                    throw new RuntimeException(
                        "Non-ISO8601 date for LastModified in bucket's versions listing output: "
                        + elementText, e);
                }
            } else if (name.equals("ETag")) {
                etag = elementText;
            } else if (name.equals("Size")) {
                size = Long.parseLong(elementText);
            } else if (name.equals("StorageClass")) {
                storageClass = elementText;
            }
            // Owner details.
            else if (name.equals("ID")) {
                owner = newOwner();
                owner.setId(elementText);
            } else if (name.equals("DisplayName")) {
                owner.setDisplayName(elementText);
            }
            // Common prefixes.
            else if (insideCommonPrefixes && name.equals("Prefix")) {
                commonPrefixes.add(elementText);
            } else if (name.equals("CommonPrefixes")) {
                insideCommonPrefixes = false;
            }
        }
    }

    public class OwnerHandler extends SimpleHandler {
        private String id;
        private String displayName;

        public OwnerHandler(XMLReader xr) {
            super(xr);
        }

        public StorageOwner getOwner() {
            StorageOwner owner = newOwner();
            owner.setId(id);
            owner.setDisplayName(displayName);
            return owner;
        }

        public void endID(String text) {
            this.id = text;
        }

        public void endDisplayName(String text) {
            this.displayName = text;
        }

        public void endOwner(String text) {
            returnControlToParentHandler();
        }

        // </Initiator> represents end of an owner item in ListMultipartUploadsResult/Upload
        public void endInitiator(String text) {
            returnControlToParentHandler();
        }
    }

    public class MultipartUploadResultHandler extends SimpleHandler {
        private String uploadId;
        private String bucketName;
        private String objectKey;
        private String storageClass;
        private S3Owner owner;
        private S3Owner initiator;
        private Date initiatedDate;

        private boolean inInitiator = false;

        public MultipartUploadResultHandler(XMLReader xr) {
            super(xr);
        }

        public MultipartUpload getMultipartUpload() {
            if (initiatedDate != null) {
                // Return the contents from a ListMultipartUploadsResult response
                return new MultipartUpload(uploadId, objectKey, storageClass,
                    initiator, owner, initiatedDate);
            } else {
                // Return the contents from an InitiateMultipartUploadsResult response
                return new MultipartUpload(uploadId, bucketName, objectKey);
            }
        }

        public void endUploadId(String text) {
            this.uploadId = text;
        }

        public void endBucket(String text) {
            this.bucketName = text;
        }

        public void endKey(String text) {
            this.objectKey = text;
        }

        public void endStorageClass(String text) {
            this.storageClass = text;
        }

        public void endInitiated(String text) throws ParseException {
            this.initiatedDate = ServiceUtils.parseIso8601Date(text);
        }

        public void startOwner() {
            inInitiator = false;
            transferControlToHandler(new OwnerHandler(xr));
        }

        public void startInitiator() {
            inInitiator = true;
            transferControlToHandler(new OwnerHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            if (inInitiator) {
                this.owner = (S3Owner) ((OwnerHandler) childHandler).getOwner();
            } else {
                this.initiator = (S3Owner) ((OwnerHandler) childHandler).getOwner();
            }
        }

        // </Upload> represents end of a MultipartUpload item in ListMultipartUploadsResult
        public void endUpload(String text) {
            returnControlToParentHandler();
        }
    }

    public class ListMultipartUploadsResultHandler extends SimpleHandler {
        private final List<MultipartUpload> uploads = new ArrayList<MultipartUpload>();
        private String bucketName = null;
        private String keyMarker = null;
        private String uploadIdMarker = null;
        private String nextKeyMarker = null;
        private String nextUploadIdMarker = null;
        private int maxUploads = 1000;
        private boolean isTruncated = false;

        public ListMultipartUploadsResultHandler(XMLReader xr) {
            super(xr);
        }

        public List<MultipartUpload> getMultipartUploadList() {
            // Update multipart upload objects with overall bucket name
            for (MultipartUpload upload: uploads) {
                upload.setBucketName(bucketName);
            }
            return uploads;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public String getKeyMarker() {
            return keyMarker;
        }

        public String getUploadIdMarker() {
            return uploadIdMarker;
        }

        public String getNextKeyMarker() {
            return nextKeyMarker;
        }

        public String getNextUploadIdMarker() {
            return nextUploadIdMarker;
        }

        public int getMaxUploads() {
            return maxUploads;
        }

        public void startUpload() {
            transferControlToHandler(new MultipartUploadResultHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            uploads.add(
                ((MultipartUploadResultHandler) childHandler).getMultipartUpload());
        }

        public void endBucket(String text) {
            this.bucketName = text;
        }

        public void endKeyMarker(String text) {
            this.keyMarker = text;
        }

        public void endUploadIdMarker(String text) {
            this.uploadIdMarker = text;
        }

        public void endNextKeyMarker(String text) {
            this.nextKeyMarker = text;
        }

        public void endNextUploadIdMarker(String text) {
            this.nextUploadIdMarker = text;
        }

        public void endMaxUploads(String text) {
            this.maxUploads = Integer.parseInt(text);
        }

        public void endIsTruncated(String text) {
            this.isTruncated = "true".equalsIgnoreCase(text);
        }
    }

    public class MultipartPartResultHandler extends SimpleHandler {
        private Integer partNumber;
        private Date lastModified;
        private String etag;
        private Long size;

        public MultipartPartResultHandler(XMLReader xr) {
            super(xr);
        }

        public MultipartPart getMultipartPart() {
            return new MultipartPart(partNumber, lastModified, etag, size);
        }

        public void endPartNumber(String text) {
            this.partNumber = Integer.parseInt(text);
        }

        public void endLastModified(String text) throws ParseException {
            this.lastModified = ServiceUtils.parseIso8601Date(text);
        }

        public void endETag(String text) {
            this.etag = text;
        }

        public void endSize(String text) {
            this.size = Long.parseLong(text);
        }

        // </Part> represents end of a Part item in ListPartsResultHandler/Part
        public void endPart(String text) {
            returnControlToParentHandler();
        }
    }

    public class ListMultipartPartsResultHandler extends SimpleHandler {
        private final List<MultipartPart> parts = new ArrayList<MultipartPart>();
        private String bucketName = null;
        private String objectKey = null;
        private String uploadId = null;
        private S3Owner initiator = null;
        private S3Owner owner = null;
        private String storageClass = null;
        private String partNumberMarker = null;
        private String nextPartNumberMarker = null;
        private int maxParts = 1000;
        private boolean isTruncated = false;

        private boolean inInitiator = false;

        public ListMultipartPartsResultHandler(XMLReader xr) {
            super(xr);
        }

        public List<MultipartPart> getMultipartPartList() {
            return parts;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public String getBucketName() {
            return bucketName;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getUploadId() {
            return uploadId;
        }

        public S3Owner getInitiator() {
            return initiator;
        }

        public S3Owner getOwner() {
            return owner;
        }

        public String getStorageClass() {
            return storageClass;
        }

        public String getPartNumberMarker() {
            return partNumberMarker;
        }

        public String getNextPartNumberMarker() {
            return nextPartNumberMarker;
        }

        public int getMaxParts() {
            return maxParts;
        }

        public void startPart() {
            transferControlToHandler(new MultipartPartResultHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            if (childHandler instanceof MultipartPartResultHandler) {
                parts.add(
                    ((MultipartPartResultHandler) childHandler).getMultipartPart());
            } else {
                if (inInitiator) {
                    initiator = (S3Owner)((OwnerHandler)childHandler).getOwner();
                } else {
                    owner = (S3Owner)((OwnerHandler)childHandler).getOwner();
                }
            }
        }

        public void startInitiator() {
            inInitiator = true;
            transferControlToHandler(new OwnerHandler(xr));
        }

        public void startOwner() {
            inInitiator = false;
            transferControlToHandler(new OwnerHandler(xr));
        }

        public void endBucket(String text) {
            this.bucketName = text;
        }

        public void endKey(String text) {
            this.objectKey = text;
        }

        public void endStorageClass(String text) {
            this.storageClass = text;
        }

        public void endUploadId(String text) {
            this.uploadId = text;
        }

        public void endPartNumberMarker(String text) {
            this.partNumberMarker = text;
        }

        public void endNextPartNumberMarker(String text) {
            this.nextPartNumberMarker = text;
        }

        public void endMaxParts(String text) {
            this.maxParts = Integer.parseInt(text);
        }

        public void endIsTruncated(String text) {
            this.isTruncated = "true".equalsIgnoreCase(text);
        }
    }

    public class CompleteMultipartUploadResultHandler extends SimpleHandler {
        private String location;
        private String bucketName;
        private String objectKey;
        private String etag;

        private ServiceException serviceException = null;

        public CompleteMultipartUploadResultHandler(XMLReader xr) {
            super(xr);
        }

        public MultipartCompleted getMultipartCompleted() {
            return new MultipartCompleted(location, bucketName, objectKey, etag);
        }

        public ServiceException getServiceException() {
            return serviceException;
        }

        public void endLocation(String text) {
            this.location = text;
        }

        public void endBucket(String text) {
            this.bucketName = text;
        }

        public void endKey(String text) {
            this.objectKey = text;
        }

        public void endETag(String text) {
            this.etag = text;
        }

        public void startError() {
            transferControlToHandler(new CompleteMultipartUploadErrorHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            this.serviceException = ((CompleteMultipartUploadErrorHandler)childHandler)
                .getServiceException();
        }
    }

    public class CompleteMultipartUploadErrorHandler extends SimpleHandler {
        private String code = null;
        private String message = null;
        private String etag = null;
        private Long minSizeAllowed = null;
        private Long proposedSize = null;
        private String hostId = null;
        private Integer partNumber = null;
        private String requestId = null;

        public CompleteMultipartUploadErrorHandler(XMLReader xr) {
            super(xr);
        }

        public ServiceException getServiceException() {
            String fullMessage = message
                + ": PartNumber=" + partNumber
                + ", MinSizeAllowed=" + minSizeAllowed
                + ", ProposedSize=" + proposedSize
                + ", ETag=" + etag;
            ServiceException e = new ServiceException(fullMessage);
            e.setErrorCode(code);
            e.setErrorMessage(message);
            e.setErrorHostId(hostId);
            e.setErrorRequestId(requestId);
            return e;
        }

        public void endCode(String text) {
            this.code = text;
        }

        public void endMessage(String text) {
            this.message = text;
        }

        public void endETag(String text) {
            this.etag = text;
        }

        public void endMinSizeAllowed(String text) {
            this.minSizeAllowed = Long.parseLong(text);
        }

        public void endProposedSize(String text) {
            this.proposedSize = Long.parseLong(text);
        }

        public void endHostId(String text) {
            this.hostId = text;
        }

        public void endPartNumber(String text) {
            this.partNumber = Integer.parseInt(text);
        }

        public void endRequestId(String text) {
            this.requestId = text;
        }

        public void endError(String text) {
            returnControlToParentHandler();
        }
    }

    public class WebsiteConfigurationHandler extends DefaultXmlHandler {
        private WebsiteConfig config = null;
        private String indexDocumentSuffix = null;
        private String errorDocumentKey = null;

        public WebsiteConfig getWebsiteConfig() {
            return config;
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("Suffix")) {
                this.indexDocumentSuffix = elementText;
            } else if (name.equals("Key")) {
                this.errorDocumentKey = elementText;
            } else if (name.equals("WebsiteConfiguration")) {
                this.config = new WebsiteConfig(
                    indexDocumentSuffix, errorDocumentKey);
            }
        }
    }

    public class NotificationConfigurationHandler extends DefaultXmlHandler {
        private NotificationConfig config = new NotificationConfig();
        private String lastTopic = null;
        private String lastEvent = null;

        public NotificationConfig getNotificationConfig() {
            return config;
        }

        @Override
        public void endElement(String name, String elementText) {
            if (name.equals("Topic")) {
                this.lastTopic = elementText;
            } else if (name.equals("Event")) {
                this.lastEvent = elementText;
                config.addTopicConfig(config.new TopicConfig(
                    this.lastTopic, this.lastEvent));
            } else if (name.equals("NotificationConfiguration")) {
            }
        }
    }

}

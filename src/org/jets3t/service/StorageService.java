/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008-2010 James Murty, 2008 Zmanda Inc
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
package org.jets3t.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.mx.MxDelegate;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.RestUtils;

/**
 * A service that handles communication with a storage service, offering all the operations that
 * can be performed on generic storage services.
 * <p>
 * This class must be extended by implementation classes that perform the communication with
 * a specific service using a specific interface, such as REST or SOAP.
 * </p>
 * <p>
 * Implementations of <code>StorageService</code> must be thread-safe as they will probably be used by
 * the multi-threaded service class {@link org.jets3t.service.multi.ThreadedStorageService}.
 * </p>
 * <p>
 * This class uses properties obtained through {@link Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author James Murty
 * @author Nikolas Coukouma
 */
public abstract class StorageService {

    private static final Log log = LogFactory.getLog(StorageService.class);

    /**
     * Status code returned by {@link #checkBucketStatus(String)} for a bucket
     * that exists and is owned by the service user.
     */
    public static final int BUCKET_STATUS__MY_BUCKET = 0;
    /**
     * Status code returned by {@link #checkBucketStatus(String)} for a bucket
     * that does not exist.
     */
    public static final int BUCKET_STATUS__DOES_NOT_EXIST = 1;
    /**
     * Status code returned by {@link #checkBucketStatus(String)} for a bucket
     * that exists but is not owned by the service user (i.e. another user has
     * already created this bucket in the service's namespace).
     */
    public static final int BUCKET_STATUS__ALREADY_CLAIMED = 2;

    protected Jets3tProperties jets3tProperties = null;

    protected ProviderCredentials credentials = null;

    private String invokingApplicationDescription = null;
    private boolean isHttpsOnly = true;
    private int internalErrorRetryMax = 5;

    private boolean isShutdown = false;

    /**
     * The approximate difference in the current time between your computer and
     * a target service, measured in milliseconds.
     *
     * This value is 0 by default. Use the {@link #getCurrentTimeWithOffset()}
     * to obtain the current time with this offset factor included, and the
     * {@link RestUtils#getAWSTimeAdjustment()} method to calculate an offset value for your
     * computer based on a response from an AWS server.
     */
    protected long timeOffset = 0;

    /**
     * Construct a <code>StorageService</code> identified by the given user credentials.
     *
     * @param credentials
     * the user credentials, may be null in which case the communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param jets3tProperties
     * JetS3t properties that will be applied within this service.
     */
    protected StorageService(ProviderCredentials credentials, String invokingApplicationDescription,
        Jets3tProperties jets3tProperties)
    {
        this.credentials = credentials;
        this.invokingApplicationDescription = invokingApplicationDescription;

        this.jets3tProperties = jets3tProperties;
        this.isHttpsOnly = this.getHttpsOnly();
        this.internalErrorRetryMax = jets3tProperties.getIntProperty(
            "storage-service.internal-error-retry-max", 5);

        // Configure the InetAddress DNS caching times to work well with remote services. The cached
        // DNS will timeout after 5 minutes, while failed DNS lookups will be retried after 1 second.
        System.setProperty("networkaddress.cache.ttl", "300");
        System.setProperty("networkaddress.cache.negative.ttl", "1");

        // (Re)initialize the JetS3t JMX delegate, in case system properties have changed.
        MxDelegate.getInstance().init();

        MxDelegate.getInstance().registerS3ServiceMBean();
        MxDelegate.getInstance().registerS3ServiceExceptionMBean();
    }

    /**
     * Construct a <code>StorageService</code> identified by the given user credentials.
     *
     * @param credentials
     * the user credentials, may be null in which case the communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     */
    protected StorageService(ProviderCredentials credentials, String invokingApplicationDescription)
    {
        this(credentials, invokingApplicationDescription,
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * Construct a <code>StorageService</code> identified by the given user credentials.
     *
     * @param credentials
     * the user credentials, may be null in which case the communication is done as an anonymous user.
     */
    protected StorageService(ProviderCredentials credentials) {
        this(credentials, null);
    }

    /**
     * Make a best-possible effort to shutdown and clean up any resources used by this
     * service such as HTTP connections, connection pools, threads etc, although there is
     * no guarantee that all such resources will indeed be fully cleaned up.
     *
     * After calling this method the service instance will no longer be usable -- a new
     * instance must be created to do more work.
     * @throws ServiceException
     */
    public void shutdown() throws ServiceException {
        this.isShutdown = true;
        this.shutdownImpl();
    }

    /**
     * @return true if the {@link #shutdown()} method has been used to shut down and
     * clean up this service. If this function returns true this service instance
     * can no longer be used to do work.
     */
    public boolean isShutdown() {
        return this.isShutdown;
    }

    /**
     * @return
     * true if this service has {@link ProviderCredentials} identifying a user, false
     * if the service is acting as an anonymous user.
     */
    public boolean isAuthenticatedConnection() {
        return credentials != null;
    }

    /**
     * Whether to use secure HTTPS or insecure HTTP for communicating with a service,
     * as configured in the {@link Jets3tProperties}.
     *
     * @return
     * true if this service should use only secure HTTPS communication channels.
     * If false, the non-secure HTTP protocol will be used.
     */
    public boolean isHttpsOnly() {
        return isHttpsOnly;
    }

    /**
     * @return
     * The maximum number of times to retry when Internal Error (500) errors are encountered,
     * as configured by the {@link Jets3tProperties}.
     */
    public int getInternalErrorRetryMax() {
        return internalErrorRetryMax;
    }

    /**
     * @return
     * the JetS3t properties that will be used by this service.
     */
    public Jets3tProperties getJetS3tProperties() {
        return jets3tProperties;
    }

    protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
        return new XmlResponsesSaxParser(this.jets3tProperties,
            (this instanceof GoogleStorageService));
    }

    protected StorageBucket newBucket() {
        if (this instanceof GoogleStorageService) {
            return new GSBucket();
        } else {
            return new S3Bucket();
        }
    }

    protected StorageObject newObject() {
        if (this instanceof GoogleStorageService) {
            return new GSObject();
        } else {
            return new S3Object();
        }
    }

    /**
     * Sleeps for a period of time based on the number of Internal Server errors a request has
     * encountered, provided the number of errors does not exceed the value set with the
     * property <code>storage-service.internal-error-retry-max</code>. If the maximum error count is
     * exceeded, this method will throw an {@link ServiceException}.
     *
     * The millisecond delay grows rapidly according to the formula
     * <code>50 * (<i>internalErrorCount</i> ^ 2)</code>.
     *
     * <table>
     * <tr><th>Error count</th><th>Delay in milliseconds</th></tr>
     * <tr><td>1</td><td>50</td></tr>
     * <tr><td>2</td><td>200</td></tr>
     * <tr><td>3</td><td>450</td></tr>
     * <tr><td>4</td><td>800</td></tr>
     * <tr><td>5</td><td>1250</td></tr>
     * </table>
     *
     * @param internalErrorCount
     * the number of Internal Server errors encountered by a request.
     *
     * @throws ServiceException
     * thrown if the number of internal errors exceeds the value of internalErrorCount.
     * @throws InterruptedException
     * thrown if the thread sleep is interrupted.
     */
    protected void sleepOnInternalError(int internalErrorCount)
        throws ServiceException, InterruptedException
    {
        if (internalErrorCount <= internalErrorRetryMax) {
            long delayMs = 50L * (int) Math.pow(internalErrorCount, 2);
            if (log.isWarnEnabled()) {
                log.warn("Encountered " + internalErrorCount
                    + " Internal Server error(s), will retry in " + delayMs + "ms");
            }
            Thread.sleep(delayMs);
        } else {
            throw new ServiceException("Encountered too many Internal Server errors ("
                + internalErrorCount + "), aborting request.");
        }
    }

    /**
     * @return the credentials identifying the service user, or null for anonymous.
     */
    public ProviderCredentials getProviderCredentials() {
        return credentials;
    }

    /**
     * @return a description of the application using this service, suitable for inclusion in the
     * user agent string of REST/HTTP requests.
     */
    public String getInvokingApplicationDescription() {
        return invokingApplicationDescription;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Assertion methods used to sanity-check parameters provided to this service
    /////////////////////////////////////////////////////////////////////////////

    /**
     * Throws an exception if this service is anonymous (that is, it was created without
     * an {@link ProviderCredentials} object representing a user account.
     * @param action
     * the action being attempted which this assertion is applied, for debugging purposes.
     * @throws ServiceException
     */
    protected void assertAuthenticatedConnection(String action) throws ServiceException {
        if (!isAuthenticatedConnection()) {
            throw new ServiceException(
                "The requested action cannot be performed with a non-authenticated service: "
                    + action);
        }
    }

    /**
     * Throws an exception if a bucket is null or contains a null/empty name.
     * @param bucket
     * @param action
     * the action being attempted which this assertion is applied, for debugging purposes.
     * @throws ServiceException
     */
    protected void assertValidBucket(StorageBucket bucket, String action) throws ServiceException {
        if (bucket == null || bucket.getName() == null || bucket.getName().length() == 0) {
            throw new ServiceException("The action " + action
                + " cannot be performed with an invalid bucket: " + bucket);
        }
    }

    /**
     * Throws an exception if an object is null or contains a null/empty key.
     * @param object
     * @param action
     * the action being attempted which this assertion is applied, for debugging purposes.
     * @throws ServiceException
     */
    protected void assertValidObject(StorageObject object, String action) throws ServiceException {
        if (object == null || object.getKey() == null || object.getKey().length() == 0) {
            throw new ServiceException("The action " + action
                + " cannot be performed with an invalid object: " + object);
        }
    }

    /**
     * Throws an exception if an object's key name is null or empty.
     * @param key
     * An object's key name.
     * @param action
     * the action being attempted which this assertion is applied, for debugging purposes.
     * @throws ServiceException
     */
    protected void assertValidObject(String key, String action) throws ServiceException {
        if (key == null || key.length() == 0) {
            throw new ServiceException("The action " + action
                + " cannot be performed with an invalid object key name: " + key);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Methods below this point perform actions in a storage service
    ////////////////////////////////////////////////////////////////

    /**
     * Lists the objects in a bucket.
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can only list the objects in a publicly-readable bucket.
     *
     * @param bucketName
     * the name of the bucket whose contents will be listed.
     * @return
     * the set of objects contained in a bucket.
     * @throws ServiceException
     */
    public StorageObject[] listObjects(String bucketName) throws ServiceException {
        return listObjects(bucketName, null, null, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);
    }

    /**
     * Lists the objects in a bucket matching a prefix and delimiter.
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can only list the objects in a publicly-readable bucket.
     * <p>
     * NOTE: If you supply a delimiter value that could cause virtual path
     * "subdirectories" to be included in the results from the service, use the
     * {@link #listObjectsChunked(String, String, String, long, String, boolean)}
     * method instead of this one to obtain both object and path values.
     *
     * @param bucketName
     * the name of the bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * See note above.
     * <b>Note</b>: If a non-null delimiter is specified, the prefix must include enough text to
     * reach the first occurrence of the delimiter in the bucket's keys, or no results will be returned.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws ServiceException
     */
    public StorageObject[] listObjects(String bucketName, String prefix, String delimiter)
        throws ServiceException
    {
        return listObjects(bucketName, prefix, delimiter, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);
    }

    /**
     * Creates a bucket.
     *
     * <b>Caution:</b> Performing this operation unnecessarily when a bucket already
     * exists may cause OperationAborted errors with the message "A conflicting conditional
     * operation is currently in progress against this resource.". To avoid this error, use the
     * {@link #getOrCreateBucket(String)} in situations where the bucket may already exist.
     *
     * This method cannot be performed by anonymous services.
     *
     * @param bucketName
     * the name of the bucket to create.
     * @return
     * the created bucket object. <b>Note:</b> the object returned has minimal information about
     * the bucket that was created, including only the bucket's name.
     * @throws ServiceException
     */
    public StorageBucket createBucket(String bucketName) throws ServiceException {
        return createBucketImpl(bucketName, null, null);
    }

    /**
     * Create a bucket with the Access Control List settings of the bucket object (if any).
     * <p>
     * <b>Caution:</b> Performing this operation unnecessarily when a bucket already
     * exists may cause OperationAborted errors with the message "A conflicting conditional
     * operation is currently in progress against this resource.". To avoid this error, use the
     * {@link #getOrCreateBucket(String)} in situations where the bucket may already exist.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucket
     * the bucket to create, including optional ACL settings.
     * @return
     * the created bucket object. <b>Note:</b> the object returned has minimal information about
     * the bucket that was created, including only the bucket's name.
     * @throws ServiceException
     */
    public StorageBucket createBucket(StorageBucket bucket) throws ServiceException
    {
        return createBucketImpl(bucket.getName(), null, bucket.getAcl());
    }

    /**
     * Convenience method to check whether an object exists in a bucket.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * false if the object is not found in the bucket, true if the object
     * exists (although it may be inaccessible to you).
     * @throws ServiceException
     */
    public boolean isObjectInBucket(String bucketName, String objectKey)
        throws ServiceException
    {
        try {
            getObjectDetails(bucketName, objectKey);
        } catch (ServiceException e) {
            if (404 == e.getResponseCode()
                || "NoSuchKey".equals(e.getErrorCode())
                || "NoSuchBucket".equals(e.getErrorCode()))
            {
                return false;
            }
            if ("AccessDenied".equals(e.getErrorCode()))
            {
                // Object is inaccessible to current user, but does exist.
                return true;
            }
            // Something else has gone wrong
            throw e;
        }
        return true;
    }

    /**
     * Returns an object representing the details and data of an item in a service,
     * without applying any preconditions.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object.
     * <p>
     * <b>Important:</b> It is the caller's responsibility to close the object's data input stream.
     * The data stream should be consumed and closed as soon as is practical as network connections
     * may be held open until the streams are closed. Excessive unclosed streams can lead to
     * connection starvation.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key, including the object's data input stream.
     * @throws ServiceException
     */
    public StorageObject getObject(String bucketName, String objectKey) throws ServiceException {
        return getObject(bucketName, objectKey,
            null, null, null, null, null, null);
    }

    /**
     * Returns an object representing the details of an item in without the object's data, and
     * without applying any preconditions.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object's details.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key, including only general details and metadata
     * (not the data input stream)
     * @throws ServiceException
     */
    public StorageObject getObjectDetails(String bucketName, String objectKey)
        throws ServiceException
    {
        return getObjectDetails(bucketName, objectKey, null, null, null, null);
    }

    /**
     * Lists the buckets belonging to the service user.
     * <p>
     * This method cannot be performed by anonymous services, and will fail with an exception
     * if the service is not authenticated.
     *
     * @return
     * the list of buckets owned by the service user.
     * @throws ServiceException
     */
    public StorageBucket[] listAllBuckets() throws ServiceException {
        assertAuthenticatedConnection("List all buckets");
        StorageBucket[] buckets = listAllBucketsImpl();
        MxDelegate.getInstance().registerStorageBucketMBeans(buckets);
        return buckets;
    }

    /**
     * Returns the owner of an account, using information available in the
     * bucket listing response.
     * <p>
     * This method cannot be performed by anonymous services, and will fail with an exception
     * if the service is not authenticated.
     *
     * @return
     * the owner of the account.
     * @throws ServiceException
     */
    public StorageOwner getAccountOwner() throws ServiceException {
        assertAuthenticatedConnection("List all buckets to find account owner");
        return getAccountOwnerImpl();

    }

    /**
     * Lists the objects in a bucket matching a prefix, while instructing the service
     * to send response messages containing no more than a given number of object
     * results.
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can list the contents of a publicly-readable bucket.
     * <p>
     * NOTE: If you supply a delimiter value that could cause virtual path
     * "subdirectories" to be included in the results from the service, use the
     * {@link #listObjectsChunked(String, String, String, long, String, boolean)}
     * method instead of this one to obtain both object and path values.
     *
     * @param bucketName
     * the name of the the bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * See note above.
     * @param maxListingLength
     * the maximum number of objects to include in each result message. This value
     * has <strong>no effect</strong> on the number of objects
     * that will be returned by this method, because it will always return all
     * the objects in the bucket.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws ServiceException
     */
    public StorageObject[] listObjects(String bucketName, String prefix, String delimiter,
        long maxListingLength) throws ServiceException
    {
        MxDelegate.getInstance().registerStorageBucketListEvent(bucketName);
        StorageObject[] objects = listObjectsImpl(bucketName, prefix, delimiter, maxListingLength);
        MxDelegate.getInstance().registerStorageObjectMBean(bucketName, objects);
        return objects;
    }

    /**
     * Lists the objects in a bucket matching a prefix, chunking the results into batches of
     * a given size, and returning each chunk separately. It is the responsibility of the caller
     * to building a complete bucket object listing by performing follow-up requests if necessary.
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can list the contents of a publicly-readable bucket.
     *
     * @param bucketName
     * the name of the the bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed, may be null.
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * @param maxListingLength
     * the maximum number of objects to include in each result chunk
     * @param priorLastKey
     * the last object key received in a prior call to this method. The next chunk of objects
     * listed will start with the next object in the bucket <b>after</b> this key name.
     * This parameter may be null, in which case the listing will start at the beginning of the
     * bucket's object contents.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws ServiceException
     */
    public StorageObjectsChunk listObjectsChunked(String bucketName, String prefix, String delimiter,
        long maxListingLength, String priorLastKey) throws ServiceException
    {
        MxDelegate.getInstance().registerStorageBucketListEvent(bucketName);
        StorageObjectsChunk chunk = listObjectsChunkedImpl(
            bucketName, prefix, delimiter, maxListingLength, priorLastKey, false);
        MxDelegate.getInstance().registerStorageObjectMBean(bucketName, chunk.getObjects());
        return chunk;
    }

    /**
     * Lists the objects in a bucket matching a prefix and also returns the
     * common prefixes. Depending on the value of the completeListing
     * variable, this method can be set to automatically perform follow-up requests
     * to build a complete object listing, or to return only a partial listing.
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can list the contents of a publicly-readable bucket.
     *
     * @param bucketName
     * the name of the the bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed, may be null.
     * @param delimiter
     * only objects with a key that starts with this prefix will be listed, may be null.
     * @param maxListingLength
     * the maximum number of objects to include in each result chunk
     * @param priorLastKey
     * the last object key received in a prior call to this method. The next chunk of objects
     * listed will start with the next object in the bucket <b>after</b> this key name.
     * This parameter may be null, in which case the listing will start at the beginning of the
     * bucket's object contents.
     * @param completeListing
     * if true, the service class will automatically perform follow-up requests to
     * build a complete bucket object listing.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws ServiceException
     */
    public StorageObjectsChunk listObjectsChunked(String bucketName, String prefix, String delimiter,
        long maxListingLength, String priorLastKey, boolean completeListing) throws ServiceException
    {
        MxDelegate.getInstance().registerStorageBucketListEvent(bucketName);
        StorageObjectsChunk chunk = listObjectsChunkedImpl(
            bucketName, prefix, delimiter, maxListingLength, priorLastKey, completeListing);
        MxDelegate.getInstance().registerStorageObjectMBean(bucketName, chunk.getObjects());
        return chunk;
    }

    /**
     * Returns a bucket in your account by listing all your buckets
     * (using {@link #listAllBuckets()}), and looking for the named bucket in
     * this list.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucketName
     * @return
     * the bucket in your account, or null if you do not own the named bucket.
     *
     * @throws ServiceException
     */
    public StorageBucket getBucket(String bucketName) throws ServiceException {
        assertAuthenticatedConnection("Get Bucket");

        // List existing buckets and return the named bucket if it exists.
        StorageBucket[] existingBuckets = listAllBuckets();
        for (int i = 0; i < existingBuckets.length; i++) {
            if (existingBuckets[i].getName().equals(bucketName)) {
                return existingBuckets[i];
            }
        }
        return null;
    }

    /**
     * Returns a bucket in your account, and creates the bucket if
     * it does not yet exist.
     *
     * @param bucketName
     * the name of the bucket to retrieve or create.
     * @return
     * the bucket in your account.
     *
     * @throws ServiceException
     */
    public StorageBucket getOrCreateBucket(String bucketName) throws ServiceException {
        StorageBucket bucket = getBucket(bucketName);
        if (bucket == null) {
            // Bucket does not exist in this user's account, create it.
            bucket = createBucket(bucketName);
        }
        return bucket;
    }

    /**
     * Deletes a bucket. Only the owner of a bucket may delete it.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucket
     * the bucket to delete.
     * @throws ServiceException
     */
    public void deleteBucket(StorageBucket bucket) throws ServiceException {
        assertValidBucket(bucket, "Delete bucket");
        deleteBucketImpl(bucket.getName());
    }

    /**
     * Deletes a bucket. Only the owner of a bucket may delete it.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucketName
     * the name of the bucket to delete.
     * @throws ServiceException
     */
    public void deleteBucket(String bucketName) throws ServiceException {
        deleteBucketImpl(bucketName);
    }

    /**
     * Puts an object inside an existing bucket, creating a new object or overwriting
     * an existing one with the same key. The Access Control List settings of the object
     * (if any) will also be applied.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can put objects into a publicly-writable bucket.
     *
     * @param bucketName
     * the name of the bucket inside which the object will be put.
     * @param object
     * the object containing all information that will be written to the service.
     * At very least this object must be valid. Beyond that it may contain: an input stream
     * with the object's data content, metadata, and access control settings.
     * <p>
     * <b>Note:</b> It is very important to set the object's Content-Length to match the size of the
     * data input stream when possible, as this can remove the need to read data into memory to
     * determine its size.
     *
     * @return
     * the object populated with any metadata.
     * @throws ServiceException
     */
    public StorageObject putObject(String bucketName, StorageObject object)
        throws ServiceException
    {
        assertValidObject(object, "Create Object in bucket " + bucketName);
        MxDelegate.getInstance().registerStorageObjectPutEvent(bucketName, object.getKey());
        return putObjectImpl(bucketName, object);
    }

    /**
     * Copy an object. You can copy an object within a single bucket or between buckets,
     * and can optionally update the object's metadata at the same time.
     * <p>
     * This method cannot be performed by anonymous services. You must have read
     * access to the source object and write access to the destination bucket.
     * <p>
     * An object can be copied over itself, in which case you can update its
     * metadata without making any other changes.
     *
     * @param sourceBucketName
     * the name of the bucket that contains the original object.
     * @param sourceObjectKey
     * the key name of the original object.
     * @param destinationBucketName
     * the name of the destination bucket to which the object will be copied.
     * @param destinationObject
     * the object that will be created by the copy operation. If this item
     * includes an AccessControlList setting the copied object will be assigned
     * that ACL, otherwise the copied object will be assigned the default private
     * ACL setting.
     * @param replaceMetadata
     * If this parameter is true, the copied object will be assigned the metadata
     * values present in the destinationObject. Otherwise, the copied object will
     * have the same metadata as the original object.
     * @param ifModifiedSince
     * a precondition specifying a date after which the object must have been
     * modified, ignored if null.
     * @param ifUnmodifiedSince
     * a precondition specifying a date after which the object must not have
     * been modified, ignored if null.
     * @param ifMatchTags
     * a precondition specifying an MD5 hash the object must match, ignored if
     * null.
     * @param ifNoneMatchTags
     * a precondition specifying an MD5 hash the object must not match, ignored
     * if null.
     *
     * @return
     * a map of the header and result information resulting from the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws ServiceException
     */
    public Map<String, Object> copyObject(String sourceBucketName, String sourceObjectKey,
        String destinationBucketName, StorageObject destinationObject, boolean replaceMetadata,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws ServiceException
    {
        assertAuthenticatedConnection("copyObject");
        Map<String, Object> destinationMetadata =
            replaceMetadata ? destinationObject.getModifiableMetadata() : null;

        MxDelegate.getInstance().registerStorageObjectCopyEvent(sourceBucketName, sourceObjectKey);
        return copyObjectImpl(sourceBucketName, sourceObjectKey,
            destinationBucketName, destinationObject.getKey(),
            destinationObject.getAcl(), destinationMetadata,
            ifModifiedSince, ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, null,
            destinationObject.getStorageClass());
    }

    /**
     * Copy an object. You can copy an object within a
     * single bucket or between buckets, and can optionally update the object's
     * metadata at the same time.
     * <p>
     * This method cannot be performed by anonymous services. You must have read
     * access to the source object and write access to the destination bucket.
     * <p>
     * An object can be copied over itself, in which case you can update its
     * metadata without making any other changes.
     *
     * @param sourceBucketName
     * the name of the bucket that contains the original object.
     * @param sourceObjectKey
     * the key name of the original object.
     * @param destinationBucketName
     * the name of the destination bucket to which the object will be copied.
     * @param destinationObject
     * the object that will be created by the copy operation. If this item
     * includes an AccessControlList setting the copied object will be assigned
     * that ACL, otherwise the copied object will be assigned the default private
     * ACL setting.
     * @param replaceMetadata
     * If this parameter is true, the copied object will be assigned the metadata
     * values present in the destinationObject. Otherwise, the copied object will
     * have the same metadata as the original object.
     *
     * @return
     * a map of the header and result information after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws ServiceException
     */
    public Map<String, Object> copyObject(String sourceBucketName, String sourceObjectKey,
        String destinationBucketName, StorageObject destinationObject,
        boolean replaceMetadata) throws ServiceException
    {
        return copyObject(sourceBucketName, sourceObjectKey, destinationBucketName,
            destinationObject, replaceMetadata, null, null, null, null);
    }

    /**
     * Move an object. This method works by invoking the
     * {@link #copyObject(String, String, String, StorageObject, boolean)} method to
     * copy the original object, then deletes the original object once the
     * copy has succeeded.
     * <p>
     * This method cannot be performed by anonymous services. You must have read
     * access to the source object, write access to the destination bucket, and
     * write access to the source bucket.
     * <p>
     * If the copy operation succeeds but the delete operation fails, this
     * method will not throw an exception but the result map object will contain
     * an item named "DeleteException" with the exception thrown by the delete
     * operation.
     *
     * @param sourceBucketName
     * the name of the bucket that contains the original object.
     * @param sourceObjectKey
     * the key name of the original object.
     * @param destinationBucketName
     * the name of the destination bucket to which the object will be copied.
     * @param destinationObject
     * the object that will be created by the move operation. If this item
     * includes an AccessControlList setting the copied object will be assigned
     * that ACL, otherwise the copied object will be assigned the default private
     * ACL setting.
     * @param replaceMetadata
     * If this parameter is true, the copied object will be assigned the metadata
     * values present in the destinationObject. Otherwise, the copied object will
     * have the same metadata as the original object.
     *
     * @return
     * a map of the header and result information after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified). If the object was
     * successfully copied but the original could not be deleted, the map will
     * also include an item named "DeleteException" with the exception thrown by
     * the delete operation.
     *
     * @throws ServiceException
     */
    public Map<String, Object> moveObject(String sourceBucketName, String sourceObjectKey,
        String destinationBucketName, StorageObject destinationObject,
        boolean replaceMetadata) throws ServiceException
    {
        Map<String, Object> copyResult = copyObject(sourceBucketName, sourceObjectKey,
            destinationBucketName, destinationObject, replaceMetadata);

        try {
            deleteObject(sourceBucketName, sourceObjectKey);
        } catch (Exception e) {
            copyResult.put("DeleteException", e);
        }
        return copyResult;
    }

    /**
     * Rename an object. This method works by invoking the
     * {@link #moveObject(String, String, String, StorageObject, boolean)} method to
     * move the original object to a new key name.
     * <p>
     * The original object's metadata is retained, but to apply an access
     * control setting other than private you must specify an ACL in the
     * destination object.
     * <p>
     * This method cannot be performed by anonymous services. You must have
     * write access to the source object and write access to the bucket.
     *
     * @param bucketName
     * the name of the bucket containing the original object that will be copied.
     * @param sourceObjectKey
     * the key name of the original object.
     * @param destinationObject
     * the object that will be created by the rename operation. If this item
     * includes an AccessControlList setting the copied object will be assigned
     * that ACL, otherwise the copied object will be assigned the default private
     * ACL setting.
     *
     * @return
     * a map of the header and result information after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified). If the object was
     * successfully copied but the original could not be deleted, the map will
     * also include an item named "DeleteException" with the exception thrown by
     * the delete operation.
     *
     * @throws ServiceException
     */
    public Map<String, Object> renameObject(String bucketName, String sourceObjectKey,
        StorageObject destinationObject) throws ServiceException
    {
        return moveObject(bucketName, sourceObjectKey,
            bucketName, destinationObject, false);
    }

    /**
     * Update an object's metadata. This method works by invoking the
     * {@link #copyObject(String, String, String, StorageObject, boolean)} method to
     * copy the original object over itself, applying the new metadata in the
     * process.
     *
     * @param bucketName
     * the name of the bucket containing the object that will be updated.
     * @param object
     * the object that will be updated. If this item includes an
     * AccessControlList setting the copied object will be assigned
     * that ACL, otherwise the copied object will be assigned the default private
     * ACL setting.
     *
     * @return
     * a map of the header and result information after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws ServiceException
     */
    public Map<String, Object> updateObjectMetadata(String bucketName, StorageObject object)
        throws ServiceException
    {
        return copyObject(bucketName, object.getKey(),
            bucketName, object, true);
    }

    /**
     * Deletes an object from a bucket.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can delete objects from publicly-writable buckets.
     *
     * @param bucketName
     * the name of the bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object
     * @throws ServiceException
     */
    public void deleteObject(String bucketName, String objectKey) throws ServiceException {
        assertValidObject(objectKey, "deleteObject");
        MxDelegate.getInstance().registerStorageObjectDeleteEvent(bucketName, objectKey);
        deleteObjectImpl(bucketName, objectKey, null, null, null);
    }

    /**
     * Returns an object representing the details of an item that meets any given preconditions.
     * The object is returned without the object's data.
     * <p>
     * An exception is thrown if any of the preconditions fail.
     * Preconditions are only applied if they are non-null.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get details of publicly-readable objects.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @param ifModifiedSince
     * a precondition specifying a date after which the object must have been modified, ignored if null.
     * @param ifUnmodifiedSince
     * a precondition specifying a date after which the object must not have been modified, ignored if null.
     * @param ifMatchTags
     * a precondition specifying an MD5 hash the object must match, ignored if null.
     * @param ifNoneMatchTags
     * a precondition specifying an MD5 hash the object must not match, ignored if null.
     * @return
     * the object with the given key, including only general details and metadata (not the data
     * input stream)
     * @throws ServiceException
     */
    public StorageObject getObjectDetails(String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws ServiceException
    {
        MxDelegate.getInstance().registerStorageObjectHeadEvent(bucketName, objectKey);
        return getObjectDetailsImpl(bucketName, objectKey, ifModifiedSince, ifUnmodifiedSince,
            ifMatchTags, ifNoneMatchTags, null);
    }

    /**
     * Returns an object representing the details and data of an item that meets any given preconditions.
     * <p>
     * <b>Important:</b> It is the caller's responsibility to close the object's data input stream.
     * The data stream should be consumed and closed as soon as is practical as network connections
     * may be held open until the streams are closed. Excessive unclosed streams can lead to
     * connection starvation.
     * <p>
     * An exception is thrown if any of the preconditions fail.
     * Preconditions are only applied if they are non-null.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object.
     * <p>
     * <b>Implementation notes</b><p>
     * Implementations should use {@link #assertValidBucket} assertion.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @param ifModifiedSince
     * a precondition specifying a date after which the object must have been modified, ignored if null.
     * @param ifUnmodifiedSince
     * a precondition specifying a date after which the object must not have been modified, ignored if null.
     * @param ifMatchTags
     * a precondition specifying an MD5 hash the object must match, ignored if null.
     * @param ifNoneMatchTags
     * a precondition specifying an MD5 hash the object must not match, ignored if null.
     * @param byteRangeStart
     * include only a portion of the object's data - starting at this point, ignored if null.
     * @param byteRangeEnd
     * include only a portion of the object's data - ending at this point, ignored if null.
     * @return
     * the object with the given key, including only general details and metadata (not the data
     * input stream)
     * @throws ServiceException
     */
    public StorageObject getObject(String bucketName, String objectKey, Calendar ifModifiedSince,
        Calendar ifUnmodifiedSince, String[] ifMatchTags, String[] ifNoneMatchTags,
        Long byteRangeStart, Long byteRangeEnd) throws ServiceException
    {
        MxDelegate.getInstance().registerStorageObjectGetEvent(bucketName, objectKey);
        return getObjectImpl(bucketName, objectKey, ifModifiedSince, ifUnmodifiedSince,
            ifMatchTags, ifNoneMatchTags, byteRangeStart, byteRangeEnd, null);
    }

    /**
     * Applies access control settings to an object. The ACL settings must be included
     * with the object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object allows you to do so.
     *
     * @param bucketName
     * the name of the bucket containing the object to modify.
     * @param object
     * the object with ACL settings that will be applied.
     * @throws ServiceException
     */
    public void putObjectAcl(String bucketName, StorageObject object) throws ServiceException {
        assertValidObject(object, "Put Object Access Control List");
        putObjectAcl(bucketName, object.getKey(), object.getAcl());
    }

    /**
     * Applies access control settings to an object. The ACL settings must be included
     * with the object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object allows you to do so.
     *
     * @param bucketName
     * the name of the bucket containing the object to modify.
     * @param objectKey
     * the key name of the object with ACL settings that will be applied.
     * @param acl
     * the ACL to apply.
     * @throws ServiceException
     */
    public void putObjectAcl(String bucketName, String objectKey, AccessControlList acl)
        throws ServiceException
    {
        if (acl == null) {
            throw new ServiceException("The object '" + objectKey +
                "' does not include ACL information");
        }
        putObjectAclImpl(bucketName, objectKey, acl, null);
    }

    /**
     * Retrieves the access control settings of an object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object allows you to do so.
     *
     * @param bucketName
     * the name of the bucket whose ACL settings will be retrieved (if objectKey is null) or the
     * name of the bucket containing the object whose ACL settings will be retrieved (if objectKey is non-null).
     * @param objectKey
     * if non-null, the key of the object whose ACL settings will be retrieved. Ignored if null.
     * @return
     * the ACL settings of the bucket or object.
     * @throws ServiceException
     */
    public AccessControlList getObjectAcl(String bucketName, String objectKey)
        throws ServiceException
    {
        return getObjectAclImpl(bucketName, objectKey, null);
    }

    /**
     * Applies access control settings to a bucket. The ACL settings must be included
     * inside the bucket.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * bucket's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of a bucket if the ACL already in place
     * for that bucket allows you to do so.
     *
     * @param bucketName
     * a name of the bucket with ACL settings to apply.
     * @param acl
     * the ACL to apply.
     * @throws ServiceException
     */
    public void putBucketAcl(String bucketName, AccessControlList acl) throws ServiceException {
        if (acl == null) {
            throw new ServiceException("The bucket '" + bucketName +
                "' does not include ACL information");
        }
        putBucketAclImpl(bucketName, acl);
    }

    /**
     * Applies access control settings to a bucket. The ACL settings must be included
     * inside the bucket.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * bucket's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of a bucket if the ACL already in place
     * for that bucket allows you to do so.
     *
     * @param bucket
     * a bucket with ACL settings to apply.
     * @throws ServiceException
     */
    public void putBucketAcl(StorageBucket bucket) throws ServiceException {
        assertValidBucket(bucket, "Put Bucket Access Control List");
        putBucketAcl(bucket.getName(), bucket.getAcl());
    }

    /**
     * Retrieves the access control settings of a bucket.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * bucket's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of a bucket if the ACL already in place
     * for that bucket allows you to do so.
     *
     * @param bucketName
     * the name of the bucket whose access control settings will be returned.
     * @return
     * the ACL settings of the bucket.
     * @throws ServiceException
     */
    public AccessControlList getBucketAcl(String bucketName) throws ServiceException {
        return getBucketAclImpl(bucketName);
    }

    /**
     * Returns the current date and time, adjusted according to the time
     * offset between your computer and an AWS server (as set by the
     * {@link RestUtils#getAWSTimeAdjustment()} method).
     *
     * @return
     * the current time, or the current time adjusted to match the AWS time
     * if the {@link RestUtils#getAWSTimeAdjustment()} method has been invoked.
     */
    public Date getCurrentTimeWithOffset() {
        return new Date(System.currentTimeMillis() + timeOffset);
    }

    /**
     * Renames metadata property names to be suitable for use as HTTP Headers. This is done
     * by renaming any non-HTTP headers to have the a service-specific prefix and leaving the
     * HTTP header names unchanged. The HTTP header names left unchanged are those found in
     * {@link RestUtils#HTTP_HEADER_METADATA_NAMES}
     *
     * @param metadata
     * @return
     * a map of metadata property name/value pairs renamed to be suitable for use as HTTP headers.
     */
    public Map<String, Object> renameMetadataKeys(Map<String, Object> metadata) {
        Map<String, Object> convertedMetadata = new HashMap<String, Object>();
        // Add all meta-data headers.
        if (metadata != null) {
            for (Map.Entry<String, Object> entry: metadata.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (!RestUtils.HTTP_HEADER_METADATA_NAMES.contains(key.toLowerCase(Locale.getDefault()))
                    && !key.startsWith(this.getRestHeaderPrefix()))
                {
                    key = this.getRestMetadataPrefix() + key;
                }
                convertedMetadata.put(key, value);
            }
        }
        return convertedMetadata;
    }

    // ///////////////////////////////////////////////////////////////////////////////
    // Abstract methods that must be implemented by interface-specific service classes
    // ///////////////////////////////////////////////////////////////////////////////

    /**
     * Indicates whether a bucket exists and is accessible to a service user.
     * <p>
     * <b>Caution:</b> This check started to cause issues in situations where you need to
     * immediately create a bucket when it does not exist. To conditionally create a bucket,
     * use the {@link #getOrCreateBucket(String)} method instead.
     * <p>
     * This method can be performed by anonymous services.
     * <p>
     * <b>Implementation notes</b><p>
     * This method can be implemented by attempting to list the objects in a bucket. If the listing
     * is successful return true, if the listing failed for any reason return false.
     *
     * @param bucketName
     * the bucket to check.
     * @return
     * true if the bucket exists and is accessible to the service user, false otherwise.
     * @throws ServiceException
     */
    public abstract boolean isBucketAccessible(String bucketName) throws ServiceException;

    /**
     * Find out the status of a bucket with the given name.
     * <p>
     * <b>Caveats:</b>
     * <ul>
     * <li>If someone else owns the bucket but has made it public, this method will
     * mistakenly return {@link #BUCKET_STATUS__MY_BUCKET}.</li>
     * <li>
     * <p>
     * S3 can act strangely when you use this method in some circumstances.
     * If you check the status of a bucket and find that it does not exist, then create the
     * bucket, the service will continue to tell you the bucket does not exists for up to 30
     * seconds. This problem has something to do with connection caching (I think).</p>
     * <p>
     * This S3 quirk makes it a bad idea to use this method to check for a bucket's
     * existence before creating that bucket. Use the {@link #getOrCreateBucket(String)}
     * method for this purpose instead.</p>
     * </li>
     * </ul>
     * </p>
     *
     * @param bucketName
     * @return
     * {@link #BUCKET_STATUS__MY_BUCKET} if you already own the bucket,
     * {@link #BUCKET_STATUS__DOES_NOT_EXIST} if the bucket does not yet exist, or
     * {@link #BUCKET_STATUS__ALREADY_CLAIMED} if someone else has
     * already created a bucket with the given name.
     *
     * @throws ServiceException
     */
    public abstract int checkBucketStatus(String bucketName) throws ServiceException;

    /**
     * @return
     * the buckets in your account.
     *
     * @throws ServiceException
     */
    protected abstract StorageBucket[] listAllBucketsImpl() throws ServiceException;

    /**
     * @return
     * the owner of an account.
     * @throws ServiceException
     */
    protected abstract StorageOwner getAccountOwnerImpl() throws ServiceException;

    /**
     * Lists objects in a bucket.
     *
     * <b>Implementation notes</b><p>
     * The implementation of this method is expected to return <b>all</b> the objects
     * in a bucket, not a subset. This may require repeating the list operation if the
     * first one doesn't include all the available objects, such as when the number of objects
     * is greater than <code>maxListingLength</code>.
     * <p>
     *
     * @param bucketName
     * @param prefix
     * only objects with a key that starts with this prefix will be listed, may be null.
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * @param maxListingLength
     * @return
     * the objects in a bucket.
     *
     * @throws ServiceException
     */
    protected abstract StorageObject[] listObjectsImpl(String bucketName, String prefix,
        String delimiter, long maxListingLength) throws ServiceException;

    /**
     * Lists objects in a bucket up to the maximum listing length specified.
     *
     * <p>
     * <b>Implementation notes</b>
     * The implementation of this method returns only as many objects as requested in the chunk
     * size. It is the responsibility of the caller to build a complete object listing from
     * multiple chunks, should this be necessary.
     * </p>
     *
     * @param bucketName
     * @param prefix
     * only objects with a key that starts with this prefix will be listed, may be null.
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * @param maxListingLength
     * @param priorLastKey
     * @param completeListing
     * @throws ServiceException
     */
    protected abstract StorageObjectsChunk listObjectsChunkedImpl(String bucketName, String prefix,
        String delimiter, long maxListingLength, String priorLastKey, boolean completeListing)
        throws ServiceException;

    /**
     * Creates a bucket.
     *
     * <b>Implementation notes</b><p>
     * The implementing method must populate the bucket object's metadata with the results of the
     * operation before returning the object. It must also apply any <code>AccessControlList</code>
     * settings included with the bucket.
     *
     * @param bucketName
     * the name of the bucket to create.
     * @param location
     * the geographical location where the bucket will be stored (if applicable for the target
     * service). A null string value will cause the bucket to be stored in the default location.
     * @param acl
     * an access control object representing the initial acl values for the bucket.
     * May be null, in which case the default permissions are applied.
     * @return
     * the created bucket object, populated with all metadata made available by the creation operation.
     * @throws ServiceException
     */
    protected abstract StorageBucket createBucketImpl(String bucketName, String location,
        AccessControlList acl) throws ServiceException;

    protected abstract void deleteBucketImpl(String bucketName) throws ServiceException;

    protected abstract StorageObject putObjectImpl(String bucketName, StorageObject object) throws ServiceException;

    /**
     * Copy an object within your account. Copies within a single bucket or between
     * buckets, and optionally updates the object's metadata at the same time. An
     * object can be copied over itself, allowing you to update the metadata without
     * making any other changes.
     *
     * @param sourceBucketName
     * the name of the bucket that contains the original object.
     * @param sourceObjectKey
     * the key name of the original object.
     * @param destinationBucketName
     * the name of the destination bucket to which the object will be copied.
     * @param destinationObjectKey
     * the key name for the copied object.
     * @param acl
     * the access control settings that will be applied to the copied object.
     * If this parameter is null, the default (private) ACL setting will be
     * applied to the copied object.
     * @param destinationMetadata
     * metadata items to apply to the copied object. If this parameter is null,
     * the metadata will be copied unchanged from the original object. If this
     * parameter is not null, the copied object will have only the supplied
     * metadata.
     *
     * @return
     * a map of the header and result information returned after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws ServiceException
     */
    protected abstract Map<String, Object> copyObjectImpl(String sourceBucketName, String sourceObjectKey,
        String destinationBucketName, String destinationObjectKey,
        AccessControlList acl, Map<String, Object> destinationMetadata,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
        String[] ifMatchTags, String[] ifNoneMatchTags, String versionId,
        String destinationObjectStorageClass)
        throws ServiceException;

    protected abstract void deleteObjectImpl(String bucketName, String objectKey,
        String versionId, String multiFactorSerialNumber, String multiFactorAuthCode)
        throws ServiceException;

    protected abstract StorageObject getObjectDetailsImpl(String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags, String versionId) throws ServiceException;

    protected abstract StorageObject getObjectImpl(String bucketName, String objectKey, Calendar ifModifiedSince,
        Calendar ifUnmodifiedSince, String[] ifMatchTags, String[] ifNoneMatchTags,
        Long byteRangeStart, Long byteRangeEnd, String versionId) throws ServiceException;

    protected abstract void putBucketAclImpl(String bucketName, AccessControlList acl)
        throws ServiceException;

    protected abstract void putObjectAclImpl(String bucketName, String objectKey,
        AccessControlList acl, String versionId) throws ServiceException;

    protected abstract AccessControlList getObjectAclImpl(String bucketName, String objectKey,
        String versionId) throws ServiceException;

    protected abstract AccessControlList getBucketAclImpl(String bucketName) throws ServiceException;

    protected abstract void shutdownImpl() throws ServiceException;

    /**
     * @return
     * the URL end-point of the target service.
     */
    public abstract String getEndpoint();
    protected abstract String getVirtualPath();
    protected abstract String getSignatureIdentifier();
    /**
     * @return
     * the REST header prefix used by the target service.
     */
    public abstract String getRestHeaderPrefix();
    /**
     * @return
     * GET parameter names that represent specific resources in the target
     * service, as opposed to representing REST operation "plumbing". For
     * example the "acl" parameter might be used to represent a resource's
     * access control list settings.
     */
    public abstract List<String> getResourceParameterNames();
    /**
     * @return
     * the REST header prefix used by the target service to identify
     * metadata information.
     */
    public abstract String getRestMetadataPrefix();
    protected abstract int getHttpPort();
    protected abstract int getHttpsPort();
    protected abstract boolean getHttpsOnly();
    protected abstract boolean getDisableDnsBuckets();
    protected abstract boolean getEnableStorageClasses();

}

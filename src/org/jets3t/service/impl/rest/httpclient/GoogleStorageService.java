/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 James Murty
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
package org.jets3t.service.impl.rest.httpclient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.security.ProviderCredentials;

/**
 * REST/HTTP implementation of Google Storage Service based on the
 * <a href="http://jakarta.apache.org/commons/httpclient/">HttpClient</a> library.
 * <p>
 * This class uses properties obtained through {@link org.jets3t.service.Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author Google Developers
 */
public class GoogleStorageService extends RestStorageService {

    private static final String GOOGLE_SIGNATURE_IDENTIFIER = "GOOG1";
    private static final String GOOGLE_REST_HEADER_PREFIX = "x-goog-";
    private static final String GOOGLE_REST_METADATA_PREFIX = "x-goog-meta-";

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the user credentials to use when communicating with Google Storage, may be null in which case the
     * communication is done as an anonymous user.
     *
     * @throws ServiceException
     */
    public GoogleStorageService(ProviderCredentials credentials) throws ServiceException {
        this(credentials, null, null);
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the user credentials to use when communicating with Google Storage, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider
     * an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     * prompting for credentials when necessary.
     *
     * @throws ServiceException
     */
    public GoogleStorageService(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider) throws ServiceException
    {
        this(credentials, invokingApplicationDescription, credentialsProvider,
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the user credentials to use when communicating with Google Storage, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider
     * an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     * prompting for credentials when necessary.
     * @param jets3tProperties
     * JetS3t properties that will be applied within this service.
     *
     * @throws ServiceException
     */
    public GoogleStorageService(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties)
        throws ServiceException
    {
        this(credentials, invokingApplicationDescription, credentialsProvider,
            jets3tProperties, new HostConfiguration());
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the user credentials to use when communicating with Google Storage, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider
     * an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     * prompting for credentials when necessary.
     * @param jets3tProperties
     * JetS3t properties that will be applied within this service.
     * @param hostConfig
     * Custom HTTP host configuration; e.g to register a custom Protocol Socket Factory
     *
     * @throws ServiceException
     */
    public GoogleStorageService(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties,
        HostConfiguration hostConfig) throws ServiceException
    {
        super(credentials, invokingApplicationDescription, credentialsProvider, jets3tProperties, hostConfig);
    }

    @Override
    protected boolean isTargettingGoogleStorageService() {
        return true;
    }

    /**
     * @return
     * the endpoint to be used to connect to Google Storage.
     */
    @Override
    public String getEndpoint() {
        return this.jets3tProperties.getStringProperty(
                "gsservice.gs-endpoint", Constants.GS_DEFAULT_HOSTNAME);
    }

    /**
     * @return
     * the virtual path inside the service.
     */
    @Override
    protected String getVirtualPath() {
        return this.jets3tProperties.getStringProperty(
                "gsservice.gs-endpoint-virtual-path", "");
    }

    /**
     * @return
     * the identifier for the signature algorithm.
     */
    @Override
    protected String getSignatureIdentifier() {
        return GOOGLE_SIGNATURE_IDENTIFIER;
    }

    /**
     * @return
     * header prefix for general Google Storage headers: x-goog-.
     */
    @Override
    public String getRestHeaderPrefix() {
        return GOOGLE_REST_HEADER_PREFIX;
    }

    /**
     * @return
     * header prefix for Google Storage metadata headers: x-goog-meta-.
     */
    @Override
    public String getRestMetadataPrefix() {
        return GOOGLE_REST_METADATA_PREFIX;
    }

    @Override
    public List<String> getResourceParameterNames() {
        // Special HTTP parameter names that refer to resources in Google Storage
        return Arrays.asList(new String[] {
            "acl"
        });
    }

    /**
     * @return
     * the port number to be used for insecure connections over HTTP.
     */
    @Override
    protected int getHttpPort() {
      return this.jets3tProperties.getIntProperty("gsservice.gs-endpoint-http-port", 80);
    }

    /**
     * @return
     * the port number to be used for secure connections over HTTPS.
     */
    @Override
    protected int getHttpsPort() {
      return this.jets3tProperties.getIntProperty("gsservice.gs-endpoint-https-port", 443);
    }

    /**
     * @return
     * If true, all communication with GS will be via encrypted HTTPS connections,
     * otherwise communications will be sent unencrypted via HTTP
     */
    @Override
    protected boolean getHttpsOnly() {
      return this.jets3tProperties.getBoolProperty("gsservice.https-only", true);
    }

    /**
     * @return
     * If true, JetS3t will specify bucket names in the request path of the HTTP message
     * instead of the Host header.
     */
    @Override
    protected boolean getDisableDnsBuckets() {
      return this.jets3tProperties.getBoolProperty("gsservice.disable-dns-buckets", false);
    }

    /**
     * @return
     * If true, JetS3t will enable support for Storage Classes.
     */
    @Override
    protected boolean getEnableStorageClasses() {
      return false;
    }

    ////////////////////////////////////////////////////////////
    // Methods below this point perform actions in GoogleStorage
    ////////////////////////////////////////////////////////////

    @Override
    public GSBucket[] listAllBuckets() throws ServiceException {
        return GSBucket.cast(super.listAllBuckets());
    }

    @Override
    public GSObject[] listObjects(String bucketName) throws ServiceException {
        return GSObject.cast(super.listObjects(bucketName));
    }

    @Override
    public GSObject[] listObjects(String bucketName, String prefix, String delimiter)
        throws ServiceException
    {
        return GSObject.cast(super.listObjects(bucketName, prefix, delimiter));
    }


    @Override
    public GSBucket createBucket(String bucketName) throws ServiceException {
        return (GSBucket) super.createBucket(bucketName);
    }

    @Override
    public GSAccessControlList getBucketAcl(String bucketName) throws ServiceException {
        return (GSAccessControlList) super.getBucketAcl(bucketName);
    }

    /**
     * Applies access control settings to a bucket. The ACL settings must be included
     * inside the bucket.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * bucket's existing ACL already allows write access by the anonymous user.
     *
     * @param bucketName
     * a name of the bucket with ACL settings to apply.
     * @throws ServiceException
     */
    public void putBucketAcl(String bucketName, GSAccessControlList acl) throws ServiceException {
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
     *
     * @param bucket
     * a bucket with ACL settings to apply.
     * @throws ServiceException
     */
    public void putBucketAcl(GSBucket bucket) throws ServiceException {
        assertValidBucket(bucket, "Put Bucket Access Control List");
        putBucketAcl(bucket.getName(), bucket.getAcl());
    }

    @Override
    public GSObject getObject(String bucketName, String objectKey) throws ServiceException {
        return (GSObject) super.getObject(bucketName, objectKey);
    }

    public GSObject putObject(String bucketName, GSObject object)
        throws ServiceException
    {
        return (GSObject) super.putObject(bucketName, object);
    }

    @Override
    public GSObject getObject(String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
        String[] ifMatchTags, String[] ifNoneMatchTags, Long byteRangeStart,
        Long byteRangeEnd) throws ServiceException
    {
        return (GSObject) super.getObject(bucketName, objectKey, ifModifiedSince,
            ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, byteRangeStart,
            byteRangeEnd);
    }

    @Override
    public GSObject getObjectDetails(String bucketName, String objectKey)
        throws ServiceException
    {
        return (GSObject) super.getObjectDetails(bucketName, objectKey);
    }

}

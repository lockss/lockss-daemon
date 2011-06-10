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
package org.jets3t.service.impl.rest.httpclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.VersionOrDeleteMarkersChunk;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.CompleteMultipartUploadResultHandler;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.ListMultipartPartsResultHandler;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.ListMultipartUploadsResultHandler;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.ListVersionsResultsHandler;
import org.jets3t.service.model.BaseVersionOrDeleteMarker;
import org.jets3t.service.model.MultipartCompleted;
import org.jets3t.service.model.MultipartPart;
import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.NotificationConfig;
import org.jets3t.service.model.S3BucketLoggingStatus;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.security.AWSDevPayCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.RestUtils;

import com.jamesmurty.utils.XMLBuilder;

/**
 * REST/HTTP implementation of an S3Service based on the
 * <a href="http://jakarta.apache.org/commons/httpclient/">HttpClient</a> library.
 * <p>
 * This class uses properties obtained through {@link Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author James Murty
 */
public class RestS3Service extends S3Service {

    private static final Log log = LogFactory.getLog(RestS3Service.class);
    private static final String AWS_SIGNATURE_IDENTIFIER = "AWS";
    private static final String AWS_REST_HEADER_PREFIX = "x-amz-";
    private static final String AWS_REST_METADATA_PREFIX = "x-amz-meta-";

    private String awsDevPayUserToken = null;
    private String awsDevPayProductToken = null;

    private boolean isRequesterPaysEnabled = false;

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the user credentials to use when communicating with S3, may be null in which case the
     * communication is done as an anonymous user.
     *
     * @throws S3ServiceException
     */
    public RestS3Service(ProviderCredentials credentials) throws S3ServiceException {
        this(credentials, null, null);
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider
     * an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     * prompting for credentials when necessary.
     *
     * @throws S3ServiceException
     */
    public RestS3Service(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider) throws S3ServiceException
    {
        this(credentials, invokingApplicationDescription, credentialsProvider,
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
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
     * @throws S3ServiceException
     */
    public RestS3Service(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties)
        throws S3ServiceException
    {
        this(credentials, invokingApplicationDescription, credentialsProvider,
            jets3tProperties, new HostConfiguration());
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
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
     * @throws S3ServiceException
     */
    public RestS3Service(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties,
        HostConfiguration hostConfig) throws S3ServiceException
    {
        super(credentials, invokingApplicationDescription, credentialsProvider, jets3tProperties, hostConfig);

        if (credentials instanceof AWSDevPayCredentials) {
            AWSDevPayCredentials awsDevPayCredentials = (AWSDevPayCredentials) credentials;
            this.awsDevPayUserToken = awsDevPayCredentials.getUserToken();
            this.awsDevPayProductToken = awsDevPayCredentials.getProductToken();
        } else {
            this.awsDevPayUserToken = jets3tProperties.getStringProperty("devpay.user-token", null);
            this.awsDevPayProductToken = jets3tProperties.getStringProperty("devpay.product-token", null);
        }

        this.setRequesterPaysEnabled(
            this.jets3tProperties.getBoolProperty("httpclient.requester-pays-buckets-enabled", false));
    }

    @Override
    protected boolean isTargettingGoogleStorageService() {
        return Constants.GS_DEFAULT_HOSTNAME.equals(
            this.getJetS3tProperties().getStringProperty("s3service.s3-endpoint", null));
    }

    /**
     * Set the User Token value to use for requests to a DevPay S3 account.
     * The user token is not required for DevPay web products for which the
     * user token was created after 15th May 2008.
     *
     * @param userToken
     * the user token value provided by the AWS DevPay activation service.
     */
    public void setDevPayUserToken(String userToken) {
        this.awsDevPayUserToken = userToken;
    }

    /**
     * @return
     * the user token value to use in requests to a DevPay S3 account, or null
     * if no such token value has been set.
     */
    public String getDevPayUserToken() {
        return this.awsDevPayUserToken;
    }

    /**
     * Set the Product Token value to use for requests to a DevPay S3 account.
     *
     * @param productToken
     * the token that identifies your DevPay product.
     */
    public void setDevPayProductToken(String productToken) {
        this.awsDevPayProductToken = productToken;
    }

    /**
     * @return
     * the product token value to use in requests to a DevPay S3 account, or
     * null if no such token value has been set.
     */
    public String getDevPayProductToken() {
        return this.awsDevPayProductToken;
    }

    /**
     * Instruct the service whether to generate Requester Pays requests when
     * uploading data to S3, or retrieving data from the service. The default
     * value for the Requester Pays Enabled setting is set according to the
     * jets3t.properties setting
     * <code>httpclient.requester-pays-buckets-enabled</code>.
     *
     * @param isRequesterPays
     * if true, all subsequent S3 service requests will include the Requester
     * Pays flag.
     */
    public void setRequesterPaysEnabled(boolean isRequesterPays) {
        this.isRequesterPaysEnabled = isRequesterPays;
    }

    /**
     * Is this service configured to generate Requester Pays requests when
     * uploading data to S3, or retrieving data from the service. The default
     * value for the Requester Pays Enabled setting is set according to the
     * jets3t.properties setting
     * <code>httpclient.requester-pays-buckets-enabled</code>.
     *
     * @return
     * true if S3 service requests will include the Requester Pays flag, false
     * otherwise.
     */
    public boolean isRequesterPaysEnabled() {
        return this.isRequesterPaysEnabled;
    }

    /**
     * Creates an {@link org.apache.commons.httpclient.HttpMethod} object to handle a particular connection method.
     *
     * @param method
     *        the HTTP method/connection-type to use, must be one of: PUT, HEAD, GET, DELETE
     * @param bucketName
     *        the bucket's name
     * @param objectKey
     *        the object's key name, may be null if the operation is on a bucket only.
     * @return
     *        the HTTP method object used to perform the request
     *
     * @throws org.jets3t.service.S3ServiceException
     */
    @Override
    protected HttpMethodBase setupConnection(HTTP_METHOD method, String bucketName, String objectKey,
        Map<String, String> requestParameters) throws S3ServiceException
    {
        HttpMethodBase httpMethod;
        try {
            httpMethod = super.setupConnection(method, bucketName, objectKey, requestParameters);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }

        // Set DevPay request headers.
        if (getDevPayUserToken() != null || getDevPayProductToken() != null) {
            // DevPay tokens have been provided, include these with the request.
            if (getDevPayProductToken() != null) {
                String securityToken = getDevPayUserToken() + "," + getDevPayProductToken();
                httpMethod.setRequestHeader(Constants.AMZ_SECURITY_TOKEN, securityToken);
                if (log.isDebugEnabled()) {
                    log.debug("Including DevPay user and product tokens in request: "
                        + Constants.AMZ_SECURITY_TOKEN + "=" + securityToken);
                }
            } else {
                httpMethod.setRequestHeader(Constants.AMZ_SECURITY_TOKEN, getDevPayUserToken());
                if (log.isDebugEnabled()) {
                    log.debug("Including DevPay user token in request: "
                        + Constants.AMZ_SECURITY_TOKEN + "=" + getDevPayUserToken());
                }
            }
        }

        // Set Requester Pays header to allow access to these buckets.
        if (this.isRequesterPaysEnabled()) {
            String[] requesterPaysHeaderAndValue = Constants.REQUESTER_PAYS_BUCKET_FLAG.split("=");
            httpMethod.setRequestHeader(requesterPaysHeaderAndValue[0], requesterPaysHeaderAndValue[1]);
            if (log.isDebugEnabled()) {
                log.debug("Including Requester Pays header in request: " +
                    Constants.REQUESTER_PAYS_BUCKET_FLAG);
            }
        }

        return httpMethod;
    }

    /**
     * @return
     * the endpoint to be used to connect to S3.
     */
    @Override
    public String getEndpoint() {
    	return this.jets3tProperties.getStringProperty(
                "s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
    }

    /**
     * @return
     * the virtual path inside the S3 server.
     */
    @Override
    protected String getVirtualPath() {
    	return this.jets3tProperties.getStringProperty(
                "s3service.s3-endpoint-virtual-path", "");
    }

    /**
     * @return
     * the identifier for the signature algorithm.
     */
    @Override
    protected String getSignatureIdentifier() {
    	return AWS_SIGNATURE_IDENTIFIER;
    }

    /**
     * @return
     * header prefix for general Amazon headers: x-amz-.
     */
    @Override
    public String getRestHeaderPrefix() {
    	return AWS_REST_HEADER_PREFIX;
    }

    @Override
    public List<String> getResourceParameterNames() {
        // Special HTTP parameter names that refer to resources in S3
        return Arrays.asList(new String[] {
            "acl", "policy",
            "torrent",
            "logging",
            "location",
            "requestPayment",
            "versions", "versioning", "versionId",
            "uploads", "uploadId", "partNumber",
            "website", "notification"
        });
    }

    /**
     * @return
     * header prefix for Amazon metadata headers: x-amz-meta-.
     */
    @Override
    public String getRestMetadataPrefix() {
    	return AWS_REST_METADATA_PREFIX;
    }

    /**
     * @return
     * the port number to be used for insecure connections over HTTP.
     */
    @Override
    protected int getHttpPort() {
      return this.jets3tProperties.getIntProperty("s3service.s3-endpoint-http-port", 80);
    }

    /**
     * @return
     * the port number to be used for secure connections over HTTPS.
     */
    @Override
    protected int getHttpsPort() {
      return this.jets3tProperties.getIntProperty("s3service.s3-endpoint-https-port", 443);
    }

    /**
     * @return
     * If true, all communication with S3 will be via encrypted HTTPS connections,
     * otherwise communications will be sent unencrypted via HTTP.
     */
    @Override
    protected boolean getHttpsOnly() {
      return this.jets3tProperties.getBoolProperty("s3service.https-only", true);
    }

    /**
     * @return
     * If true, JetS3t will specify bucket names in the request path of the HTTP message
     * instead of the Host header.
     */
    @Override
    protected boolean getDisableDnsBuckets() {
      return this.jets3tProperties.getBoolProperty("s3service.disable-dns-buckets", false);
    }

    /**
     * @return
     * If true, JetS3t will enable support for Storage Classes.
     */
    @Override
    protected boolean getEnableStorageClasses() {
        return this.jets3tProperties.getBoolProperty("s3service.enable-storage-classes",
            // Enable non-standard storage classes by default for AWS, not for Google endpoints.
            isTargettingGoogleStorageService() ? false : true);
    }

    @Override
    protected BaseVersionOrDeleteMarker[] listVersionedObjectsImpl(String bucketName,
        String prefix, String delimiter, String keyMarker, String versionMarker,
        long maxListingLength) throws S3ServiceException
    {
        return listVersionedObjectsInternal(bucketName, prefix, delimiter,
            maxListingLength, true, keyMarker, versionMarker).getItems();
    }

    @Override
    protected void updateBucketVersioningStatusImpl(String bucketName,
        boolean enabled, boolean multiFactorAuthDeleteEnabled,
        String multiFactorSerialNumber, String multiFactorAuthCode)
        throws S3ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug( (enabled ? "Enabling" : "Suspending")
                + " versioning for bucket " + bucketName
                + (multiFactorAuthDeleteEnabled ? " with Multi-Factor Auth enabled" : ""));
        }
        try {
            XMLBuilder builder = XMLBuilder
                .create("VersioningConfiguration").a("xmlns", Constants.XML_NAMESPACE)
                    .e("Status").t( (enabled ? "Enabled" : "Suspended") ).up()
                    .e("MfaDelete").t( (multiFactorAuthDeleteEnabled ? "Enabled" : "Disabled"));
            Map<String, String> requestParams = new HashMap<String, String>();
            requestParams.put("versioning", null);
            Map<String, Object> metadata = new HashMap<String, Object>();
            if (multiFactorSerialNumber != null || multiFactorAuthCode != null) {
                metadata.put(Constants.AMZ_MULTI_FACTOR_AUTH_CODE,
                    multiFactorSerialNumber + " " + multiFactorAuthCode);
            }
            try {
                performRestPutWithXmlBuilder(bucketName, null, metadata, requestParams, builder);
            } catch (ServiceException se) {
                throw new S3ServiceException(se);
            }
        } catch (ParserConfigurationException e) {
            throw new S3ServiceException("Failed to build XML document for request", e);
        }
    }

    @Override
    protected S3BucketVersioningStatus getBucketVersioningStatusImpl(String bucketName)
        throws S3ServiceException
    {
        try {
            if (log.isDebugEnabled()) {
                log.debug( "Checking status of versioning for bucket " + bucketName);
            }
            Map<String, String> requestParams = new HashMap<String, String>();
            requestParams.put("versioning", null);
            HttpMethodBase method = performRestGet(bucketName, null, requestParams, null);
            return getXmlResponseSaxParser()
                .parseVersioningConfigurationResponse(new HttpMethodReleaseInputStream(method));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    protected VersionOrDeleteMarkersChunk listVersionedObjectsInternal(
        String bucketName, String prefix, String delimiter, long maxListingLength,
        boolean automaticallyMergeChunks, String nextKeyMarker, String nextVersionIdMarker)
        throws S3ServiceException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("versions", null);
        if (prefix != null) {
            parameters.put("prefix", prefix);
        }
        if (delimiter != null) {
            parameters.put("delimiter", delimiter);
        }
        if (maxListingLength > 0) {
            parameters.put("max-keys", String.valueOf(maxListingLength));
        }

        List<BaseVersionOrDeleteMarker> items = new ArrayList<BaseVersionOrDeleteMarker>();
        List<String> commonPrefixes = new ArrayList<String>();

        boolean incompleteListing = true;
        int ioErrorRetryCount = 0;

        while (incompleteListing) {
            if (nextKeyMarker != null) {
                parameters.put("key-marker", nextKeyMarker);
            } else {
                parameters.remove("key-marker");
            }
            if (nextVersionIdMarker != null) {
                parameters.put("version-id-marker", nextVersionIdMarker);
            } else {
                parameters.remove("version-id-marker");
            }

            HttpMethodBase httpMethod;
            try {
                httpMethod = performRestGet(bucketName, null, parameters, null);
            } catch (ServiceException se) {
                throw new S3ServiceException(se);
            }
            ListVersionsResultsHandler handler = null;

            try {
                handler = getXmlResponseSaxParser()
                    .parseListVersionsResponse(
                        new HttpMethodReleaseInputStream(httpMethod));
                ioErrorRetryCount = 0;
            } catch (ServiceException se) {
                if (se.getCause() instanceof IOException && ioErrorRetryCount < 5) {
                    ioErrorRetryCount++;
                    if (log.isWarnEnabled()) {
                        log.warn("Retrying bucket listing failure due to IO error", se);
                    }
                    continue;
                } else {
                    throw new S3ServiceException(se);
                }
            }

            BaseVersionOrDeleteMarker[] partialItems = handler.getItems();
            if (log.isDebugEnabled()) {
                log.debug("Found " + partialItems.length + " items in one batch");
            }
            items.addAll(Arrays.asList(partialItems));

            String[] partialCommonPrefixes = handler.getCommonPrefixes();
            if (log.isDebugEnabled()) {
                log.debug("Found " + partialCommonPrefixes.length + " common prefixes in one batch");
            }
            commonPrefixes.addAll(Arrays.asList(partialCommonPrefixes));

            incompleteListing = handler.isListingTruncated();
            nextKeyMarker = handler.getNextKeyMarker();
            nextVersionIdMarker = handler.getNextVersionIdMarker();
            if (incompleteListing) {
                if (log.isDebugEnabled()) {
                    log.debug("Yet to receive complete listing of bucket versions, "
                        + "continuing with key-marker=" + nextKeyMarker
                        + " and version-id-marker=" + nextVersionIdMarker);
                }
            }

            if (!automaticallyMergeChunks) {
                break;
            }
        }
        if (automaticallyMergeChunks) {
            if (log.isDebugEnabled()) {
                log.debug("Found " + items.size() + " items in total");
            }
            return new VersionOrDeleteMarkersChunk(
                prefix, delimiter,
                items.toArray(new BaseVersionOrDeleteMarker[items.size()]),
                commonPrefixes.toArray(new String[commonPrefixes.size()]),
                null, null);
        } else {
            return new VersionOrDeleteMarkersChunk(
                prefix, delimiter,
                items.toArray(new BaseVersionOrDeleteMarker[items.size()]),
                commonPrefixes.toArray(new String[commonPrefixes.size()]),
                nextKeyMarker, nextVersionIdMarker);
        }
    }

    @Override
    protected VersionOrDeleteMarkersChunk listVersionedObjectsChunkedImpl(String bucketName,
        String prefix, String delimiter, long maxListingLength, String priorLastKey,
        String priorLastVersion, boolean completeListing) throws S3ServiceException
    {
        return listVersionedObjectsInternal(bucketName, prefix, delimiter,
            maxListingLength, completeListing, priorLastKey, priorLastVersion);
    }

    @Override
    protected String getBucketLocationImpl(String bucketName)
        throws S3ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving location of Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("location", "");

        try {
            HttpMethodBase httpMethod = performRestGet(bucketName, null, requestParameters, null);
            return getXmlResponseSaxParser()
                .parseBucketLocationResponse(
                    new HttpMethodReleaseInputStream(httpMethod));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected S3BucketLoggingStatus getBucketLoggingStatusImpl(String bucketName)
        throws S3ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving Logging Status for Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("logging", "");

        try {
            HttpMethodBase httpMethod = performRestGet(bucketName, null, requestParameters, null);
            return getXmlResponseSaxParser()
                .parseLoggingStatusResponse(
                    new HttpMethodReleaseInputStream(httpMethod)).getBucketLoggingStatus();
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected void setBucketLoggingStatusImpl(String bucketName, S3BucketLoggingStatus status)
        throws S3ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug("Setting Logging Status for bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("logging", "");

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Content-Type", "text/plain");

        String statusAsXml = null;
        try {
            statusAsXml = status.toXml();
        } catch (Exception e) {
            throw new S3ServiceException("Unable to generate LoggingStatus XML document", e);
        }
        try {
            metadata.put("Content-Length", String.valueOf(statusAsXml.length()));
            performRestPut(bucketName, null, metadata, requestParameters,
                new StringRequestEntity(statusAsXml, "text/plain", Constants.DEFAULT_ENCODING),
                true);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException("Unable to encode LoggingStatus XML document", e);
        }
    }

    @Override
    protected String getBucketPolicyImpl(String bucketName)
        throws S3ServiceException
    {
        try {
            Map<String, String> requestParameters = new HashMap<String, String>();
            requestParameters.put("policy", "");

            HttpMethodBase httpMethod = performRestGet(bucketName, null, requestParameters, null);
            return httpMethod.getResponseBodyAsString();
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (IOException  e) {
            throw new S3ServiceException(e);
        }
    }

    @Override
    protected void setBucketPolicyImpl(String bucketName, String policyDocument)
        throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("policy", "");

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Content-Type", "text/plain");

        try {
            metadata.put("Content-Length", String.valueOf(policyDocument.length()));
            performRestPut(bucketName, null, metadata, requestParameters,
                new StringRequestEntity(policyDocument, "text/plain", Constants.DEFAULT_ENCODING),
                true);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException("Unable to encode LoggingStatus XML document", e);
        }
    }

    @Override
    protected void deleteBucketPolicyImpl(String bucketName)
        throws S3ServiceException
    {
        try {
            Map<String, String> requestParameters = new HashMap<String, String>();
            requestParameters.put("policy", "");
            performRestDelete(bucketName, null, requestParameters, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected boolean isRequesterPaysBucketImpl(String bucketName)
        throws S3ServiceException
    {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving Request Payment Configuration settings for Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("requestPayment", "");

        try {
            HttpMethodBase httpMethod = performRestGet(bucketName, null, requestParameters, null);
            return getXmlResponseSaxParser()
                .parseRequestPaymentConfigurationResponse(
                    new HttpMethodReleaseInputStream(httpMethod));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected void setRequesterPaysBucketImpl(String bucketName, boolean requesterPays) throws S3ServiceException {
        if (log.isDebugEnabled()) {
            log.debug("Setting Request Payment Configuration settings for bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("requestPayment", "");

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Content-Type", "text/plain");

        try {
            String xml =
                "<RequestPaymentConfiguration xmlns=\"" + Constants.XML_NAMESPACE + "\">" +
                    "<Payer>" +
                        (requesterPays ? "Requester" : "BucketOwner") +
                    "</Payer>" +
                "</RequestPaymentConfiguration>";

            metadata.put("Content-Length", String.valueOf(xml.length()));
            performRestPut(bucketName, null, metadata, requestParameters,
                new StringRequestEntity(xml, "text/plain", Constants.DEFAULT_ENCODING),
                true);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException("Unable to encode RequestPaymentConfiguration XML document", e);
        }
    }

    @Override
    protected MultipartUpload multipartStartUploadImpl(String bucketName, String objectKey,
        Map<String, Object> metadataProvided, AccessControlList acl, String storageClass)
        throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploads", "");

        Map<String, Object> metadata = new HashMap<String, Object>();

        // Use metadata provided, but ignore some items that don't make sense
        if (metadataProvided != null) {
            for (Map.Entry<String, Object> entry: metadataProvided.entrySet()) {
                if (!entry.getKey().toLowerCase().equals("content-length")) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Apply per-object or default storage class when uploading object
        prepareStorageClass(metadata, storageClass, objectKey);

        try {
            HttpMethodBase postMethod = performRestPost(
                bucketName, objectKey, metadata, requestParameters, null, false);
            MultipartUpload multipartUpload = getXmlResponseSaxParser()
                .parseInitiateMultipartUploadResult(
                    new HttpMethodReleaseInputStream(postMethod));
            multipartUpload.setMetadata(metadata); // Add object's known metadata to result object.
            return multipartUpload;
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected MultipartPart multipartUploadPartImpl(String uploadId, String bucketName,
        Integer partNumber, S3Object object) throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploadId", uploadId);
        requestParameters.put("partNumber", "" + partNumber);

        // Remove all non-HTTP headers from object metadata for multipart part uploads
        List<String> metadataNamesToRemove = new ArrayList<String>();
        for (String name: object.getMetadataMap().keySet()) {
            if (!RestUtils.HTTP_HEADER_METADATA_NAMES.contains(name.toLowerCase())) {
                // Actual metadata name in object does not include the prefix
                metadataNamesToRemove.add(name);
            }
        }
        for (String name: metadataNamesToRemove) {
            object.removeMetadata(name);
        }

        try {
            // We do not need to calculate the data MD5 hash during upload if the
            // expected hash value was provided as the object's Content-MD5 header.
            boolean isLiveMD5HashingRequired =
                (object.getMetadata(StorageObject.METADATA_HEADER_CONTENT_MD5) == null);

            RequestEntity requestEntity = null;
            if (object.getDataInputStream() != null) {
                if (object.containsMetadata(StorageObject.METADATA_HEADER_CONTENT_LENGTH)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Uploading multipart part data with Content-Length: "
                            + object.getContentLength());
                    }
                    requestEntity = new RepeatableRequestEntity(object.getKey(),
                        object.getDataInputStream(), object.getContentType(), object.getContentLength(),
                        this.jets3tProperties, isLiveMD5HashingRequired);
                } else {
                    // Use InputStreamRequestEntity for objects with an unknown content length, as the
                    // entity will cache the results and doesn't need to know the data length in advance.
                    if (log.isWarnEnabled()) {
                        log.warn("Content-Length of multipart part stream not set, "
                            + "will automatically determine data length in memory");
                    }
                    requestEntity = new InputStreamRequestEntity(
                        object.getDataInputStream(), InputStreamRequestEntity.CONTENT_LENGTH_AUTO);
                }
            }

            this.putObjectWithRequestEntityImpl(bucketName, object, requestEntity, requestParameters);

            // Populate part with response data that is accessible via the object's metadata
            MultipartPart part = new MultipartPart(partNumber, object.getLastModifiedDate(),
                object.getETag(), object.getContentLength());
            return part;
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected void multipartAbortUploadImpl(String uploadId, String bucketName,
        String objectKey) throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploadId", uploadId);

        try {
            performRestDelete(bucketName, objectKey, requestParameters, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected MultipartCompleted multipartCompleteUploadImpl(String uploadId, String bucketName,
        String objectKey, List<MultipartPart> parts) throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploadId", uploadId);

        // Ensure part list is sorted by part number
        MultipartPart[] sortedParts = parts.toArray(new MultipartPart[parts.size()]);
        Arrays.sort(sortedParts, new MultipartPart.PartNumberComparator());
        try {
            XMLBuilder builder = XMLBuilder
                .create("CompleteMultipartUpload").a("xmlns", Constants.XML_NAMESPACE);
            for (MultipartPart part: sortedParts) {
                builder.e("Part")
                    .e("PartNumber").t("" + part.getPartNumber()).up()
                    .e("ETag").t(part.getEtag());
            }

            HttpMethodBase postMethod = performRestPostWithXmlBuilder(
                bucketName, objectKey, null, requestParameters, builder);
            CompleteMultipartUploadResultHandler handler = getXmlResponseSaxParser()
                .parseCompleteMultipartUploadResult(
                    new HttpMethodReleaseInputStream(postMethod));

            // Check whether completion actually succeeded
            if (handler.getServiceException() != null) {
                ServiceException e = handler.getServiceException();
                e.setResponseHeaders(RestUtils.convertHeadersToMap(
                    postMethod.getResponseHeaders()));
                throw e;
            }
            return handler.getMultipartCompleted();
        } catch (S3ServiceException se) {
            throw se;
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (ParserConfigurationException e) {
            throw new S3ServiceException(e);
        } catch (FactoryConfigurationError e) {
            throw new S3ServiceException(e);
        }
    }

    @Override
    protected List<MultipartUpload> multipartListUploadsImpl(String bucketName,
        String nextKeyMarker, String nextUploadIdMarker, Integer maxUploads)
        throws S3ServiceException
    {
        if (bucketName == null || bucketName.length()==0) {
            throw new IllegalArgumentException(
                "The bucket name parameter must be specified when listing multipart uploads");
        }
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploads", "");
        requestParameters.put("max-uploads",
            (maxUploads == null ? "1000" : maxUploads.toString()));

        try {
            List<MultipartUpload> uploads = new ArrayList<MultipartUpload>();
            boolean incompleteListing = true;
            do {
                if (nextKeyMarker != null) {
                    requestParameters.put("key-marker", nextKeyMarker);
                } else {
                    requestParameters.remove("key-marker");
                }
                if (nextUploadIdMarker != null) {
                    requestParameters.put("upload-id-marker", nextUploadIdMarker);
                } else {
                    requestParameters.remove("upload-id-marker");
                }

                HttpMethodBase getMethod = performRestGet(
                    bucketName, null, requestParameters, null);
                ListMultipartUploadsResultHandler handler = getXmlResponseSaxParser()
                    .parseListMultipartUploadsResult(
                        new HttpMethodReleaseInputStream(getMethod));
                uploads.addAll(handler.getMultipartUploadList());

                incompleteListing = handler.isTruncated();
                nextKeyMarker = handler.getNextKeyMarker();
                nextUploadIdMarker = handler.getNextUploadIdMarker();

                // Sanity check for valid pagination values.
                if (incompleteListing && nextKeyMarker == null && nextUploadIdMarker == null)
                {
                    throw new ServiceException("Unable to retrieve paginated "
                        + "ListMultipartUploadsResult without valid NextKeyMarker "
                        + " or NextUploadIdMarker value.");
                }
            } while (incompleteListing);
            return uploads;
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected List<MultipartPart> multipartListPartsImpl(String uploadId,
        String bucketName, String objectKey) throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("uploadId", uploadId);
        requestParameters.put("max-parts", "1000");

        try {
            List<MultipartPart> parts = new ArrayList<MultipartPart>();
            String nextPartNumberMarker = null;
            boolean incompleteListing = true;
            do {
                if (nextPartNumberMarker != null) {
                    requestParameters.put("part-number-marker", nextPartNumberMarker);
                } else {
                    requestParameters.remove("part-number-marker");
                }

                HttpMethodBase getMethod = performRestGet(bucketName, objectKey, requestParameters, null);
                ListMultipartPartsResultHandler handler = getXmlResponseSaxParser()
                    .parseListMultipartPartsResult(
                        new HttpMethodReleaseInputStream(getMethod));
                parts.addAll(handler.getMultipartPartList());

                incompleteListing = handler.isTruncated();
                nextPartNumberMarker = handler.getNextPartNumberMarker();

                // Sanity check for valid pagination values.
                if (incompleteListing && nextPartNumberMarker == null)
                {
                    throw new ServiceException("Unable to retrieve paginated "
                        + "ListMultipartPartsResult without valid NextKeyMarker value.");
                }
            } while (incompleteListing);
            return parts;
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected WebsiteConfig getWebsiteConfigImpl(String bucketName) throws S3ServiceException
    {
        try {
            Map<String, String> requestParameters = new HashMap<String, String>();
            requestParameters.put("website", "");

            HttpMethodBase getMethod = performRestGet(bucketName, null, requestParameters, null);
            return getXmlResponseSaxParser().parseWebsiteConfigurationResponse(
                new HttpMethodReleaseInputStream(getMethod));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected void setWebsiteConfigImpl(String bucketName, WebsiteConfig config)
        throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("website", "");

        Map<String, Object> metadata = new HashMap<String, Object>();

        String xml = null;
        try {
            xml = config.toXml();
        } catch (Exception e) {
            throw new S3ServiceException("Unable to build WebsiteConfig XML document", e);
        }

        try {
            metadata.put("Content-Length", xml.length());
            performRestPut(bucketName, null, metadata, requestParameters,
                new StringRequestEntity(xml, "text/plain", Constants.DEFAULT_ENCODING),
                true);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException("Unable to encode XML document", e);
        }
    }

    @Override
    protected void deleteWebsiteConfigImpl(String bucketName)
        throws S3ServiceException
    {
        try {
            Map<String, String> requestParameters = new HashMap<String, String>();
            requestParameters.put("website", "");
            performRestDelete(bucketName, null, requestParameters, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected NotificationConfig getNotificationConfigImpl(String bucketName)
        throws S3ServiceException
    {
        try {
            Map<String, String> requestParameters = new HashMap<String, String>();
            requestParameters.put("notification", "");

            HttpMethodBase getMethod = performRestGet(bucketName, null, requestParameters, null);
            return getXmlResponseSaxParser().parseNotificationConfigurationResponse(
                new HttpMethodReleaseInputStream(getMethod));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    protected void setNotificationConfigImpl(String bucketName, NotificationConfig config)
        throws S3ServiceException
    {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("notification", "");

        Map<String, Object> metadata = new HashMap<String, Object>();

        String xml = null;
        try {
            xml = config.toXml();
        } catch (Exception e) {
            throw new S3ServiceException("Unable to build NotificationConfig XML document", e);
        }

        try {
            metadata.put("Content-Length", xml.length());
            performRestPut(bucketName, null, metadata, requestParameters,
                new StringRequestEntity(xml, "text/plain", Constants.DEFAULT_ENCODING),
                true);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException("Unable to encode XML document", e);
        }
    }

}

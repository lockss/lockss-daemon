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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.BaseVersionOrDeleteMarker;
import org.jets3t.service.model.MultipartCompleted;
import org.jets3t.service.model.MultipartPart;
import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.NotificationConfig;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3BucketLoggingStatus;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3DeleteMarker;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.S3Version;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.mx.MxDelegate;
import org.jets3t.service.security.AWSDevPayCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.MultipartUtils;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.signedurl.SignedUrlHandler;

/**
 * A service that handles communication with S3, offering all the operations that can be performed
 * on S3 accounts.
 * <p>
 * This class must be extended by implementation classes that perform the communication with S3 via
 * a particular interface, such as REST or SOAP. The JetS3t suite includes a REST implementation
 * in {@link org.jets3t.service.impl.rest.httpclient.RestS3Service}.
 * </p>
 * <p>
 * Implementations of <code>S3Service</code> must be thread-safe as they will probably be used by
 * the multi-threaded service class {@link org.jets3t.service.multithread.S3ServiceMulti}.
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
public abstract class S3Service extends RestStorageService implements SignedUrlHandler {

    private static final Log log = LogFactory.getLog(S3Service.class);

    protected S3Service(ProviderCredentials credentials, String invokingApplicationDescription,
        CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties,
        HostConfiguration hostConfig)
    {
        super(credentials, invokingApplicationDescription, credentialsProvider,
            jets3tProperties, hostConfig);
    }

    /**
     * Construct an <code>S3Service</code> identified by the given user credentials.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param jets3tProperties
     * JetS3t properties that will be applied within this service.
     */
    protected S3Service(ProviderCredentials credentials, String invokingApplicationDescription,
        Jets3tProperties jets3tProperties)
    {
        super(credentials, invokingApplicationDescription, null, jets3tProperties);
    }

    /**
     * Construct an <code>S3Service</code> identified by the given user credentials.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
     * communication is done as an anonymous user.
     * @param invokingApplicationDescription
     * a short description of the application using the service, suitable for inclusion in a
     * user agent string for REST/HTTP requests. Ideally this would include the application's
     * version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     */
    protected S3Service(ProviderCredentials credentials, String invokingApplicationDescription)
    {
        this(credentials, invokingApplicationDescription,
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * Construct an <code>S3Service</code> identified by the given user credentials.
     *
     * @param credentials
     * the S3 user credentials to use when communicating with S3, may be null in which case the
     * communication is done as an anonymous user.
     */
    protected S3Service(ProviderCredentials credentials) {
        this(credentials, null);
    }

    /**
     * @return the credentials identifying the service user, or null for anonymous.
     * @deprecated 0.8.0 use {@link #getProviderCredentials()} instead
     */
    @Deprecated
    public ProviderCredentials getAWSCredentials() {
        return credentials;
    }

    /**
     * Returns the URL representing an object in S3 without a signature. This URL
     * can only be used to download publicly-accessible objects.
     *
     * @param bucketName
     * the name of the bucket that contains the object.
     * @param objectKey
     * the key name of the object.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     * @param isHttps
     * if true, the signed URL will use the HTTPS protocol. If false, the signed URL will
     * use the HTTP protocol.
     * @param isDnsBucketNamingDisabled
     * if true, the signed URL will not use the DNS-name format for buckets eg.
     * <tt>jets3t.s3.amazonaws.com</tt>. Unless you have a specific reason to disable
     * DNS bucket naming, leave this value false.
     *
     * @return
     * the object's URL.
     *
     * @throws S3ServiceException
     */
    public String createUnsignedObjectUrl(String bucketName, String objectKey,
        boolean isVirtualHost, boolean isHttps, boolean isDnsBucketNamingDisabled)
        throws S3ServiceException
    {
        // Create a signed GET URL then strip away the signature query components.
        String signedGETUrl = createSignedUrl("GET", bucketName, objectKey,
            null, null, 0, isVirtualHost, isHttps, isDnsBucketNamingDisabled);
        return signedGETUrl.split("\\?")[0];
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified.
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging', or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     * @param isHttps
     * if true, the signed URL will use the HTTPS protocol. If false, the signed URL will
     * use the HTTP protocol.
     * @param isDnsBucketNamingDisabled
     * if true, the signed URL will not use the DNS-name format for buckets eg.
     * <tt>jets3t.s3.amazonaws.com</tt>. Unless you have a specific reason to disable
     * DNS bucket naming, leave this value false.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    public String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, long secondsSinceEpoch,
        boolean isVirtualHost, boolean isHttps, boolean isDnsBucketNamingDisabled)
        throws S3ServiceException
    {
        try {
            String s3Endpoint = this.getEndpoint();
            String uriPath = "";

            String hostname = (isVirtualHost
                ? bucketName
                : ServiceUtils.generateS3HostnameForBucket(
                    bucketName, isDnsBucketNamingDisabled, s3Endpoint));

            if (headersMap == null) {
                headersMap = new HashMap<String, Object>();
            }

            // If we are using an alternative hostname, include the hostname/bucketname in the resource path.
            String virtualBucketPath = "";
            if (!s3Endpoint.equals(hostname)) {
                int subdomainOffset = hostname.lastIndexOf("." + s3Endpoint);
                if (subdomainOffset > 0) {
                    // Hostname represents an S3 sub-domain, so the bucket's name is the CNAME portion
                    virtualBucketPath = hostname.substring(0, subdomainOffset) + "/";
                } else {
                    // Hostname represents a virtual host, so the bucket's name is identical to hostname
                    virtualBucketPath = hostname + "/";
                }
                uriPath = (objectKey != null ? RestUtils.encodeUrlPath(objectKey, "/") : "");
            } else {
                uriPath = bucketName + (objectKey != null ? "/" + RestUtils.encodeUrlPath(objectKey, "/") : "");
            }

            if (specialParamName != null) {
                uriPath += "?" + specialParamName + "&";
            } else {
                uriPath += "?";
            }

            // Include any DevPay tokens in signed request
            if (credentials instanceof AWSDevPayCredentials) {
                AWSDevPayCredentials devPayCredentials = (AWSDevPayCredentials) credentials;
                if (devPayCredentials.getProductToken() != null) {
                    String securityToken = devPayCredentials.getUserToken()
                        + "," + devPayCredentials.getProductToken();
                    headersMap.put(Constants.AMZ_SECURITY_TOKEN, securityToken);
                } else {
                    headersMap.put(Constants.AMZ_SECURITY_TOKEN, devPayCredentials.getUserToken());
                }

                uriPath += Constants.AMZ_SECURITY_TOKEN + "=" +
                    RestUtils.encodeUrlString((String) headersMap.get(Constants.AMZ_SECURITY_TOKEN)) + "&";
            }

            uriPath += "AWSAccessKeyId=" + credentials.getAccessKey();
            uriPath += "&Expires=" + secondsSinceEpoch;

            // Include Requester Pays header flag, if the flag is included as a request parameter.
            if (specialParamName != null
                && specialParamName.toLowerCase().indexOf(Constants.REQUESTER_PAYS_BUCKET_FLAG) >= 0)
            {
                String[] requesterPaysHeaderAndValue = Constants.REQUESTER_PAYS_BUCKET_FLAG.split("=");
                headersMap.put(requesterPaysHeaderAndValue[0], requesterPaysHeaderAndValue[1]);
            }

            String serviceEndpointVirtualPath = this.getVirtualPath();

            String canonicalString = RestUtils.makeServiceCanonicalString(method,
                serviceEndpointVirtualPath + "/" + virtualBucketPath + uriPath,
                renameMetadataKeys(headersMap), String.valueOf(secondsSinceEpoch),
                this.getRestHeaderPrefix(), this.getResourceParameterNames());
            if (log.isDebugEnabled()) {
                log.debug("Signing canonical string:\n" + canonicalString);
            }

            String signedCanonical = ServiceUtils.signWithHmacSha1(credentials.getSecretKey(),
                canonicalString);
            String encodedCanonical = RestUtils.encodeUrlString(signedCanonical);
            uriPath += "&Signature=" + encodedCanonical;

            if (isHttps) {
                int httpsPort = this.getHttpsPort();
                return "https://" + hostname
                    + (httpsPort != 443 ? ":" + httpsPort : "")
                    + serviceEndpointVirtualPath
                    + "/" + uriPath;
            } else {
                int httpPort = this.getHttpPort();
                return "http://" + hostname
                + (httpPort != 80 ? ":" + httpPort : "")
                + serviceEndpointVirtualPath
                + "/" + uriPath;
            }
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        } catch (UnsupportedEncodingException e) {
            throw new S3ServiceException(e);
        }
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified. The URL will use the default
     * JetS3t property settings in the <tt>jets3t.properties</tt> file to determine whether
     * to generate HTTP or HTTPS links (<tt>s3service.https-only</tt>), and whether to disable
     * DNS bucket naming (<tt>s3service.disable-dns-buckets</tt>).
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging' or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    public String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, long secondsSinceEpoch,
        boolean isVirtualHost) throws S3ServiceException
    {
        boolean isHttps = this.isHttpsOnly();
        boolean disableDnsBuckets = this.getDisableDnsBuckets();

        return createSignedUrl(method, bucketName, objectKey, specialParamName,
            headersMap, secondsSinceEpoch, isVirtualHost, isHttps, disableDnsBuckets);
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified.
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging' or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    public String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, long secondsSinceEpoch)
        throws S3ServiceException
    {
        return createSignedUrl(method, bucketName, objectKey, specialParamName, headersMap,
            secondsSinceEpoch, false);
    }


    /**
     * Generates a signed GET URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant GET access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    public String createSignedGetUrl(String bucketName, String objectKey,
        Date expiryTime, boolean isVirtualHost) throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("GET", bucketName, objectKey, null, null,
            secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed GET URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to grant GET access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    public String createSignedGetUrl(String bucketName, String objectKey,
        Date expiryTime) throws S3ServiceException
    {
        return createSignedGetUrl(bucketName, objectKey, expiryTime, false);
    }


    /**
     * Generates a signed PUT URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to allow anyone to PUT an object into S3.
     * @throws S3ServiceException
     */
    public String createSignedPutUrl(String bucketName, String objectKey,
        Map<String, Object> headersMap, Date expiryTime, boolean isVirtualHost)
        throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("PUT", bucketName, objectKey, null, headersMap,
            secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed PUT URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to allow anyone to PUT an object into S3.
     * @throws S3ServiceException
     */
    public String createSignedPutUrl(String bucketName, String objectKey,
        Map<String, Object> headersMap, Date expiryTime) throws S3ServiceException
    {
        return createSignedPutUrl(bucketName, objectKey, headersMap, expiryTime, false);
    }


    /**
     * Generates a signed DELETE URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to allow anyone do DELETE an object in S3.
     * @throws S3ServiceException
     */
    public String createSignedDeleteUrl(String bucketName, String objectKey,
        Date expiryTime, boolean isVirtualHost) throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("DELETE", bucketName, objectKey, null, null,
            secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed DELETE URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to allow anyone do DELETE an object in S3.
     * @throws S3ServiceException
     */
    public String createSignedDeleteUrl(String bucketName, String objectKey,
        Date expiryTime) throws S3ServiceException
    {
        return createSignedDeleteUrl(bucketName, objectKey, expiryTime, false);
    }


    /**
     * Generates a signed HEAD URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant HEAD access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    public String createSignedHeadUrl(String bucketName, String objectKey,
        Date expiryTime, boolean isVirtualHost) throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("HEAD", bucketName, objectKey, null, null,
            secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed HEAD URL.
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to grant HEAD access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    public String createSignedHeadUrl(String bucketName, String objectKey,
        Date expiryTime) throws S3ServiceException
    {
        return createSignedHeadUrl(bucketName, objectKey, expiryTime, false);
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified.
     *
     * @deprecated 0.7.4
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging', or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     * @param isHttps
     * if true, the signed URL will use the HTTPS protocol. If false, the signed URL will
     * use the HTTP protocol.
     * @param isDnsBucketNamingDisabled
     * if true, the signed URL will not use the DNS-name format for buckets eg.
     * <tt>jets3t.s3.amazonaws.com</tt>. Unless you have a specific reason to disable
     * DNS bucket naming, leave this value false.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    @Deprecated
    public static String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, ProviderCredentials credentials,
        long secondsSinceEpoch, boolean isVirtualHost, boolean isHttps,
        boolean isDnsBucketNamingDisabled) throws S3ServiceException
    {
        S3Service s3Service = new RestS3Service(credentials);
        return s3Service.createSignedUrl(method, bucketName, objectKey,
            specialParamName, headersMap, secondsSinceEpoch,
            isVirtualHost, isHttps, isDnsBucketNamingDisabled);
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified. The URL will use the default
     * JetS3t property settings in the <tt>jets3t.properties</tt> file to determine whether
     * to generate HTTP or HTTPS links (<tt>s3service.https-only</tt>), and whether to disable
     * DNS bucket naming (<tt>s3service.disable-dns-buckets</tt>).
     *
     * @deprecated 0.7.4
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging' or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, ProviderCredentials credentials,
        long secondsSinceEpoch, boolean isVirtualHost) throws S3ServiceException
    {
        boolean isHttps = this.getHttpsOnly();
        boolean disableDnsBuckets = this.getDisableDnsBuckets();

        return createSignedUrl(method, bucketName, objectKey, specialParamName,
            headersMap, credentials, secondsSinceEpoch, isVirtualHost, isHttps,
            disableDnsBuckets);
    }

    /**
     * Generates a signed URL string that will grant access to an S3 resource (bucket or object)
     * to whoever uses the URL up until the time specified.
     *
     * @deprecated 0.7.4
     *
     * @param method
     * the HTTP method to sign, such as GET or PUT (note that S3 does not support POST requests).
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param specialParamName
     * the name of a request parameter to add to the URL generated by this method. 'Special'
     * parameters may include parameters that specify the kind of S3 resource that the URL
     * will refer to, such as 'acl', 'torrent', 'logging' or 'location'.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param secondsSinceEpoch
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *  <b>Note:</b> This time is specified in seconds since the epoch, not milliseconds.
     *
     * @return
     * a URL signed in such a way as to grant access to an S3 resource to whoever uses it.
     *
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedUrl(String method, String bucketName, String objectKey,
        String specialParamName, Map<String, Object> headersMap, ProviderCredentials credentials,
        long secondsSinceEpoch) throws S3ServiceException
    {
        return createSignedUrl(method, bucketName, objectKey, specialParamName, headersMap,
            credentials, secondsSinceEpoch, false);
    }


    /**
     * Generates a signed GET URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant GET access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedGetUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime, boolean isVirtualHost)
        throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("GET", bucketName, objectKey, null, null,
            credentials, secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed GET URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to grant GET access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedGetUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime)
        throws S3ServiceException
    {
        return createSignedGetUrl(bucketName, objectKey, credentials, expiryTime, false);
    }


    /**
     * Generates a signed PUT URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to allow anyone to PUT an object into S3.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedPutUrl(String bucketName, String objectKey,
        Map<String, Object> headersMap, ProviderCredentials credentials, Date expiryTime,
        boolean isVirtualHost) throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("PUT", bucketName, objectKey, null, headersMap,
            credentials, secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed PUT URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param headersMap
     * headers to add to the signed URL, may be null.
     * Headers that <b>must</b> match between the signed URL and the actual request include:
     * content-md5, content-type, and any header starting with 'x-amz-'.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to allow anyone to PUT an object into S3.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedPutUrl(String bucketName, String objectKey,
        Map<String, Object> headersMap, ProviderCredentials credentials, Date expiryTime)
        throws S3ServiceException
    {
        return createSignedPutUrl(bucketName, objectKey, headersMap, credentials, expiryTime, false);
    }


    /**
     * Generates a signed DELETE URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to allow anyone do DELETE an object in S3.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedDeleteUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime, boolean isVirtualHost)
        throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("DELETE", bucketName, objectKey, null, null,
            credentials, secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed DELETE URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to allow anyone do DELETE an object in S3.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedDeleteUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime)
        throws S3ServiceException
    {
        return createSignedDeleteUrl(bucketName, objectKey, credentials, expiryTime, false);
    }


    /**
     * Generates a signed HEAD URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     * @param isVirtualHost
     * if this parameter is true, the bucket name is treated as a virtual host name. To use
     * this option, the bucket name must be a valid DNS name that is an alias to an S3 bucket.
     *
     * @return
     * a URL signed in such a way as to grant HEAD access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedHeadUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime, boolean isVirtualHost)
        throws S3ServiceException
    {
        long secondsSinceEpoch = expiryTime.getTime() / 1000;
        return createSignedUrl("HEAD", bucketName, objectKey, null, null,
            credentials, secondsSinceEpoch, isVirtualHost);
    }


    /**
     * Generates a signed HEAD URL.
     *
     * @deprecated 0.7.4
     *
     * @param bucketName
     * the name of the bucket to include in the URL, must be a valid bucket name.
     * @param objectKey
     * the name of the object to include in the URL, if null only the bucket name is used.
     * @param credentials
     * the credentials of someone with sufficient privileges to grant access to the bucket/object
     * @param expiryTime
     * the time after which URL's signature will no longer be valid. This time cannot be null.
     *
     * @return
     * a URL signed in such a way as to grant HEAD access to an S3 resource to whoever uses it.
     * @throws S3ServiceException
     */
    @Deprecated
    public String createSignedHeadUrl(String bucketName, String objectKey,
        ProviderCredentials credentials, Date expiryTime)
        throws S3ServiceException
    {
        return createSignedHeadUrl(bucketName, objectKey, credentials, expiryTime, false);
    }

    /**
     * Generates a URL string that will return a Torrent file for an object in S3,
     * which file can be downloaded and run in a BitTorrent client.
     *
     * @param bucketName
     * the name of the bucket containing the object.
     * @param objectKey
     * the name of the object.
     * @return
     * a URL to a Torrent file representing the object.
     */
    public String createTorrentUrl(String bucketName, String objectKey)
    {
        String s3Endpoint = this.getEndpoint();
        String serviceEndpointVirtualPath = this.getVirtualPath();
        int httpPort = this.getHttpPort();
        boolean disableDnsBuckets = this.getDisableDnsBuckets();

        try {
            String bucketNameInPath =
                !disableDnsBuckets && ServiceUtils.isBucketNameValidDNSName(bucketName)
                ? ""
                : RestUtils.encodeUrlString(bucketName) + "/";
            String urlPath =
                RestUtils.encodeUrlPath(serviceEndpointVirtualPath, "/")
                + "/" + bucketNameInPath
                + RestUtils.encodeUrlPath(objectKey, "/");
            return "http://" + ServiceUtils.generateS3HostnameForBucket(
                bucketName, disableDnsBuckets, s3Endpoint)
                + (httpPort != 80 ? ":" + httpPort : "")
                + urlPath
                + "?torrent";
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a policy document condition statement to represent an operation.
     *
     * @param operation
     * the name of the test operation this condition statement will apply.
     * @param name
     * the name of the data item the condition applies to.
     * @param value
     * the test value that will be used by the condition operation.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition(String operation, String name, String value) {
        return "[\"" + operation + "\", \"$" + name + "\", \"" + value + "\"]";
    }

    /**
     * Generates a policy document condition statement that will allow the named
     * data item in a POST request to take on any value.
     *
     * @param name
     * the name of the data item that will be allowed to take on any value.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition_AllowAnyValue(String name) {
        return "[\"starts-with\", \"$" + name + "\", \"\"]";
    }

    /**
     * Generates a policy document condition statement to represent an
     * equality test.
     *
     * @param name
     * the name of the data item that will be tested.
     * @param value
     * the value that the named data item must match.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition_Equality(String name, String value) {
        return "{\"" + name + "\": \"" + value + "\"}";
    }

    /**
     * Generates a policy document condition statement to represent an
     * equality test.
     *
     * @param name
     * the name of the data item that will be tested.
     * @param values
     * a list of values, one of which must match the named data item.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition_Equality(String name, String[] values) {
        return "{\"" + name + "\": \"" + ServiceUtils.join(values, ",") + "\"}";
    }

    /**
     * Generates a policy document condition statement to represent an
     * equality test.
     *
     * @param name
     * the name of the data item that will be tested.
     * @param values
     * a list of values, one of which must match the named data item.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition_Equality(String name, List<String> values) {
        return "{\"" + name + "\": \"" + ServiceUtils.join(values, ",") + "\"}";
    }

    /**
     * Generates a policy document condition statement to represent a test that
     * imposes a limit on the minimum and maximum amount of data the user can
     * upload via a POST form.
     *
     * @param min
     * the minimum number of bytes the user must upload. This value should be
     * greater than or equal to zero.
     * @param max
     * the maximum number of bytes the user can upload. This value must be
     * greater than or equal to the min value.
     * @return
     * a condition statement that can be included in the policy document
     * belonging to an S3 POST form.
     */
    public static String generatePostPolicyCondition_Range(int min, int max) {
        return "[\"content-length-range\", " + min + ", " + max + "]";
    }


    /**
     * Generates an <b>unauthenticated</b> HTML POST form that can be used to
     * upload files or data to S3 from a standard web browser.
     * <p>
     * Because the generated form is unauthenticated, it will not contain a
     * policy document and will only allow uploads to be sent to S3 buckets
     * that are publicly writable.
     *
     * @param bucketName
     * the name of the target bucket to which the data will be uploaded.
     * @param key
     * the key name for the object that will store the data. The key name can
     * include the special variable <tt>${filename}</tt> which expands to the
     * name of the file the user uploaded in the form.
     * @return
     * A form document that can be included in a UTF-8 encoded HTML web page
     * to allow uploads to a publicly-writable S3 bucket via a web browser.
     *
     * @throws S3ServiceException
     * @throws UnsupportedEncodingException
     */
    public static String buildPostForm(String bucketName, String key)
        throws S3ServiceException, UnsupportedEncodingException
    {
        return buildPostForm(bucketName, key, null, null, null, null, null, true);
    }


    /**
     * Generates an HTML POST form that can be used to upload files or data to
     * S3 from a standard web browser.
     * <p>
     * Depending on the parameter values provided, this method will generate an
     * authenticated or unauthenticated form. If the form is unauthenticated, it
     * will not include a policy document and will therefore not have an
     * expiry date or any usage conditions. Unauthenticated forms may only be
     * used to upload data to a publicly writable bucket.
     * <p>
     * If both the expiration and conditions parameters are non-null, the form
     * will include a policy document and will be authenticated. In this case,
     * you must provide your AWS credentials to sign the authenticated form.
     *
     * @param bucketName
     * the name of the target bucket to which the data will be uploaded.
     * @param key
     * the key name for the object that will store the data. The key name can
     * include the special variable <tt>${filename}</tt> which expands to the
     * name of the file the user uploaded in the form.
     * @param credentials
     * your Storage Provideer credentials. Credentials are only required if the form
     * includes policy document conditions, otherwise this can be null.
     * @param expiration
     * the expiration date beyond which the form will cease to work. If this
     * parameter is null, the generated form will not include a policy document
     * and will not have an expiry date.
     * @param conditions
     * the policy conditions applied to the form, specified as policy document
     * condition statements. These statements can be generated with the
     * convenience method {@link #generatePostPolicyCondition(String, String, String)}
     * and its siblings. If this parameter is null, the generated form will not
     * include a policy document and will not apply any usage conditions.
     * @param inputFields
     * optional input field strings that will be added to the form. Each string
     * must be a valid HTML form input field definition, such as
     * <tt>&lt;input type="hidden" name="acl" value="public-read"></tt>
     * @param textInput
     * an optional input field definition that is used instead of the default
     * file input field <tt>&lt;input name=\"file\" type=\"file\"></tt>. If this
     * parameter is null, the default file input field will be used to allow
     * file uploads. If this parameter is non-null, the provided string must
     * define an input field named "file" that allows the user to provide input,
     * such as <tt>&lt;textarea name="file" cols="60" rows="3">&lt;/textarea></tt>
     * @param isSecureHttp
     * if this parameter is true the form will upload data to S3 using HTTPS,
     * otherwise it will use HTTP.
     * @return
     * A form document that can be included in a UTF-8 encoded HTML web page
     * to allow uploads to S3 via a web browser.
     *
     * @throws S3ServiceException
     * @throws UnsupportedEncodingException
     */
    public static String buildPostForm(String bucketName, String key,
        ProviderCredentials credentials, Date expiration, String[] conditions,
        String[] inputFields, String textInput, boolean isSecureHttp)
        throws S3ServiceException, UnsupportedEncodingException
    {
        return buildPostForm(bucketName, key, credentials, expiration,
                conditions, inputFields, textInput, isSecureHttp,
                false, "Upload to Amazon S3");
    }

    /**
     * Generates an HTML POST form that can be used to upload files or data to
     * S3 from a standard web browser.
     * <p>
     * Depending on the parameter values provided, this method will generate an
     * authenticated or unauthenticated form. If the form is unauthenticated, it
     * will not include a policy document and will therefore not have an
     * expiry date or any usage conditions. Unauthenticated forms may only be
     * used to upload data to a publicly writable bucket.
     * <p>
     * If both the expiration and conditions parameters are non-null, the form
     * will include a policy document and will be authenticated. In this case,
     * you must provide your AWS credentials to sign the authenticated form.
     *
     * @param bucketName
     * the name of the target bucket to which the data will be uploaded.
     * @param key
     * the key name for the object that will store the data. The key name can
     * include the special variable <tt>${filename}</tt> which expands to the
     * name of the file the user uploaded in the form.
     * @param credentials
     * your Storage Provider credentials. Credentials are only required if the form
     * includes policy document conditions, otherwise this can be null.
     * @param expiration
     * the expiration date beyond which the form will cease to work. If this
     * parameter is null, the generated form will not include a policy document
     * and will not have an expiry date.
     * @param conditions
     * the policy conditions applied to the form, specified as policy document
     * condition statements. These statements can be generated with the
     * convenience method {@link #generatePostPolicyCondition(String, String, String)}
     * and its siblings. If this parameter is null, the generated form will not
     * include a policy document and will not apply any usage conditions.
     * @param inputFields
     * optional input field strings that will be added to the form. Each string
     * must be a valid HTML form input field definition, such as
     * <tt>&lt;input type="hidden" name="acl" value="public-read"></tt>
     * @param textInput
     * an optional input field definition that is used instead of the default
     * file input field <tt>&lt;input name=\"file\" type=\"file\"></tt>. If this
     * parameter is null, the default file input field will be used to allow
     * file uploads. If this parameter is non-null, the provided string must
     * define an input field named "file" that allows the user to provide input,
     * such as <tt>&lt;textarea name="file" cols="60" rows="3">&lt;/textarea></tt>
     * @param isSecureHttp
     * if this parameter is true the form will upload data to S3 using HTTPS,
     * otherwise it will use HTTP.
     * @param usePathStyleUrl
     * if true the deprecated path style URL will be used to specify the bucket
     * name, for example: http://s3.amazon.com/BUCKET_NAME. If false, the
     * recommended sub-domain style will be used, for example:
     * http://BUCKET_NAME.s3.amazon.com/.
     * The path style can be useful for accessing US-based buckets with SSL,
     * however non-US buckets are inaccessible with this style URL.
     * @param submitButtonName
     * the name to display on the form's submit button.
     *
     * @return
     * A form document that can be included in a UTF-8 encoded HTML web page
     * to allow uploads to S3 via a web browser.
     *
     * @throws S3ServiceException
     * @throws UnsupportedEncodingException
     */
    public static String buildPostForm(String bucketName, String key,
        ProviderCredentials credentials, Date expiration, String[] conditions,
        String[] inputFields, String textInput, boolean isSecureHttp,
        boolean usePathStyleUrl, String submitButtonName)
        throws S3ServiceException, UnsupportedEncodingException
    {
        List<String> myInputFields = new ArrayList<String>();

        // Form is only authenticated if a policy is specified.
        if (expiration != null || conditions != null) {
            // Generate policy document
            String policyDocument =
                "{\"expiration\": \"" + ServiceUtils.formatIso8601Date(expiration)
                + "\", \"conditions\": [" + ServiceUtils.join(conditions, ",") + "]}";
            if (log.isDebugEnabled()) {
                log.debug("Policy document for POST form:\n" + policyDocument);
            }

            // Add the base64-encoded policy document as the 'policy' form field
            String policyB64 = ServiceUtils.toBase64(
                policyDocument.getBytes(Constants.DEFAULT_ENCODING));
            myInputFields.add("<input type=\"hidden\" name=\"policy\" value=\""
                + policyB64 + "\">");

            // Add the AWS access key as the 'AWSAccessKeyId' field
            myInputFields.add("<input type=\"hidden\" name=\"AWSAccessKeyId\" " +
                "value=\"" + credentials.getAccessKey() + "\">");

            // Add signature for encoded policy document as the 'AWSAccessKeyId' field
            String signature;
            try {
                signature = ServiceUtils.signWithHmacSha1(
                    credentials.getSecretKey(), policyB64);
            } catch (ServiceException se) {
                throw new S3ServiceException(se);
            }
            myInputFields.add("<input type=\"hidden\" name=\"signature\" " +
                "value=\"" + signature + "\">");
        }

        // Include any additional user-specified form fields
        if (inputFields != null) {
            myInputFields.addAll(Arrays.asList(inputFields));
        }

        // Add the vital 'file' input item, which may be a textarea or file.
        if (textInput != null) {
            // Use a caller-specified string as the input field.
            myInputFields.add(textInput);
        } else {
            myInputFields.add("<input name=\"file\" type=\"file\">");
        }

        // Construct a URL to refer to the target bucket using either the
        // deprecated path style, or the recommended sub-domain style. The
        // HTTPS protocol will be used if the secure HTTP option is enabled.
        String url = null;
        if (usePathStyleUrl) {
            url = "http" + (isSecureHttp? "s" : "") +
                "://s3.amazonaws.com/" +  bucketName;
        } else {
            // Sub-domain URL style
            url = "http" + (isSecureHttp? "s" : "") +
                "://" + bucketName + ".s3.amazonaws.com/";
        }

        // Construct the entire form.
        String form =
          "<form action=\"" + url + "\" method=\"post\" " +
              "enctype=\"multipart/form-data\">\n" +
            "<input type=\"hidden\" name=\"key\" value=\"" + key + "\">\n" +
            ServiceUtils.join(myInputFields, "\n") +
            "\n<br>\n" +
            "<input type=\"submit\" value=\"" + submitButtonName + "\">\n" +
          "</form>";

        if (log.isDebugEnabled()) {
            log.debug("POST Form:\n" + form);
        }
        return form;
    }

    /////////////////////////////////////////////////
    // Methods below this point perform actions in S3
    /////////////////////////////////////////////////

    @Override
    public S3Bucket[] listAllBuckets() throws S3ServiceException {
        try {
            StorageBucket[] buckets = super.listAllBuckets();
            return S3Bucket.cast(buckets);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Object getObject(String bucketName, String objectKey) throws S3ServiceException {
        try {
            return (S3Object) super.getObject(bucketName, objectKey);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Lists the objects in a bucket.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can only list the objects in a publicly-readable bucket.
     *
     * @param bucket
     * the bucket whose contents will be listed.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @return
     * the set of objects contained in a bucket.
     * @throws S3ServiceException
     */
    @Deprecated
    public S3Object[] listObjects(S3Bucket bucket) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "listObjects");
            return listObjects(bucket, null, null, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Object[] listObjects(String bucketName) throws S3ServiceException {
        try {
            return S3Object.cast(super.listObjects(bucketName));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Object[] listObjects(String bucketName, String prefix,
        String delimiter, long maxListingLength) throws S3ServiceException
    {
        try {
            return S3Object.cast(super.listObjects(bucketName, prefix, delimiter, maxListingLength));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Lists the objects in a bucket matching a prefix and delimiter.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can only list the objects in a publicly-readable bucket.
     * <p>
     * NOTE: If you supply a delimiter value that could cause CommonPrefixes
     * ("subdirectory paths") to be included in the results from S3, use the
     * {@link #listObjectsChunked(String, String, String, long, String, boolean)}
     * method instead of this one to obtain both object and CommonPrefix values.
     *
     * @param bucket
     * the bucket whose contents will be listed.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * See note above.
     * <b>Note</b>: If a non-null delimiter is specified, the prefix must include enough text to
     * reach the first occurrence of the delimiter in the bucket's keys, or no results will be returned.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws S3ServiceException
     */
    @Deprecated
    public S3Object[] listObjects(S3Bucket bucket, String prefix, String delimiter) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "listObjects");
            return listObjects(bucket, prefix, delimiter, Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Object[] listObjects(String bucketName, String prefix, String delimiter)
            throws S3ServiceException
    {
        try {
            return S3Object.cast(super.listObjects(bucketName, prefix, delimiter));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Bucket createBucket(String bucketName) throws S3ServiceException {
        try {
            return this.createBucket(bucketName,
                this.jets3tProperties.getStringProperty(
                    "s3service.default-bucket-location", "US"), null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Bucket getOrCreateBucket(String bucketName) throws S3ServiceException {
        try {
            return this.getOrCreateBucket(bucketName,
                this.jets3tProperties.getStringProperty(
                    "s3service.default-bucket-location", "US"));
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Creates a bucket in a specific location, without checking whether the bucket already
     * exists. <b>Caution:</b> Performing this operation unnecessarily when a bucket already
     * exists may cause OperationAborted errors with the message "A conflicting conditional
     * operation is currently in progress against this resource.". To avoid this error, use the
     * {@link #getOrCreateBucket(String)} in situations where the bucket may already exist.
     * <p>
     * <b>Warning:</b> Prior to version 0.7.0 this method did check whether a bucket already
     * existed using {@link #isBucketAccessible(String)}. After changes to the way S3 operates,
     * this check started to cause issues so it was removed.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucketName
     * the name of the bucket to create.
     * @param location
     * the location of the S3 data centre in which the bucket will be created, or null for the
     * default {@link S3Bucket#LOCATION_US_STANDARD} location. Valid values
     * include {@link S3Bucket#LOCATION_EUROPE}, {@link S3Bucket#LOCATION_US_WEST},
     * {@link S3Bucket#LOCATION_ASIA_PACIFIC}, and the default US location that can be
     * expressed in two ways:
     * {@link S3Bucket#LOCATION_US_STANDARD} or {@link S3Bucket#LOCATION_US}.
     * @param acl
     * the access control settings to apply to the new bucket, or null for default ACL values.
     *
     * @return
     * the created bucket object. <b>Note:</b> the object returned has minimal information about
     * the bucket that was created, including only the bucket's name.
     * @throws S3ServiceException
     */
    public S3Bucket createBucket(String bucketName, String location, AccessControlList acl)
        throws S3ServiceException
    {
        try {
            assertAuthenticatedConnection("createBucket");
            return (S3Bucket) createBucketImpl(bucketName, location, acl);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Creates a bucket in a specific location, without checking whether the bucket already
     * exists. <b>Caution:</b> Performing this operation unnecessarily when a bucket already
     * exists may cause OperationAborted errors with the message "A conflicting conditional
     * operation is currently in progress against this resource.". To avoid this error, use the
     * {@link #getOrCreateBucket(String)} in situations where the bucket may already exist.
     * <p>
     * <b>Warning:</b> Prior to version 0.7.0 this method did check whether a bucket already
     * existed using {@link #isBucketAccessible(String)}. After changes to the way S3 operates,
     * this check started to cause issues so it was removed.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucketName
     * the name of the bucket to create.
     * @param location
     * the location of the S3 data centre in which the bucket will be created, or null for the
     * default {@link S3Bucket#LOCATION_US_STANDARD} location. Valid values
     * include {@link S3Bucket#LOCATION_EUROPE}, {@link S3Bucket#LOCATION_US_WEST},
     * {@link S3Bucket#LOCATION_ASIA_PACIFIC}, and the default US location that can be
     * expressed in two ways:
     * {@link S3Bucket#LOCATION_US_STANDARD} or {@link S3Bucket#LOCATION_US}.
     *
     * @return
     * the created bucket object. <b>Note:</b> the object returned has minimal information about
     * the bucket that was created, including only the bucket's name.
     * @throws S3ServiceException
     */
    public S3Bucket createBucket(String bucketName, String location) throws S3ServiceException
    {
        try {
            assertAuthenticatedConnection("createBucket");
            return (S3Bucket) createBucketImpl(bucketName, location, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details and data of an item in S3, without applying any
     * preconditions.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object.
     * <p>
     * <b>Important:</b> It is the caller's responsibility to close the object's data input stream.
     * The data stream should be consumed and closed as soon as is practical as network connections
     * may be held open until the streams are closed. Excessive unclosed streams can lead to
     * connection starvation.
     *
     * @param bucket
     * the bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key in S3, including the object's data input stream.
     * @throws S3ServiceException
     */
    @Deprecated
    public S3Object getObject(S3Bucket bucket, String objectKey) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "getObject");
            return getObject(bucket, objectKey, null, null, null, null, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details and data of an item in S3 with a specific
     * given version, without applying any preconditions. Versioned objects are only available
     * from buckets with versioning enabled, see {@link #enableBucketVersioning(String)}.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object.
     * <p>
     * <b>Important:</b> It is the caller's responsibility to close the object's data input stream.
     * The data stream should be consumed and closed as soon as is practical as network connections
     * may be held open until the streams are closed. Excessive unclosed streams can lead to
     * connection starvation.
     *
     * @param versionId
     * identifier matching an existing object version that will be retrieved.
     * @param bucketName
     * the name of the versioned bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key in S3, including the object's data input stream.
     * @throws S3ServiceException
     */
    public S3Object getVersionedObject(String versionId, String bucketName, String objectKey)
        throws S3ServiceException
    {
        try {
            MxDelegate.getInstance().registerStorageObjectGetEvent(bucketName, objectKey);
            return (S3Object) getObjectImpl(bucketName, objectKey,
                null, null, null, null, null, null, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details of an item in S3 without the object's data, and
     * without applying any preconditions.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object's details.
     *
     * @param bucket
     * the bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    @Deprecated
    public S3Object getObjectDetails(S3Bucket bucket, String objectKey) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "getObjectDetails");
            return getObjectDetails(bucket, objectKey, null, null, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details of an item in S3 with a specific given version,
     * without the object's data and without applying any preconditions. Versioned objects are only
     * available from buckets with versioning enabled, see {@link #enableBucketVersioning(String)}.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get a publicly-readable object's details.
     *
     * @param versionId
     * object's version identifier
     * @param bucketName
     * the name of the versioned bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getVersionedObjectDetails(String versionId, String bucketName,
        String objectKey) throws S3ServiceException
    {
        try {
            MxDelegate.getInstance().registerStorageObjectHeadEvent(bucketName, objectKey);
            return (S3Object) getObjectDetailsImpl(bucketName, objectKey,
                null, null, null, null, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Lists the objects in a bucket matching a prefix, while instructing S3 to
     * send response messages containing no more than a given number of object
     * results.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * The objects returned by this method contain only minimal information
     * such as the object's size, ETag, and LastModified timestamp. To retrieve
     * the objects' metadata you must perform follow-up <code>getObject</code>
     * or <code>getObjectDetails</code> operations.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can list the contents of a publicly-readable bucket.
     * <p>
     * NOTE: If you supply a delimiter value that could cause CommonPrefixes
     * ("subdirectory paths") to be included in the results from S3, use the
     * {@link #listObjectsChunked(String, String, String, long, String, boolean)}
     * method instead of this one to obtain both object and CommonPrefix values.
     *
     * @param bucket
     * the bucket whose contents will be listed.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * See note above.
     * @param maxListingLength
     * the maximum number of objects to include in each result message sent by
     * S3. This value has <strong>no effect</strong> on the number of objects
     * that will be returned by this method, because it will always return all
     * the objects in the bucket.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws S3ServiceException
     */
    @Deprecated
    public S3Object[] listObjects(S3Bucket bucket, String prefix, String delimiter,
        long maxListingLength) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "List objects in bucket");
            return listObjects(bucket.getName(), prefix, delimiter, maxListingLength);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Lists versioning information in a versioned bucket where the objects
     * match a given constraints. The S3 service will also be instructed to send
     * response messages containing no more than a given number of object results.
     * <p>
     * This operation can only be performed by the bucket owner.
     *
     * @param bucketName
     * the name of the the versioned bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * See note above.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws S3ServiceException
     */
    public BaseVersionOrDeleteMarker[] listVersionedObjects(String bucketName, String prefix,
        String delimiter)
        throws S3ServiceException
    {
        return listVersionedObjectsImpl(bucketName, prefix, delimiter, null, null, 1000);
    }

    /**
     * Return version information for a specific object.
     * <p>
     * This is a convenience function that applies logic in addition to the LISTVERSIONS
     * S3 operation to simplify retrieval of an object's version history. This method
     * is *not* the most efficient way of retrieving version history in bulk, so if you
     * need version history for multiple objects you should use the
     * {@link #listVersionedObjects(String, String, String)} or
     * {@link #listVersionedObjectsChunked(String, String, String, long, String, String, boolean)}
     * methods instead.
     *
     * @param bucketName
     * the name of the versioned bucket containing the object.
     * @param objectKey
     * the key identifying the object.
     * @return
     * an array of {@link S3Version} and {@link S3DeleteMarker} objects that describe
     * the version history of the given object.
     *
     * @throws S3ServiceException
     */
    public BaseVersionOrDeleteMarker[] getObjectVersions(String bucketName, String objectKey)
        throws S3ServiceException
    {
        BaseVersionOrDeleteMarker[] matchesForNamePrefix =
            listVersionedObjectsImpl(bucketName, objectKey, null, null, null, 1000);
        // Limit results to only matches for the exact object key name
        int exactMatchCount = 0;
        for (int i = 0; i < matchesForNamePrefix.length && i <= exactMatchCount; i++) {
            if (matchesForNamePrefix[i].getKey().equals(objectKey)) {
                exactMatchCount++;
            }
        }
        BaseVersionOrDeleteMarker[] exactMatches = new BaseVersionOrDeleteMarker[exactMatchCount];
        System.arraycopy(matchesForNamePrefix, 0, exactMatches, 0, exactMatchCount);
        return exactMatches;
    }

    /**
     * Lists information for a versioned bucket where the items match given constarints.
     * Depending on the value of the completeListing variable, this method can be set to
     * automatically perform follow-up requests to build a complete object listing, or to
     * return only a partial listing.
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
     * the name of the versioned bucket whose contents will be listed.
     * @param prefix
     * only objects with a key that starts with this prefix will be listed
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * @param maxListingLength
     * the maximum number of objects to include in each result chunk
     * @param priorLastKey
     * the last object key received in a prior call to this method. The next chunk of items
     * listed will start with the next object in the bucket <b>after</b> this key name.
     * This parameter may be null, in which case the listing will start at the beginning of the
     * bucket's object contents.
     * @param priorLastVersionId
     * the last version ID received in a prior call to this method. The next chunk of items
     * listed will start with the next object version <b>after</b> this version.
     * This parameter can only be used with a non-null priorLastKey.
     * @param completeListing
     * if true, the service class will automatically perform follow-up requests to
     * build a complete bucket object listing.
     * @return
     * the set of objects contained in a bucket whose keys start with the given prefix.
     * @throws S3ServiceException
     */
    public VersionOrDeleteMarkersChunk listVersionedObjectsChunked(String bucketName,
        String prefix, String delimiter, long maxListingLength, String priorLastKey,
        String priorLastVersionId, boolean completeListing) throws S3ServiceException
    {
        return listVersionedObjectsChunkedImpl(bucketName, prefix, delimiter,
            maxListingLength, priorLastKey, priorLastVersionId, completeListing);
    }

    /**
     * Creates a bucket in S3 based on the provided bucket object, with the Access Control List
     * settings and location properties of the bucket object (if any).
     * <p>
     * <b>Caution:</b> Performing this operation unnecessarily when a bucket already
     * exists may cause OperationAborted errors with the message "A conflicting conditional
     * operation is currently in progress against this resource.". To avoid this error, use the
     * {@link #getOrCreateBucket(String)} in situations where the bucket may already exist.
     * <p>
     * This method cannot be performed by anonymous services.
     *
     * @param bucket
     * an object representing the bucket to create which must be valid, and which may contain
     * location and ACL settings that will be applied upon creation.
     * @return
     * the created bucket object, populated with all metadata made available by the creation operation.
     * @throws S3ServiceException
     */
    public S3Bucket createBucket(S3Bucket bucket) throws S3ServiceException {
        try {
            assertAuthenticatedConnection("Create Bucket");
            assertValidBucket(bucket, "Create Bucket");
            return (S3Bucket) createBucketImpl(bucket.getName(), bucket.getLocation(), bucket.getAcl());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Bucket getBucket(String bucketName) throws S3ServiceException {
        try {
            return (S3Bucket) super.getBucket(bucketName);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns a bucket in your S3 account, and creates the bucket in the given S3 location
     * if it does not yet exist.
     * <p>
     * Note: This method will not change the location of an existing bucket if you specify
     * a different location from a bucket's current location. To move a bucket between
     * locations you must first delete it in the original location, then re-create it
     * in the new location.
     *
     * @param bucketName
     * the name of the bucket to retrieve or create.
     * @param location
     * the location of the S3 data centre in which the bucket will be created. Valid values
     * include {@link S3Bucket#LOCATION_EUROPE}, {@link S3Bucket#LOCATION_US_WEST},
     * {@link S3Bucket#LOCATION_ASIA_PACIFIC}, and the default US location that can be
     * expressed in two ways:
     * {@link S3Bucket#LOCATION_US_STANDARD} or {@link S3Bucket#LOCATION_US}.
     * @return
     * the bucket in your account.
     *
     * @throws S3ServiceException
     */
    public S3Bucket getOrCreateBucket(String bucketName, String location)
        throws S3ServiceException
    {
        try {
            assertAuthenticatedConnection("Get or Create Bucket with location");
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }

        S3Bucket bucket = getBucket(bucketName);
        if (bucket == null) {
            // Bucket does not exist in this user's account, create it.
            bucket = createBucket(new S3Bucket(bucketName, location));
        }
        return bucket;
    }

    /**
     * Deletes an S3 bucket. Only the owner of a bucket may delete it.
     *
     * @deprecated 0.8.0
     *
     * <p>
     * This method cannot be performed by anonymous services.
     *
     *
     * @param bucket
     * the bucket to delete.
     * @throws S3ServiceException
     */
    @Deprecated
    public void deleteBucket(S3Bucket bucket) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "Delete bucket");
            deleteBucketImpl(bucket.getName());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Enable the S3 object versioning feature for a bucket.
     * Multi-factor authentication will not be required to delete versions.
     *
     * @param bucketName
     * the name of the bucket that will have versioning enabled.
     * @throws S3ServiceException
     */
    public void enableBucketVersioning(String bucketName) throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, true, false, null, null);
    }

    /**
     * Enable the S3 object versioning feature and also enable the
     * multi-factor authentication (MFA) feature for a bucket which
     * does not yet have MFA enabled.
     *
     * @param bucketName
     * the name of the bucket that will have versioning enabled.
     * @throws S3ServiceException
     */
    public void enableBucketVersioningAndMFA(String bucketName)
        throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, true, true, null, null);
    }

    /**
     * Enable the S3 object versioning feature for a bucket that
     * already has multi-factor authentication (MFA) enabled.
     *
     * @param bucketName
     * the name of the bucket that will have versioning enabled.
     * @param multiFactorSerialNumber
     * the serial number for a multi-factor authentication device.
     * @param multiFactorAuthCode
     * a multi-factor authentication code generated by a device.
     * @throws S3ServiceException
     */
    public void enableBucketVersioningWithMFA(String bucketName,
        String multiFactorSerialNumber, String multiFactorAuthCode)
        throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, true, true,
            multiFactorSerialNumber, multiFactorAuthCode);
    }

    /**
     * Disable the multi-factor authentication (MFA) feature for a
     * bucket that already has S3 object versioning and MFA enabled.
     *
     * @param bucketName
     * the name of the bucket that will have versioning enabled.
     * versioning status of the bucket.
     * @param multiFactorSerialNumber
     * the serial number for a multi-factor authentication device.
     * @param multiFactorAuthCode
     * a multi-factor authentication code generated by a device.
     * @throws S3ServiceException
     */
    public void disableMFAForVersionedBucket(String bucketName,
        String multiFactorSerialNumber, String multiFactorAuthCode)
        throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, true, false,
            multiFactorSerialNumber, multiFactorAuthCode);
    }

    /**
     * Suspend (disable) the S3 object versioning feature for a bucket.
     * The bucket must not have the multi-factor authentication (MFA)
     * feature enabled.
     *
     * @param bucketName
     * the name of the versioned bucket that will have versioning suspended.
     * @throws S3ServiceException
     */
    public void suspendBucketVersioning(String bucketName)
        throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, false, false, null, null);
    }

    /**
     * Suspend (disable) the S3 object versioning feature for a bucket that
     * requires multi-factor authentication.
     *
     * @param bucketName
     * the name of the versioned bucket that will have versioning suspended.
     * @param multiFactorSerialNumber
     * the serial number for a multi-factor authentication device.
     * @param multiFactorAuthCode
     * a multi-factor authentication code generated by a device.
     * @throws S3ServiceException
     */
    public void suspendBucketVersioningWithMFA(String bucketName,
        String multiFactorSerialNumber, String multiFactorAuthCode)
        throws S3ServiceException
    {
        updateBucketVersioningStatusImpl(bucketName, false,
            false, multiFactorSerialNumber, multiFactorAuthCode);
    }

    /**
     * Return versioning status of bucket, which reports on whether the given bucket
     * has S3 object versioning enabled and whether multi-factor authentication is
     * required to delete versions.
     *
     * @param bucketName
     * the name of the bucket.
     * @return
     * versioning status of bucket
     * @throws S3ServiceException
     */
    public S3BucketVersioningStatus getBucketVersioningStatus(String bucketName)
        throws S3ServiceException
    {
        return getBucketVersioningStatusImpl(bucketName);
    }

    /**
     * Puts an object inside an existing bucket in S3, creating a new object or overwriting
     * an existing one with the same key.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can put objects into a publicly-writable bucket.
     *
     * @param bucketName
     * the name of the bucket inside which the object will be put.
     * @param object
     * the object containing all information that will be written to S3. At very least this object must
     * be valid. Beyond that it may contain: an input stream with the object's data content, metadata,
     * and access control settings.<p>
     * <b>Note:</b> It is very important to set the object's Content-Length to match the size of the
     * data input stream when possible, as this can remove the need to read data into memory to
     * determine its size.
     *
     * @return
     * the object populated with any metadata information made available by S3.
     * @throws S3ServiceException
     */
    public S3Object putObject(String bucketName, S3Object object) throws S3ServiceException {
        try {
            return (S3Object) super.putObject(bucketName, object);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Copy an object with a specific version within your S3 account. You can copy an object
     * within a single bucket or between buckets, and can optionally update the object's
     * metadata at the same time.
     * <p>
     * This method cannot be performed by anonymous services. You must have read
     * access to the source object and write access to the destination bucket.
     * <p>
     * An object can be copied over itself, in which case you can update its
     * metadata without making any other changes.
     *
     * @param versionId
     * identifier matching an existing object version that will be copied.
     * @param sourceBucketName
     * the name of the versioned bucket that contains the original object.
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
     * a map of the header and result information returned by S3 after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws S3ServiceException
     */
    public Map<String, Object> copyVersionedObject(String versionId, String sourceBucketName,
        String sourceObjectKey, String destinationBucketName, S3Object destinationObject,
        boolean replaceMetadata, Calendar ifModifiedSince,
        Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws S3ServiceException
    {
        try {
            assertAuthenticatedConnection("copyVersionedObject");
            Map<String, Object> destinationMetadata =
                replaceMetadata ? destinationObject.getModifiableMetadata() : null;

            MxDelegate.getInstance().registerStorageObjectCopyEvent(sourceBucketName, sourceObjectKey);
            return copyObjectImpl(sourceBucketName, sourceObjectKey,
                destinationBucketName, destinationObject.getKey(),
                destinationObject.getAcl(), destinationMetadata,
                ifModifiedSince, ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, versionId,
                destinationObject.getStorageClass());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Copy an object with a specific version within your S3 account. You can copy an object
     * within a single bucket or between buckets, and can optionally update the object's
     * metadata at the same time.
     * <p>
     * This method cannot be performed by anonymous services. You must have read
     * access to the source object and write access to the destination bucket.
     * <p>
     * An object can be copied over itself, in which case you can update its
     * metadata without making any other changes.
     *
     * @param versionId
     * identifier matching an existing object version that will be copied.
     * @param sourceBucketName
     * the name of the versioned bucket that contains the original object.
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
     * a map of the header and result information returned by S3 after the object
     * copy. The map includes the object's MD5 hash value (ETag), its size
     * (Content-Length), and update timestamp (Last-Modified).
     *
     * @throws S3ServiceException
     */
    public Map<String, Object> copyVersionedObject(String versionId, String sourceBucketName,
        String sourceObjectKey, String destinationBucketName, S3Object destinationObject,
        boolean replaceMetadata) throws S3ServiceException
    {
        return copyVersionedObject(versionId, sourceBucketName, sourceObjectKey,
            destinationBucketName, destinationObject, replaceMetadata, null, null, null, null);
    }

    /**
     * Puts an object inside an existing bucket in S3, creating a new object or overwriting
     * an existing one with the same key.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can put objects into a publicly-writable bucket.
     *
     * @param bucket
     * the bucket inside which the object will be put, which must be valid.
     * @param object
     * the object containing all information that will be written to S3. At very least this object must
     * be valid. Beyond that it may contain: an input stream with the object's data content, metadata,
     * and access control settings.<p>
     * <b>Note:</b> It is very important to set the object's Content-Length to match the size of the
     * data input stream when possible, as this can remove the need to read data into memory to
     * determine its size.
     *
     * @return
     * the object populated with any metadata information made available by S3.
     * @throws S3ServiceException
     */
    public S3Object putObject(S3Bucket bucket, S3Object object) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "Create Object in bucket");
            return putObject(bucket.getName(), object);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Deletes an object from a bucket in S3.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can delete objects from publicly-writable buckets.
     *
     * @param bucket
     * the bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     * @throws S3ServiceException
     */
    public void deleteObject(S3Bucket bucket, String objectKey) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "deleteObject");
            assertValidObject(objectKey, "deleteObject");
            deleteObject(bucket.getName(), objectKey);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Deletes a object version from a bucket in S3. This will delete only the specific
     * version identified and will not affect any other Version or DeleteMarkers related
     * to the object.
     * <p>
     * This operation can only be performed by the owner of the S3 bucket.
     *
     * @param versionId
     * the identifier of an object version that will be deleted.
     * @param multiFactorSerialNumber
     * the serial number for a multi-factor authentication device.
     * @param multiFactorAuthCode
     * a multi-factor authentication code generated by a device.
     * @param bucketName
     * the name of the versioned bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     * @throws S3ServiceException
     */
    public void deleteVersionedObjectWithMFA(String versionId,
        String multiFactorSerialNumber, String multiFactorAuthCode,
        String bucketName, String objectKey) throws S3ServiceException
    {
        try {
            assertValidObject(objectKey, "deleteVersionedObjectWithMFA");
            MxDelegate.getInstance().registerStorageObjectDeleteEvent(bucketName, objectKey);
            deleteObjectImpl(bucketName, objectKey, versionId,
                multiFactorSerialNumber, multiFactorAuthCode);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Deletes a object version from a bucket in S3. This will delete only the specific
     * version identified and will not affect any other Version or DeleteMarkers related
     * to the object.
     * <p>
     * This operation can only be performed by the owner of the S3 bucket.
     *
     * @param versionId
     * the identifier of an object version that will be deleted.
     * @param bucketName
     * the name of the versioned bucket containing the object to be deleted.
     * @param objectKey
     * the key representing the object in S3.
     * @throws S3ServiceException
     */
    public void deleteVersionedObject(String versionId, String bucketName, String objectKey)
        throws S3ServiceException
    {
        try {
            assertValidObject(objectKey, "deleteVersionedObject");
            MxDelegate.getInstance().registerStorageObjectDeleteEvent(bucketName, objectKey);
            deleteObjectImpl(bucketName, objectKey, versionId, null, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details of an item in S3 that meets any given preconditions.
     * The object is returned without the object's data.
     * <p>
     * An exception is thrown if any of the preconditions fail.
     * Preconditions are only applied if they are non-null.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get details of publicly-readable objects.
     *
     * @param bucket
     * the bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getObjectDetails(S3Bucket bucket, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get Object Details");
            MxDelegate.getInstance().registerStorageObjectHeadEvent(bucket.getName(), objectKey);
            return (S3Object) getObjectDetailsImpl(bucket.getName(), objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details of a versioned object in S3 that also
     * meets any given preconditions. The object is returned without the object's data.
     * <p>
     * An exception is thrown if any of the preconditions fail.
     * Preconditions are only applied if they are non-null.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get details of publicly-readable objects.
     *
     * @param versionId
     * the identifier of the object version to return.
     * @param bucket
     * the versioned bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getVersionedObjectDetails(String versionId, S3Bucket bucket, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get Versioned Object Details");
            MxDelegate.getInstance().registerStorageObjectHeadEvent(bucket.getName(), objectKey);
            return (S3Object) getObjectDetailsImpl(bucket.getName(), objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details of a versioned object in S3 that also meets
     * any given preconditions. The object is returned without the object's data.
     * <p>
     * An exception is thrown if any of the preconditions fail.
     * Preconditions are only applied if they are non-null.
     * <p>
     * This method can be performed by anonymous services. Anonymous services
     * can get details of publicly-readable objects.
     *
     * @param versionId
     * the identifier of the object version to return.
     * @param bucketName
     * the name of the versioned bucket containing the object.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getVersionedObjectDetails(String versionId, String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
        String[] ifNoneMatchTags) throws S3ServiceException
    {
        try {
            MxDelegate.getInstance().registerStorageObjectHeadEvent(bucketName, objectKey);
            return (S3Object) getObjectDetailsImpl(bucketName, objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    @Override
    public S3Object getObject(String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
        String[] ifMatchTags, String[] ifNoneMatchTags, Long byteRangeStart,
        Long byteRangeEnd) throws S3ServiceException
    {
        try {
            return (S3Object) super.getObject(bucketName, objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, byteRangeStart,
                byteRangeEnd);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details and data of an item in S3 that meets any given preconditions.
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
     * can get publicly-readable objects.
     * <p>
     * <b>Implementation notes</b><p>
     * Implementations should use {@link #assertValidBucket} assertion.
     *
     * @param bucket
     * the bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getObject(S3Bucket bucket, String objectKey, Calendar ifModifiedSince,
        Calendar ifUnmodifiedSince, String[] ifMatchTags, String[] ifNoneMatchTags,
        Long byteRangeStart, Long byteRangeEnd) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get Object");
            MxDelegate.getInstance().registerStorageObjectGetEvent(bucket.getName(), objectKey);
            return (S3Object) getObjectImpl(bucket.getName(), objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, byteRangeStart, byteRangeEnd, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details and data of a versioned object in S3 that
     * also meets any given preconditions.
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
     * can get publicly-readable objects.
     * <p>
     * <b>Implementation notes</b><p>
     * Implementations should use {@link #assertValidBucket} assertion.
     *
     * @param versionId
     * the identifier of the object version to return.
     * @param bucket
     * the versioned bucket containing the object.
     * This must be a valid S3Bucket object that is non-null and contains a name.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getVersionedObject(String versionId, S3Bucket bucket, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
        String[] ifMatchTags, String[] ifNoneMatchTags,
        Long byteRangeStart, Long byteRangeEnd) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get Versioned Object");
            MxDelegate.getInstance().registerStorageObjectGetEvent(bucket.getName(), objectKey);
            return (S3Object) getObjectImpl(bucket.getName(), objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, byteRangeStart, byteRangeEnd, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Returns an object representing the details and data of a versioned object in S3 that
     * also meets any given preconditions.
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
     * @param versionId
     * the identifier of the object version to return.
     * @param bucketName
     * the name of the versioned bucket containing the object.
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
     * the object with the given key in S3, including only general details and metadata (not the data
     * input stream)
     * @throws S3ServiceException
     */
    public S3Object getVersionedObject(String versionId, String bucketName, String objectKey,
        Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
        String[] ifMatchTags, String[] ifNoneMatchTags,
        Long byteRangeStart, Long byteRangeEnd) throws S3ServiceException
    {
        try {
            MxDelegate.getInstance().registerStorageObjectGetEvent(bucketName, objectKey);
            return (S3Object) getObjectImpl(bucketName, objectKey, ifModifiedSince,
                ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, byteRangeStart, byteRangeEnd, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Applies access control settings to an object. The ACL settings must be included
     * with the object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param bucket
     * the bucket containing the object to modify.
     * @param object
     * the object with ACL settings that will be applied.
     * @throws S3ServiceException
     */
    public void putObjectAcl(S3Bucket bucket, S3Object object) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "Put Object Access Control List");
            assertValidObject(object, "Put Object Access Control List");
            putObjectAcl(bucket.getName(), object.getKey(), object.getAcl());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Applies access control settings to an object. The ACL settings must be included
     * with the object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param bucketName
     * the name of the bucket containing the object to modify.
     * @param object
     * the object with ACL settings that will be applied.
     * @throws S3ServiceException
     */
    public void putObjectAcl(String bucketName, S3Object object) throws S3ServiceException {
        try {
            assertValidObject(object, "Put Object Access Control List");
            putObjectAcl(bucketName, object.getKey(), object.getAcl());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Applies access control settings to a versioned object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param versionId
     * the identifier of the object version whose ACL will be updated.
     * @param bucketName
     * the name of the versioned bucket containing the object to modify.
     * @param objectKey
     * the key name of the object to which ACL settings will be applied.
     * @param acl
     * ACL settings to apply.
     * @throws S3ServiceException
     */
    public void putVersionedObjectAcl(String versionId, String bucketName,
        String objectKey, AccessControlList acl) throws S3ServiceException
    {
        try {
            putObjectAclImpl(bucketName, objectKey, acl, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Applies access control settings to a versioned object.
     * The ACL settings must be included with the object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param versionId
     * the identifier of the object version whose ACL will be updated.
     * @param bucket
     * the bucket containing the object to modify.
     * @param object
     * the object with ACL settings that will be applied.
     *
     * @throws S3ServiceException
     */
    public void putVersionedObjectAcl(String versionId, S3Bucket bucket, S3Object object)
        throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Put Versioned Object Access Control List");
            assertValidObject(object, "Put Versioned Object Access Control List");
            String objectKey = object.getKey();
            AccessControlList acl = object.getAcl();
            if (acl == null) {
                throw new S3ServiceException("The object '" + objectKey +
                    "' does not include ACL information");
            }
            putObjectAclImpl(bucket.getName(), objectKey, acl, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Retrieves the access control settings of an object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows read access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param bucket
     * the bucket whose ACL settings will be retrieved (if objectKey is null) or the bucket containing the
     * object whose ACL settings will be retrieved (if objectKey is non-null).
     * @param objectKey
     * if non-null, the key of the object whose ACL settings will be retrieved. Ignored if null.
     * @return
     * the ACL settings of the bucket or object.
     * @throws S3ServiceException
     */
    public AccessControlList getObjectAcl(S3Bucket bucket, String objectKey)
        throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get Object Access Control List");
            return getObjectAclImpl(bucket.getName(), objectKey, null);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Retrieves the access control settings of a versioned object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows read access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param versionId
     * the identifier of the object version whose ACL will be returned.
     * @param bucket
     * the versioned bucket whose ACL settings will be retrieved (if objectKey is null) or the bucket
     * containing the object whose ACL settings will be retrieved (if objectKey is non-null).
     * @param objectKey
     * if non-null, the key of the object whose ACL settings will be retrieved. Ignored if null.
     * @return
     * the ACL settings of the bucket or object.
     * @throws S3ServiceException
     */
    public AccessControlList getVersionedObjectAcl(String versionId, S3Bucket bucket,
        String objectKey) throws S3ServiceException
    {
        try {
            assertValidBucket(bucket, "Get versioned Object Access Control List");
            return getObjectAclImpl(bucket.getName(), objectKey, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Retrieves the access control settings of a versioned object.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * object's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of an object if the ACL already in place
     * for that object (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param versionId
     * the identifier of the object version whose ACL will be returned.
     * @param bucketName
     * the name of the versioned bucket containing the object whose ACL settings will be retrieved.
     * @param objectKey
     * if non-null, the key of the object whose ACL settings will be retrieved. Ignored if null.
     * @return
     * the ACL settings of the bucket or object.
     * @throws S3ServiceException
     */
    public AccessControlList getVersionedObjectAcl(String versionId, String bucketName,
        String objectKey) throws S3ServiceException
    {
        try {
            return getObjectAclImpl(bucketName, objectKey, versionId);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Retrieves the access control settings of a bucket.
     *
     * This method can be performed by anonymous services, but can only succeed if the
     * bucket's existing ACL already allows write access by the anonymous user.
     * In general, you can only access the ACL of a bucket if the ACL already in place
     * for that bucket (in S3) allows you to do so. See
     * <a href="http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?S3_ACLs.html">
     * the S3 documentation on ACLs</a> for more details on access to ACLs.
     *
     * @param bucket
     * the bucket whose access control settings will be returned.
     * This must be a valid S3Bucket object that is non-null and contains a name.
     * @return
     * the ACL settings of the bucket.
     * @throws S3ServiceException
     */
    public AccessControlList getBucketAcl(S3Bucket bucket) throws S3ServiceException {
        try {
            assertValidBucket(bucket, "Get Bucket Access Control List");
            return getBucketAclImpl(bucket.getName());
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Retrieves the location of a bucket. Only the owner of a bucket may retrieve its location.
     *
     * @param bucketName
     * the name of the bucket whose location will be returned.
     * @return
     * a string representing the location of the bucket, such as "EU" for a bucket
     * located in Europe or null for a bucket in the default US location.
     * @throws S3ServiceException
     */
    public String getBucketLocation(String bucketName) throws S3ServiceException {
        return getBucketLocationImpl(bucketName);
    }

    /**
     * Retrieves the logging status settings of a bucket. Only the owner of a bucket may retrieve
     * its logging status.
     *
     * @param bucketName
     * the name of the bucket whose logging status settings will be returned.
     * @return
     * the Logging Status settings of the bucket.
     * @throws S3ServiceException
     */
    public S3BucketLoggingStatus getBucketLoggingStatus(String bucketName) throws S3ServiceException {
        return getBucketLoggingStatusImpl(bucketName);
    }

    /**
     * Applies logging settings to a bucket, optionally modifying the ACL permissions for the
     * logging target bucket to ensure log files can be written to it. Only the owner of
     * a bucket may change its logging status.
     *
     * @param bucketName
     * the name of the bucket the logging settings will apply to.
     * @param status
     * the logging status settings to apply to the bucket.
     * @param updateTargetACLifRequired
     * if true, when logging is enabled the method will check the target bucket to ensure it has the
     * necessary ACL permissions set to allow logging (that is, WRITE and READ_ACP for the group
     * <tt>http://acs.amazonaws.com/groups/s3/LogDelivery</tt>). If the target bucket does not
     * have the correct permissions the bucket's ACL will be updated to have the correct
     * permissions. If this parameter is false, no ACL checks or updates will occur.
     *
     * @throws S3ServiceException
     */
    public void setBucketLoggingStatus(String bucketName, S3BucketLoggingStatus status,
        boolean updateTargetACLifRequired)
        throws S3ServiceException
    {
        try {
            if (status.isLoggingEnabled() && updateTargetACLifRequired) {
                // Check whether the target bucket has the ACL permissions necessary for logging.
                if (log.isDebugEnabled()) {
                    log.debug("Checking whether the target logging bucket '" +
                        status.getTargetBucketName() + "' has the appropriate ACL settings");
                }
                boolean isSetLoggingGroupWrite = false;
                boolean isSetLoggingGroupReadACP = false;
                String groupIdentifier = GroupGrantee.LOG_DELIVERY.getIdentifier();

                AccessControlList logBucketACL = getBucketAcl(status.getTargetBucketName());

                for (GrantAndPermission gap: logBucketACL.getGrantAndPermissions()) {
                    if (groupIdentifier.equals(gap.getGrantee().getIdentifier())) {
                        // Found a Group Grantee.
                        if (gap.getPermission().equals(Permission.PERMISSION_WRITE)) {
                            isSetLoggingGroupWrite = true;
                            if (log.isDebugEnabled()) {
                                log.debug("Target bucket '" + status.getTargetBucketName() + "' has ACL "
                                        + "permission " + Permission.PERMISSION_WRITE + " for group " +
                                        groupIdentifier);
                            }
                        } else if (gap.getPermission().equals(Permission.PERMISSION_READ_ACP)) {
                            isSetLoggingGroupReadACP = true;
                            if (log.isDebugEnabled()) {
                                log.debug("Target bucket '" + status.getTargetBucketName() + "' has ACL "
                                    + "permission " + Permission.PERMISSION_READ_ACP + " for group " +
                                    groupIdentifier);
                            }
                        }
                    }
                }

                // Update target bucket's ACL if necessary.
                if (!isSetLoggingGroupWrite || !isSetLoggingGroupReadACP) {
                    if (log.isWarnEnabled()) {
                        log.warn("Target logging bucket '" + status.getTargetBucketName()
                            + "' does not have the necessary ACL settings, updating ACL now");
                    }

                    logBucketACL.grantPermission(GroupGrantee.LOG_DELIVERY, Permission.PERMISSION_WRITE);
                    logBucketACL.grantPermission(GroupGrantee.LOG_DELIVERY, Permission.PERMISSION_READ_ACP);
                    putBucketAcl(status.getTargetBucketName(), logBucketACL);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Target logging bucket '" + status.getTargetBucketName()
                            + "' has the necessary ACL settings");
                    }
                }
            }

            setBucketLoggingStatusImpl(bucketName, status);
        } catch (ServiceException se) {
            throw new S3ServiceException(se);
        }
    }

    /**
     * Apply a JSON access control policy document to a bucket.
     *
     * @param bucketName
     * @param policyDocument
     * @throws S3ServiceException
     */
    public void setBucketPolicy(String bucketName, String policyDocument)
        throws S3ServiceException
    {
        setBucketPolicyImpl(bucketName, policyDocument);
    }

    /**
     * Retrieve the JSON access control policy document for a bucket,
     * or null if the bucket does not have a policy.
     *
     * @param bucketName
     * @return
     * JSON policy document for bucket, or null if the bucket has no policy.
     * @throws S3ServiceException
     */
    public String getBucketPolicy(String bucketName)
        throws S3ServiceException
    {
        try {
            return getBucketPolicyImpl(bucketName);
        } catch (S3ServiceException e) {
            if (e.getResponseCode() == 404) {
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Delete the acces control policy document for a bucket.
     *
     * @param bucketName
     * @throws S3ServiceException
     */
    public void deleteBucketPolicy(String bucketName)
        throws S3ServiceException
    {
        deleteBucketPolicyImpl(bucketName);
    }

    /**
     * Return true if the given bucket is configured as a
     * <a href="http://docs.amazonwebservices.com/AmazonS3/latest/RequesterPaysBuckets.html">
     * Requester Pays</a> bucket, in which case the requester must supply their own AWS
     * credentials when accessing objects in the bucket, and will be responsible for request
     * and data transfer fees.
     *
     * @param bucketName
     * the name of the bucket whose request payment configuration setting will be returned.
     *
     * @return
     * true if the given bucket is configured to be Requester Pays, false if it is has the
     * default Owner pays configuration.
     *
     * @throws S3ServiceException
     */
    public boolean isRequesterPaysBucket(String bucketName) throws S3ServiceException
    {
        return isRequesterPaysBucketImpl(bucketName);
    }

    /**
     * Applies <a href="http://docs.amazonwebservices.com/AmazonS3/latest/RequesterPaysBuckets.html">
     * request payment configuration</a> settings to a bucket, setting the bucket to
     * be either Requester Pays or Bucket Owner pays. Only the owner of a bucket may change
     * its request payment status.
     *
     * @param bucketName
     * the name of the bucket to which the request payment configuration settings will be applied.
     * @param requesterPays
     * if true, the bucket will be configured to be Requester Pays. If false, the bucket will
     * be configured to be Owner pays (the default configuration setting).
     *
     * @throws S3ServiceException
     */
    public void setRequesterPaysBucket(String bucketName, boolean requesterPays)
        throws S3ServiceException
    {
        setRequesterPaysBucketImpl(bucketName, requesterPays);
    }

    /**
     * Convenience method that uploads a file-based object to a storage service using
     * the regular {@link #putObject(String, StorageObject)} mechanism, or as a
     * multipart upload if the object's file data is larger than the given maximum
     * part size parameter.
     *
     * If a multipart upload is performed this method will perform all the necessary
     * steps, including:
     * <ol>
     * <li>Start a new multipart upload process, based on the object's key name,
     *     metadata, ACL etc.</li>
     * <li>Poll the service for a little while to ensure the just-started upload
     *     is actually available for use before proceeding -- this can take some
     *     time, we give up after 5 seconds (with 1 lookup attempt per second)</li>
     * <li>Divide the object's underlying file into parts with size <= the given
     *     maximum part size</li>
     * <li>Upload each of these parts in turn, with part numbers 1..n</li>
     * <li>Complete the upload once all the parts have been uploaded, or...</li>
     * <li>If there was a failure uploading parts or completing the upload, attempt
     *     to clean up by calling {@link #multipartAbortUpload(MultipartUpload)}
     *     then throw the original exception</li>
     * </ol>
     * This means that any multipart upload will involve sending around 2 + n separate
     * HTTP requests, where n is ceil(objectDataSize / maxPartSize).
     *
     * @param bucketName
     * the name of the bucket in which the object will be stored.
     * @param object
     * a file-based object containing all information that will be written to the service.
     * If the object provided is not file-based -- i.e. it returns null from
     * {@link StorageObject#getDataInputFile()} -- an exception will be thrown immediately.
     * @param maxPartSize
     * the maximum size in bytes for any single upload part. If the given object's data is
     * less than this value it will be uploaded using a regular PUT. If the object has more
     * data than this value it will be uploaded using a multipart upload.
     * The maximum part size value should be <= 5 GB and >= 5 MB.
     *
     * @throws ServiceException
     */
    public void putObjectMaybeAsMultipart(String bucketName, StorageObject object,
        long maxPartSize) throws ServiceException
    {
        // Only file-based objects are supported
        if (object.getDataInputFile() == null) {
            throw new ServiceException(
                "multipartUpload method only supports file-based objects");
        }

        MultipartUtils multipartUtils = new MultipartUtils(maxPartSize);

        // Upload object normally if it doesn't exceed maxPartSize
        if (!multipartUtils.isFileLargerThanMaxPartSize(object.getDataInputFile())) {
            log.debug("Performing normal PUT upload for object with data <= " + maxPartSize);
            putObject(bucketName, object);
        } else {
            log.debug("Performing multipart upload for object with data > " + maxPartSize);

            // Start upload
            MultipartUpload upload = multipartStartUpload(bucketName, object.getKey(),
                object.getMetadataMap(), object.getAcl(), object.getStorageClass());

            // Ensure upload is present on service-side, might take a little time
            boolean foundUpload = false;
            int maxTries = 5; // Allow up to 5 lookups for upload before we give up
            int tries = 0;
            do {
                try {
                    multipartListParts(upload);
                    foundUpload = true;
                } catch (S3ServiceException e) {
                    if ("NoSuchUpload".equals(e.getErrorCode())) {
                        tries++;
                        try {
                            Thread.sleep(1000); // Wait for a second
                        } catch (InterruptedException ie) {
                            tries = maxTries;
                        }
                    } else {
                        // Bail out if we get a (relatively) unexpected exception
                        throw e;
                    }
                }
            } while (!foundUpload && tries < maxTries);

            if (!foundUpload) {
                throw new ServiceException(
                    "Multipart upload was started but unavailable for use after "
                    + tries + " attempts, giving up");
            }

            // Will attempt to delete multipart upload upon failure.
            try {
                List<S3Object> partObjects = multipartUtils.splitFileIntoObjectsByMaxPartSize(
                    object.getKey(), object.getDataInputFile());

                List<MultipartPart> parts = new ArrayList<MultipartPart>();
                int partNumber = 1;
                for (S3Object partObject: partObjects) {
                    MultipartPart part = multipartUploadPart(upload, partNumber, partObject);
                    parts.add(part);
                    partNumber++;
                }

                multipartCompleteUpload(upload, parts);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // If upload fails for any reason after the upload was started, try to clean up.
                log.warn("Multipart upload failed, attempting clean-up by aborting upload", e);
                try {
                    multipartAbortUpload(upload);
                } catch (S3ServiceException e2) {
                    log.warn("Multipart upload failed and could not clean-up by aborting upload", e2);
                }
                // Throw original failure exception
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                } else {
                    throw new ServiceException("Multipart upload failed", e);
                }
            }
        }
    }

    /**
     * Start a multipart upload process for a given object; must be done before
     * individual parts can be uploaded.
     *
     * @param bucketName
     * the name of the bucket in which the object will be stored.
     * @param objectKey
     * the key name of the object.
     * @param metadata
     * metadata to apply to the completed object, may be null.
     * @return
     * object representing this multipart upload.
     * @throws S3ServiceException
     */
    public MultipartUpload multipartStartUpload(String bucketName, String objectKey,
        Map<String, Object> metadata) throws S3ServiceException
    {
        return multipartStartUpload(bucketName, objectKey, metadata, null, null);
    }

    /**
     * Start a multipart upload process for a given object; must be done before
     * individual parts can be uploaded.
     *
     * @param bucketName
     * the name of the bucket in which the object will be stored.
     * @param objectKey
     * the key name of the object.
     * @param metadata
     * metadata to apply to the completed object, may be null.
     * @param acl
     * ACL to apply to the completed upload, may be null.
     * @param storageClass
     * storage class to apply to the completed upload, may be null.
     * @return
     * object representing this multipart upload.
     * @throws S3ServiceException
     */
    public MultipartUpload multipartStartUpload(String bucketName, String objectKey,
        Map<String, Object> metadata, AccessControlList acl, String storageClass)
        throws S3ServiceException
    {
        return multipartStartUploadImpl(bucketName, objectKey, metadata, acl, storageClass);
    }

    /**
     * Start a multipart upload process for a given object; must be done before
     * individual parts can be uploaded.
     *
     * @param bucketName
     * the name of the bucket in which the object will be stored.
     * @param object
     * object containing details to apply to the completed object, including:
     * key name, metadata, ACL, storage class
     * @return
     * object representing this multipart upload.
     * @throws S3ServiceException
     */
    public MultipartUpload multipartStartUpload(String bucketName, S3Object object)
        throws S3ServiceException
    {
        return multipartStartUploadImpl(bucketName, object.getKey(),
            object.getMetadataMap(), object.getAcl(), object.getStorageClass());
    }

    /**
     * Abort the given multipart upload process. Also deletes any parts that may
     * have already been uploaded.
     *
     * @param upload
     * the multipart upload to abort.
     * @throws S3ServiceException
     */
    public void multipartAbortUpload(MultipartUpload upload) throws S3ServiceException
    {
        multipartAbortUploadImpl(upload.getUploadId(), upload.getBucketName(), upload.getObjectKey());
    }

    /**
     * List the multipart uploads that have been started within a bucket and
     * have not yet been completed or aborted.
     *
     * @param bucketName
     * the bucket whose multipart uploads will be listed.
     * @return
     * a list of incomplete multipart uploads.
     * @throws S3ServiceException
     */
    public List<MultipartUpload> multipartListUploads(String bucketName)
        throws S3ServiceException
    {
        return multipartListUploads(bucketName, null, null, null);
    }

    /**
     * List a subset of the multipart uploads that have been started within
     * a bucket and have not yet been completed or aborted.
     *
     * @param bucketName
     * the bucket whose multipart uploads will be listed.
     * @param nextKeyMarker
     * marker indicating where this list subset should start by key name.
     * @param nextUploadIdMarker
     * marker indicating where this list subset should start by upload ID.
     * @param maxUploads
     * maximum number of uploads to list in a subset.
     * @return
     * a list of incomplete multipart uploads.
     * @throws S3ServiceException
     */
    public List<MultipartUpload> multipartListUploads(String bucketName,
        String nextKeyMarker, String nextUploadIdMarker, Integer maxUploads)
        throws S3ServiceException
    {
        return multipartListUploadsImpl(
            bucketName, nextKeyMarker, nextUploadIdMarker, maxUploads);
    }

    /**
     * List the parts that have been uploaded for a given multipart upload.
     *
     * @param upload
     * the multipart upload whose parts will be listed.
     * @return
     * a list of multipart parts that have been successfully uploaded.
     * @throws S3ServiceException
     */
    public List<MultipartPart> multipartListParts(MultipartUpload upload)
        throws S3ServiceException
    {
        return multipartListPartsImpl(upload.getUploadId(),
            upload.getBucketName(), upload.getObjectKey());
    }

    /**
     * Complete a multipart upload by combining all the given parts into
     * the final object.
     *
     * @param upload
     * the multipart upload whose parts will be completed.
     * @param parts
     * the parts comprising the final object.
     * @return
     * information about the completion operation.
     * @throws S3ServiceException
     */
    public MultipartCompleted multipartCompleteUpload(MultipartUpload upload,
        List<MultipartPart> parts) throws S3ServiceException
    {
        return multipartCompleteUploadImpl(upload.getUploadId(), upload.getBucketName(),
            upload.getObjectKey(), parts);
    }

    /**
     * Convenience method to complete a multipart upload by automatically finding
     * its parts. This method does more work than the lower-level
     * {@link #multipartCompleteUpload(MultipartUpload, List)} API operation, but
     * relieves the caller of having to keep track of all the parts uploaded
     * for a multipart upload.
     *
     * @param upload
     * the multipart upload whose parts will be completed.
     * @return
     * information about the completion operation.
     * @throws S3ServiceException
     */
    public MultipartCompleted multipartCompleteUpload(MultipartUpload upload) throws S3ServiceException
    {
        List<MultipartPart> parts = multipartListParts(upload);
        return multipartCompleteUploadImpl(upload.getUploadId(), upload.getBucketName(),
            upload.getObjectKey(), parts);
    }

    /**
     * Upload an individual part that will comprise a piece of a multipart upload object.
     *
     * @param upload
     * the multipart upload to which this part will be added.
     * @param partNumber
     * the part's number; must be between 1 and 10,000 and must uniquely identify a given
     * part and represent its order compared to all other parts. Part numbers need not
     * be sequential.
     * @param object
     * an object containing a input stream with data that will be sent to the storage service.
     * @return
     * information about the uploaded part, retain this information to eventually complete
     * the object with {@link #multipartCompleteUpload(MultipartUpload, List)}.
     * @throws S3ServiceException
     */
    public MultipartPart multipartUploadPart(MultipartUpload upload, Integer partNumber,
        S3Object object) throws S3ServiceException
    {
        try {
            MultipartPart part = multipartUploadPartImpl(upload.getUploadId(),
                upload.getBucketName(),  partNumber, object);
            upload.addMultipartPartToUploadedList(part);
            return part;
        } catch (S3ServiceException e) {
            throw e;
        }
    }

    /**
     * Apply a website configuration to a bucket.
     *
     * @param bucketName
     * bucket to which the website configuration will be applied.
     * @param config
     * the website configuration details.
     * @throws S3ServiceException
     */
    public void setWebsiteConfig(String bucketName, WebsiteConfig config)
        throws S3ServiceException
    {
        setWebsiteConfigImpl(bucketName, config);
    }

    /**
     * @param bucketName
     * a bucket with a website configuration.
     * @return
     * the website configuration details.
     * @throws S3ServiceException
     */
    public WebsiteConfig getWebsiteConfig(String bucketName) throws S3ServiceException {
        return getWebsiteConfigImpl(bucketName);
    }

    /**
     * Delete a bucket's website configuration; removes the effect of any
     * previously-applied configuration.
     *
     * @param bucketName
     * a bucket with a website configuration.
     * @throws S3ServiceException
     */
    public void deleteWebsiteConfig(String bucketName) throws S3ServiceException {
        deleteWebsiteConfigImpl(bucketName);
    }

    /**
     * Apply a notification configuration to a bucket.
     *
     * @param bucketName
     * the bucket to which the notification configuration will be applied.
     * @param config
     * the notification configuration to apply.
     * @throws S3ServiceException
     */
    public void setNotificationConfig(String bucketName, NotificationConfig config)
        throws S3ServiceException
    {
        setNotificationConfigImpl(bucketName, config);
    }

    /**
     * @param bucketName
     * a bucket with a notification configuration.
     * @return
     * the notification configuration details.
     * @throws S3ServiceException
     */
    public NotificationConfig getNotificationConfig(String bucketName) throws S3ServiceException {
        return getNotificationConfigImpl(bucketName);
    }

    /**
     * Unset (delete) a bucket's notification configuration; removes the effect of any
     * previously-applied configuration.
     *
     * @param bucketName
     * a bucket with a notification configuration.
     * @throws S3ServiceException
     */
    public void unsetNotificationConfig(String bucketName) throws S3ServiceException {
        setNotificationConfigImpl(bucketName, new NotificationConfig());
    }

    ///////////////////////////////////////////////////////////
    // Abstract methods that must be implemented by S3 services
    ///////////////////////////////////////////////////////////

    protected abstract String getBucketLocationImpl(String bucketName)
        throws S3ServiceException;

    protected abstract S3BucketLoggingStatus getBucketLoggingStatusImpl(String bucketName)
        throws S3ServiceException;

    protected abstract void setBucketLoggingStatusImpl(String bucketName, S3BucketLoggingStatus status)
        throws S3ServiceException;

    protected abstract void setBucketPolicyImpl(String bucketName, String policyDocument)
        throws S3ServiceException;

    protected abstract String getBucketPolicyImpl(String bucketName) throws S3ServiceException;

    protected abstract void deleteBucketPolicyImpl(String bucketName) throws S3ServiceException;

    protected abstract void setRequesterPaysBucketImpl(String bucketName, boolean requesterPays)
        throws S3ServiceException;

    protected abstract boolean isRequesterPaysBucketImpl(String bucketName)
        throws S3ServiceException;

    protected abstract BaseVersionOrDeleteMarker[] listVersionedObjectsImpl(String bucketName,
        String prefix, String delimiter, String keyMarker, String versionMarker,
        long maxListingLength) throws S3ServiceException;

    /**
     * Lists version or delete markers in a versioned bucket, up to the maximum listing
     * length specified.
     *
     * <p>
     * <b>Implementation notes</b>
     * The implementation of this method returns only as many items as requested in the chunk
     * size. It is the responsibility of the caller to build a complete object listing from
     * multiple chunks, should this be necessary.
     * </p>
     *
     * @param bucketName
     * @param prefix
     * @param delimiter
     * only list objects with key names up to this delimiter, may be null.
     * @param maxListingLength
     * @param priorLastKey
     * @param completeListing
     * @throws S3ServiceException
     */
    protected abstract VersionOrDeleteMarkersChunk listVersionedObjectsChunkedImpl(
        String bucketName, String prefix, String delimiter, long maxListingLength,
        String priorLastKey, String priorLastVersion, boolean completeListing)
        throws S3ServiceException;

    protected abstract void updateBucketVersioningStatusImpl(String bucketName,
        boolean enabled, boolean multiFactorAuthDeleteEnabled,
        String multiFactorSerialNumber, String multiFactorAuthCode)
        throws S3ServiceException;

    protected abstract S3BucketVersioningStatus getBucketVersioningStatusImpl(
        String bucketName) throws S3ServiceException;

    protected abstract MultipartUpload multipartStartUploadImpl(String bucketName, String objectKey,
        Map<String, Object> metadata, AccessControlList acl, String storageClass) throws S3ServiceException;

    protected abstract void multipartAbortUploadImpl(String uploadId, String bucketName,
        String objectKey) throws S3ServiceException;

    protected abstract List<MultipartUpload> multipartListUploadsImpl(String bucketName,
        String nextKeyMarker, String nextUploadIdMarker, Integer maxUploads)
        throws S3ServiceException;

    protected abstract List<MultipartPart> multipartListPartsImpl(String uploadId,
        String bucketName, String objectKey) throws S3ServiceException;

    protected abstract MultipartCompleted multipartCompleteUploadImpl(String uploadId, String bucketName,
        String objectKey, List<MultipartPart> parts) throws S3ServiceException;

    protected abstract MultipartPart multipartUploadPartImpl(String uploadId, String bucketName,
        Integer partNumber, S3Object object) throws S3ServiceException;

    protected abstract void setWebsiteConfigImpl(String bucketName, WebsiteConfig config)
        throws S3ServiceException;

    protected abstract WebsiteConfig getWebsiteConfigImpl(String bucketName)
        throws S3ServiceException;

    protected abstract void deleteWebsiteConfigImpl(String bucketName)
        throws S3ServiceException;

    protected abstract void setNotificationConfigImpl(String bucketName, NotificationConfig config)
        throws S3ServiceException;

    protected abstract NotificationConfig getNotificationConfigImpl(String bucketName)
        throws S3ServiceException;

}

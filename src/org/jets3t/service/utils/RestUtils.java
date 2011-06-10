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
package org.jets3t.service.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.contrib.proxy.PluginProxyUtil;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.AWSRequestAuthorizer;
import org.jets3t.service.impl.rest.httpclient.HttpClientAndConnectionManager;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.UnrecoverableIOException;

/**
 * Utilities useful for REST/HTTP S3Service implementations.
 *
 * @author James Murty
 */
public class RestUtils {

    private static final Log log = LogFactory.getLog(RestUtils.class);

    /**
     * A list of HTTP-specific header names, that may be present in S3Objects as metadata but
     * which should be treated as plain HTTP headers during transmission (ie not converted into
     * S3 Object metadata items). All items in this list are in lower case.
     * <p>
     * This list includes the items:
     * <table>
     * <tr><th>Unchanged metadata names</th></tr>
     * <tr><td>content-type</td></tr>
     * <tr><td>content-md5</td></tr>
     * <tr><td>content-length</td></tr>
     * <tr><td>content-language</td></tr>
     * <tr><td>expires</td></tr>
     * <tr><td>cache-control</td></tr>
     * <tr><td>content-disposition</td></tr>
     * <tr><td>content-encoding</td></tr>
     * </table>
     */
    public static final List<String> HTTP_HEADER_METADATA_NAMES = Arrays.asList(new String[] {
        "content-type",
        "content-md5",
        "content-length",
        "content-language",
        "expires",
        "cache-control",
        "content-disposition",
        "content-encoding"
        });


    /**
     * Encodes a URL string, and ensures that spaces are encoded as "%20" instead of "+" to keep
     * fussy web browsers happier.
     *
     * @param path
     * @return
     * encoded URL.
     * @throws ServiceException
     */
    public static String encodeUrlString(String path) throws ServiceException {
        try {
            String encodedPath = URLEncoder.encode(path, Constants.DEFAULT_ENCODING);
            // Web browsers do not always handle '+' characters well, use the well-supported '%20' instead.
            encodedPath = encodedPath.replaceAll("\\+", "%20");
            // '@' character need not be URL encoded and Google Chrome balks on signed URLs if it is.
            encodedPath = encodedPath.replaceAll("%40", "@");
            return encodedPath;
        } catch (UnsupportedEncodingException uee) {
            throw new ServiceException("Unable to encode path: " + path, uee);
        }
    }

    /**
     * Encodes a URL string but leaves a delimiter string unencoded.
     * Spaces are encoded as "%20" instead of "+".
     *
     * @param path
     * @param delimiter
     * @return
     * encoded URL string.
     * @throws ServiceException
     */
    public static String encodeUrlPath(String path, String delimiter) throws ServiceException {
        StringBuffer result = new StringBuffer();
        String tokens[] = path.split(delimiter);
        for (int i = 0; i < tokens.length; i++) {
            result.append(encodeUrlString(tokens[i]));
            if (i < tokens.length - 1) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }

    /**
     * Calculate the canonical string for a REST/HTTP request to a storage service.
     *
     * When expires is non-null, it will be used instead of the Date header.
     * @throws UnsupportedEncodingException
     */
    public static String makeServiceCanonicalString(String method, String resource,
        Map<String, Object> headersMap, String expires, String headerPrefix,
        List<String> serviceResourceParameterNames) throws UnsupportedEncodingException
    {
        StringBuffer canonicalStringBuf = new StringBuffer();
        canonicalStringBuf.append(method + "\n");

        // Add all interesting headers to a list, then sort them.  "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        SortedMap<String, Object> interestingHeaders = new TreeMap<String, Object>();
        if (headersMap != null && headersMap.size() > 0) {
            for (Map.Entry<String, Object> entry: headersMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (key == null) {
                    continue;
                }
                String lk = key.toString().toLowerCase(Locale.getDefault());

                // Ignore any headers that are not particularly interesting.
                if (lk.equals("content-type") || lk.equals("content-md5") || lk.equals("date") ||
                    lk.startsWith(headerPrefix))
                {
                    interestingHeaders.put(lk, value);
                }
            }
        }

        // Remove default date timestamp if "x-amz-date" is set.
        if (interestingHeaders.containsKey(Constants.REST_METADATA_ALTERNATE_DATE)) {
            interestingHeaders.put("date", "");
        }

        // Use the expires value as the timestamp if it is available. This trumps both the default
        // "date" timestamp, and the "x-amz-date" header.
        if (expires != null) {
            interestingHeaders.put("date", expires);
        }

        // these headers require that we still put a new line in after them,
        // even if they don't exist.
        if (! interestingHeaders.containsKey("content-type")) {
            interestingHeaders.put("content-type", "");
        }
        if (! interestingHeaders.containsKey("content-md5")) {
            interestingHeaders.put("content-md5", "");
        }

        // Finally, add all the interesting headers (i.e.: all that start with x-amz- ;-))
        for (Map.Entry<String, Object> entry: interestingHeaders.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith(headerPrefix)) {
                canonicalStringBuf.append(key).append(':').append(value);
            } else {
                canonicalStringBuf.append(value);
            }
            canonicalStringBuf.append("\n");
        }

        // don't include the query parameters...
        int queryIndex = resource.indexOf('?');
        if (queryIndex == -1) {
            canonicalStringBuf.append(resource);
        } else {
            canonicalStringBuf.append(resource.substring(0, queryIndex));
        }

        // ...unless the parameter(s) are in the set of special params
        // that actually identify a service resource.
        if (queryIndex >= 0) {
            SortedMap<String, String> sortedResourceParams = new TreeMap<String, String>();

            // Parse parameters from resource string
            String query = resource.substring(queryIndex + 1);
            for (String paramPair: query.split("&")) {
                String[] paramNameValue = paramPair.split("=");
                String name = URLDecoder.decode(paramNameValue[0], "UTF-8");
                String value = null;
                if (paramNameValue.length > 1) {
                    value = URLDecoder.decode(paramNameValue[1], "UTF-8");
                }
                // Only include parameter (and its value if present) in canonical
                // string if it is a resource-identifying parameter
                if (serviceResourceParameterNames.contains(name)) {
                    sortedResourceParams.put(name, value);
                }
            }

            // Add resource parameters
            if (sortedResourceParams.size() > 0) {
                canonicalStringBuf.append("?");
            }
            boolean addedParam = false;
            for (Map.Entry<String, String> entry: sortedResourceParams.entrySet()) {
                if (addedParam) {
                    canonicalStringBuf.append("&");
                }
                canonicalStringBuf.append(entry.getKey());
                if (entry.getValue() != null) {
                    canonicalStringBuf.append("=" + entry.getValue());
                }
                addedParam = true;
            }
        }

        return canonicalStringBuf.toString();
    }

    /**
     * Initialises, or re-initialises, the underlying HttpConnectionManager and
     * HttpClient objects a service will use to communicate with an AWS service.
     * If proxy settings are specified in this service's {@link Jets3tProperties} object,
     * these settings will also be passed on to the underlying objects.
     *
     * @param hostConfig
     * Custom HTTP host configuration; e.g to register a custom Protocol Socket Factory.
     * This parameter may be null, in which case a default host configuration will be
     * used.
     */
    public static HttpClientAndConnectionManager initHttpConnection(final AWSRequestAuthorizer awsRequestAuthorizer,
        HostConfiguration hostConfig, Jets3tProperties jets3tProperties, String userAgentDescription,
        CredentialsProvider credentialsProvider)
    {
        // Configure HttpClient properties based on Jets3t Properties.
        HttpConnectionManagerParams connectionParams = new HttpConnectionManagerParams();
        connectionParams.setConnectionTimeout(jets3tProperties.
            getIntProperty("httpclient.connection-timeout-ms", 60000));
        connectionParams.setSoTimeout(jets3tProperties.
            getIntProperty("httpclient.socket-timeout-ms", 60000));
        connectionParams.setStaleCheckingEnabled(jets3tProperties.
            getBoolProperty("httpclient.stale-checking-enabled", true));

        // Set the maximum connections per host for the HTTP connection manager,
        // *and* also set the maximum number of total connections (new in 0.7.1).
        // The max connections per host setting is made the same value as the max
        // global connections if there is no per-host property.
        int maxConnections =
            jets3tProperties.getIntProperty("httpclient.max-connections", 20);
        int maxConnectionsPerHost =
            jets3tProperties.getIntProperty("httpclient.max-connections-per-host", 0);
        if (maxConnectionsPerHost == 0) {
            maxConnectionsPerHost = maxConnections;
        }

        connectionParams.setMaxConnectionsPerHost(
            HostConfiguration.ANY_HOST_CONFIGURATION, maxConnectionsPerHost);
        connectionParams.setMaxTotalConnections(maxConnections);

        // Connection properties to take advantage of S3 window scaling.
        if (jets3tProperties.containsKey("httpclient.socket-receive-buffer")) {
            connectionParams.setReceiveBufferSize(jets3tProperties.
                getIntProperty("httpclient.socket-receive-buffer", 0));
        }
        if (jets3tProperties.containsKey("httpclient.socket-send-buffer")) {
            connectionParams.setSendBufferSize(jets3tProperties.
                getIntProperty("httpclient.socket-send-buffer", 0));
        }

        connectionParams.setTcpNoDelay(true);

        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.setParams(connectionParams);

        // Set user agent string.
        HttpClientParams clientParams = new HttpClientParams();
        String userAgent = jets3tProperties.getStringProperty("httpclient.useragent", null);
        if (userAgent == null) {
            userAgent = ServiceUtils.getUserAgentDescription(userAgentDescription);
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting user agent string: " + userAgent);
        }
        clientParams.setParameter(HttpMethodParams.USER_AGENT, userAgent);

        clientParams.setParameter(HttpMethodParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        clientParams.setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);

        // Replace default error retry handler.
        final int retryMaxCount = jets3tProperties.getIntProperty("httpclient.retry-max", 5);

        clientParams.setParameter(HttpClientParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(retryMaxCount, false) {
            @Override
            public boolean retryMethod(HttpMethod httpMethod, IOException ioe, int executionCount) {
                if (super.retryMethod(httpMethod, ioe, executionCount)) {
                    if  (ioe instanceof UnrecoverableIOException) {
                        if (log.isDebugEnabled()) {
                            log.debug("Deliberate interruption, will not retry");
                        }
                        return false;
                    }

                    // Release underlying connection so we will get a new one (hopefully) when we retry.
                    httpMethod.releaseConnection();

                    if (log.isDebugEnabled()) {
                        log.debug("Retrying " + httpMethod.getName() + " request with path '"
                            + httpMethod.getPath() + "' - attempt " + executionCount
                            + " of " + retryMaxCount);
                    }
                    // Build the authorization string for the method.
                    try {
                        awsRequestAuthorizer.authorizeHttpRequest(httpMethod);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Unable to generate updated authorization string for retried request", e);
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        long connectionManagerTimeout = jets3tProperties.getLongProperty(
            "httpclient.connection-manager-timeout", 0);
        clientParams.setConnectionManagerTimeout(connectionManagerTimeout);

        HttpClient httpClient = new HttpClient(clientParams, connectionManager);
        httpClient.setHostConfiguration(hostConfig);

        if (credentialsProvider != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using credentials provider class: " + credentialsProvider.getClass().getName());
            }
            httpClient.getParams().setParameter(CredentialsProvider.PROVIDER, credentialsProvider);
            if (jets3tProperties.getBoolProperty("httpclient.authentication-preemptive", false)) {
                httpClient.getParams().setAuthenticationPreemptive(true);
            }
        }

        return new HttpClientAndConnectionManager(httpClient, connectionManager);
    }

    /**
     * Initialises this service's HTTP proxy by auto-detecting the proxy settings.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties) {
        initHttpProxy(httpClient, jets3tProperties, true, null, -1, null, null, null);
    }

    /**
     * Initialises this service's HTTP proxy by auto-detecting the proxy settings using the given endpoint.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties,
        String endpoint) {
        initHttpProxy(httpClient, jets3tProperties, true, null, -1, null, null, null, endpoint);
    }

    /**
     * Initialises this service's HTTP proxy with the given proxy settings.
     *
     * @param proxyHostAddress
     * @param proxyPort
     */
    public static void initHttpProxy(HttpClient httpClient, String proxyHostAddress,
        int proxyPort, Jets3tProperties jets3tProperties) {
        initHttpProxy(httpClient, jets3tProperties, false,
            proxyHostAddress, proxyPort, null, null, null);
    }

    /**
     * Initialises this service's HTTP proxy for authentication using the given
     * proxy settings.
     *
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     * if a proxy domain is provided, an {@link NTCredentials} credential provider
     * will be used. If the proxy domain is null, a
     * {@link UsernamePasswordCredentials} credentials provider will be used.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties,
        String proxyHostAddress, int proxyPort, String proxyUser,
        String proxyPassword, String proxyDomain)
    {
        initHttpProxy(httpClient, jets3tProperties, false,
            proxyHostAddress, proxyPort, proxyUser, proxyPassword, proxyDomain);
    }

    /**
     * @param httpClient
     * @param proxyAutodetect
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     */
    public static void initHttpProxy(HttpClient httpClient,
        Jets3tProperties jets3tProperties, boolean proxyAutodetect,
        String proxyHostAddress, int proxyPort, String proxyUser,
        String proxyPassword, String proxyDomain)
    {
        String s3Endpoint = jets3tProperties.getStringProperty(
                "s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
        initHttpProxy(httpClient, jets3tProperties, proxyAutodetect, proxyHostAddress, proxyPort,
            proxyUser, proxyPassword, proxyDomain, s3Endpoint);
    }

    /**
     * @param httpClient
     * @param proxyAutodetect
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     * @param endpoint
     */
    public static void initHttpProxy(HttpClient httpClient,
        Jets3tProperties jets3tProperties, boolean proxyAutodetect,
        String proxyHostAddress, int proxyPort, String proxyUser,
        String proxyPassword, String proxyDomain, String endpoint)
    {
        HostConfiguration hostConfig = httpClient.getHostConfiguration();

        // Use explicit proxy settings, if available.
        if (proxyHostAddress != null && proxyPort != -1) {
            if (log.isInfoEnabled()) {
                log.info("Using Proxy: " + proxyHostAddress + ":" + proxyPort);
            }
            hostConfig.setProxy(proxyHostAddress, proxyPort);

            if (proxyUser != null && !proxyUser.trim().equals("")) {
                if (proxyDomain != null) {
                    httpClient.getState().setProxyCredentials(
                        new AuthScope(proxyHostAddress, proxyPort),
                            new NTCredentials(proxyUser, proxyPassword, proxyHostAddress, proxyDomain));
                }
                else {
                    httpClient.getState().setProxyCredentials(
                        new AuthScope(proxyHostAddress, proxyPort),
                            new UsernamePasswordCredentials(proxyUser, proxyPassword));
                }
            }
        }
        // If no explicit settings are available, try autodetecting proxies (unless autodetect is disabled)
        else if (proxyAutodetect) {
            // Try to detect any proxy settings from applet.
            ProxyHost proxyHost = null;
            try {
                proxyHost = PluginProxyUtil.detectProxy(new URL("http://" + endpoint));
                if (proxyHost != null) {
                    if (log.isInfoEnabled()) {
                        log.info("Using Proxy: " + proxyHost.getHostName() + ":" + proxyHost.getPort());
                    }
                    hostConfig.setProxyHost(proxyHost);
                }
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to set proxy configuration", t);
                }
            }
        }
    }

    /**
     * Calculates a time offset value to reflect the time difference between your
     * computer's clock and the current time according to an AWS server, and
     * returns the calculated time difference.
     *
     * Ideally you should not rely on this method to overcome clock-related
     * disagreements between your computer and AWS. If you computer is set
     * to update its clock periodically and has the correct timezone setting
     * you should never have to resort to this work-around.
     */
    public static long getAWSTimeAdjustment() throws Exception {
        RestS3Service restService = new RestS3Service(null);
        HttpClient client = restService.getHttpClient();
        long timeOffset = 0;

        // Connect to an AWS server to obtain response headers.
        GetMethod getMethod = new GetMethod("http://aws.amazon.com/");
        int result = client.executeMethod(getMethod);

        if (result == 200) {
            Header dateHeader = getMethod.getResponseHeader("Date");
            // Retrieve the time according to AWS, based on the Date header
            Date awsTime = ServiceUtils.parseRfc822Date(dateHeader.getValue());

            // Calculate the difference between the current time according to AWS,
            // and the current time according to your computer's clock.
            Date localTime = new Date();
            timeOffset = awsTime.getTime() - localTime.getTime();

            if (log.isDebugEnabled()) {
                log.debug("Calculated time offset value of " + timeOffset +
                        " milliseconds between the local machine and an AWS server");
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Unable to calculate value of time offset between the "
                    + "local machine and AWS server");
            }
        }

        return timeOffset;
    }

    public static Map<String, String> convertHeadersToMap(Header[] headers) {
        Map<String, String> s3Headers = new HashMap<String, String>();
        for (Header header: headers) {
            s3Headers.put(header.getName(), header.getValue());
        }
        return s3Headers;
    }

}

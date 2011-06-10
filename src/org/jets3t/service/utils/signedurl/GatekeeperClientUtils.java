/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007 James Murty
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
package org.jets3t.service.utils.signedurl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.contrib.proxy.PluginProxyUtil;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;

/**
 * Utility class to handle common operations performed by Gatekeeper client applications.
 *
 * @author James Murty
 */
public class GatekeeperClientUtils {

    private HttpClient httpClientGatekeeper = null;

    private static final Log log = LogFactory.getLog(GatekeeperClientUtils.class);

    /**
     * Variable to store application exceptions, so that client failure information can be
     * included in the information provided to the Gatekeeper when uploads are retried.
     */
    private Exception priorFailureException = null;

    private String gatekeeperUrl = null;
    private String userAgentDescription;
    private int maxRetryCount;
    private int connectionTimeout;
    private CredentialsProvider credentialsProvider = null;

    /**
     * @param gatekeeperUrl
     * @param userAgentDescription
     * @param maxRetryCount
     * @param connectionTimeoutMS
     * @param credentialsProvider
     */
    public GatekeeperClientUtils(String gatekeeperUrl, String userAgentDescription,
        int maxRetryCount, int connectionTimeoutMS, CredentialsProvider credentialsProvider)
    {
        this.gatekeeperUrl = gatekeeperUrl;
        this.userAgentDescription = userAgentDescription;
        this.maxRetryCount = maxRetryCount;
        this.connectionTimeout = connectionTimeoutMS;
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Prepares objects for HTTP communications with the Gatekeeper servlet.
     * @return
     */
    private HttpClient initHttpConnection() {
        // Set client parameters.
        HttpClientParams clientParams = new HttpClientParams();

        clientParams.setParameter(HttpMethodParams.USER_AGENT,
            ServiceUtils.getUserAgentDescription(userAgentDescription));

        // Replace default error retry handler.
        clientParams.setParameter(HttpClientParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
            public boolean retryMethod(HttpMethod httpMethod, IOException ioe, int executionCount) {
                if (executionCount > maxRetryCount) {
                    if (log.isErrorEnabled()) {
                        log.error("Retried connection " + executionCount
                            + " times, which exceeds the maximum retry count of " + maxRetryCount);
                    }
                    return false;
                }
                if (log.isWarnEnabled()) {
                    log.warn("Retrying request - attempt " + executionCount + " of " + maxRetryCount);
                }
                return true;
            }
        });


        // Set connection parameters.
        HttpConnectionManagerParams connectionParams = new HttpConnectionManagerParams();
        connectionParams.setConnectionTimeout(connectionTimeout);
        connectionParams.setSoTimeout(connectionTimeout);
        connectionParams.setStaleCheckingEnabled(false);

        HttpClient httpClient = new HttpClient(clientParams);
        httpClient.getHttpConnectionManager().setParams(connectionParams);

        // httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getParams().setParameter(CredentialsProvider.PROVIDER, credentialsProvider);

        return httpClient;
    }

    /**
     * Request permission from the Gatekeeper for a particular operation.
     *
     * @param operationType
     * @param bucketName
     * @param objects
     * @param applicationPropertiesMap
     * @throws HttpException
     * @throws Exception
     */
    public GatekeeperMessage requestActionThroughGatekeeper(String operationType, String bucketName,
            S3Object[] objects, Map applicationPropertiesMap)
        throws HttpException, Exception
    {
        /*
         *  Build Gatekeeper request.
         */
        GatekeeperMessage gatekeeperMessage = new GatekeeperMessage();
        gatekeeperMessage.addApplicationProperties(applicationPropertiesMap);
        gatekeeperMessage.addApplicationProperty(
                GatekeeperMessage.PROPERTY_CLIENT_VERSION_ID, userAgentDescription);

        // If a prior failure has occurred, add information about this failure.
        if (priorFailureException != null) {
            gatekeeperMessage.addApplicationProperty(GatekeeperMessage.PROPERTY_PRIOR_FAILURE_MESSAGE,
                priorFailureException.getMessage());
            // Now reset the prior failure variable.
            priorFailureException = null;
        }

        // Add all S3 objects as candiates for PUT signing.
        for (int i = 0; i < objects.length; i++) {
            SignatureRequest signatureRequest = new SignatureRequest(
                    operationType, objects[i].getKey());
            signatureRequest.setObjectMetadata(objects[i].getMetadataMap());
            signatureRequest.setBucketName(bucketName);

            gatekeeperMessage.addSignatureRequest(signatureRequest);
        }

        /*
         *  Build HttpClient POST message.
         */

        // Add all properties/parameters to credentials POST request.
        PostMethod postMethod = new PostMethod(gatekeeperUrl);
        Properties properties = gatekeeperMessage.encodeToProperties();
        Iterator propsIter = properties.entrySet().iterator();
        while (propsIter.hasNext()) {
            Map.Entry entry = (Map.Entry) propsIter.next();
            String fieldName = (String) entry.getKey();
            String fieldValue = (String) entry.getValue();
            postMethod.setParameter(fieldName, fieldValue);
        }

        // Create Http Client if necessary, and include User Agent information.
        if (httpClientGatekeeper == null) {
            httpClientGatekeeper = initHttpConnection();
        }

        // Try to detect any necessary proxy configurations.
        try {
            ProxyHost proxyHost = PluginProxyUtil.detectProxy(new URL(gatekeeperUrl));
            if (proxyHost != null) {
                HostConfiguration hostConfig = new HostConfiguration();
                hostConfig.setProxyHost(proxyHost);
                httpClientGatekeeper.setHostConfiguration(hostConfig);
            }
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("No proxy detected");
            }
        }

        // Perform Gateway request.
        if (log.isDebugEnabled()) {
            log.debug("Contacting Gatekeeper at: " + gatekeeperUrl);
        }
        try {
            int responseCode = httpClientGatekeeper.executeMethod(postMethod);
            String contentType = postMethod.getResponseHeader("Content-Type").getValue();
            if (responseCode == 200) {
                InputStream responseInputStream = null;

                Header encodingHeader = postMethod.getResponseHeader("Content-Encoding");
                if (encodingHeader != null && "gzip".equalsIgnoreCase(encodingHeader.getValue())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Inflating gzip-encoded response");
                    }
                    responseInputStream = new GZIPInputStream(postMethod.getResponseBodyAsStream());
                } else {
                    responseInputStream = postMethod.getResponseBodyAsStream();
                }

                if (responseInputStream == null) {
                    throw new IOException("No response input stream available from Gatekeeper");
                }

                Properties responseProperties = new Properties();
                try {
                    responseProperties.load(responseInputStream);
                } finally {
                    responseInputStream.close();
                }

                GatekeeperMessage gatekeeperResponseMessage =
                    GatekeeperMessage.decodeFromProperties(responseProperties);

                // Check for Gatekeeper Error Code in response.
                String gatekeeperErrorCode = gatekeeperResponseMessage.getApplicationProperties()
                    .getProperty(GatekeeperMessage.APP_PROPERTY_GATEKEEPER_ERROR_CODE);
                if (gatekeeperErrorCode != null) {
                    if (log.isWarnEnabled()) {
                        log.warn("Received Gatekeeper error code: " + gatekeeperErrorCode);
                    }
                }

                return gatekeeperResponseMessage;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("The Gatekeeper did not permit a request. Response code: "
                        + responseCode + ", Response content type: " + contentType);
                }
                throw new IOException("The Gatekeeper did not permit your request");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception("Gatekeeper did not respond", e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    /**
     * Parse the data in a set of SignatureRequest objects and build the corresponding
     * S3Objects represented by that data.
     *
     * @param srs
     * signature requests that represent S3 objects.
     * @return
     * objects reconstructed from the provided signature requests.
     */
    public S3Object[] buildS3ObjectsFromSignatureRequests(SignatureRequest[] srs) {
        S3Object[] objects = new S3Object[srs.length];
        for (int i = 0; i < srs.length; i++) {
            objects[i] = new S3Object(srs[i].getObjectKey());
            objects[i].addAllMetadata(srs[i].getObjectMetadata());
        }
        return objects;
    }

    public String getGatekeeperUrl() {
        return gatekeeperUrl;
    }

}

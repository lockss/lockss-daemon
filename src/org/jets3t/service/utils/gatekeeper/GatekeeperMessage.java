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
package org.jets3t.service.utils.gatekeeper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Represents a set of properties that will be sent to or received from a Gatekeeper service as
 * a message document. This class includes utility methods to generate and parse plain text
 * encodings of messages.
 * <p>
 * For more information about the Gatekeeper message format, please see:
 * <a href="http://www.jets3t.org/applications/gatekeeper-concepts.html">
 * Gatekeeper Concepts</a>
 *
 * @author James Murty
 */
public class GatekeeperMessage {
    private static final Log log = LogFactory.getLog(GatekeeperMessage.class);

    /**
     * All message property names are delimited with a vertical bar (<tt>|</tt>).
     */
    public static final String DELIM = "|";

    /**
     * The property name for message-specific transaction IDs: transactionId
     */
    public static final String PROPERTY_TRANSACTION_ID = "transactionId";

    /**
     * The property name for storing information about prior failures in the gatekeeper client application.
     */
    public static final String PROPERTY_PRIOR_FAILURE_MESSAGE = "priorFailureMessage";

    /**
     * The property name for storing information about a client application such as its version
     * number. This information can be useful to server-side components to confirm compatibility
     * with the client.
     */
    public static final String PROPERTY_CLIENT_VERSION_ID = "clientVersionId";

    /**
     * The property name for storing error codes a Gatekeeper can return to a client.
     * The error codes can be any string value.
     */
    public static final String APP_PROPERTY_GATEKEEPER_ERROR_CODE = "gatekeeperErrorCode";

    /**
     * A flag name used to indicate when an S3Object is a summary XML document, as generated
     * by the Uploader application.
     */
    public static final String SUMMARY_DOCUMENT_METADATA_FLAG = "jets3t-uploader-summary-doc";

    /**
     * A flag name that indicates the Gatekeeper servlet should perform a bucket listing -
     * for example as used by CockpitLite
     */
    public static final String LIST_OBJECTS_IN_BUCKET_FLAG = "list-objects-in-bucket";

    private Properties applicationProperties = new Properties();
    private Properties messageProperties = new Properties();
    private List signatureRequestList = new ArrayList();

    /**
     * Constructs a message with no properties.
     */
    public GatekeeperMessage() {
    }

    /**
     * Adds a Signature Request to the message, indicating a request that a particular
     * operation be allowed on a particular object.
     *
     * @param signatureRequest
     */
    public void addSignatureRequest(SignatureRequest signatureRequest) {
        signatureRequestList.add(signatureRequest);
    }

    /**
     * Adds multiple signature requests to the message.
     *
     * @param signatureRequests
     */
    public void addSignatureRequests(SignatureRequest[] signatureRequests) {
        for (int i = 0; i < signatureRequests.length; i++) {
            addSignatureRequest(signatureRequests[i]);
        }
    }

    /**
     * Returns the signature requests in a message. When this method is called on a request message,
     * this list will include only the requested operations. When this method is called on a
     * message that is a response from a Gatekeeper service, the resulting list will include the
     * signed URLs or reasons why requests were declined.
     *
     * @return
     * the set of signature requests in this message.
     */
    public SignatureRequest[] getSignatureRequests() {
        return (SignatureRequest[]) signatureRequestList
            .toArray(new SignatureRequest[signatureRequestList.size()]);
    }

    /**
     * Adds an application-specific property to the message.
     *
     * @param propertyName
     * @param propertyValue
     */
    public void addApplicationProperty(String propertyName, String propertyValue) {
        applicationProperties.put(propertyName, propertyValue);
    }

    /**
     * Adds a set of application-specific properties to the message.
     *
     * @param propertiesMap
     */
    public void addApplicationProperties(Map propertiesMap) {
        applicationProperties.putAll(propertiesMap);
    }

    /**
     * @return
     * the application-specific properties in this message.
     */
    public Properties getApplicationProperties() {
        return applicationProperties;
    }

    /**
     * Adds a message-specific property to the message.
     *
     * @param propertyName
     * @param propertyValue
     */
    public void addMessageProperty(String propertyName, String propertyValue) {
        messageProperties.put(propertyName, propertyValue);
    }

    /**
     * Adds a set of message-specific properties to the message.
     *
     * @param propertiesMap
     */
    public void addMessageProperties(Map propertiesMap) {
        messageProperties.putAll(propertiesMap);
    }

    /**
     * @return
     * the message-specific properties in this message.
     */
    public Properties getMessageProperties() {
        return messageProperties;
    }

    private void encodeProperty(Properties properties, String propertyName, Object value) {
        if (value != null) {
            if (value instanceof Date) {
                properties.put(propertyName, ServiceUtils.formatIso8601Date((Date)value));
            } else {
                properties.put(propertyName, value.toString());
            }
            if (log.isDebugEnabled()) {
                log.debug("Encoded property: " + propertyName + "=" + properties.getProperty(propertyName));
            }
        }
    }

    /**
     * Encodes a Gatekeeper message as a properties object, with all signature requests identified
     * with a unique zero-based index number.
     *
     * @return
     * all the properties of the message.
     */
    public Properties encodeToProperties() {
        if (log.isDebugEnabled()) {
            log.debug("Encoding GatekeeperMessage to properties");
        }

        Properties encodedProperties = new Properties();
        Iterator iter = null;

        String prefix = "application";
        iter = applicationProperties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            encodeProperty(encodedProperties, prefix + DELIM + key, value);
        }

        prefix = "message";
        iter = messageProperties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            encodeProperty(encodedProperties, prefix + DELIM + key, value);
        }

        prefix = "request";
        SignatureRequest[] requests = getSignatureRequests();
        for (int i = 0; i < requests.length; i++) {
            SignatureRequest request = requests[i];
            String propertyPrefix = prefix + DELIM + i + DELIM;

            encodeProperty(encodedProperties, propertyPrefix + "signatureType", request.getSignatureType());
            encodeProperty(encodedProperties, propertyPrefix + "objectKey", request.getObjectKey());
            encodeProperty(encodedProperties, propertyPrefix + "bucketName", request.getBucketName());
            encodeProperty(encodedProperties, propertyPrefix + "signedUrl", request.getSignedUrl());
            encodeProperty(encodedProperties, propertyPrefix + "declineReason", request.getDeclineReason());

            propertyPrefix += "metadata" + DELIM;
            Map metadata = request.getObjectMetadata();
            iter = metadata.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String metadataName = (String) entry.getKey();
                Object metadataValue = entry.getValue();
                encodeProperty(encodedProperties, propertyPrefix + metadataName, metadataValue);
            }
        }

        return encodedProperties;
    }

    /**
     * Decodes (parses) a Gatekeeper message from the given properties. Any properties that are
     * not part of the message format are ignored.
     *
     * @param postProperties
     *
     * @return
     * a Gatekeeper message object representing the contents of the properties.
     */
    public static GatekeeperMessage decodeFromProperties(Map postProperties) {
        if (log.isDebugEnabled()) {
            log.debug("Decoding GatekeeperMessage from properties");
        }

        GatekeeperMessage gatekeeperMessage = new GatekeeperMessage();

        Map signatureRequestMap = new HashMap();

        Iterator propsIter = postProperties.entrySet().iterator();
        while (propsIter.hasNext()) {
            Map.Entry entry = (Map.Entry) propsIter.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();

            String propertyValue = null;
            if (value instanceof String[]) {
                propertyValue = ((String[]) value)[0];
            } else {
                propertyValue = (String) value;
            }

            if (key.startsWith("application")) {
                String propertyName = key.substring(key.lastIndexOf(DELIM) + 1);
                gatekeeperMessage.addApplicationProperty(propertyName, propertyValue);
            } else if (key.startsWith("message")) {
                String propertyName = key.substring(key.lastIndexOf(DELIM) + 1);
                gatekeeperMessage.addMessageProperty(propertyName, propertyValue);
            } else if (key.startsWith("request")) {
                StringTokenizer st = new StringTokenizer(key, DELIM);
                st.nextToken(); // Consume request prefix
                String objectIndexStr = st.nextToken();

                boolean isMetadata = false;
                String propertyName = st.nextToken();
                if (st.hasMoreTokens()) {
                    isMetadata = true;
                    propertyName = st.nextToken();
                }

                Integer objectIndex = Integer.valueOf(objectIndexStr);
                SignatureRequest request = null;

                if (signatureRequestMap.containsKey(objectIndex)) {
                    request = (SignatureRequest) signatureRequestMap.get(objectIndex);
                } else {
                    request = new SignatureRequest();
                    signatureRequestMap.put(objectIndex, request);
                }

                if (isMetadata) {
                    request.addObjectMetadata(propertyName, propertyValue);
                } else {
                    if ("signatureType".equals(propertyName)) {
                        request.setSignatureType(propertyValue);
                    } else if ("objectKey".equals(propertyName)) {
                        request.setObjectKey(propertyValue);
                    } else if ("bucketName".equals(propertyName)) {
                        request.setBucketName(propertyValue);
                    } else if ("signedUrl".equals(propertyName)) {
                        request.signRequest(propertyValue);
                    } else if ("declineReason".equals(propertyName)) {
                        request.declineRequest(propertyValue);
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Ignoring unrecognised SignatureRequest property: " + propertyName);
                        }
                    }
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Ignoring unrecognised property name: " + key);
                }
            }
        }

        for (int i = 0; i < signatureRequestMap.size(); i++) {
            Integer objectIndex = new Integer(i);
            SignatureRequest request = (SignatureRequest) signatureRequestMap.get(objectIndex);
            gatekeeperMessage.addSignatureRequest(request);
        }

        return gatekeeperMessage;
    }


//    public static void main(String[] args) {
//        SignatureRequest requests[] = new SignatureRequest[12];
//        for (int i = 0; i < requests.length; i++) {
//            requests[i] = new SignatureRequest(SignatureRequest.SIGNATURE_TYPE_PUT, "Request " + i);
//            requests[i].addObjectMetadata("object-index", String.valueOf(i));
//        }
//
//        GatekeeperMessage request = new GatekeeperMessage();
//        request.addSignatureRequests(requests);
//        request.addMessageProperty("id", "123");
//        request.addMessageProperty("date", (new Date()).toString());
//        request.addApplicationProperty("username", "jmurty");
//
//        System.err.println("=== Original WRITE");
//        Properties properties = request.encodeToProperties();
//
//        GatekeeperMessage response = GatekeeperMessage.decodeFromProperties(properties);
//
//        System.err.println("=== Second WRITE");
//        response.encodeToProperties();
//    }

}

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
package org.jets3t.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.jets3t.service.mx.MxDelegate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Exception for use by {@link StorageService} and related utilities.
 * This exception can hold useful additional information about errors that occur
 * when communicating with a service.
 *
 * @author James Murty
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = -757626557833455141L;

    private String xmlMessage = null;

    // Fields from error messages.
    private String errorCode = null;
    private String errorMessage = null;
    private String errorRequestId = null;
    private String errorHostId = null;

    // Map<String, String> - name => value pairs of response headers.
    private Map<String, String> responseHeaders = null;

    private int responseCode = -1;
    private String responseStatus = null;
    private String responseDate = null;
    private String requestVerb = null;
    private String requestPath = null;
    private String requestHost = null;

    /**
     * Create a service exception that includes the XML error document returned by service.
     *
     * @param message
     * @param xmlMessage
     */
    public ServiceException(String message, String xmlMessage) {
        this(message, xmlMessage, null);
    }

    /**
     * Create a service exception that includes a specific message, an optional XML error
     * document returned by service, and an optional underlying cause exception.
     *
     * @param message
     * @param xmlMessage
     * @param cause
     */
    public ServiceException(String message, String xmlMessage, Throwable cause) {
        super(message, cause);
        if (xmlMessage != null) {
            parseXmlMessage(xmlMessage);
        }
        MxDelegate.getInstance().registerS3ServiceExceptionEvent(getErrorCode());
    }

    /**
     * Create a service exception without any useful information.
     */
    public ServiceException() {
        super();
    }

    /**
     * Create a service exception that includes a specific message and an
     * optional underlying cause exception.
     *
     * @param message
     * @param cause
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a service exception that includes a specific message.
     *
     * @param message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Create a service exception that includes an underlying cause exception.
     *
     * @param cause
     */
    public ServiceException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        String myString = super.toString();

        // Add request-specific information, if it's available.
        if (requestVerb != null) {
            myString +=
                " " + requestVerb
                + " '" + requestPath + "'"
                + (requestHost != null ? " on Host '" + requestHost + "'" : "")
                + (responseDate != null ? " @ '" + responseDate + "'" : "");
        }
        if (responseCode != -1) {
            myString +=
                " -- ResponseCode: " + responseCode
                + ", ResponseStatus: " + responseStatus;
        }
        if (isParsedFromXmlMessage()) {
            myString += ", XML Error Message: " + xmlMessage;
        }  else {
            if (errorRequestId != null) {
                myString += ", RequestId: " + errorRequestId
                    + ", HostId: " + errorHostId;
            }
        }
        return myString;
    }

    private String findXmlElementText(String xmlMessage, String elementName) {
        Pattern pattern = Pattern.compile(".*<" + elementName + ">(.*)</" + elementName + ">.*");
        Matcher matcher = pattern.matcher(xmlMessage);
        if (matcher.matches() && matcher.groupCount() == 1) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void parseXmlMessage(String xmlMessage) {
        xmlMessage = xmlMessage.replaceAll("\n", "");
        this.xmlMessage = xmlMessage;

        this.errorCode = findXmlElementText(xmlMessage, "Code");
        this.errorMessage = findXmlElementText(xmlMessage, "Message");
        this.errorRequestId = findXmlElementText(xmlMessage, "RequestId");
        this.errorHostId = findXmlElementText(xmlMessage, "HostId");

        // Add Details element present in some Google Storage error
        // messages to Message field.
        String errorDetails = findXmlElementText(xmlMessage, "Details");
        if (errorDetails != null && errorDetails.length() > 0) {
            this.errorMessage += " " + errorDetails;
        }
    }

    /**
     * @return The service-specific Error Code returned by the service, if a response is available.
     * For example "AccessDenied", "InternalError"
     * Null otherwise.
     */
    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Set the exception's error code; for internal use only.
     * @param code
     */
    public void setErrorCode(String code) {
        this.errorCode = code;
    }

    /**
     * @return The service-specific Error Message returned by the service, if a response is available.
     * For example: "Access Denied", "We encountered an internal error. Please try again."
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Set the exception's error message; for internal use only.
     * @param message
     */
    public void setErrorMessage(String message) {
        this.errorMessage= message;
    }

    /**
     * @return The Error Host ID returned by the service, if a response is available.
     * Null otherwise.
     */
    public String getErrorHostId() {
        return errorHostId;
    }

    /**
     * Set the exception's host ID; for internal use only.
     * @param hostId
     */
    public void setErrorHostId(String hostId) {
        this.errorHostId = hostId;
    }

    /**
     * @return The Error Request ID returned by the service, if a response is available.
     * Null otherwise.
     */
    public String getErrorRequestId() {
        return errorRequestId;
    }

    /**
     * Set the exception's request ID; for internal use only.
     * @param requestId
     */
    public void setErrorRequestId(String requestId) {
        this.errorRequestId = requestId;
    }

    /**
     * @return The XML Error message returned by the service, if a response is available.
     * Null otherwise.
     */
    public String getXmlMessage() {
        return xmlMessage;
    }

    /**
     * @return
     * an XML error message returned by the services as an <code>XMLBuilder</code>
     * object that allows for simple XPath querying via {@link XMLBuilder#xpathFind(String)},
     * or null if no XML error document is available.
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public XMLBuilder getXmlMessageAsBuilder()
        throws IOException, ParserConfigurationException, SAXException
    {
        if (this.xmlMessage == null) {
            return null;
        }
        XMLBuilder builder = XMLBuilder.parse(
            new InputSource(new StringReader(this.xmlMessage)));
        return builder;
    }

    /**
     * @return
     * true if this exception contains information from an XML error
     * document returned by a service, e.g. with error code details.
     */
    public boolean isParsedFromXmlMessage() {
        return (xmlMessage != null);
    }

    /**
     * @return The HTTP Response Code returned by the service, if an HTTP response is available.
     * For example: 401, 404, 500
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Set the exception's HTTP response code; for internal use only.
     * @param responseCode
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return
     * The HTTP Status message returned by the service, if an HTTP response is available.
     * For example: "Forbidden", "Not Found", "Internal Server Error"
     */
    public String getResponseStatus() {
        return responseStatus;
    }

    /**
     * Set the exception's HTTP response status; for internal use only.
     * @param responseStatus
     */
    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * @return
     * The exception's HTTP response date, if any.
     */
    public String getResponseDate() {
        return responseDate;
    }

    /**
     * Set the exception's HTTP response date; for internal use only.
     * @param responseDate
     */
    public void setResponseDate(String responseDate) {
        this.responseDate = responseDate;
    }

    /**
     * @return The HTTP Verb used in the request, if available.
     * For example: "GET", "PUT", "DELETE"
     */
    public String getRequestVerb() {
        return requestVerb;
    }

    /**
     * Set the exception's HTTP request verb; for internal use only.
     * @param requestVerb
     */
    public void setRequestVerb(String requestVerb) {
        this.requestVerb = requestVerb;
    }

    /**
     * @return
     * the exception's HTTP request path; if any.
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Set the exception's HTTP request path; for internal use only.
     * @param requestPath
     */
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    /**
     * @return
     * the exception's HTTP request hostname; if any.
     */
    public String getRequestHost() {
        return requestHost;
    }

    /**
     * Set the exception's HTTP request hostname; for internal use only.
     * @param requestHost
     */
    public void setRequestHost(String requestHost) {
        this.requestHost = requestHost;
    }

    /**
     * Allow the Request and Host Id fields to be populated in situations where
     * this information is not available from an XML response error document.
     * If there is no XML error response document, the RequestId and HostId will
     * generally be available as the HTTP response headers
     * <code>x-amz-request-id</code> and <code>x-amz-id-2</code> respectively.
     *
     * @param errorRequestId
     * @param errorHostId
     */
    public void setRequestAndHostIds(String errorRequestId, String errorHostId) {
        this.errorRequestId = errorRequestId;
        this.errorHostId = errorHostId;
    }

    /**
     * @return
     * the exception's HTTP response headers, if any.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Set the exception's HTTP response headers; for internal use only.
     * @param responseHeaders
     */
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}

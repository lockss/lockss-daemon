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


/**
 * Exception for use by <code>S3Service</code>s and related utilities.
 * This exception can hold useful additional information about errors that occur
 * when communicating with S3.
 *
 * @author James Murty
 */
public class S3ServiceException extends ServiceException {

    private static final long serialVersionUID = -4594639645399926038L;

    /**
     * Constructor that includes the XML error document returned by S3.
     * @param message
     * @param xmlMessage
     */
    public S3ServiceException(String message, String xmlMessage) {
        super(message, xmlMessage);
    }

    public S3ServiceException() {
        super();
    }

    public S3ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3ServiceException(String message) {
        super(message);
    }

    public S3ServiceException(Throwable cause) {
        super(cause);
    }

    /**
     * Wrap a ServiceException as an S3ServiceException.
     * @param se
     */
    public S3ServiceException(ServiceException se) {
        super(se.getMessage(), se.getXmlMessage(), se.getCause());

        this.setResponseHeaders(se.getResponseHeaders());
        this.setResponseCode(se.getResponseCode());
        this.setResponseStatus(se.getResponseStatus());
        this.setResponseDate(se.getResponseDate());
        this.setRequestVerb(se.getRequestVerb());
        this.setRequestPath(se.getRequestPath());
        this.setRequestHost(se.getRequestHost());
    }

    /**
     * @return The service-specific Error Code returned by S3, if an S3 response is available.
     * For example "AccessDenied", "InternalError"
     * Null otherwise.
     */
    public String getS3ErrorCode() {
        return this.getErrorCode();
    }

    /**
     * @return The service-specific Error Message returned by S3, if an S3 response is available.
     * For example: "Access Denied", "We encountered an internal error. Please try again."
     */
    public String getS3ErrorMessage() {
        return this.getErrorMessage();
    }

    /**
     * @return The Error Host ID returned by S3, if an S3 response is available.
     * Null otherwise.
     */
    public String getS3ErrorHostId() {
        return this.getErrorHostId();
    }

    /**
     * @return The Error Request ID returned by S3, if an S3 response is available.
     * Null otherwise.
     */
    public String getS3ErrorRequestId() {
        return this.getErrorRequestId();
    }

}

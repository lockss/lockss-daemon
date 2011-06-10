/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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
 * Exception for use by <code>CloudFrontService</code> and related utilities.
 * This exception can hold useful additional information about errors that occur
 * when communicating with CloudFront.
 *
 * @author James Murty
 */
public class CloudFrontServiceException extends Exception {
    private static final long serialVersionUID = -3139479267586698970L;

    private int responseCode = -1;
    private String errorType = null;
    private String errorCode = null;
    private String errorMessage = null;
    private String errorDetail = null;
    private String errorRequestId = null;

    public CloudFrontServiceException(String message, int responseCode,
        String errorType, String errorCode, String errorMessage,
        String errorDetail, String errorRequestId)
    {
        super(message);
        this.responseCode = responseCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetail = errorDetail;
        this.errorRequestId = errorRequestId;
    }

    public CloudFrontServiceException() {
        super();
    }

    public CloudFrontServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudFrontServiceException(String message) {
        super(message);
    }

    public CloudFrontServiceException(Throwable cause) {
        super(cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorRequestId() {
        return errorRequestId;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String toString() {
        if (responseCode == -1) {
            return super.toString();
        } else {
            return "CloudFrontServiceException: httpResponseCode=" + responseCode +
                ", errorType=" + errorType + ", errorCode=" + errorCode +
                ", errorMessage=" + errorMessage + ", errorDetail=" + errorDetail +
                ", errorRequestId=" + errorRequestId;
        }
    }
}

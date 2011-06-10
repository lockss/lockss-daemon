/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2009 Doug MacEachern
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
package contribs.mx;

//generated from
//http://docs.amazonwebservices.com/AmazonS3/2006-03-01/index.html?ErrorCodeList.html

public class S3ServiceErrorCodeTable {
    static final String[][] TABLE = {
    {
      "ServiceUnavailable", //not included in table above
      "Service Unavailable",
      "N/A", "Server"
    },
    {
      "AccessDenied",
      "Access Denied",
      "403 Forbidden", "Client"
    },
    {
      "AccountProblem",
      "There is a problem with your AWS account that prevents the operation from completing successfully. Please contact customer service at webservices@amazon.com.",
      "403 Forbidden", "Client"
    },
    {
      "AmbiguousGrantByEmailAddress",
      "The e-mail address you provided is associated with more than one account.",
      "400 Bad Request", "Client"
    },
    {
      "BadDigest",
      "The Content-MD5 you specified did not match what we received.",
      "400 Bad Request", "Client"
    },
    {
      "BucketAlreadyExists",
      "The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.",
      "409 Conflict", "Client"
    },
    {
      "BucketAlreadyOwnedByYou",
      "Your previous request to create the named bucket succeeded and you already own it.",
      "409 Conflict", "Client"
    },
    {
      "BucketNotEmpty",
      "The bucket you tried to delete is not empty.",
      "409 Conflict", "Client"
    },
    {
      "CredentialsNotSupported",
      "This request does not support credentials.",
      "400 Bad Request", "Client"
    },
    {
      "CrossLocationLoggingProhibited",
      "Cross location logging not allowed. Buckets in one geographic location cannot log information to a bucket in another location.",
      "403 Forbidden", "Client"
    },
    {
      "EntityTooSmall",
      "Your proposed upload is smaller than the minimum allowed object size.",
      "400 Bad Request", "Client"
    },
    {
      "EntityTooLarge",
      "Your proposed upload exceeds the maximum allowed object size.",
      "400 Bad Request", "Client"
    },
    {
      "ExpiredToken",
      "The provided token has expired.",
      "400 Bad Request", "Client"
    },
    {
      "IncompleteBody",
      "You did not provide the number of bytes specified by the Content-Length HTTP header",
      "400 Bad Request", "Client"
    },
    {
      "IncorrectNumberOfFilesInPostRequest",
      "POST requires exactly one file upload per request.",
      "400 Bad Request", "Client"
    },
    {
      "InlineDataTooLarge",
      "Inline data exceeds the maximum allowed size.",
      "400 Bad Request", "Client"
    },
    {
      "InternalError",
      "We encountered an internal error. Please try again.",
      "500 Internal Server Error", "Server"
    },
    {
      "InvalidAccessKeyId",
      "The AWS Access Key Id you provided does not exist in our records.",
      "403 Forbidden", "Client"
    },
    {
      "InvalidAddressingHeader",
      "You must specify the Anonymous role.",
      "N/A", "Client"
    },
    {
      "InvalidArgument",
      "Invalid Argument",
      "400 Bad Request", "Client"
    },
    {
      "InvalidBucketName",
      "The specified bucket is not valid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidDigest",
      "The Content-MD5 you specified was an invalid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidLocationConstraint",
      "The specified location constraint is not valid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidPayer",
      "All access to this object has been disabled.",
      "403 Forbidden", "Client"
    },
    {
      "InvalidPolicyDocument",
      "The content of the form does not meet the conditions specified in the policy document.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidRange",
      "The requested range cannot be satisfied.",
      "416 Requested Range Not Satisfiable", "Client"
    },
    {
      "InvalidSecurity",
      "The provided security credentials are not valid.",
      "403 Forbidden", "Client"
    },
    {
      "InvalidSOAPRequest",
      "The SOAP request body is invalid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidStorageClass",
      "The storage class you specified is not valid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidTargetBucketForLogging",
      "The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group. ",
      "400 Bad Request", "Client"
    },
    {
      "InvalidToken",
      "The provided token is malformed or otherwise invalid.",
      "400 Bad Request", "Client"
    },
    {
      "InvalidURI",
      "Couldn't parse the specified URI.",
      "400 Bad Request", "Client"
    },
    {
      "KeyTooLong",
      "Your key is too long.",
      "400 Bad Request", "Client"
    },
    {
      "MalformedACLError",
      "The XML you provided was not well-formed or did not validate against our published schema.",
      "400 Bad Request", "Client"
    },
    {
      "MalformedXML",
      "The XML you provided was not well-formed or did not validate against our published schema.",
      "400 Bad Request", "Client"
    },
    {
      "MaxMessageLengthExceeded",
      "Your request was too big.",
      "400 Bad Request", "Client"
    },
    {
      "MaxPostPreDataLengthExceededError",
      "Your POST request fields preceding the upload file were too large.",
      "400 Bad Request", "Client"
    },
    {
      "MetadataTooLarge",
      "Your metadata headers exceed the maximum allowed metadata size.",
      "400 Bad Request", "Client"
    },
    {
      "MethodNotAllowed",
      "The specified method is not allowed against this resource.",
      "405 Method Not Allowed", "Client"
    },
    {
      "MissingAttachment",
      "A SOAP attachment was expected, but none were found.",
      "N/A", "Client"
    },
    {
      "MissingContentLength",
      "You must provide the Content-Length HTTP header.",
      "411 Length Required", "Client"
    },
    {
      "MissingSecurityElement",
      "The SOAP 1.1 request is missing a security element.",
      "400 Bad Request", "Client"
    },
    {
      "MissingSecurityHeader",
      "Your request was missing a required header.",
      "400 Bad Request", "Client"
    },
    {
      "NoLoggingStatusForKey",
      "There is no such thing as a logging status sub-resource for a key.",
      "400 Bad Request", "Client"
    },
    {
      "NoSuchBucket",
      "The specified bucket does not exist.",
      "404 Not Found", "Client"
    },
    {
      "NoSuchKey",
      "The specified key does not exist.",
      "404 Not Found", "Client"
    },
    {
      "NotImplemented",
      "A header you provided implies functionality that is not implemented.",
      "501 Not Implemented", "Server"
    },
    {
      "NotSignedUp",
      "Your account is not signed up for the Amazon S3 service. You must sign up before you can use Amazon S3. You can sign up at the following URL: http://aws.amazon.com/s3",
      "403 Forbidden", "Client"
    },
    {
      "OperationAborted",
      "A conflicting conditional operation is currently in progress against this resource. Please try again.",
      "409 Conflict", "Client"
    },
    {
      "PermanentRedirect",
      "The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint.",
      "301 Moved Permanently", "Client"
    },
    {
      "PreconditionFailed",
      "At least one of the pre-conditions you specified did not hold.",
      "412 Precondition Failed", "Client"
    },
    {
      "Redirect",
      "Temporary redirect.",
      "307 Moved Temporarily", "Client"
    },
    {
      "RequestIsNotMultiPartContent",
      "Bucket POST must be of the enclosure-type multipart/form-data.",
      "400 Bad Request", "Client"
    },
    {
      "RequestTimeout",
      "Your socket connection to the server was not read from or written to within the timeout period.",
      "400 Bad Request", "Client"
    },
    {
      "RequestTimeTooSkewed",
      "The difference between the request time and the server's time is too large.",
      "403 Forbidden", "Client"
    },
    {
      "RequestTorrentOfBucketError",
      "Requesting the torrent file of a bucket is not permitted.",
      "400 Bad Request", "Client"
    },
    {
      "SignatureDoesNotMatch",
      "The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method. For more information, see Authenticating REST Requests and Authenticating SOAP Requests for details.",
      "403 Forbidden", "Client"
    },
    {
      "SlowDown",
      "Please reduce your request rate.",
      "503 Service Unavailable", "Server"
    },
    {
      "TemporaryRedirect",
      "You are being redirected to the bucket while DNS updates.",
      "307 Moved Temporarily", "Client"
    },
    {
      "TokenRefreshRequired",
      "The provided token must be refreshed.",
      "400 Bad Request", "Client"
    },
    {
      "TooManyBuckets",
      "You have attempted to create more buckets than allowed.",
      "400 Bad Request", "Client"
    },
    {
      "UnexpectedContent",
      "This request does not support content.",
      "400 Bad Request", "Client"
    },
    {
      "UnresolvableGrantByEmailAddress",
      "The e-mail address you provided does not match any account on record.",
      "400 Bad Request", "Client"
    },
    {
      "UserKeyMustBeSpecified",
      "The bucket POST must contain the specified field name. If it is specified, please check the order of the fields.",
      "400 Bad Request", "Client"
    },

    };
}

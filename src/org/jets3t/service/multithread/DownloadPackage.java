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
package org.jets3t.service.multithread;

import java.io.File;
import java.io.OutputStream;

import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.EncryptionUtil;

/**
 * A simple container object to associate one of an {@link S3Object} or a signed URL string
 * with an output file or output stream to which the S3 object's data will be written.
 * <p>
 * This class is used by
 * {@link S3ServiceMulti#downloadObjects(org.jets3t.service.model.S3Bucket, DownloadPackage[])}
 * to download objects.
 *
 * @author James Murty
 * @deprecated 0.8.0 use {@link org.jets3t.service.multi.DownloadPackage} instead.
 */
@Deprecated
public class DownloadPackage extends org.jets3t.service.multi.DownloadPackage {

    private String signedUrl = null;

    public DownloadPackage(String signedUrl, File outputFile, boolean isUnzipping,
        EncryptionUtil encryptionUtil)
    {
        super(null, outputFile, isUnzipping, encryptionUtil);
        this.signedUrl = signedUrl;
    }

    public DownloadPackage(String signedUrl, OutputStream outputStream, boolean isUnzipping,
        EncryptionUtil encryptionUtil)
    {
        super(null, outputStream, isUnzipping, encryptionUtil);
        this.signedUrl = signedUrl;
    }

    public DownloadPackage(S3Object object, File outputFile) {
        this(object, outputFile, false, null);
    }

    public DownloadPackage(S3Object object, File outputFile, boolean isUnzipping,
        EncryptionUtil encryptionUtil)
    {
        super(object, outputFile, isUnzipping, encryptionUtil);
    }

    public DownloadPackage(S3Object object, OutputStream outputStream) {
        this(object, outputStream, false, null);
    }

    public DownloadPackage(S3Object object, OutputStream outputStream, boolean isUnzipping,
        EncryptionUtil encryptionUtil)
    {
        super(object, outputStream, isUnzipping, encryptionUtil);
    }

    public String getSignedUrl() {
        return signedUrl;
    }

    public void setSignedUrl(String url) {
        signedUrl = url;
    }

    public boolean isSignedDownload() {
        return signedUrl != null;
    }

    @Override
    public S3Object getObject() {
        // TODO Auto-generated method stub
        return (S3Object) super.getObject();
    }

}

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
package org.jets3t.service.utils.signedurl;

import org.jets3t.service.model.S3Object;

/**
 * A package containing an object and a signed URL associated with the object.
 *
 * @author James Murty
 */
public class SignedUrlAndObject {
    private String signedUrl = null;
    private S3Object object = null;

    public SignedUrlAndObject(String signedUrl, S3Object object) {
        this.signedUrl = signedUrl;
        this.object = object;
    }

    public S3Object getObject() {
        return object;
    }

    public String getSignedUrl() {
        return signedUrl;
    }

}

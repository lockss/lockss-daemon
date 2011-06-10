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
package org.jets3t.service.model;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jets3t.service.Constants;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents the location configuraton to be applied to a newly
 * created Bucket.
 *
 * @author James Murty
 *
 */
public class CreateBucketConfiguration {
    private String location = null;

    public CreateBucketConfiguration() {
    }

    public CreateBucketConfiguration(String location) {
        this.location = location;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     *
     * @return
     * An XML representation of the object suitable for use as an input to the REST/HTTP interface.
     *
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public String toXml() throws ParserConfigurationException,
        FactoryConfigurationError, TransformerException
    {
        XMLBuilder builder = XMLBuilder.create("CreateBucketConfiguration")
            .attr("xmlns", Constants.XML_NAMESPACE)
            .elem("LocationConstraint").text(location);
        return builder.asString();
    }

}

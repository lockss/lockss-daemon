/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 James Murty
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
package org.jets3t.service.model.cloudfront;


public class CustomOrigin extends Origin {

    public enum OriginProtocolPolicy {
        HTTP_ONLY ("http-only"),
        MATCH_VIEWER ("match-viewer");

        private final String textValue;

        OriginProtocolPolicy(String textValue) {
            this.textValue = textValue;
        }

        public String toText() {
            return textValue;
        }

        public static OriginProtocolPolicy fromText(String text) {
            for (OriginProtocolPolicy e: OriginProtocolPolicy.values()) {
                if (e.toText().equalsIgnoreCase(text)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Invalid OriginProtocolPolicy: " + text);
        }
    };

    private OriginProtocolPolicy originProtocolPolicy = null;
    private int httpPort = 80;   // Not customizable in 2010-11-01 API
    private int httpsPort = 443; // Not customizable in 2010-11-01 API

    public CustomOrigin(String dnsName, OriginProtocolPolicy originProtocolPolicy) {
        super(dnsName);
        this.originProtocolPolicy = originProtocolPolicy;
    }

    public CustomOrigin(String dnsName, OriginProtocolPolicy originProtocolPolicy,
        int httpPort, int httpsPort)
    {
        super(dnsName);
        this.originProtocolPolicy = originProtocolPolicy;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
    }

    public OriginProtocolPolicy getOriginProtocolPolicy() {
        return this.originProtocolPolicy;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public int getHttpsPort() {
        return this.httpsPort;
    }

    @Override
    public String toString() {
        return "CustomOrigin: dnsName=" + getDnsName() +
            ", originProtocolPolicy=" + getOriginProtocolPolicy() +
            ", httpPort=" + getHttpPort() +
            ", httpsPort=" + getHttpsPort();
    }

}

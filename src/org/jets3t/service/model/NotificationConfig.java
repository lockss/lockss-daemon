/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents the notification configuraton of a bucket
 *
 * @author James Murty
 */
public class NotificationConfig {
    public static final String EVENT_REDUCED_REDUNDANCY_LOST_OBJECT =
        "s3:ReducedRedundancyLostObject";

    private List<TopicConfig> topicConfigs = new ArrayList<TopicConfig>();

    public NotificationConfig(List<TopicConfig> topicConfigs) {
        this.topicConfigs = topicConfigs;
    }

    public NotificationConfig() {
    }

    public List<TopicConfig> getTopicConfigs() {
        return topicConfigs;
    }

    public void addTopicConfig(TopicConfig config) {
        this.topicConfigs.add(config);
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
        XMLBuilder builder = XMLBuilder.create("NotificationConfiguration");
        for (TopicConfig topicConfig: this.topicConfigs) {
            builder
                .elem("TopicConfiguration")
                    .elem("Topic").text(topicConfig.topic).up()
                    .elem("Event").text(topicConfig.event);
        }
        return builder.asString();
    }

    public class TopicConfig {
        protected String topic;
        protected String event;

        public TopicConfig(String topic, String event) {
            this.topic = topic;
            this.event = event;
        }

        public String getTopic() {
            return topic;
        }

        public String getEvent() {
            return event;
        }
    }

}

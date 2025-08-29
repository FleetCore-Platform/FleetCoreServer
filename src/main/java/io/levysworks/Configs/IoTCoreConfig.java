package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws.iotcore")
public interface IoTCoreConfig {
    @WithName("region")
    String region();

    @WithName("thing-type")
    String thingType();

    @WithName("pubsub-client-id")
    String pubsubClientId();
}
package io.levysworks;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws.iotcore")
public interface IoTCoreConfig {
    @WithName("region")
    String region();

    @WithName("thing_type")
    String thingType();
}
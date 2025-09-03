package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws")
public interface ApplicationConfig {
    @WithName("region")
    String region();
    @WithName("s3")
    S3Config s3();
    @WithName("iot")
    IoTCoreConfig iot();
    interface S3Config {
        @WithName("bucket-name")
        String bucketName();
    }
    interface IoTCoreConfig {
        @WithName("thing-type")
        String thingType();

        @WithName("pubsub-client-id")
        String pubsubClientId();
    }
}

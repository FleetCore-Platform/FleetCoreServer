package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws.s3")
public interface S3Config {
    @WithName("bucket-name")
    String bucketName();

    @WithName("region")
    String region();
}

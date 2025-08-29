package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws.sqs")
public interface SQSConfig {
    @WithName("region")
    String region();
}

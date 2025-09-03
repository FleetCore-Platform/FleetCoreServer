package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "aws")
public interface AwsConfig {
    @ConfigMapping(prefix = "sns")
    interface SnsConfig {

    }
}

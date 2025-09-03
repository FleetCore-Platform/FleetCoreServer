package io.levysworks.Configs;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "aws.rds")
public interface RDSConfig {
    @WithName("region")
    String region();

    @WithName("master-user")
    String masterUser();

    @WithName("host")
    String host();

    @WithName("port")
    Integer port();
}

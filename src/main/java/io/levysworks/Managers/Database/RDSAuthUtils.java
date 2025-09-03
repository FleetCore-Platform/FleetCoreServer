package io.levysworks.Managers.Database;

import io.levysworks.Configs.RDSConfig;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.awssdk.services.rds.model.RdsException;

@Startup
@ApplicationScoped
public class RDSAuthUtils {
    private RdsClient rdsClient;

    @Inject
    RDSConfig config;

    @PostConstruct
    public void init() {
        rdsClient = RdsClient.builder()
                .region(Region.of(config.region()))
                .build();
    }

    public String getAuthToken() {
        RdsUtilities authUtils = rdsClient.utilities();
        try {
            GenerateAuthenticationTokenRequest generateAuthenticationTokenRequest = GenerateAuthenticationTokenRequest.builder()
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .username(config.masterUser())
                    .hostname(config.host())
                    .port(config.port())
                    .build();

            return authUtils.generateAuthenticationToken(generateAuthenticationTokenRequest);
        } catch (RdsException e) {
            System.err.println("Failed to generate authentication token: " + e.awsErrorDetails());
            return "";
        }
    }
}

package io.levysworks.Managers.Cognito;

import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Models.CognitoCreatedResponse;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Startup
@ApplicationScoped
public class CognitoManager {
    @Inject
    ApplicationConfig config;

    private Logger logger;

    private CognitoIdentityProviderAsyncClient client;

    @PostConstruct
    public void init() {
        logger = Logger.getLogger(CognitoManager.class.getName());

        client = CognitoIdentityProviderAsyncClient.builder()
                .region(Region.of(config.region()))
                .build();
    }

    @PreDestroy
    public void destroy() {
        client.close();
    }

    public CognitoCreatedResponse createUser(String email, String firstName, String lastName) throws SdkException {
        AttributeType emailAttribute = AttributeType.builder()
                .name("email")
                .value(email)
                .build();

        AttributeType firstNameAttribute = AttributeType.builder()
                .name("given_name")
                .value(firstName)
                .build();

        AttributeType lastNameAttribute = AttributeType.builder()
                .name("family_name")
                .value(lastName)
                .build();

        List<AttributeType> attributes = Arrays.asList(emailAttribute, firstNameAttribute, lastNameAttribute);

        String tempPassword = UUID.randomUUID().toString().replace("-", "").concat("A%");

        AdminCreateUserRequest adminCreateUserRequest = AdminCreateUserRequest.builder()
                .userAttributes(attributes)
                .userPoolId(config.cognito().userPoolId())
                .username(email)
                .messageAction(MessageActionType.SUPPRESS)
                .temporaryPassword(tempPassword)
                .build();

        CompletableFuture<AdminCreateUserResponse> future =  client.adminCreateUser(adminCreateUserRequest);

        try {
            AdminCreateUserResponse response = future.join();

            if (response.sdkHttpResponse().isSuccessful()) {
                return new CognitoCreatedResponse(tempPassword, response.user().attributes().getLast().value());
            } else {
                return null;
            }

        } catch (SdkException ignored) {
            return null;
        }
    }
}

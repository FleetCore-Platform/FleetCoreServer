package io.levysworks.Managers.IoTCore;

import io.levysworks.Configs.IoTCoreConfig;
import io.levysworks.Models.IoTCertContainer;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotAsyncClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.IotDataPlaneException;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Startup
@ApplicationScoped
public class IotManager {
    @Inject
    IoTCoreConfig config;

    private IotAsyncClient iotAsyncClient;

    @PostConstruct
    void init() {
        SdkAsyncHttpClient asyncHttpClient = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(100)
                .connectionTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();

        iotAsyncClient = IotAsyncClient.builder()
                .region(Region.of(config.region()))
                .httpClient(asyncHttpClient)
                .build();
    }


//    public void createPolicy(String thingName) {
//        CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
//                .build()
//
//    }

    public void addDeviceToGroup(String thingName, String groupName) {
        AddThingToThingGroupRequest addThingToThingGroupRequest = AddThingToThingGroupRequest.builder()
                .thingName(thingName)
                .thingGroupName(groupName)
                .build();

        CompletableFuture<AddThingToThingGroupResponse> future = iotAsyncClient.addThingToThingGroup(addThingToThingGroupRequest);
        future.whenComplete((response, throwable) -> {
            if (throwable == null && response.sdkHttpResponse().isSuccessful()) {
                System.out.printf("Successfully added %S device to group %s%n", thingName, groupName);
            } else  {
                System.out.printf("Failed to add device to group %s%n", thingName);
            }
        });
    }

    public IoTCertContainer generateCertificate() {
        CompletableFuture<CreateKeysAndCertificateResponse> future = iotAsyncClient.createKeysAndCertificate();
        CreateKeysAndCertificateResponse response = future.join();

        return new IoTCertContainer(response.certificatePem(), response.certificateArn());
    }

    public void attachCertificate(String deviceName, String certificateARN) {
        AttachThingPrincipalRequest attachThingPrincipalRequest = AttachThingPrincipalRequest.builder()
                .thingName(deviceName)
                .principal(certificateARN)
                .build();

        CompletableFuture<AttachThingPrincipalResponse> future = iotAsyncClient.attachThingPrincipal(attachThingPrincipalRequest);
        future.whenComplete((attachThingPrincipalResponse, ex) -> {
            if (ex != null && attachThingPrincipalResponse.sdkHttpResponse().isSuccessful()) {
                System.out.printf("Successfully attached certificate with ARN %s to device %s%n", certificateARN, deviceName);
            } else if (ex instanceof IotException) {
                System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
            } else {
                System.err.println(ex.getMessage());
            }
        });

        future.join();
    }

    public void createDevice(String deviceName) {
        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(deviceName)
                .thingTypeName(config.thingType())
                .build();

        CompletableFuture<CreateThingResponse> future = iotAsyncClient.createThing(createThingRequest);
        future.whenComplete((createThingResponse, ex) -> {
            if (createThingResponse != null && createThingResponse.sdkHttpResponse().isSuccessful()) {
                System.out.println(deviceName + " was successfully created. The ARN value is " + createThingResponse.thingArn());
            } else {
                Throwable cause = ex.getCause();
                if (cause instanceof IotException) {
                    System.err.println(((IotException) cause).awsErrorDetails().errorMessage());
                } else {
                    System.err.println("Unexpected error: " + cause.getMessage());
                }
            }
        });

        future.join();
    }

    public void createDeviceGroup(String groupName) {
        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(groupName)
                .build();

        CompletableFuture<CreateThingGroupResponse> future = iotAsyncClient.createThingGroup(createThingGroupRequest);
        future.whenComplete((createThingGroupResponse, ex) -> {
            if (createThingGroupResponse != null && createThingGroupResponse.sdkHttpResponse().isSuccessful()) {
                System.out.println("Successfully created group " + groupName);
            } else if (ex instanceof IotException) {
                System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
            }
        });

        future.join();
    }

    public void createIoTJob(String jobName, String groupARN, String bucketPresignedUrl) {
        CreateJobRequest createJobRequest = CreateJobRequest.builder()
                .jobId(jobName)
                .targets(groupARN)
                .targetSelection(TargetSelection.SNAPSHOT)
                .jobTemplateArn("arn:aws:iot:eu-north-1::jobtemplate/AWS-Download-File:1.0")
                .documentParameters(Map.ofEntries(
                        Map.entry("downloadUrl", bucketPresignedUrl),
                        Map.entry("filePath", "/home/pi/missions/")
                ))
                .build();

        CompletableFuture<CreateJobResponse> future = iotAsyncClient.createJob(createJobRequest);
        future.whenComplete((jobResponse, ex) -> {
            if (jobResponse != null && jobResponse.sdkHttpResponse().isSuccessful()) {
                System.out.println("New job was successfully created.");
            } else {
                Throwable cause = ex.getCause();
                if (cause instanceof IotException) {
                    System.err.println(((IotException) cause).awsErrorDetails().errorMessage());
                } else {
                    System.err.println("Unexpected error: " + cause.getMessage());
                }
            }
        });

        future.join();
    }

    public void deleteDevice(String deviceName) {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(deviceName)
                .build();

        CompletableFuture<DeleteThingResponse> future = iotAsyncClient.deleteThing(deleteThingRequest);
        future.whenComplete((deleteThingResponse, ex) -> {
            if (deleteThingResponse != null) {
                System.out.println(deviceName + " was successfully deleted.");
            } else  {
                Throwable cause = ex.getCause();
                if (cause instanceof IotException) {
                    System.err.println(((IotException) cause).awsErrorDetails().errorMessage());
                }
            }
        });

        future.join();
    }

    public void describeDevice(String deviceName) {
        DescribeThingRequest thingRequest = DescribeThingRequest.builder()
                .thingName(deviceName)
                .build();

        CompletableFuture<DescribeThingResponse> future = iotAsyncClient.describeThing(thingRequest);
        future.whenComplete((describeResponse, ex) -> {
            if (describeResponse != null) {
                System.out.println("Thing Details:");
                System.out.println("Thing Name: " + describeResponse.thingName());
                System.out.println("Thing ARN: " + describeResponse.thingArn());
            } else {
                Throwable cause = ex != null ? ex.getCause() : null;
                if (cause instanceof IotException) {
                    System.err.println(((IotException) cause).awsErrorDetails().errorMessage());
                } else if (cause != null) {
                    System.err.println("Unexpected error: " + cause.getMessage());
                } else {
                    System.err.println("Failed to describe Thing.");
                }
            }
        });

        future.join();
    }
}

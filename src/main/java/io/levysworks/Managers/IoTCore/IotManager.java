package io.levysworks.Managers.IoTCore;

import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Models.IoTCertContainer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotAsyncClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.sts.StsClient;

@ApplicationScoped
public class IotManager {
    @Inject ApplicationConfig config;

    private IotAsyncClient iotAsyncClient;
    private String accountIdentifier;

    @PostConstruct
    void init() {
        SdkAsyncHttpClient asyncHttpClient =
                NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(100)
                        .connectionTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(60))
                        .writeTimeout(Duration.ofSeconds(60))
                        .build();

        iotAsyncClient =
                IotAsyncClient.builder()
                        .region(Region.of(config.region()))
                        .httpClient(asyncHttpClient)
                        .build();

        try (StsClient stsClient = StsClient.create()) {
            accountIdentifier = stsClient.getCallerIdentity().account();
        }
    }

    public String createPolicy(String thingName) throws CompletionException {
        String policyName = thingName + "-policy";
        String policyDocument =
                IotPolicyMaker.buildPolicyDocument(accountIdentifier, config.region());

        CreatePolicyRequest createPolicyRequest =
                CreatePolicyRequest.builder()
                        .policyName(policyName)
                        .policyDocument(policyDocument)
                        .build();

        CompletableFuture<CreatePolicyResponse> future =
                iotAsyncClient.createPolicy(createPolicyRequest);

        CreatePolicyResponse createPolicyResponse = future.join();
        return createPolicyResponse.policyName();
    }

    public void attachPolicyToCertificate(String certificateARN, String policyARN) {
        AttachPolicyRequest attachPolicyRequest =
                AttachPolicyRequest.builder().policyName(policyARN).target(certificateARN).build();

        AttachPolicyResponse attachPolicyResponse =
                iotAsyncClient.attachPolicy(attachPolicyRequest).join();
    }

    public JobExecutionStatus getJobExecutionStatus(String jobId, String thingName) {
        DescribeJobExecutionRequest describeJobExecutionRequest =
                DescribeJobExecutionRequest.builder().jobId(jobId).thingName(thingName).build();

        CompletableFuture<DescribeJobExecutionResponse> response =
                iotAsyncClient.describeJobExecution(describeJobExecutionRequest);
        return response.join().execution().status();
    }

    public String getLastJobId(String thingName) {
        ListJobExecutionsForThingRequest listJobExecutionsForThingRequest =
                ListJobExecutionsForThingRequest.builder().thingName(thingName).build();

        CompletableFuture<ListJobExecutionsForThingResponse> future =
                iotAsyncClient.listJobExecutionsForThing(listJobExecutionsForThingRequest);

        try {
            ListJobExecutionsForThingResponse response = future.join();

            return response.executionSummaries().getFirst().jobId();
        } catch (Exception e) {
            return null;
        }
    }

    public void addDeviceToGroup(String thingName, String groupARN) {
        AddThingToThingGroupRequest addThingToThingGroupRequest =
                AddThingToThingGroupRequest.builder()
                        .thingName(thingName)
                        .thingGroupArn(groupARN)
                        .build();

        CompletableFuture<AddThingToThingGroupResponse> future =
                iotAsyncClient.addThingToThingGroup(addThingToThingGroupRequest);
        future.whenComplete(
                (response, throwable) -> {
                    if (throwable == null && response.sdkHttpResponse().isSuccessful()) {
                        System.out.printf(
                                "Successfully added %S device to group %s%n", thingName, groupARN);
                    } else {
                        System.out.printf("Failed to add device to group %s%n", thingName);
                    }
                });
    }

    public String getGroupARN(String groupName) {
        DescribeThingGroupRequest describeThingGroupRequest =
                DescribeThingGroupRequest.builder().thingGroupName(groupName).build();

        CompletableFuture<DescribeThingGroupResponse> future =
                iotAsyncClient.describeThingGroup(describeThingGroupRequest);
        DescribeThingGroupResponse response = future.join();

        return response.thingGroupArn();
    }

    public IoTCertContainer generateCertificate() {
        CompletableFuture<CreateKeysAndCertificateResponse> future =
                iotAsyncClient.createKeysAndCertificate();
        CreateKeysAndCertificateResponse response = future.join();

        return new IoTCertContainer(
                response.keyPair().privateKey(),
                response.certificatePem(),
                response.certificateArn());
    }

    public void attachCertificate(String deviceName, String certificateARN) {
        AttachThingPrincipalRequest attachThingPrincipalRequest =
                AttachThingPrincipalRequest.builder()
                        .thingName(deviceName)
                        .principal(certificateARN)
                        .build();

        CompletableFuture<AttachThingPrincipalResponse> future =
                iotAsyncClient.attachThingPrincipal(attachThingPrincipalRequest);
        future.whenComplete(
                (attachThingPrincipalResponse, ex) -> {
                    if (ex != null
                            && attachThingPrincipalResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.printf(
                                "Successfully attached certificate with ARN %s to device %s%n",
                                certificateARN, deviceName);
                    } else if (ex instanceof IotException) {
                        System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
                    } else {
                        System.err.println(ex.getMessage());
                    }
                });

        future.join();
    }

    public void createThing(String thingName, String px4Version, String agentVersion) {
        CreateThingRequest createThingRequest =
                CreateThingRequest.builder()
                        .thingName(thingName)
                        .thingTypeName(config.iot().thingType())
                        .attributePayload(
                                AttributePayload.builder()
                                        .attributes(
                                                Map.ofEntries(
                                                        Map.entry("px4_version", px4Version),
                                                        Map.entry("agent_version", agentVersion)))
                                        .build())
                        .build();

        CompletableFuture<CreateThingResponse> future =
                iotAsyncClient.createThing(createThingRequest);
        future.whenComplete(
                (createThingResponse, ex) -> {
                    if (createThingResponse != null
                            && createThingResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println(
                                thingName
                                        + " was successfully created. The ARN value is "
                                        + createThingResponse.thingArn());
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        } else {
                            System.err.println("Unexpected error: " + cause.getMessage());
                        }
                    }
                });

        future.join();
    }

    public String getThingGroup(String thingName) {
        ListThingGroupsForThingRequest listThingGroupsForThingRequest =
                ListThingGroupsForThingRequest.builder().thingName(thingName).maxResults(1).build();

        ListThingGroupsForThingResponse future =
                iotAsyncClient.listThingGroupsForThing(listThingGroupsForThingRequest).join();
        return future.thingGroups().getFirst().groupArn();
    }

    public void removeThing(String thingName) {
        DeleteThingRequest deleteThingRequest =
                DeleteThingRequest.builder().thingName(thingName).build();

        CompletableFuture<DeleteThingResponse> future =
                iotAsyncClient.deleteThing(deleteThingRequest);

        DeleteThingResponse response = future.join();
    }

    public void removeThingFromGroup(String thingName, String groupARN) {
        RemoveThingFromThingGroupRequest removeThingFromThingGroupRequest =
                RemoveThingFromThingGroupRequest.builder()
                        .thingName(thingName)
                        .thingGroupArn(groupARN)
                        .build();

        iotAsyncClient.removeThingFromThingGroup(removeThingFromThingGroupRequest).join();
    }

    public void removePolicies(String principalARN) {
        ListAttachedPoliciesRequest listAttachedPoliciesRequest =
                ListAttachedPoliciesRequest.builder().target(principalARN).build();

        CompletableFuture<ListAttachedPoliciesResponse> future =
                iotAsyncClient.listAttachedPolicies(listAttachedPoliciesRequest);

        ListAttachedPoliciesResponse response = future.join();
        String policyName = response.policies().getFirst().policyName();

        DeletePolicyRequest deletePolicyRequest =
                DeletePolicyRequest.builder().policyName(policyName).build();

        iotAsyncClient.deletePolicy(deletePolicyRequest).join();
    }

    public void detachCertificates(String deviceName) {
        ListThingPrincipalsRequest listThingPrincipalsRequest =
                ListThingPrincipalsRequest.builder().thingName(deviceName).build();

        CompletableFuture<ListThingPrincipalsResponse> future =
                iotAsyncClient.listThingPrincipals(listThingPrincipalsRequest);

        ListThingPrincipalsResponse response = future.join();
        List<String> principalARNs = new ArrayList<>(response.principals());

        principalARNs.forEach(
                arn -> {
                    DetachThingPrincipalRequest detachThingPrincipalRequest =
                            DetachThingPrincipalRequest.builder()
                                    .principal(arn)
                                    .thingName(deviceName)
                                    .build();

                    iotAsyncClient.detachThingPrincipal(detachThingPrincipalRequest).join();
                });
    }

    public void createThingGroup(String groupName, String outpostName) {
        CreateThingGroupRequest createThingGroupRequest =
                CreateThingGroupRequest.builder()
                        .thingGroupProperties(
                                ThingGroupProperties.builder()
                                        .attributePayload(
                                                AttributePayload.builder()
                                                        .attributes(Map.of("outpost", outpostName))
                                                        .build())
                                        .build())
                        .thingGroupName(groupName)
                        .build();

        CompletableFuture<CreateThingGroupResponse> future =
                iotAsyncClient.createThingGroup(createThingGroupRequest);
        future.whenComplete(
                (createThingGroupResponse, ex) -> {
                    if (createThingGroupResponse != null
                            && createThingGroupResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully created group " + groupName);
                    } else if (ex instanceof IotException) {
                        System.err.println(((IotException) ex).awsErrorDetails().errorMessage());
                    }
                });

        future.join();
    }

    public void removeThingGroup(String groupName) {
        DeleteThingGroupRequest deleteGroupRequest =
                DeleteThingGroupRequest.builder().thingGroupName(groupName).build();

        CompletableFuture<DeleteThingGroupResponse> future =
                iotAsyncClient.deleteThingGroup(deleteGroupRequest);
        future.whenComplete(
                (res, ex) -> {
                    if (ex != null) {
                        if (ex instanceof IotException) {
                            System.err.println(
                                    ((IotException) ex).awsErrorDetails().errorMessage());
                        }
                    }
                    if (res != null && res.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully removed group " + groupName);
                    }
                });
    }

    public void updateThingGroupOutpost(String groupName, String newOutpostName) {
        UpdateThingGroupRequest updateThingGroupRequest =
                UpdateThingGroupRequest.builder()
                        .thingGroupName(groupName)
                        .thingGroupProperties(
                                ThingGroupProperties.builder()
                                        .attributePayload(
                                                AttributePayload.builder()
                                                        .attributes(
                                                                Map.of("outpost", newOutpostName))
                                                        .build())
                                        .build())
                        .build();

        CompletableFuture<UpdateThingGroupResponse> future =
                iotAsyncClient.updateThingGroup(updateThingGroupRequest);
        future.whenComplete(
                (res, ex) -> {
                    if (ex != null) {
                        if (ex instanceof IotException) {
                            System.err.println(
                                    ((IotException) ex).awsErrorDetails().errorMessage());
                        }
                    }
                    if (res != null && res.sdkHttpResponse().isSuccessful()) {
                        System.out.println("Successfully updated group " + groupName);
                    }
                });

        future.join();
    }

    public void createIoTJob(
            String jobName,
            String groupARN,
            String templateARN,
            Map<String, String> missionParameters) {
        CreateJobRequest createJobRequest =
                CreateJobRequest.builder()
                        .jobId(jobName)
                        .targets(groupARN)
                        .targetSelection(TargetSelection.SNAPSHOT)
                        .jobTemplateArn(templateARN)
                        .documentParameters(missionParameters)
                        .build();

        CompletableFuture<CreateJobResponse> future = iotAsyncClient.createJob(createJobRequest);
        future.whenComplete(
                (jobResponse, ex) -> {
                    if (jobResponse != null && jobResponse.sdkHttpResponse().isSuccessful()) {
                        System.out.println("New job was successfully created.");
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        } else {
                            System.err.println("Unexpected error: " + cause.getMessage());
                        }
                    }
                });

        future.join();
    }

    public void deleteDevice(String deviceName) {
        DeleteThingRequest deleteThingRequest =
                DeleteThingRequest.builder().thingName(deviceName).build();

        CompletableFuture<DeleteThingResponse> future =
                iotAsyncClient.deleteThing(deleteThingRequest);
        future.whenComplete(
                (deleteThingResponse, ex) -> {
                    if (deleteThingResponse != null) {
                        System.out.println(deviceName + " was successfully deleted.");
                    } else {
                        Throwable cause = ex.getCause();
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
                        }
                    }
                });

        future.join();
    }

    public void describeDevice(String deviceName) {
        DescribeThingRequest thingRequest =
                DescribeThingRequest.builder().thingName(deviceName).build();

        CompletableFuture<DescribeThingResponse> future =
                iotAsyncClient.describeThing(thingRequest);
        future.whenComplete(
                (describeResponse, ex) -> {
                    if (describeResponse != null) {
                        System.out.println("Thing Details:");
                        System.out.println("Thing Name: " + describeResponse.thingName());
                        System.out.println("Thing ARN: " + describeResponse.thingArn());
                    } else {
                        Throwable cause = ex != null ? ex.getCause() : null;
                        if (cause instanceof IotException) {
                            System.err.println(
                                    ((IotException) cause).awsErrorDetails().errorMessage());
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

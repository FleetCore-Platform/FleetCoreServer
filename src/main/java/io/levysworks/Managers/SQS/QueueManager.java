package io.levysworks.Managers.SQS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Models.DroneTelemetryModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@ApplicationScoped
public class QueueManager {
    private SqsClient sqsClient;
    private ObjectMapper mapper;

    @Inject ApplicationConfig config;

    @PostConstruct
    public void init() {
        sqsClient = SqsClient.builder().region(Region.of(config.region())).build();
        mapper = new CBORMapper();
    }

    @PreDestroy
    public void destroy() {
        sqsClient.close();
    }

    public List<DroneTelemetryModel> ingestQueue(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest =
                GetQueueUrlRequest.builder().queueName(queueName).build();

        String queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

        System.out.println("Got queue url: " + queueUrl);

        ReceiveMessageRequest receiveRequest =
                ReceiveMessageRequest.builder().queueUrl(queueUrl).build();

        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);

        List<DroneTelemetryModel> result = new ArrayList<>();

        receiveMessageResponse
                .messages()
                .forEach(
                        message -> {
                            byte[] cborData;
                            try {
                                cborData = mapper.writeValueAsBytes(message.body());
                                DroneTelemetryModel telemetryData =
                                        mapper.readValue(cborData, DroneTelemetryModel.class);

                                result.add(telemetryData);
                            } catch (IOException e) {
                                System.err.println("Failed to serialize CBOR message.");
                            }
                        });

        //        for (Message msg : messages) {
        //            DeleteMessageRequest deleteRequest =
        //                    DeleteMessageRequest.builder()
        //                            .queueUrl(queueUrl)
        //                            .receiptHandle(msg.receiptHandle())
        //                            .build();
        //
        //            sqsClient.deleteMessage(deleteRequest);
        //        }

        return result;
    }
}

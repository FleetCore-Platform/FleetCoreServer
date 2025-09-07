package io.levysworks.Managers.SQS;

import io.levysworks.Configs.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@ApplicationScoped
public class QueueManager {
    private SqsClient sqsClient;

    @Inject ApplicationConfig config;

    @PostConstruct
    public void init() {
        sqsClient = SqsClient.builder().region(Region.of(config.region())).build();
    }

    @PreDestroy
    public void destroy() {
        sqsClient.close();
    }

    public List<Message> ingestQueue(String queueName) {
        GetQueueUrlRequest getQueueUrlRequest =
                GetQueueUrlRequest.builder().queueName(queueName).build();

        String queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();

        System.out.println("Got queue url: " + queueUrl);

        ReceiveMessageRequest receiveRequest =
                ReceiveMessageRequest.builder().queueUrl(queueUrl).build();

        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);

        List<Message> messages = receiveMessageResponse.messages();

        for (Message msg : messages) {
            DeleteMessageRequest deleteRequest =
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(msg.receiptHandle())
                            .build();

            sqsClient.deleteMessage(deleteRequest);
        }

        return messages;
    }
}

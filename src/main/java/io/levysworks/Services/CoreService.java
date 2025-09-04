package io.levysworks.Services;

import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Managers.Database.DatabaseManager;
import io.levysworks.Managers.IoTCore.IotDataPlaneManager;
import io.levysworks.Managers.IoTCore.IotManager;
import io.levysworks.Managers.S3.StorageManager;
import io.levysworks.Managers.SQS.QueueManager;
import io.levysworks.Models.IoTCertContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Startup
@ApplicationScoped
public class CoreService {
    @Inject
    IotManager iotManager;
    @Inject
    IotDataPlaneManager iotDataPlaneManager;
    @Inject
    DatabaseManager dbManager;
    @Inject
    QueueManager queueManager;
    @Inject
    StorageManager storageManager;
    @Inject
    ApplicationConfig applicationConfig;

    // TODO: Implement underlying methods for database operations
    public void registerNewDrone(String outpost, String group, String droneName, String address, String px4Version, String agentVersion) {
        IoTCertContainer certContainer = iotManager.generateCertificate();
        iotManager.createDevice(droneName, outpost, px4Version, agentVersion);
        iotManager.attachCertificate(droneName, certContainer.getCertificateARN());

        iotManager.addDeviceToGroup(droneName, group);

        String groupUUID = dbManager.getGroupUUID(droneName);
        dbManager.addDrone(droneName, groupUUID, address, agentVersion);
    }
}

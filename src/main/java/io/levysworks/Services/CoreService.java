package io.levysworks.Services;

import io.levysworks.Algorithms.PolygonCoverageAlgorithm;
import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Managers.Database.DatabaseManager;
import io.levysworks.Managers.IoTCore.IotDataPlaneManager;
import io.levysworks.Managers.IoTCore.IotManager;
import io.levysworks.Managers.S3.StorageManager;
import io.levysworks.Managers.SQS.QueueManager;
import io.levysworks.Mavlink.MissionFile;
import io.levysworks.Models.IoTCertContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.postgis.Geometry;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
    ApplicationConfig config;
    DatabaseManager databaseManager;

    // TODO: Implement underlying methods for database operations
    public void registerNewDrone(String outpost, String group, String droneName, String address, String px4Version, String agentVersion) {
        IoTCertContainer certContainer = iotManager.generateCertificate();
        iotManager.createDevice(droneName, outpost, px4Version, agentVersion);
        iotManager.attachCertificate(droneName, certContainer.getCertificateARN());

        iotManager.addDeviceToGroup(droneName, group);

        String groupUUID = dbManager.getGroupUUID(droneName);
        dbManager.addDrone(droneName, groupUUID, address, agentVersion);
    }

    public void createNewMission(String outpost, String group, String coordinatorUUID) throws IOException {
        String missionName = "mission-" + outpost + ":" + group + "-" + System.currentTimeMillis();

        Geometry area = dbManager.getOutpostGeometry(outpost);

        File missionBundle = PolygonCoverageAlgorithm.calculateSinge(area);
        String key = storageManager.uploadMissionBundle(missionName, missionBundle);
        String presignedUrl = storageManager.getPresignedObjectUrl(key);

        String groupARN = iotManager.getGroupARN(group);

        // Create 'Download-File' mission -> download mission file from url and execute mission from it
        iotManager.createIoTJob(missionName, groupARN,
                config.iot().newMissionJobArn(),
                Map.ofEntries(
                        Map.entry("downloadUrl", presignedUrl),
                        Map.entry("filePath", "/tmp/missions/")
                )
        );

        String groupUUID = databaseManager.getGroupUUID(group);
        String bundleUrl = storageManager.getInternalObjectUrl(key);

        databaseManager.createMission(groupUUID, missionName, bundleUrl, coordinatorUUID);
    }
}

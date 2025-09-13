package io.levysworks.Services;

import io.levysworks.Algorithms.PolygonCoverageAlgorithm;
import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Managers.Database.DatabaseManager;
import io.levysworks.Managers.Database.Mappers.CoordinatorMapper;
import io.levysworks.Managers.Database.Mappers.DroneMapper;
import io.levysworks.Managers.Database.Mappers.GroupMapper;
import io.levysworks.Managers.Database.Mappers.MissionMapper;
import io.levysworks.Managers.IoTCore.IotDataPlaneManager;
import io.levysworks.Managers.IoTCore.IotManager;
import io.levysworks.Managers.S3.StorageManager;
import io.levysworks.Models.IoTCertContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.postgis.Geometry;

@Startup
@ApplicationScoped
public class CoreService {
    @Inject IotManager iotManager;
    @Inject IotDataPlaneManager iotDataPlaneManager;
    @Inject DatabaseManager dbManager;
    @Inject StorageManager storageManager;
    @Inject ApplicationConfig config;

    @Inject DroneMapper droneMapper;
    @Inject GroupMapper groupMapper;
    @Inject MissionMapper missionMapper;
    @Inject CoordinatorMapper coordinatorMapper;

    DatabaseManager databaseManager;

    /**
     * Groups multiple manager operations to register a new drone in IoT Core, and RDS
     *
     * @param outpost Name of the outpost to create the drone in
     * @param group The name of the groups to create the drone in
     * @param droneName Desired name of the drone
     * @param address Public IP address of the drone
     * @param px4Version PX4 firmware version running on the pixhawk hardware
     * @param agentVersion Version of the OnboardAgent client
     */
    public IoTCertContainer registerNewDrone(
            String outpost,
            String group,
            String droneName,
            String address,
            String px4Version,
            String agentVersion) {
        UUID uuid = UUID.randomUUID();

        IoTCertContainer certContainer = iotManager.generateCertificate();
        iotManager.createDevice(droneName, outpost, px4Version, agentVersion);

        String policyName = iotManager.createPolicy(droneName);
        iotManager.attachPolicyToCertificate(certContainer.getCertificateARN(), policyName);

        iotManager.attachCertificate(droneName, certContainer.getCertificateARN());

        String groupARN = iotManager.getGroupARN(group);
        iotManager.addDeviceToGroup(droneName, groupARN);

        UUID groupUUID = groupMapper.findByName(group).getOutpost_uuid();
        Timestamp addedDate = new Timestamp(System.currentTimeMillis());
        droneMapper.insertDrone(uuid, droneName, groupUUID, address, agentVersion, addedDate);

        return certContainer;
    }

    /**
     * Coordinates multiple managers to create an IoT Core mission for a group of drones
     *
     * @param outpost The name of the outpost the groups is in
     * @param group Name of the group of drones
     * @param coordinatorUUID The UUID of the coordinator performing this operation
     * @throws IOException If there was an error generating the mission bundle
     */
    public void createNewMission(String outpost, String group, String coordinatorUUID)
            throws IOException {
        Timestamp startedAt = new Timestamp(System.currentTimeMillis());
        String missionName = "missions/" + outpost + "/" + group + "/mission-" + startedAt;

        Geometry area = dbManager.getOutpostGeometry(outpost);

        File missionBundle = PolygonCoverageAlgorithm.calculateSinge(area);
        String key = storageManager.uploadMissionBundle(missionName, missionBundle);
        String presignedUrl = storageManager.getPresignedObjectUrl(key);

        String groupARN = iotManager.getGroupARN(group);

        // Create 'Download-File' mission -> download mission file from url and execute mission from
        // it
        iotManager.createIoTJob(
                missionName,
                groupARN,
                config.iot().newMissionJobArn(),
                Map.ofEntries(
                        Map.entry("downloadUrl", presignedUrl),
                        Map.entry("filePath", "/tmp/missions/")));

        UUID groupUUID = groupMapper.findByName(group).getOutpost_uuid();
        String bundleUrl = storageManager.getInternalObjectUrl(key);

        missionMapper.insert(
                UUID.randomUUID(),
                groupUUID,
                missionName,
                bundleUrl,
                startedAt,
                UUID.fromString(coordinatorUUID));
    }
}

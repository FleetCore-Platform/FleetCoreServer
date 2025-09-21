package io.levysworks.Services;

import io.levysworks.Algorithms.PolygonCoverageAlgorithm;
import io.levysworks.Configs.ApplicationConfig;
import io.levysworks.Exceptions.GroupNotEmptyException;
import io.levysworks.Managers.Database.DatabaseManager;
import io.levysworks.Managers.Database.DbModels.DbDrone;
import io.levysworks.Managers.Database.DbModels.DbGroup;
import io.levysworks.Managers.Database.DbModels.DbOutpost;
import io.levysworks.Managers.Database.Mappers.*;
import io.levysworks.Managers.IoTCore.IotDataPlaneManager;
import io.levysworks.Managers.IoTCore.IotManager;
import io.levysworks.Managers.S3.StorageManager;
import io.levysworks.Models.DroneRequestModel;
import io.levysworks.Models.IoTCertContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
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
    @Inject OutpostMapper outpostMapper;
    @Inject MissionMapper missionMapper;
    @Inject CoordinatorMapper coordinatorMapper;

    /**
     * Groups multiple manager operations to register a new drone in IoT Core, and RDS
     *
     * @param group The name of the groups to create the drone in
     * @param droneName Desired name of the drone
     * @param address Public IP address of the drone
     * @param px4Version PX4 firmware version running on the pixhawk hardware
     * @param agentVersion Version of the OnboardAgent client
     */
    public IoTCertContainer registerNewDrone(
            String group, String droneName, String address, String px4Version, String agentVersion)
            throws NotFoundException {
        DbGroup dbGroup = groupMapper.findByName(group);

        if (dbGroup == null) {
            throw new NotFoundException("Group not found with name " + group);
        }

        UUID uuid = UUID.randomUUID();

        IoTCertContainer certContainer = iotManager.generateCertificate();
        iotManager.createThing(droneName, px4Version, agentVersion);

        String policyName = iotManager.createPolicy(droneName);
        iotManager.attachPolicyToCertificate(certContainer.getCertificateARN(), policyName);

        iotManager.attachCertificate(droneName, certContainer.getCertificateARN());

        String groupARN = iotManager.getGroupARN(group);
        iotManager.addDeviceToGroup(droneName, groupARN);

        UUID groupUUID = dbGroup.getUuid();
        Timestamp addedDate = new Timestamp(System.currentTimeMillis());
        droneMapper.insertDrone(uuid, droneName, groupUUID, address, agentVersion, addedDate);

        return certContainer;
    }

    public void updateDrone(UUID drone_uuid, DroneRequestModel data) throws NotFoundException {
        DbDrone drone = droneMapper.findByUuid(drone_uuid);
        if (drone == null) {
            throw new NotFoundException("Drone not found with UUID " + drone_uuid);
        }

        DbGroup currentGroup = groupMapper.findByUuid(drone.getGroup_uuid());

        String currentGroupARN = iotManager.getGroupARN(currentGroup.getName());
        String newGroupARN = iotManager.getGroupARN(data.groupName());

        if (data.address() != null) {
            drone.setAddress(data.address());
        }
        if (data.droneName() != null) {
            drone.setName(data.droneName());
        }
        if (data.agentVersion() != null) {
            drone.setManager_version(data.agentVersion());
        }
        if (data.groupName() != null) {
            DbGroup dbGroup = groupMapper.findByName(data.groupName());
            if (dbGroup == null) {
                throw new NotFoundException("Group not found with name " + data.groupName());
            }
            drone.setGroup_uuid(dbGroup.getUuid());
        }

        droneMapper.updateDrone(drone_uuid, drone);

        iotManager.removeThingFromGroup(drone.getName(), currentGroupARN);
        iotManager.addDeviceToGroup(drone.getName(), newGroupARN);

    }

    public void removeDrone(String droneName) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByName(droneName);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with name " + droneName);
        }

        UUID droneUUID = dbDrone.getUuid();

        iotManager.detachCertificates(droneName);

        String groupARN = iotManager.getThingGroup(droneName);

        if (groupARN != null) {
            iotManager.removeThingFromGroup(droneName, groupARN);
        } else {
            System.err.printf("%s not in a group!", droneName);
        }

        iotManager.removeThing(droneName);
        droneMapper.deleteDrone(droneUUID);
    }

    public void removeDroneFromGroup(UUID droneUUID) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUUID);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUUID);
        }
        DbGroup dbGroup = groupMapper.findByUuid(dbDrone.getGroup_uuid());
        if (dbGroup == null) {
            return;
        }

        String groupARN = iotManager.getGroupARN(dbGroup.getName());
        iotManager.removeThingFromGroup(dbDrone.getName(), groupARN);

        droneMapper.ungroupDrone(droneUUID);
    }

    public void addDroneToGroup(UUID droneUUID, UUID groupUUID) throws NotFoundException {
        DbDrone dbDrone = droneMapper.findByUuid(droneUUID);
        if (dbDrone == null) {
            throw new NotFoundException("Drone not found with UUID " + droneUUID);
        }
        DbGroup dbGroup = groupMapper.findByUuid(groupUUID);
        if  (dbGroup == null) {
            throw new NotFoundException("Group not found with UUID " + groupUUID);
        }

        String groupARN = iotManager.getGroupARN(dbGroup.getName());

        droneMapper.addToGroup(droneUUID, dbGroup.getUuid());
        iotManager.addDeviceToGroup(dbDrone.getName(), groupARN);
    }

    public void createNewGroup(String groupName, String outpostUuid) throws NotFoundException {
        UUID groupUUID = UUID.randomUUID();
        DbOutpost dbOutpost = outpostMapper.findByUuid(UUID.fromString(outpostUuid));

        if (dbOutpost == null) {
            throw new NotFoundException("Outpost not found with UUID " + outpostUuid);
        }

        String outpostName = dbOutpost.getName();

        Timestamp createdDate = new Timestamp(System.currentTimeMillis());

        iotManager.createDeviceGroup(groupName, outpostName);
        groupMapper.insert(groupUUID, dbOutpost.getUuid(), groupName, createdDate);
    }

    public void tryDeleteGroup(String groupName) throws GroupNotEmptyException {
        DbGroup group = groupMapper.findByName(groupName);
        if (group == null) {
            throw new NotFoundException("Group not found with name " + groupName);
        }

         if (!droneMapper.listDronesByGroupUuid(group.getUuid(), 1).isEmpty()) {
            throw new GroupNotEmptyException("Group " + groupName + " is not empty");
         };
         iotManager.removeDeviceGroup(groupName);
         groupMapper.deleteByName(groupName);
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

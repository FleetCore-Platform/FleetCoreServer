package io.levysworks.Managers.Database;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import javax.sql.DataSource;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

@ApplicationScoped
public class DatabaseManager {
    @Inject DataSource dataSource;

    private Connection conn;

    @PostConstruct
    public void init() throws SQLException {
        conn = dataSource.getConnection();

        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM outposts")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String wkt = rs.getString("area");
                Geometry geometry = PGgeometry.geomFromString(wkt);
                System.out.println(geometry.toString());
            }
        }
    }

    // TODO: Implement database insert methods
    public String createOutpost(
            String name, Long latitude, Long longitude, Geometry area, String coordinatorUUID) {
        return "";
    }

    public String createGroup(String outpostUUID, String name) {
        return "";
    }

    public String addDrone(String name, String groupUUID, String address, String softwareVersion) {
        return "";
    }

    public String addCoordinator(
            String cognitoSub, String firstName, String lastName, String email) {
        return "";
    }

    public String createMission(
            String groupUUID, String name, String bundleUrl, String coordinatorUUID) {
        return "";
    }

    public void createMaintenance(
            String droneUUID,
            String coordinatorUUID,
            String type,
            String description,
            Timestamp performedAt) {}

    public void createLogFile(String droneUUID, String coordinatorUUID) {}

    // TODO: Implement database query methods
    public String getGroupUUID(String groupName) {
        return "";
    }

    public Geometry getOutpostGeometry(String outpost) {
        return null;
    }
}

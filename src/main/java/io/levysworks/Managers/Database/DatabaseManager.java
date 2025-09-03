package io.levysworks.Managers.Database;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

import javax.sql.DataSource;
import java.sql.*;

@Startup
@ApplicationScoped
public class DatabaseManager {
    @Inject
    DataSource dataSource;

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
}

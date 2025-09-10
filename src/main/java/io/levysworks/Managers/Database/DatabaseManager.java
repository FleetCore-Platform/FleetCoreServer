package io.levysworks.Managers.Database;

import io.levysworks.Managers.Database.DbModels.DbOutpost;
import io.levysworks.Managers.Database.Mappers.OutpostMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import java.util.UUID;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

@ApplicationScoped
public class DatabaseManager {
    @Inject OutpostMapper outpostMapper;

    public Geometry getOutpostGeometry(String name) {
        DbOutpost row =
                outpostMapper.findByName(name);
        if (row == null) return null;

        String wkt = row.getArea();

        try {
            return PGgeometry.geomFromString(wkt);
        } catch (SQLException e) {
            System.err.println("Error parsing geometry: " + wkt);
        }

        return null;
    }
}

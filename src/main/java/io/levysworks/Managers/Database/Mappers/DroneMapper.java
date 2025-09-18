package io.levysworks.Managers.Database.Mappers;

import io.levysworks.Managers.Database.DbModels.DbDrone;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DroneMapper {
    @Select("SELECT * FROM drones WHERE uuid = #{uuid}")
    DbDrone findByUuid(@Param("uuid") UUID uuid);

    @Select("SELECT * FROM drones LIMIT ${limit}")
    List<DbDrone> listDrones(@Param("limit") int limit);

    @Select("SELECT * FROM drones WHERE group_uuid = #{group_uuid} LIMIT ${limit}")
    List<DbDrone> listDronesByGroupUuid(
            @Param("group_uuid") UUID group_uuid, @Param("limit") int limit);

    @Select("SELECT * FROM drones WHERE name = #{name}")
    DbDrone findByName(@Param("name") String name);

    @Insert(
            "INSERT INTO drones (uuid, name, group_uuid, address, manager_version,"
                    + " first_discovered) VALUES (#{uuid}, #{name}, #{groupUuid}, #{address},"
                    + " #{managerVersion}, #{firstDiscovered})")
    void insertDrone(
            @Param("uuid") UUID uuid,
            @Param("name") String name,
            @Param("groupUuid") UUID groupUuid,
            @Param("address") String address,
            @Param("managerVersion") String managerVersion,
            @Param("firstDiscovered") Timestamp firstDiscovered);

    @Delete("DELETE FROM drones WHERE uuid = #{uuid}")
    void deleteDrone(UUID uuid);
}

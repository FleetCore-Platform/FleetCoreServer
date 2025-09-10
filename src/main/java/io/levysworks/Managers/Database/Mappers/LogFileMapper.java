package io.levysworks.Managers.Database.Mappers;

import io.levysworks.Managers.Database.DbModels.DbLogFile;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LogFileMapper {
    @Insert(
            "INSERT INTO log_files (uuid, drone_uuid, created_at, archived, archived_date) VALUES"
                    + " (#{uuid}, #{drone_uuid}, #{created_at}, #{archived}, #{archived_date})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("drone_uuid") UUID drone_uuid,
            @Param("created_at") Timestamp created_at,
            @Param("archived") Boolean archived,
            @Param("archived_date") Timestamp archived_date);

    @Select("SELECT * FROM log_files WHERE uuid = #{uuid}")
    DbLogFile findById(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE log_files SET drone_uuid = #{drone_uuid}, created_at = #{created_at}, archived"
                    + " = #{archived}, archived_date = #{archived_date} WHERE uuid = #{uuid}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("drone_uuid") UUID drone_uuid,
            @Param("created_at") Timestamp created_at,
            @Param("archived") Boolean archived,
            @Param("archived_date") Timestamp archived_date);

    @Delete("DELETE FROM log_files WHERE uuid = #{uuid}")
    void delete(@Param("uuid") UUID uuid);
}

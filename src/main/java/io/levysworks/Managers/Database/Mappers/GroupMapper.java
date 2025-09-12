package io.levysworks.Managers.Database.Mappers;

import io.levysworks.Managers.Database.DbModels.DbGroup;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GroupMapper {
    @Insert(
            "INSERT INTO groups (uuid, outpost_uuid, name, created_at) VALUES (#{uuid},"
                    + " #{outpost_uuid}, #{name}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("outpost_uuid") UUID outpost_uuid,
            @Param("name") String name,
            @Param("created_at") Timestamp created_at);

    @Select("SELECT * FROM groups WHERE uuid = #{uuid}")
    DbGroup findByUuid(@Param("uuid") UUID uuid);

    @Select("SELECT * FROM groups WHERE name = #{name}")
    DbGroup findByName(@Param("name") String name);

    @Update(
            "UPDATE groups SET outpost_uuid = #{outpost_uuid}, name = #{name}, created_at ="
                    + " #{created_at} WHERE uuid = #{uuid}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("outpost_uuid") UUID outpost_uuid,
            @Param("name") String name,
            @Param("created_at") Timestamp created_at);

    @Delete("DELETE FROM groups WHERE uuid = #{uuid}")
    void deleteByUuid(@Param("uuid") UUID uuid);

    @Delete("DELETE FROM groupds WHERE name = #{name}")
    void deleteByName(@Param("name") String name);
}

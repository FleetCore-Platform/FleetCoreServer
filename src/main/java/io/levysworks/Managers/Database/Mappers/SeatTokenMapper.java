package io.levysworks.Managers.Database.Mappers;

import io.levysworks.Managers.Database.DbModels.DbSeatToken;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SeatTokenMapper {
    @Insert(
            "INSERT INTO seat_tokens (uuid, created_by, \"group\", created_at) VALUES (#{uuid},"
                    + " #{created_by}, #{group}, #{created_at})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("created_by") UUID created_by,
            @Param("group") UUID group,
            @Param("created_at") Timestamp created_at);

    @Select("SELECT * FROM seat_tokens WHERE uuid = #{uuid}")
    DbSeatToken findByUuid(UUID uuid);

    @Update(
            "UPDATE seat_tokens SET created_by = #{created_by}, \"group\" = #{group}, created_at ="
                    + " #{created_at} WHERE uuid = #{uuid}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("created_by") UUID created_by,
            @Param("group") UUID group,
            @Param("created_at") Timestamp created_at);

    @Delete("DELETE FROM seat_tokens WHERE uuid = #{uuid}")
    void delete(@Param("uuid") UUID uuid);
}

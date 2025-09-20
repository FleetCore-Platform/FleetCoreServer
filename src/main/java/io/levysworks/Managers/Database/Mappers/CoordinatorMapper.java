package io.levysworks.Managers.Database.Mappers;

import io.levysworks.Managers.Database.DbModels.DbCoordinator;
import java.sql.Timestamp;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CoordinatorMapper {
    @Insert(
            "INSERT INTO coordinators (uuid, cognito_sub, first_name, last_name, email,"
                    + " registration_date) VALUES (#{uuid, jdbcType=OTHER}, #{cognito_sub},"
                    + " #{first_name}, #{last_name}, #{email}, #{registration_date})")
    void insert(
            @Param("uuid") UUID uuid,
            @Param("cognito_sub") String cognito_sub,
            @Param("first_name") String first_name,
            @Param("last_name") String last_name,
            @Param("email") String email,
            @Param("registration_date") Timestamp registration_date);

    @Select("SELECT * FROM coordinators WHERE uuid = #{uuid, jdbcType=OTHER}")
    DbCoordinator findByUuid(@Param("uuid") UUID uuid);

    @Update(
            "UPDATE coordinators SET cognito_sub = #{cognito_sub}, first_name = #{first_name},"
                    + " last_name = #{last_name}, email = #{email}, registration_date ="
                    + " #{registration_date} WHERE uuid = #{uuid, jdbcType=OTHER}")
    void update(
            @Param("uuid") UUID uuid,
            @Param("cognito_sub") String cognito_sub,
            @Param("first_name") String first_name,
            @Param("last_name") String last_name,
            @Param("email") String email,
            @Param("registration_date") Timestamp registration_date);

    @Delete("DELETE FROM coordinators WHERE uuid = #{uuid, jdbcType=OTHER}")
    void delete(@Param("uuid") UUID uuid);
}

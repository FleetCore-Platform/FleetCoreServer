package io.levysworks.Endpoints;

import io.levysworks.Managers.Database.DbModels.DbGroup;
import io.levysworks.Managers.Database.Mappers.GroupMapper;
import io.levysworks.Managers.IoTCore.IotManager;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/api/v1/groups/")
public class GroupsEndpoint {
    @Inject GroupMapper groupMapper;
    @Inject IotManager iotManager;
    Logger logger = Logger.getLogger(GroupsEndpoint.class.getName());

    @GET
    @Path("/list/{outpost_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGroups(@PathParam("outpost_uuid") String outpost_uuid) {
        if (outpost_uuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            UUID uuid = UUID.fromString(outpost_uuid);

            List<DbGroup> groups = groupMapper.listGroupsByOutpostUuid(uuid);
            if (groups == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(groups).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

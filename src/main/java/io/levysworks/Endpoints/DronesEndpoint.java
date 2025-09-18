package io.levysworks.Endpoints;

import io.levysworks.Managers.Database.DbModels.DbDrone;
import io.levysworks.Managers.Database.Mappers.DroneMapper;
import io.levysworks.Managers.IoTCore.IotManager;
import io.levysworks.Models.IoTCertContainer;
import io.levysworks.Models.RegisterDroneRequestModel;
import io.levysworks.Services.CoreService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/api/v1/drones/")
public class DronesEndpoint {
    @Inject CoreService coreService;
    @Inject DroneMapper droneMapper;

    Logger logger = Logger.getLogger(DronesEndpoint.class.getName());

    @GET
    @Path("/list/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGroupDrones(@DefaultValue("10") @QueryParam("limit") int limit, @PathParam("group_uuid") String group_uuid) {

        if (limit <= 0 || limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UUID uuid = UUID.fromString(group_uuid);
            List<DbDrone> drones = droneMapper.listDronesByGroupUuid(uuid, limit);
            return Response.ok(drones).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/register/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerDrone(RegisterDroneRequestModel body) {
        if (body == null || body.groupName() == null || body.droneName() == null || body.address() == null || body.px4Version() == null || body.agentVersion() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            IoTCertContainer certs = coreService.registerNewDrone(body.groupName(), body.droneName(), body.address(), body.px4Version(), body.agentVersion());

            return Response.ok(certs).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage()).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/delete/{drone_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDrone(@PathParam("drone_name") String droneName) {
        if (droneName == null || droneName.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.removeDrone(droneName);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage()).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

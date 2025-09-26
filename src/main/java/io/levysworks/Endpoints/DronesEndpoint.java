package io.levysworks.Endpoints;

import io.levysworks.Managers.Database.DbModels.DbDrone;
import io.levysworks.Managers.Database.Mappers.DroneMapper;
import io.levysworks.Models.DroneRequestModel;
import io.levysworks.Models.IoTCertContainer;
import io.levysworks.Models.SetDroneGroupRequestModel;
import io.levysworks.Services.CoreService;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/drones/")
@RolesAllowed("${allowed.role-name}")
public class DronesEndpoint {
    @Inject CoreService coreService;
    @Inject DroneMapper droneMapper;
    Logger logger = Logger.getLogger(DronesEndpoint.class.getName());

    @GET
    @Path("/list/{group_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response listGroupDrones(
            @DefaultValue("10") @QueryParam("limit") int limit,
            @PathParam("group_uuid") String group_uuid) {

        if (limit <= 0 || limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UUID uuid = UUID.fromString(group_uuid);
            List<DbDrone> drones = droneMapper.listDronesByGroupUuid(uuid, limit);
            return Response.ok(drones).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{drone_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 25, window = 1, windowUnit = ChronoUnit.SECONDS)
    public Response getDrone(@PathParam("drone_uuid") UUID drone_uuid) {
        if (drone_uuid == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbDrone drone = droneMapper.findByUuid(drone_uuid);
        if (drone == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(drone).build();
    }

    @POST
    @Path("/register/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response registerDrone(DroneRequestModel body) {
        if (body == null
                || body.groupName() == null
                || body.droneName() == null
                || body.address() == null
                || body.px4Version() == null
                || body.agentVersion() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            IoTCertContainer certs =
                    coreService.registerNewDrone(
                            body.groupName(),
                            body.droneName(),
                            body.address(),
                            body.px4Version(),
                            body.agentVersion());

            return Response.ok(certs).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/update/{drone_uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response updateDrone(
            @PathParam("drone_uuid") String drone_uuid, DroneRequestModel body) {
        if (drone_uuid == null || drone_uuid.isEmpty() || body == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbDrone droneCheck = droneMapper.findByUuid(UUID.fromString(drone_uuid));
        if (droneCheck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            UUID uuid = UUID.fromString(drone_uuid);
            coreService.updateDrone(uuid, body);

            return Response.ok().build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/delete/{drone_name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 3, window = 1, windowUnit = ChronoUnit.MINUTES)
    public Response deleteDrone(@PathParam("drone_name") String droneName) {
        if (droneName == null || droneName.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.removeDrone(droneName);

            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{drone_uuid}/ungroup/")
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response ungroupDrone(@PathParam("drone_uuid") String drone_uuid) {
        if (drone_uuid == null || drone_uuid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UUID uuid = UUID.fromString(drone_uuid);
            coreService.removeDroneFromGroup(uuid);

            return Response.noContent().build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PATCH
    @Path("/{drone_uuid}/group/")
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response ungroupDrone(
            @PathParam("drone_uuid") String drone_uuid, SetDroneGroupRequestModel body) {
        if (drone_uuid == null || body.group_uuid() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            UUID uuid = UUID.fromString(drone_uuid);
            UUID group_uuid = UUID.fromString(body.group_uuid());

            coreService.addDroneToGroup(uuid, group_uuid);

            return Response.noContent().build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

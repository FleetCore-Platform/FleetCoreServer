package io.levysworks.Endpoints;

import io.levysworks.Managers.Database.DbModels.DbDrone;
import io.levysworks.Managers.Database.Mappers.DroneMapper;
import io.levysworks.Models.DroneRequestModel;
import io.levysworks.Models.IoTCertContainer;
import io.levysworks.Models.SetDroneGroupRequestModel;
import io.levysworks.Services.CoreService;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.NoCache;

@NoCache
@Path("/api/v1/drones/")
// @RolesAllowed("${allowed.role-name}")
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
            @PathParam("group_uuid") UUID group_uuid) {

        if (limit <= 0 || limit > 1000) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            List<DbDrone> drones = droneMapper.listDronesByGroupUuid(group_uuid, limit);
            if (drones == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(drones).build();
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
        try {
            DbDrone drone = droneMapper.findByUuid(drone_uuid);
            if (drone == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(drone).build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
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
    public Response updateDrone(@PathParam("drone_uuid") UUID drone_uuid, DroneRequestModel body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DbDrone droneCheck = droneMapper.findByUuid(drone_uuid);
        if (droneCheck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            coreService.updateDrone(drone_uuid, body);

            return Response.ok().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RateLimit(value = 10, window = 5, windowUnit = ChronoUnit.SECONDS)
    public Response ungroupDrone(@PathParam("drone_uuid") UUID drone_uuid) {
        try {
            coreService.removeDroneFromGroup(drone_uuid);

            return Response.noContent().build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ungroupDrone(
            @PathParam("drone_uuid") UUID drone_uuid, SetDroneGroupRequestModel body) {
        if (body == null || body.group_uuid() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            coreService.addDroneToGroup(drone_uuid, body.group_uuid());

            return Response.noContent().build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode(), nfe.getMessage())
                    .build();
        } catch (Exception e) {
            logger.severe(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

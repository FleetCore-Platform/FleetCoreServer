package io.levysworks.Endpoints;

import io.levysworks.Managers.SQS.QueueManager;
import io.levysworks.Services.CoreService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.iot.model.JobExecutionStatus;

import java.util.UUID;

@Path("/api/v1/missions")
//@RolesAllowed("${allowed.role-name}")
public class MissionsEndpoint {
    @Inject CoreService coreService;

    @GET
    @Path("/status/{drone_uuid}")
    public Response getMissionStatus(@PathParam("drone_uuid") UUID droneUUID) throws NotFoundException {
        try {
            JobExecutionStatus status = coreService.getMissionStatus(droneUUID);

            if (status == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            return Response.ok(status).build();

        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}

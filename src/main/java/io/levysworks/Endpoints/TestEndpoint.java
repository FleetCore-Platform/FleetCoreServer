package io.levysworks.Endpoints;

import io.levysworks.Managers.IoTManager;
import io.levysworks.Models.IoTCertContainer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/")
public class TestEndpoint {
    @Inject
    IoTManager ioTManager;

    @GET
    @Path("/describe/{device_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response describeThing(@PathParam("device_name") String deviceName) {
        ioTManager.describeDevice(deviceName);
        return Response.ok(deviceName).build();
    }

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createThing(@RestQuery String deviceName) {
        ioTManager.createDevice(deviceName);
        return Response.ok(deviceName).build();
    }

    @DELETE
    @Path("/delete/{device_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteThing(@PathParam("device_name") String deviceName) {
        ioTManager.deleteDevice(deviceName);
        return Response.ok(deviceName).build();
    }

    @PATCH
    @Path("/addcert/{device_name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addCert(@PathParam("device_name") String deviceName) {
        IoTCertContainer certs = ioTManager.generateCertificate();

        ioTManager.attachCertificate(deviceName, certs.getCertificateARN());

        return Response.ok(certs.getCertificatePEM() + " " + deviceName).build();
    }

    @POST
    @Path("/groups/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(@RestQuery String groupName) {
        ioTManager.createDeviceGroup(groupName);
        return Response.ok(groupName).build();
    }
}

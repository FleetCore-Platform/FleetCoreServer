package io.levysworks.Models;

public record DroneRequestModel(
        String groupName,
        String droneName,
        String address,
        String px4Version,
        String agentVersion) {}

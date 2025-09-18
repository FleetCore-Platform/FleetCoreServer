package io.levysworks.Models;

public record RegisterDroneRequestModel(String groupName, String droneName, String address, String px4Version, String agentVersion) {}

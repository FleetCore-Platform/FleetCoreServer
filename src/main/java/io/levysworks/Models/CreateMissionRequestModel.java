package io.levysworks.Models;

import java.util.UUID;

public record CreateMissionRequestModel(String outpost, String group, UUID coordinatorUUID) {}

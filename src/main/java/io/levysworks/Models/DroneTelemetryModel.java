package io.levysworks.Models;

public class DroneTelemetryModel {
    Position position;
    Battery battery;
    Health health;
    boolean in_air;

    static class Position {
        Float latitude_deg;
        Float longitude_deg;
        Float absolute_altitude_m;
        Float relative_altitude_m;
    }

    static class Battery {
        Float temperature_degc;
        Float voltage_v;
        Float current_battery_a;
        Float capacity_consumed_ah;
        Float remaining_percent;
    }

    static class Health {
        Boolean is_gyrometer_calibration_ok;
        Boolean is_accelerometer_calibration_ok;
        Boolean is_magnetometer_calibration_ok;
        Boolean is_local_position_ok;
        Boolean is_global_position_ok;
        Boolean is_home_position_ok;
        Boolean is_armable;
    }
}

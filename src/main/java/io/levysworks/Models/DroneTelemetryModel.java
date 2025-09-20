package io.levysworks.Models;

public record DroneTelemetryModel(
        Position position, Battery battery, Health health, boolean in_air) {
    public record Position(
            Double latitude_deg,
            Double longitude_deg,
            Double absolute_altitude_m,
            Double relative_altitude_m) {}

    public record Battery(
            Double temperature_degc,
            Double voltage_v,
            Double current_battery_a,
            Double capacity_consumed_ah,
            Double remaining_percent) {}

    public record Health(
            Boolean is_gyrometer_calibration_ok,
            Boolean is_accelerometer_calibration_ok,
            Boolean is_magnetometer_calibration_ok,
            Boolean is_local_position_ok,
            Boolean is_global_position_ok,
            Boolean is_home_position_ok,
            Boolean is_armable) {}
}

// public class DroneTelemetryModel {
//    public Position position;
//    public Battery battery;
//    public Health health;
//    public boolean in_air;
//
//    public static class Position {
//        public Double latitude_deg;
//        public Double longitude_deg;
//        public Double absolute_altitude_m;
//        public Double relative_altitude_m;
//    }
//
//    public static class Battery {
//        public Double temperature_degc;
//        public Double voltage_v;
//        public Double current_battery_a;
//        public Double capacity_consumed_ah;
//        public Double remaining_percent;
//    }
//
//    public static class Health {
//        public Boolean is_gyrometer_calibration_ok;
//        public Boolean is_accelerometer_calibration_ok;
//        public Boolean is_magnetometer_calibration_ok;
//        public Boolean is_local_position_ok;
//        public Boolean is_global_position_ok;
//        public Boolean is_home_position_ok;
//        public Boolean is_armable;
//    }
// }

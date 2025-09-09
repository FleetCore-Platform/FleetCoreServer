package io.levysworks.Models;

public class DroneTelemetryModel {
    public Position position;
    public Battery battery;
    public Health health;
    public boolean in_air;

    public static class Position {
        public Double latitude_deg;
        public Double longitude_deg;
        public Double absolute_altitude_m;
        public Double relative_altitude_m;
    }

    public static class Battery {
        public Double temperature_degc;
        public Double voltage_v;
        public Double current_battery_a;
        public Double capacity_consumed_ah;
        public Double remaining_percent;
    }

    public static class Health {
        public Boolean is_gyrometer_calibration_ok;
        public Boolean is_accelerometer_calibration_ok;
        public Boolean is_magnetometer_calibration_ok;
        public Boolean is_local_position_ok;
        public Boolean is_global_position_ok;
        public Boolean is_home_position_ok;
        public Boolean is_armable;
    }
}

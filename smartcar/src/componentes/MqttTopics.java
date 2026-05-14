package componentes;

public class MqttTopics {
    public static final String DEFAULT_TOPIC_BASE = "es/upv/pros/tatami/smartcities/traffic/PTPaterna";

    public static String normalizeBase(String topicBase) {
        if (topicBase == null || topicBase.trim().isEmpty()) {
            return DEFAULT_TOPIC_BASE;
        }
        String base = topicBase.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    public static String roadInfo(String topicBase, String roadSegment) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/info";
    }

    public static String roadAlerts(String topicBase, String roadSegment) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/alerts";
    }

    public static String roadTraffic(String topicBase, String roadSegment) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/traffic";
    }

    public static String allRoadTraffic(String topicBase) {
        return normalizeBase(topicBase) + "/road/+/traffic";
    }

    public static String roadParkingStatus(String topicBase, String roadSegment, String parkingId) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/parking/status/" + parkingId;
    }

    public static String allParkingStatus(String topicBase) {
        return normalizeBase(topicBase) + "/road/+/parking/status/+";
    }

    public static String roadParkingStatusWildcard(String topicBase, String roadSegment) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/parking/status/+";
    }

    public static String roadParkingCommand(String topicBase, String roadSegment, String parkingId) {
        return normalizeBase(topicBase) + "/road/" + roadSegment + "/parking/command/" + parkingId;
    }

    public static String parkingQuery(String topicBase) {
        return normalizeBase(topicBase) + "/parking/query";
    }

    public static String parkingResponse(String topicBase, String smartCarId) {
        return normalizeBase(topicBase) + "/vehicle/" + smartCarId + "/parking/response";
    }

    public static String parkingOccupy(String topicBase) {
        return normalizeBase(topicBase) + "/parking/occupy";
    }

    public static String parkingOccupyResponse(String topicBase, String smartCarId) {
        return normalizeBase(topicBase) + "/vehicle/" + smartCarId + "/parking/occupy/response";
    }
}

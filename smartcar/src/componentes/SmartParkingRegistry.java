package componentes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONArray;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartParkingRegistry extends MyMqttClient {
    private String topicBase;
    private Map<String, ParkingSpot> spots = new ConcurrentHashMap<String, ParkingSpot>();
    private Map<String, RoadPlace> vehiclePositions = new ConcurrentHashMap<String, RoadPlace>();

    public SmartParkingRegistry(String clientId, String brokerURL, String topicBase) {
        super(clientId, null, brokerURL);
        this.topicBase = MqttTopics.normalizeBase(topicBase);
    }

    public void start() {
        connect();
        subscribe(MqttTopics.allParkingStatus(topicBase));
        subscribe(MqttTopics.parkingQuery(topicBase));
        subscribe(MqttTopics.parkingOccupy(topicBase));
        // Interacción con STS: escucha ubicaciones de vehículos en el canal traffic.
        subscribe(MqttTopics.allRoadTraffic(topicBase));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);
        String payload = new String(message.getPayload());
        JSONObject envelope = new JSONObject(payload);
        String type = envelope.optString("type");

        if ("PARKING_STATUS".equals(type)) {
            ParkingSpot spot = ParkingSpot.fromJson(envelope);
            spots.put(spot.getId(), spot);
            MySimpleLogger.info(this.clientId, "Parking updated: " + spot.getId() + " available=" + spot.isAvailable() + " occupiedBy=" + spot.getOccupiedBy());
            return;
        }

        if ("PARKING_QUERY".equals(type)) {
            answerQuery(envelope);
            return;
        }

        if ("PARKING_OCCUPY_REQUEST".equals(type)) {
            handleOccupyRequest(envelope);
            return;
        }

        if ("TRAFFIC".equals(type)) {
            handleTrafficMessage(envelope);
        }
    }

    private void handleTrafficMessage(JSONObject envelope) {
        try {
            JSONObject msg = envelope.getJSONObject("msg");
            String action = msg.optString("action");
            String vehicleId = msg.optString("vehicle-id");
            String roadSegment = msg.optString("road-segment", msg.optString("road"));
            int position = msg.optInt("position", msg.optInt("kp", 0));

            if (vehicleId == null || vehicleId.trim().isEmpty()) return;

            if ("VEHICLE_OUT".equalsIgnoreCase(action)) {
                vehiclePositions.remove(vehicleId);
                MySimpleLogger.info(this.clientId, "Vehicle out: " + vehicleId);
            } else {
                vehiclePositions.put(vehicleId, new RoadPlace(roadSegment, position));
                MySimpleLogger.info(this.clientId, "Vehicle position updated: " + vehicleId + " " + roadSegment + " kp=" + position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerQuery(JSONObject envelope) {
        try {
            JSONObject msg = envelope.getJSONObject("msg");
            String vehicleId = msg.getString("vehicle-id");
            String roadSegment = msg.optString("road-segment", "");
            int position = msg.optInt("position", 0);
            int limit = msg.optInt("limit", 5);
            boolean onlyAvailable = msg.optBoolean("only-available", true);
            String mode = msg.optString("mode", "NEAREST");
            String replyTo = msg.optString("reply-to", MqttTopics.parkingResponse(topicBase, vehicleId));

            // Si el coche no manda ubicación en la consulta, usamos la última recibida por road/{id}/traffic.
            if ((roadSegment == null || roadSegment.isEmpty()) && vehiclePositions.containsKey(vehicleId)) {
                RoadPlace place = vehiclePositions.get(vehicleId);
                roadSegment = place.getRoad();
                position = place.getKm();
            }

            List<ParkingSpot> candidates = new ArrayList<ParkingSpot>();
            for (ParkingSpot spot : spots.values()) {
                if (onlyAvailable && !spot.isAvailable()) continue;
                if ("ROAD_ONLY".equals(mode) && !spot.getRoadSegment().equalsIgnoreCase(roadSegment)) continue;
                candidates.add(spot);
            }

            final String compareRoadSegment = roadSegment;
            final int comparePosition = position;
            candidates.sort(new Comparator<ParkingSpot>() {
                @Override
                public int compare(ParkingSpot a, ParkingSpot b) {
                    return Integer.compare(a.distanceTo(compareRoadSegment, comparePosition), b.distanceTo(compareRoadSegment, comparePosition));
                }
            });

            JSONArray arr = new JSONArray();
            for (int i = 0; i < candidates.size() && i < limit; i++) {
                ParkingSpot spot = candidates.get(i);
                JSONObject item = spot.toJson();
                item.put("estimated-distance", spot.distanceTo(roadSegment, position));
                arr.put(item);
            }

            JSONObject responseMsg = new JSONObject();
            responseMsg.put("vehicle-id", vehicleId);
            responseMsg.put("request-road-segment", roadSegment);
            responseMsg.put("request-position", position);
            responseMsg.put("mode", mode);
            responseMsg.put("available-count", arr.length());
            responseMsg.put("parkings", arr);

            JSONObject response = new JSONObject();
            response.put("id", "MSG_" + System.currentTimeMillis());
            response.put("type", "PARKING_RESPONSE");
            response.put("timestamp", System.currentTimeMillis());
            response.put("msg", responseMsg);

            publishJson(replyTo, response, false);
            MySimpleLogger.info(this.clientId, "Answering parking query to " + replyTo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleOccupyRequest(JSONObject envelope) {
        try {
            JSONObject msg = envelope.getJSONObject("msg");
            String vehicleId = msg.getString("vehicle-id");
            String parkingId = msg.getString("parking-id");
            String roadSegment = msg.optString("road-segment", "");
            String replyTo = msg.optString("reply-to", MqttTopics.parkingOccupyResponse(topicBase, vehicleId));

            ParkingSpot spot = spots.get(parkingId);
            if (spot == null) {
                publishOccupyResponse(replyTo, vehicleId, parkingId, false, "Parking not found", null);
                return;
            }

            if (!roadSegment.isEmpty() && !spot.getRoadSegment().equalsIgnoreCase(roadSegment)) {
                publishOccupyResponse(replyTo, vehicleId, parkingId, false, "Parking is not in requested road segment", spot);
                return;
            }

            if (!spot.isAvailable()) {
                publishOccupyResponse(replyTo, vehicleId, parkingId, false, "Parking is already occupied", spot);
                return;
            }

            // Reserva optimista para evitar que dos coches reciban aceptación a la vez.
            spot.ocuparParking(vehicleId);
            spots.put(parkingId, spot);

            JSONObject commandMsg = new JSONObject();
            commandMsg.put("action", "OCCUPY");
            commandMsg.put("parking-id", parkingId);
            commandMsg.put("vehicle-id", vehicleId);

            JSONObject command = new JSONObject();
            command.put("id", "MSG_" + System.currentTimeMillis());
            command.put("type", "PARKING_COMMAND");
            command.put("timestamp", System.currentTimeMillis());
            command.put("msg", commandMsg);

            String commandTopic = MqttTopics.roadParkingCommand(topicBase, spot.getRoadSegment(), parkingId);
            publishJson(commandTopic, command, false);

            publishOccupyResponse(replyTo, vehicleId, parkingId, true, "Parking occupied", spot);
            MySimpleLogger.info(this.clientId, "Occupy accepted: " + parkingId + " by " + vehicleId + ". Command sent to " + commandTopic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void publishOccupyResponse(String replyTo, String vehicleId, String parkingId, boolean accepted, String reason, ParkingSpot spot) {
        try {
            JSONObject responseMsg = new JSONObject();
            responseMsg.put("vehicle-id", vehicleId);
            responseMsg.put("parking-id", parkingId);
            responseMsg.put("accepted", accepted);
            responseMsg.put("reason", reason);
            if (spot != null) {
                responseMsg.put("parking", spot.toJson());
            }

            JSONObject response = new JSONObject();
            response.put("id", "MSG_" + System.currentTimeMillis());
            response.put("type", "PARKING_OCCUPY_RESPONSE");
            response.put("timestamp", System.currentTimeMillis());
            response.put("msg", responseMsg);

            publishJson(replyTo, response, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void publishJson(String topicName, JSONObject envelope, boolean retained) throws Exception {
        MqttTopic topic = myClient.getTopic(topicName);
        MqttMessage message = new MqttMessage(envelope.toString().getBytes());
        message.setQos(0);
        message.setRetained(retained);
        MqttDeliveryToken token = topic.publish(message);
        token.waitForCompletion();
    }
}

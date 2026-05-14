package componentes;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartParkingDevice extends MyMqttClient {
    private String topicBase;
    private ParkingSpot spot;

    public SmartParkingDevice(String clientId, String brokerURL, String topicBase, ParkingSpot spot) {
        super(clientId, null, brokerURL);
        this.topicBase = MqttTopics.normalizeBase(topicBase);
        this.spot = spot;
    }

    /**
     * Arranca el dispositivo IoT de parking:
     * 1) se conecta al broker,
     * 2) se suscribe a su topic de comandos,
     * 3) publica su estado inicial.
     */
    public void start() {
        connect();
        subscribe(MqttTopics.roadParkingCommand(topicBase, spot.getRoadSegment(), spot.getId()));
        publishStatus();
    }

    public void publishStatus() {
        try {
            String topicName = MqttTopics.roadParkingStatus(topicBase, spot.getRoadSegment(), spot.getId());
            MqttTopic topic = myClient.getTopic(topicName);

            JSONObject envelope = new JSONObject();
            envelope.put("id", "MSG_" + System.currentTimeMillis());
            envelope.put("type", "PARKING_STATUS");
            envelope.put("timestamp", System.currentTimeMillis());
            envelope.put("msg", spot.toJson());

            MqttMessage message = new MqttMessage(envelope.toString().getBytes());
            message.setQos(0);
            // retained=true permite que un coche/servicio que se suscriba después reciba el último estado.
            message.setRetained(true);

            MySimpleLogger.trace(this.clientId, "Publishing parking status to " + topicName);
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAvailable(boolean available) {
        this.spot.setAvailable(available);
        publishStatus();
    }

    public void occupy(String vehicleId) {
        this.spot.ocuparParking(vehicleId);
        publishStatus();
    }

    public void free() {
        this.spot.libre();
        publishStatus();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);

        JSONObject envelope = new JSONObject(new String(message.getPayload()));
        if (!"PARKING_COMMAND".equals(envelope.optString("type"))) return;

        JSONObject msg = envelope.getJSONObject("msg");
        String parkingId = msg.optString("parking-id");
        String action = msg.optString("action");
        String vehicleId = msg.optString("vehicle-id", "unknown");

        if (!this.spot.getId().equalsIgnoreCase(parkingId)) {
            return;
        }

        if ("OCCUPY".equalsIgnoreCase(action)) {
            MySimpleLogger.info(this.clientId, "Parking " + spot.getId() + " occupied by " + vehicleId);
            occupy(vehicleId);
            return;
        }

        if ("FREE".equalsIgnoreCase(action)) {
            MySimpleLogger.info(this.clientId, "Parking " + spot.getId() + " is now free");
            free();
        }
    }

    public ParkingSpot getSpot() {
        return spot;
    }
}

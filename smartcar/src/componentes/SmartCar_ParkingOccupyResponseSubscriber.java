package componentes;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartCar_ParkingOccupyResponseSubscriber extends MyMqttClient {
    public SmartCar_ParkingOccupyResponseSubscriber(String clientId, SmartCar smartcar, String brokerURL) {
        super(clientId, smartcar, brokerURL);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);
        JSONObject envelope = new JSONObject(new String(message.getPayload()));
        if (!"PARKING_OCCUPY_RESPONSE".equals(envelope.optString("type"))) return;

        JSONObject msg = envelope.getJSONObject("msg");
        boolean accepted = msg.optBoolean("accepted", false);
        String parkingId = msg.optString("parking-id");
        String reason = msg.optString("reason");

        if (accepted) {
            MySimpleLogger.info(this.clientId, "Parking ocupado correctamente: " + parkingId);
        } else {
            MySimpleLogger.warn(this.clientId, "No se pudo ocupar el parking " + parkingId + ": " + reason);
        }
    }
}

package componentes;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartCar_ParkingResponseSubscriber extends MyMqttClient {
    public SmartCar_ParkingResponseSubscriber(String clientId, SmartCar smartcar, String brokerURL) {
        super(clientId, smartcar, brokerURL);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        super.messageArrived(topic, message);
        JSONObject envelope = new JSONObject(new String(message.getPayload()));
        if (!"PARKING_RESPONSE".equals(envelope.optString("type"))) return;

        JSONObject msg = envelope.getJSONObject("msg");
        JSONArray parkings = msg.getJSONArray("parkings");

        if (parkings.length() == 0) {
            MySimpleLogger.info(this.clientId, "No hay parkings disponibles para " + msg.optString("request-road-segment"));
            return;
        }

        MySimpleLogger.info(this.clientId, "Parkings disponibles recibidos: " + parkings.length());
        for (int i = 0; i < parkings.length(); i++) {
            JSONObject p = parkings.getJSONObject(i);
            MySimpleLogger.info(this.clientId,
                " - " + p.getString("parking-id") +
                " en " + p.getString("road-segment") +
                " kp=" + p.getInt("position") +
                " distancia-estimada=" + p.optInt("estimated-distance") + "m");
        }

        smartcar.handleParkingResponse(parkings);
    }
}

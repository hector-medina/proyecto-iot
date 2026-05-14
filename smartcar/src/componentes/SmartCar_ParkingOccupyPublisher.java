package componentes;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartCar_ParkingOccupyPublisher extends MyMqttClient {
    public SmartCar_ParkingOccupyPublisher(String clientId, SmartCar smartcar, String brokerURL) {
        super(clientId, smartcar, brokerURL);
    }

    public void occupyParking(String parkingId, String roadSegment) {
        try {
            RoadPlace place = smartcar.getCurrentPlace();
            String topicName = MqttTopics.parkingOccupy(smartcar.getTopicBase());
            String replyTo = MqttTopics.parkingOccupyResponse(smartcar.getTopicBase(), smartcar.getSmartCarID());

            JSONObject msg = new JSONObject();
            msg.put("vehicle-id", smartcar.getSmartCarID());
            msg.put("parking-id", parkingId);
            msg.put("road-segment", roadSegment);
            if (place != null) {
                msg.put("vehicle-road-segment", place.getRoad());
                msg.put("vehicle-position", place.getKm());
            }
            msg.put("reply-to", replyTo);

            JSONObject envelope = new JSONObject();
            envelope.put("id", "MSG_" + System.currentTimeMillis());
            envelope.put("type", "PARKING_OCCUPY_REQUEST");
            envelope.put("timestamp", System.currentTimeMillis());
            envelope.put("msg", msg);

            MqttTopic topic = myClient.getTopic(topicName);
            MqttMessage message = new MqttMessage(envelope.toString().getBytes());
            message.setQos(0);
            message.setRetained(false);

            MySimpleLogger.info(this.clientId, "Requesting parking occupation: " + parkingId + " at " + roadSegment);
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package componentes;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartCar_ParkingQueryPublisher extends MyMqttClient {
    public SmartCar_ParkingQueryPublisher(String clientId, SmartCar smartcar, String brokerURL) {
        super(clientId, smartcar, brokerURL);
    }

    public void consultaParkings (String mode, int limit) {
        try {
            RoadPlace place = smartcar.getCurrentPlace();
            String topicName = MqttTopics.parkingQuery(smartcar.getTopicBase());
            String replyTo = MqttTopics.parkingResponse(smartcar.getTopicBase(), smartcar.getSmartCarID());

            JSONObject msg = new JSONObject();
            msg.put("vehicle-id", smartcar.getSmartCarID());
            msg.put("road-segment", place.getRoad());
            msg.put("position", place.getKm());
            msg.put("only-available", true);
            msg.put("mode", mode);
            msg.put("limit", limit);
            msg.put("reply-to", replyTo);

            JSONObject envelope = new JSONObject();
            envelope.put("id", "MSG_" + System.currentTimeMillis());
            envelope.put("type", "PARKING_QUERY");
            envelope.put("timestamp", System.currentTimeMillis());
            envelope.put("msg", msg);

            MqttTopic topic = myClient.getTopic(topicName);
            MqttMessage message = new MqttMessage(envelope.toString().getBytes());
            message.setQos(0);
            message.setRetained(false);

            MySimpleLogger.info(this.clientId, "Querying parkings: mode=" + mode + " road=" + place.getRoad() + " kp=" + place.getKm());
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

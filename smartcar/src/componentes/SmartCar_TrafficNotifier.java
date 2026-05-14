package componentes;

import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

import utils.MySimpleLogger;

public class SmartCar_TrafficNotifier extends MyMqttClient {
    public SmartCar_TrafficNotifier(String clientId, SmartCar smartcar, String brokerURL) {
        super(clientId, smartcar, brokerURL);
    }

    public void notifyTraffic(String action, RoadPlace place) {
        try {
            if (place == null) return;

            String topicName = MqttTopics.roadTraffic(smartcar.getTopicBase(), place.getRoad());

            JSONObject msg = new JSONObject();
            msg.put("action", action);
            msg.put("road-segment", place.getRoad());
            msg.put("vehicle-id", smartcar.getSmartCarID());
            msg.put("position", place.getKm());
            msg.put("role", "PrivateUsage");

            JSONObject envelope = new JSONObject();
            envelope.put("id", "MSG_" + System.currentTimeMillis());
            envelope.put("type", "TRAFFIC");
            envelope.put("timestamp", System.currentTimeMillis());
            envelope.put("msg", msg);

            MqttTopic topic = myClient.getTopic(topicName);
            MqttMessage message = new MqttMessage(envelope.toString().getBytes());
            message.setQos(0);
            message.setRetained(false);

            MySimpleLogger.info(this.clientId, "Publishing traffic " + action + " to " + topicName);
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

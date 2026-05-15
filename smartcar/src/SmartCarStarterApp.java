import componentes.SmartCar;
import componentes.MqttTopics;
import componentes.MyMqttClient;

public class SmartCarStarterApp {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: SmartCarStarterApp <smartCarID> <brokerURL> [topicBase] [rootCA] [certificate] [privateKey]");
            System.out.println("Example: SmartCarStarterApp SmartCar001 tcp://tambori.dsic.upv.es:10083 es/upv/pros/tatami/smartcities/traffic/PTPaterna");
            System.out.println("AWS IoT: SmartCarStarterApp SmartCar001 ssl://<endpoint>:8883 iot/2023/07 certs/AmazonRootCA1.pem certs/Device-18e7e0f1-certificate.pem.crt certs/Device-18e7e0f1-private.pem.key");
            System.exit(1);
        }
        String smartCarID = args[0];
        String brokerURL = args[1];
        String topicBase = args.length >= 3 ? args[2] : MqttTopics.DEFAULT_TOPIC_BASE;
        if (args.length >= 6) {
            MyMqttClient.configureAwsIotCertificates(args[3], args[4], args[5]);
        }
        SmartCar sc1 = new SmartCar(smartCarID, brokerURL, topicBase);
        Thread.sleep(2000);
        // Indicamos que el coche está en el segmento R5s1, punto 100.
        // Esto publica VEHICLE_IN en iot/2023/07/road/R5s1/traffic.
        sc1.getIntoRoad("R5s1", 100);
        // Consulta los parkings disponibles de la calle/segmento actual.
        sc1.consultaParkingDisponibleEnCalle(10);
        Thread.sleep(2000);
        // Consulta los parkings disponibles más cercanos.
        sc1.consultarParkingCercanoDisponible(3);
        Thread.sleep(2000);
        // Nuevo: consulta y ocupa automáticamente el parking disponible más cercano.
        sc1.consultarYOcuparParkingCercanoDisponible(3);        // Ejemplo de accidente, si también quieres probar alerts/info.
        // sc1.notifyIncident("TRAFFIC_ACCIDENT");
        Thread.sleep(60000);
    }
}

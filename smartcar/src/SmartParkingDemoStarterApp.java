import componentes.MqttTopics;
import componentes.ParkingSpot;
import componentes.SmartCar;
import componentes.SmartParkingDevice;
import componentes.SmartParkingRegistry;

public class SmartParkingDemoStarterApp {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("tcp://tambori.dsic.upv.es:10083 es/upv/pros/tatami/smartcities/traffic/PTPaterna");
            System.exit(1);
        }

        String brokerURL = args[0];
        String topicBase = args.length >= 2 ? args[1] : MqttTopics.DEFAULT_TOPIC_BASE;

        SmartParkingRegistry registry = new SmartParkingRegistry("parking-register", brokerURL, topicBase);
        registry.start();

        SmartParkingDevice p1 = new SmartParkingDevice("parking-P1", brokerURL, topicBase, new ParkingSpot("P1", "R5s1", 80, true));
        SmartParkingDevice p2 = new SmartParkingDevice("parking-P2", brokerURL, topicBase, new ParkingSpot("P2", "R5s1", 160, false));
        SmartParkingDevice p3 = new SmartParkingDevice("parking-P3", brokerURL, topicBase, new ParkingSpot("P3", "R5s1", 210, true));
        SmartParkingDevice p4 = new SmartParkingDevice("parking-P4", brokerURL, topicBase, new ParkingSpot("P4", "R1s4a", 600, true));

        p1.start();
        p2.start();
        p3.start();
        p4.start();

        SmartCar car = new SmartCar("SmartCarParking001", brokerURL, topicBase); //me subscribo a todos los canales
        car.getIntoRoad("R5s1", 100);
        Thread.sleep(2000);
        car.consultaParkingDisponibleEnCalle(10);
        Thread.sleep(2000);
        car.consultarParkingCercanoDisponible(3);
        Thread.sleep(2000);
        // Nuevo flujo: el coche consulta y ocupa automáticamente el parking disponible más cercano.
        car.consultarYOcuparParkingCercanoDisponible(3);
        // Si quieres probar una ocupación directa sin consulta previa:
        // car.occupyParking("P3", "R5s1");

        // Mantiene el SmartParking corriendo
        System.out.println("SmartParking demo running. Press Ctrl+C to stop.");
        new java.util.concurrent.CountDownLatch(1).await();

    }
}

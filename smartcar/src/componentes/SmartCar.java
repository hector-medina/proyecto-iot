package componentes;

import org.json.JSONArray;
import org.json.JSONObject;

public class SmartCar {

    protected String brokerURL = null;
    protected String topicBase = MqttTopics.DEFAULT_TOPIC_BASE;

    protected String smartCarID = null;
    protected RoadPlace rp = null;   // simula la ubicación actual del vehículo

    protected SmartCar_RoadInfoSubscriber subscriber = null;
    protected String subscribedRoadInfoTopic = null;

    protected SmartCar_InicidentNotifier notifier = null;
    protected SmartCar_TrafficNotifier trafficNotifier = null;
    protected SmartCar_ParkingQueryPublisher parkingQueryPublisher = null;
    protected SmartCar_ParkingResponseSubscriber parkingResponseSubscriber = null;
    protected SmartCar_ParkingOccupyPublisher parkingOccupyPublisher = null;
    protected SmartCar_ParkingOccupyResponseSubscriber parkingOccupyResponseSubscriber = null;

    private boolean occupyFirstAvailableOnNextResponse = false;

    public SmartCar(String id, String brokerURL) {
        this(id, brokerURL, MqttTopics.DEFAULT_TOPIC_BASE);
    }

    public SmartCar(String id, String brokerURL, String topicBase) {
        this.setSmartCarID(id);
        this.brokerURL = brokerURL;
        this.topicBase = MqttTopics.normalizeBase(topicBase);

        this.notifier = new SmartCar_InicidentNotifier(id + ".incident-notifier", this, this.brokerURL);
        this.notifier.connect();

        this.trafficNotifier = new SmartCar_TrafficNotifier(id + ".traffic-notifier", this, this.brokerURL);
        this.trafficNotifier.connect();

        this.parkingQueryPublisher = new SmartCar_ParkingQueryPublisher(id + ".parking-query", this, this.brokerURL);
        this.parkingQueryPublisher.connect();

        this.parkingOccupyPublisher = new SmartCar_ParkingOccupyPublisher(id + ".parking-occupy", this, this.brokerURL);
        this.parkingOccupyPublisher.connect();

        this.parkingResponseSubscriber = new SmartCar_ParkingResponseSubscriber(id + ".parking-response", this, this.brokerURL);
        this.parkingResponseSubscriber.connect();
        this.parkingResponseSubscriber.subscribe(MqttTopics.parkingResponse(this.topicBase, this.smartCarID));

        this.parkingOccupyResponseSubscriber = new SmartCar_ParkingOccupyResponseSubscriber(id + ".parking-occupy-response", this, this.brokerURL);
        this.parkingOccupyResponseSubscriber.connect();
        this.parkingOccupyResponseSubscriber.subscribe(MqttTopics.parkingOccupyResponse(this.topicBase, this.smartCarID));

        this.setCurrentRoadPlace(new RoadPlace("R5s1", 0));
    }

    public void setSmartCarID(String smartCarID) {
        this.smartCarID = smartCarID;
    }

    public String getSmartCarID() {
        return smartCarID;
    }

    public String getTopicBase() {
        return topicBase;
    }

    public void setCurrentRoadPlace(RoadPlace newPlace) {
        RoadPlace oldPlace = this.rp;

        // Si ya teníamos un suscriptor al tramo antiguo, lo cancelamos y publicamos VEHICLE_OUT.
        if (this.subscriber != null && this.subscribedRoadInfoTopic != null && oldPlace != null) {
            this.subscriber.unsubscribe(this.subscribedRoadInfoTopic);
            if (this.trafficNotifier != null) {
                this.trafficNotifier.notifyTraffic("VEHICLE_OUT", oldPlace);
            }
        }

        this.rp = newPlace;

        // El coche se suscribe a la información del segmento por el que circula.
        if (this.subscriber == null) {
            this.subscriber = new SmartCar_RoadInfoSubscriber(this.smartCarID + ".road-info", this, this.brokerURL);
            this.subscriber.connect();
        }

        this.subscribedRoadInfoTopic = MqttTopics.roadInfo(this.topicBase, newPlace.getRoad());
        this.subscriber.subscribe(this.subscribedRoadInfoTopic);

        // Interacción con STS: notificamos la entrada del coche al segmento por road/{id}/traffic.
        if (this.trafficNotifier != null) {
            this.trafficNotifier.notifyTraffic("VEHICLE_IN", newPlace);
        }
    }

    public RoadPlace getCurrentPlace() {
        return rp;
    }

    public void changeKm(int km) {
        this.getCurrentPlace().setKm(km);
        if (this.trafficNotifier != null) {
            this.trafficNotifier.notifyTraffic("CHECK_IN", this.getCurrentPlace());
        }
    }

    public void getIntoRoad(String road, int km) {
        this.setCurrentRoadPlace(new RoadPlace(road, km));
    }

    public void notifyIncident(String incidentType) {
        if (this.notifier == null) return;
        this.notifier.alert(this.getSmartCarID(), incidentType, this.getCurrentPlace());
    }

    // Consulta los parkings disponibles SOLO en el segmento actual
    public void consultaParkingDisponibleEnCalle(int limit) {
        if (this.parkingQueryPublisher == null) return;
        this.parkingQueryPublisher.consultaParkings("ROAD_ONLY", limit);
    }

    //Consulta los parkings disponibles más cercanos
    public void consultarParkingCercanoDisponible(int limit) {
        if (this.parkingQueryPublisher == null) return;
        this.parkingQueryPublisher.consultaParkings("NEAREST", limit);
    }

    //Consulta los parkings cercanos y, cuando llegue la respuesta, intenta ocupar el primero disponible
    public void consultarYOcuparParkingCercanoDisponible(int limit) {
        this.occupyFirstAvailableOnNextResponse = true;
        consultarParkingCercanoDisponible(limit);
    }

    //Solicita ocupar una plaza, la validación la hace SmartParkingRegistry
    public void occupyParking(String parkingId, String roadSegment) {
        if (this.parkingOccupyPublisher == null) return;
        this.parkingOccupyPublisher.occupyParking(parkingId, roadSegment);
    }

    //Lo invoca SmartCar_ParkingResponseSubscriber cuando recibe PARKING_RESPONSE
    public void handleParkingResponse(JSONArray parkings) {
        if (!this.occupyFirstAvailableOnNextResponse) return;
        this.occupyFirstAvailableOnNextResponse = false;

        if (parkings == null || parkings.length() == 0) return;

        try {
            JSONObject first = parkings.getJSONObject(0);
            String parkingId = first.getString("parking-id");
            String roadSegment = first.getString("road-segment");
            occupyParking(parkingId, roadSegment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

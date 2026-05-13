package componentes;

import utils.MySimpleLogger;


public class SmartCar {


	protected String brokerURL = null;

	protected String smartCarID = null;
	protected RoadPlace rp = null;	// simula la ubicación actual del vehículo
	protected SmartCar_RoadInfoSubscriber subscriber = null;
	protected SmartCar_InicidentNotifier notifier = null;
	
	public SmartCar(String id, String brokerURL) {
		
		this.setSmartCarID(id);
		this.brokerURL = brokerURL;

		this.setCurrentRoadPlace(new RoadPlace("R5s1", 0));
		
		this.notifier = new SmartCar_InicidentNotifier(id + ".incident-notifier", this, this.brokerURL);
		this.notifier.connect();

	}
	
	
	public void setSmartCarID(String smartCarID) {
		this.smartCarID = smartCarID;
	}
	
	public String getSmartCarID() {
		return smartCarID;
	}

	public void setCurrentRoadPlace(RoadPlace rp) {
		this.rp = rp;

		// 1.- Si ya teníamos algún suscriptor conectado al tramo de carretera antiguo, primero los desconectamos
		// 2.- Ahora debemos crear suscriptor/es para conocer 'cosas' de dicho tramo de carretra, y conectarlo/s
		// 3.- Debemos suscribir este/os suscriptor/es a los canales adecuados
	}

	public RoadPlace getCurrentPlace() {
		return rp;
	}

	public void changeKm(int km) {
		this.getCurrentPlace().setKm(km);
	}
	
	public void getIntoRoad(String road, int km) {
		this.getCurrentPlace().setRoad(road);
		this.getCurrentPlace().setKm(km);
	}
	
	public void notifyIncident(String incidentType) {
		if ( this.notifier == null )
			return;
		
		this.notifier.alert(this.getSmartCarID(), incidentType, this.getCurrentPlace());
		
	}

}

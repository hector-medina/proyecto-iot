package componentes;

import org.json.JSONException;
import org.json.JSONObject;

public class ParkingSpot {
    private String id;
    private String roadSegment;
    private int position;
    private boolean available;
    private String occupiedBy;
    private long timestamp;

    public ParkingSpot(String id, String roadSegment, int position, boolean available) {
        this(id, roadSegment, position, available, null);
    }

    public ParkingSpot(String id, String roadSegment, int position, boolean available, String occupiedBy) {
        this.id = id;
        this.roadSegment = roadSegment;
        this.position = position;
        this.available = available;
        this.occupiedBy = occupiedBy;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getRoadSegment() { return roadSegment; }
    public int getPosition() { return position; }
    public boolean isAvailable() { return available; }
    public String getOccupiedBy() { return occupiedBy; }
    public long getTimestamp() { return timestamp; }

    public void setAvailable(boolean available) {
        this.available = available;
        if (available) {
            this.occupiedBy = null;
        }
        this.timestamp = System.currentTimeMillis();
    }

    public void ocuparParking (String vehicleId) {
        this.available = false;
        this.occupiedBy = vehicleId;
        this.timestamp = System.currentTimeMillis();
    }

    public void libre () {
        this.available = true;
        this.occupiedBy = null;
        this.timestamp = System.currentTimeMillis();
    }

    public int distanceTo(String roadSegment, int position) {
        if (this.roadSegment.equalsIgnoreCase(roadSegment)) {
            return Math.abs(this.position - position);
        }
        // Penalizamos segmentos distintos porque sin grafo de intersecciones no podemos saber distancia real.
        return 100000 + Math.abs(this.position - position);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("rt", "smart-parking");
        msg.put("parking-id", this.id);
        msg.put("road-segment", this.roadSegment);
        msg.put("position", this.position);
        msg.put("available", this.available);
        msg.put("occupied-by", this.occupiedBy == null ? JSONObject.NULL : this.occupiedBy);
        msg.put("timestamp", this.timestamp);
        return msg;
    }

    public static ParkingSpot fromJson(JSONObject obj) throws JSONException {
        JSONObject msg = obj.has("msg") ? obj.getJSONObject("msg") : obj;
        String parkingId = msg.optString("parking-id", msg.optString("id"));
        String roadSegment = msg.optString("road-segment", msg.optString("road"));
        int position = msg.optInt("position", msg.optInt("kp"));
        boolean available = msg.optBoolean("available", false);
        String occupiedBy = null;
        if (msg.has("occupied-by") && !msg.isNull("occupied-by")) {
            occupiedBy = msg.optString("occupied-by", null);
        }
        ParkingSpot spot = new ParkingSpot(parkingId, roadSegment, position, available, occupiedBy);
        spot.timestamp = msg.optLong("timestamp", System.currentTimeMillis());
        return spot;
    }
}

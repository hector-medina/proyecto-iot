# SmartTraffic Parking

Extensión del seminario Smart Traffic con plazas de parking inteligentes.

## Ejecutar demo completa

```text
SmartParkingDemoStarterApp tcp://tambori.dsic.upv.es:10083 iot/2023/07
```

La demo crea:

- `SmartParkingRegistry`: servicio IoT de parking.
- `P1`, `P2`, `P3`, `P4`: dispositivos IoT de parking.
- `SmartCarParking001`: coche que consulta parkings y ocupa el más cercano disponible.

## Topics principales

```text
iot/2023/07/road/+/parking/status/+
iot/2023/07/parking/query
iot/2023/07/parking/occupy
iot/2023/07/road/+/parking/command/+
iot/2023/07/vehicle/SmartCarParking001/parking/response
iot/2023/07/vehicle/SmartCarParking001/parking/occupy/response
iot/2023/07/road/+/traffic
```

Más detalle en `../MODIFICACIONES_PARKING.md`.

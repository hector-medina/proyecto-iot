# Nuevas modificaciones: consulta y ocupación de parkings

Parámetros de ejemplo:

```text
Broker:    tcp://tambori.dsic.upv.es:10083
TopicBase: iot/2023/07
```

## 1. Topics usados

### Estado de plazas de parking

Cada dispositivo de parking publica su disponibilidad en:

```text
iot/2023/07/road/{roadSegment}/parking/status/{parkingId}
```

Ejemplos:

```text
iot/2023/07/road/R5s1/parking/status/P1
iot/2023/07/road/R5s1/parking/status/P2
iot/2023/07/road/R5s1/parking/status/P3
iot/2023/07/road/R1s4a/parking/status/P4
```

Para verlos todos en MQTT Explorer o MQTT.fx:

```text
iot/2023/07/road/+/parking/status/+
```

### Consulta de parkings

El coche publica consultas en:

```text
iot/2023/07/parking/query
```

El servicio responde al coche en:

```text
iot/2023/07/vehicle/{vehicleId}/parking/response
```

Ejemplo:

```text
iot/2023/07/vehicle/SmartCarParking001/parking/response
```

### Ocupación de parkings

El coche solicita ocupar una plaza en:

```text
iot/2023/07/parking/occupy
```

El servicio manda el comando al dispositivo físico en:

```text
iot/2023/07/road/{roadSegment}/parking/command/{parkingId}
```

Ejemplo:

```text
iot/2023/07/road/R5s1/parking/command/P1
```

El servicio responde al coche en:

```text
iot/2023/07/vehicle/{vehicleId}/parking/occupy/response
```

Ejemplo:

```text
iot/2023/07/vehicle/SmartCarParking001/parking/occupy/response
```

### Interacción con STS por traffic

El coche publica ubicación en el canal existente de STS:

```text
iot/2023/07/road/{roadSegment}/traffic
```

Ejemplo:

```text
iot/2023/07/road/R5s1/traffic
```

El servicio de parking se suscribe a:

```text
iot/2023/07/road/+/traffic
```

Así puede saber la posición del vehículo y calcular parkings cercanos.

## 2. Flujo funcional

1. `SmartParkingDevice` publica `PARKING_STATUS`.
2. `SmartParkingRegistry` guarda el estado de cada plaza.
3. `SmartCar` publica `PARKING_QUERY`.
4. `SmartParkingRegistry` responde con `PARKING_RESPONSE`.
5. `SmartCar` puede seleccionar el primer parking libre y publicar `PARKING_OCCUPY_REQUEST`.
6. `SmartParkingRegistry` valida que sigue libre.
7. `SmartParkingRegistry` publica `PARKING_COMMAND` al dispositivo.
8. `SmartParkingDevice` cambia a `available=false` y publica de nuevo `PARKING_STATUS`.
9. `SmartCar` recibe `PARKING_OCCUPY_RESPONSE`.

## 3. Clases añadidas/modificadas

### Añadidas

```text
SmartCar_TrafficNotifier.java
SmartCar_ParkingOccupyPublisher.java
SmartCar_ParkingOccupyResponseSubscriber.java
```

### Modificadas

```text
MqttTopics.java
ParkingSpot.java
SmartParkingDevice.java
SmartParkingRegistry.java
SmartCar.java
SmartCar_ParkingResponseSubscriber.java
SmartParkingDemoStarterApp.java
SmartCarStarterApp.java
```

## 4. Cómo probar

Ejecutar:

```text
SmartParkingDemoStarterApp tcp://tambori.dsic.upv.es:10083 iot/2023/07
```

En MQTT Explorer, suscribirse a:

```text
iot/2023/07/#
```

O a estos topics concretos:

```text
iot/2023/07/road/+/parking/status/+
iot/2023/07/parking/query
iot/2023/07/parking/occupy
iot/2023/07/road/+/parking/command/+
iot/2023/07/vehicle/SmartCarParking001/parking/response
iot/2023/07/vehicle/SmartCarParking001/parking/occupy/response
```

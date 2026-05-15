# SmartTraffic Parking

Proyecto de IoT que extiende el seminario de **Smart Traffic** con un sistema de plazas de parking inteligentes. La aplicacion usa MQTT para que un coche inteligente consulte plazas disponibles, reciba respuestas del servicio de parking y pueda ocupar automaticamente la plaza libre mas cercana.

## Que incluye

- `SmartParkingRegistry`: servicio que registra el estado de las plazas, recibe consultas y coordina ocupaciones.
- `SmartParkingDevice`: dispositivo IoT que representa una plaza de parking y publica su disponibilidad.
- `SmartCar`: coche inteligente que publica su posicion, consulta parkings y solicita ocupar una plaza.
- `SmartParkingDemoStarterApp`: demo completa con servicio, plazas y coche de prueba.
- `SmartCarStarterApp`: arranque independiente de un coche inteligente.

## Requisitos

- JDK instalado.
- Acceso a un broker MQTT.
- Librerias incluidas en la carpeta `lib/`.

El proyecto esta preparado para abrirse con IntelliJ IDEA mediante `smarttraffic-parking.iml`. La carpeta de fuentes es `smartcar/src`.

## Como compilar

Desde la raiz del repositorio:

```bash
javac -cp "lib/*" -d bin $(find smartcar/src -name "*.java")
```

Esto genera las clases compiladas en `bin/`.

## Como ejecutar la demo completa

```bash
java -cp "bin:lib/*" SmartParkingDemoStarterApp tcp://tambori.dsic.upv.es:10083 iot/2023/07
```

La demo arranca:

- un registro de parkings (`SmartParkingRegistry`);
- cuatro plazas de parking (`P1`, `P2`, `P3`, `P4`);
- un coche (`SmartCarParking001`) que entra en el segmento `R5s1`, consulta plazas disponibles y ocupa una plaza cercana.

Para detener la demo, pulsa `Ctrl+C`.

## Como ejecutar solo un coche

Si ya hay un servicio de parking funcionando, puedes arrancar solo un coche:

```bash
java -cp "bin:lib/*" SmartCarStarterApp SmartCar001 tcp://tambori.dsic.upv.es:10083 iot/2023/07
```

Parametros:

- `SmartCar001`: identificador del coche.
- `tcp://tambori.dsic.upv.es:10083`: URL del broker MQTT.
- `iot/2023/07`: topic base. Si no se indica, se usa `es/upv/pros/tatami/smartcities/traffic/PTPaterna`.

## Flujo basico de uso

1. `SmartParkingDevice` publica el estado de cada plaza mediante `PARKING_STATUS`.
2. `SmartParkingRegistry` guarda si cada plaza esta libre u ocupada.
3. `SmartCar` publica su ubicacion en el topic de trafico del segmento de carretera.
4. `SmartCar` envia una consulta de parking mediante `PARKING_QUERY`.
5. `SmartParkingRegistry` responde con las plazas candidatas mediante `PARKING_RESPONSE`.
6. `SmartCar` puede elegir la primera plaza libre y solicitar ocuparla con `PARKING_OCCUPY_REQUEST`.
7. `SmartParkingRegistry` valida que la plaza sigue libre y envia un `PARKING_COMMAND` al dispositivo.
8. `SmartParkingDevice` cambia su estado a `available=false` y publica de nuevo su estado.
9. `SmartCar` recibe el resultado mediante `PARKING_OCCUPY_RESPONSE`.

## Topics MQTT principales

Con el topic base `iot/2023/07`, puedes observar todo el flujo en MQTT Explorer suscribiendote a:

```text
iot/2023/07/#
```

Topics concretos:

```text
iot/2023/07/road/+/traffic
iot/2023/07/road/+/parking/status/+
iot/2023/07/parking/query
iot/2023/07/parking/occupy
iot/2023/07/road/+/parking/command/+
iot/2023/07/vehicle/SmartCarParking001/parking/response
iot/2023/07/vehicle/SmartCarParking001/parking/occupy/response
```

### Para que sirve cada topic

| Topic | Uso |
| --- | --- |
| `iot/2023/07/road/{roadSegment}/traffic` | El coche publica su ubicacion dentro de un segmento de carretera. |
| `iot/2023/07/road/+/traffic` | El servicio de parking escucha ubicaciones de coches para calcular plazas cercanas. |
| `iot/2023/07/road/{roadSegment}/parking/status/{parkingId}` | Cada plaza publica si esta disponible u ocupada. |
| `iot/2023/07/parking/query` | El coche solicita parkings disponibles. |
| `iot/2023/07/vehicle/{vehicleId}/parking/response` | El servicio responde al coche con los parkings encontrados. |
| `iot/2023/07/parking/occupy` | El coche solicita ocupar una plaza. |
| `iot/2023/07/road/{roadSegment}/parking/command/{parkingId}` | El servicio envia el comando de ocupacion al dispositivo de parking. |
| `iot/2023/07/vehicle/{vehicleId}/parking/occupy/response` | El servicio confirma al coche si la ocupacion se ha realizado. |

Ejemplos de plazas publicadas por la demo:

```text
iot/2023/07/road/R5s1/parking/status/P1
iot/2023/07/road/R5s1/parking/status/P2
iot/2023/07/road/R5s1/parking/status/P3
iot/2023/07/road/R1s4a/parking/status/P4
```
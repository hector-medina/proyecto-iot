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

## AWS IoT

En AWS IoT Core necesitas:

- un Thing creado, por ejemplo `parking-P1`;
- el certificado del dispositivo activo;
- la policy de `ParkingPolicy.json` adjuntada al certificado;
- el endpoint de AWS IoT (`Settings > Device data endpoint`).

Coloca los certificados en:

```text
certs/AmazonRootCA1.pem
certs/Device-18e7e0f1-certificate.pem.crt
certs/Device-18e7e0f1-private.pem.key
```

Ejecuta contra AWS IoT:

```bash
java -cp "bin:lib/*" SmartParkingDemoStarterApp \
  ssl://<endpoint-aws-iot>:8883 \
  iot/2023/07 \
  certs/AmazonRootCA1.pem \
  certs/Device-18e7e0f1-certificate.pem.crt \
  certs/Device-18e7e0f1-private.pem.key
```

No hace falta pasar un ID de dispositivo aparte: AWS usa el `clientId` MQTT y el certificado. En la demo los `clientId` son `parking-P1`, `parking-register`, `SmartCarParking001`, etc.

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

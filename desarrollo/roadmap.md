# Roadmap: Sistema Inteligente de Gestion de Aparcamiento en Calle

## Objetivo

El objetivo es extender el sistema Smart Traffic actual con una funcionalidad IoT para gestionar plazas de aparcamiento en calle. La propuesta consiste en asociar plazas de parking a una carretera o calle del simulador, por ejemplo la calle Benjamin Franklin, y publicar eventos MQTT cuando una plaza quede libre u ocupada.

Los vehiculos que esten buscando aparcamiento se suscribiran a esos eventos y podran decidir dirigirse a la plaza disponible mas cercana. De esta forma se reduce el trafico generado por vehiculos dando vueltas en busca de aparcamiento.

## Situacion actual del proyecto

El proyecto ya tiene una base adecuada para implementar esta idea:

- `SmartCar` representa un vehiculo inteligente.
- `RoadPlace` representa la ubicacion actual del vehiculo mediante carretera y punto kilometrico.
- `SmartCar_InicidentNotifier` publica incidencias en un topic MQTT asociado a una carretera.
- `SmartCar_RoadInfoSubscriber` esta preparado para recibir informacion de una carretera, aunque todavia no procesa mensajes.
- `MyMqttClient` encapsula la conexion, publicacion y suscripcion MQTT usando Eclipse Paho.

El patron actual para publicar incidencias es:

```text
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road}/alerts
```

La solucion de parking debe seguir el mismo estilo, pero usando un canal especifico de aparcamiento.

## Arquitectura propuesta

La arquitectura minima seria:

```text
Sensor o simulador de plaza
        |
        v
ParkingSpot
        |
        v
SmartParkingNotifier
        |
        v
MQTT topic de parking de la carretera
        |
        v
SmartCar_RoadInfoSubscriber
        |
        v
SmartCar decide si va a esa plaza
```

El parking se modela como informacion dinamica asociada a una carretera. No es necesario crear un sistema completamente separado: puede integrarse como otro tipo de evento publicado por la infraestructura Smart Traffic.

## Topic MQTT recomendado

Para mantener coherencia con el codigo existente, se recomienda usar:

```text
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road}/parking
```

Ejemplo para una calle o tramo identificado como `R5s1`:

```text
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/parking
```

## Formato de mensaje

Cuando una plaza quede libre, el sistema publicaria un mensaje JSON como este:

```json
{
  "event": "parking.free",
  "parkingId": "BF-P01",
  "road": "R5s1",
  "kp": 120,
  "occupied": false,
  "freeSlots": 3,
  "totalSlots": 10
}
```

Cuando una plaza quede ocupada:

```json
{
  "event": "parking.occupied",
  "parkingId": "BF-P01",
  "road": "R5s1",
  "kp": 120,
  "occupied": true,
  "freeSlots": 2,
  "totalSlots": 10
}
```

Campos principales:

- `event`: tipo de evento publicado.
- `parkingId`: identificador unico de la plaza.
- `road`: carretera o calle asociada.
- `kp`: posicion aproximada de la plaza dentro de la carretera.
- `occupied`: indica si la plaza esta ocupada.
- `freeSlots`: numero de plazas libres en esa zona.
- `totalSlots`: numero total de plazas gestionadas.

## Nuevas clases recomendadas

### `ParkingSpot`

Representa una plaza individual de aparcamiento.

Responsabilidades:

- Guardar el identificador de la plaza.
- Guardar la carretera o calle a la que pertenece.
- Guardar la posicion de la plaza.
- Guardar si esta libre u ocupada.

Ejemplo conceptual:

```java
public class ParkingSpot {
    private String id;
    private String road;
    private int kp;
    private boolean occupied;
}
```

### `SmartParkingNotifier`

Publica eventos MQTT cuando cambia el estado de una plaza.

Responsabilidades:

- Construir el topic MQTT de parking.
- Construir el mensaje JSON.
- Publicar eventos `parking.free` y `parking.occupied`.

Esta clase deberia parecerse a `SmartCar_InicidentNotifier`, pero orientada a plazas de parking.

### `ParkingArea` opcional

Si se quiere modelar un conjunto de plazas en Benjamin Franklin, se puede crear una clase `ParkingArea`.

Responsabilidades:

- Mantener una lista de plazas.
- Calcular cuantas plazas libres quedan.
- Buscar la plaza libre mas cercana a un punto kilometrico.
- Notificar cambios de disponibilidad.

Esta clase es opcional para una primera version. Para una prueba sencilla, basta con `ParkingSpot` y `SmartParkingNotifier`.

## Cambios necesarios en `SmartCar`

El vehiculo necesita saber si esta buscando aparcamiento.

Campos recomendados:

```java
private boolean searchingParking;
private ParkingSpot targetParking;
```

Metodos recomendados:

```java
public void startLookingForParking()
public void stopLookingForParking()
public boolean isLookingForParking()
public void goToParking(String parkingId, RoadPlace place)
```

Cuando el coche entre en una carretera, `SmartCar` deberia suscribirse a los topics relevantes de esa carretera:

```text
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road}/alerts
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road}/parking
```

El metodo actual `setCurrentRoadPlace` ya tiene comentarios indicando este trabajo pendiente:

```java
public void setCurrentRoadPlace(RoadPlace rp) {
    this.rp = rp;

    // desconectar suscriptor antiguo
    // crear nuevo suscriptor
    // suscribir a canales adecuados
}
```

Ese es el punto principal de integracion.

## Cambios necesarios en `SmartCar_RoadInfoSubscriber`

`SmartCar_RoadInfoSubscriber` debe procesar los mensajes recibidos.

Flujo recomendado:

1. Recibir el mensaje MQTT.
2. Convertir el payload a JSON.
3. Comprobar si el evento es `parking.free`.
4. Comprobar si el coche esta buscando aparcamiento.
5. Evaluar si la plaza esta cerca.
6. Si conviene, asignarla como destino.

Ejemplo conceptual:

```java
if ("parking.free".equals(event) && smartcar.isLookingForParking()) {
    smartcar.goToParking(parkingId, new RoadPlace(road, kp));
}
```

## Simulacion de sensores

Para simular el comportamiento IoT, se puede crear una clase o una prueba en `SmartCarStarterApp` que cambie el estado de una plaza:

```java
ParkingSpot spot = new ParkingSpot("BF-P01", "R5s1", 120);
spot.setOccupied(false);

parkingNotifier.notifyFreeSpot(spot, 3, 10);
```

Esto representa que el sensor de la plaza detecta que el sitio ha quedado libre y publica el evento correspondiente.

## Roadmap de implementacion

### Fase 1: Modelo basico

- Crear `ParkingSpot`.
- Anadir atributos `id`, `road`, `kp` y `occupied`.
- Anadir getters y setters.

Resultado esperado: el proyecto puede representar plazas de parking.

### Fase 2: Publicacion MQTT

- Crear `SmartParkingNotifier`.
- Implementar publicacion en el topic `road/{road}/parking`.
- Publicar eventos `parking.free` y `parking.occupied`.
- Usar mensajes JSON con informacion de plaza y disponibilidad.

Resultado esperado: una plaza puede anunciar cambios de estado por MQTT.

### Fase 3: Suscripcion del vehiculo

- Completar `SmartCar.setCurrentRoadPlace`.
- Crear un `SmartCar_RoadInfoSubscriber` cuando el coche entra en una carretera.
- Suscribir al coche a `alerts` y `parking`.
- Desuscribir o desconectar el suscriptor anterior cuando el coche cambie de carretera.

Resultado esperado: el coche recibe eventos de la carretera por la que circula.

### Fase 4: Decision del vehiculo

- Anadir estado `searchingParking` a `SmartCar`.
- Implementar `startLookingForParking`.
- Implementar `stopLookingForParking`.
- Implementar `goToParking`.
- Procesar eventos `parking.free` en `SmartCar_RoadInfoSubscriber`.

Resultado esperado: si el coche busca parking y recibe una plaza libre, puede seleccionarla como destino.

### Fase 5: Simulacion completa

- Preparar un escenario con varias plazas en Benjamin Franklin.
- Simular que una plaza queda libre.
- Lanzar uno o varios coches buscando aparcamiento.
- Verificar que reciben el evento MQTT.
- Verificar que el coche decide dirigirse a la plaza.

Resultado esperado: demostracion funcional del sistema inteligente de aparcamiento.

### Fase 6: Integracion con AWS IoT

- Cambiar broker MQTT local por endpoint de AWS IoT.
- Configurar certificados, clave privada y CA.
- Mantener los mismos topics.
- Validar publicacion y suscripcion desde AWS IoT Core.

Resultado esperado: el sistema funciona sobre infraestructura cloud IoT.

## Criterios de aceptacion

La funcionalidad se puede considerar terminada cuando:

- Existe un modelo de plaza de aparcamiento.
- Una plaza puede cambiar entre libre y ocupada.
- El cambio de estado se publica por MQTT.
- Los coches pueden suscribirse al topic de parking de una carretera.
- Un coche buscando parking puede recibir una notificacion de plaza libre.
- El coche puede registrar esa plaza como destino.
- La solucion mantiene la estructura de topics del sistema Smart Traffic.

## Beneficio esperado

Esta extension permite que el sistema Smart Traffic no solo gestione incidencias de trafico, sino tambien informacion de aparcamiento en tiempo real. El resultado es una solucion Smart City mas completa, donde los vehiculos reducen el tiempo de busqueda de aparcamiento y se disminuye la congestion urbana.

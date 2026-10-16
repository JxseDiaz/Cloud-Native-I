# rabbitmq-avanzado

Proyecto de la **Guía 2.3.1 – RabbitMQ Cluster con Docker Compose** (Duoc UC).
Levanta un cluster RabbitMQ de 2 nodos con Docker Compose y una aplicación Spring Boot que:

- Crea, consulta, purga y elimina **colas, exchanges y bindings** en tiempo de ejecución (`RabbitAdmin`).
- **Pausa y reanuda listeners** dinámicamente (`RabbitListenerEndpointRegistry`).
- Consume mensajes de `orders.queue` con un listener de ejemplo.

---

## 1. Arquitectura

```
                  ┌──────────────────────────┐
                  │  Spring Boot (puerto 8080)│
                  │  /api/rabbitmq  /api/listeners
                  └─────────────┬────────────┘
                                │ AMQP localhost:5672
              ┌─────────────────┴─────────────────┐
              │        Docker (red rabbitmq_cluster)        │
              │  ┌──────────────┐   ┌──────────────┐ │
              │  │ rabbitmq-1   │◄─►│ rabbitmq-2   │ │
              │  │ 5672 / 15672 │   │ 5673 / 15673 │ │
              │  └──────────────┘   └──────────────┘ │
              └───────────────────────────────────┘
```

| Servicio     | AMQP (host) | Management UI (host)                           |
|--------------|-------------|------------------------------------------------|
| rabbitmq-1   | 5672        | http://localhost:15672                         |
| rabbitmq-2   | 5673        | http://localhost:15673                         |
| Spring Boot  | —           | http://localhost:8080                          |

Credenciales de RabbitMQ (solo para desarrollo): `admin` / `admin123`.

---

## 2. Requisitos

| Herramienta        | Versión                | Verificar con             |
|--------------------|------------------------|---------------------------|
| Docker Desktop     | Reciente, con WSL 2 (Windows) | `docker --version` y `docker compose version` |
| JDK                | 17 o 21                | `java -version`           |
| Maven              | 3.9 o superior         | `mvn -version`            |
| Conexión a internet| Primera ejecución (imagen de Docker y dependencias de Maven) | — |

Puertos libres en el equipo: **5672, 5673, 15672, 15673 y 8080**.

> **Docker Desktop debe estar abierto** y mostrar "Engine running" antes de usar cualquier comando `docker`.

### Instalar Maven sin permisos de administrador (Windows / equipos de laboratorio)

```powershell
cd $HOME\Desktop
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile maven.zip
Expand-Archive maven.zip -DestinationPath .
```

En **cada terminal nueva** hay que activar Maven y Java (solo dura mientras la ventana esté abierta):

```powershell
$env:PATH = "$HOME\Desktop\apache-maven-3.9.9\bin;$env:PATH"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"   # ajusta a tu carpeta de JDK
mvn -version
```

---

## 3. Estructura del proyecto

```
rabbitmq-avanzado/
├── docker-compose.yml        # Cluster de 2 nodos
├── rabbitmq.conf             # Configuración del cluster
├── definitions.json          # vhost, usuario, permisos y política ha-all
├── pom.xml
└── src/main/
    ├── resources/application.properties
    └── java/com/example/
        ├── RabbitMQAdvancedApplication.java
        ├── config/       RabbitAdminConfig, RabbitListenerConfig
        ├── service/      RabbitResourceManager, ListenerManagementService
        ├── controller/   RabbitMQController, ListenerController
        └── listener/     OrderListener
```

> `definitions.json` es obligatorio: al cargar definiciones, RabbitMQ no crea solo el vhost `/` ni el usuario `admin`, por lo que el archivo los declara explícitamente. Sin ellos, el nodo falla al arrancar con `no_such_vhost`.

---

## 4. Puesta en marcha

### Paso 1. Levantar el cluster
Desde la carpeta del proyecto (donde está `docker-compose.yml`):

```powershell
docker compose up -d
docker compose ps
```

`rabbitmq-1` debe aparecer como **healthy** y `rabbitmq-2` como **running** (puede tardar ~1 minuto).

### Paso 2. Verificar el cluster
```powershell
docker exec rabbitmq-1 rabbitmqctl cluster_status
docker exec rabbitmq-1 rabbitmqctl list_users
docker exec rabbitmq-1 rabbitmqctl list_policies
```

Esperado:
- En `Running Nodes`: `rabbit@rabbitmq-1` y `rabbit@rabbitmq-2`.
- Usuario `admin` con el tag `[administrator]`.
- Política `ha-all` sobre el vhost `/`.

### Paso 3. Compilar y ejecutar la aplicación
```powershell
mvn clean install
mvn spring-boot:run
```

Deja esa terminal abierta hasta ver:
```
Started RabbitMQAdvancedApplication in X.XXX seconds
```

> Si abriste la app antes de que el cluster estuviera `healthy`, verás errores `EOFException` al conectar. Detén la app con `Ctrl + C` y vuelve a ejecutarla cuando el cluster esté sano.

---

## 5. Endpoints de la API

> En PowerShell usa **`curl.exe`** (no `curl`, que es un alias de otro comando).

### Recursos de RabbitMQ — `/api/rabbitmq`

| Método | Ruta | Parámetros | Descripción |
|--------|------|------------|-------------|
| POST   | `/queues` | `queueName` | Crea una cola durable |
| POST   | `/exchanges` | `exchangeName` | Crea un exchange tipo direct |
| POST   | `/bindings` | `queueName`, `exchangeName`, `routingKey` | Enlaza cola con exchange |
| GET    | `/queues/{queueName}` | — | Nombre, mensajes y consumidores |
| POST   | `/queues/{queueName}/purge` | — | Elimina los mensajes de la cola |
| DELETE | `/queues/{queueName}` | — | Elimina la cola |

### Listeners — `/api/listeners`

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET    | `/` | Lista los ids de listeners |
| POST   | `/{listenerId}/pause` | Pausa el listener (detiene sus consumidores) |
| POST   | `/{listenerId}/resume` | Reanuda el listener |

---

## 6. Pruebas

```powershell
# Crear la cola (idempotente)
curl.exe -X POST "http://localhost:8080/api/rabbitmq/queues?queueName=orders.queue"

# Consultar la cola: debe mostrar "consumerCount":3
curl.exe http://localhost:8080/api/rabbitmq/queues/orders.queue

# Crear exchange y binding
curl.exe -X POST "http://localhost:8080/api/rabbitmq/exchanges?exchangeName=orders.exchange"
curl.exe -X POST "http://localhost:8080/api/rabbitmq/bindings?queueName=orders.queue&exchangeName=orders.exchange&routingKey=orders"

# Listeners
curl.exe http://localhost:8080/api/listeners
curl.exe -X POST "http://localhost:8080/api/listeners/order-listener/pause"
curl.exe -X POST "http://localhost:8080/api/listeners/order-listener/resume"
```

### Probar el consumo de mensajes
1. Abre http://localhost:15672 (`admin` / `admin123`) → **Queues → orders.queue → Publish message**.
2. Publica el payload `orden-1`.
3. En la terminal de Spring Boot debe aparecer `Procesando orden: orden-1` y, 2 segundos después, `Orden procesada: orden-1`.

### Probar pausa y reanudación
1. Ejecuta `pause`.
2. Publica `orden-2`: debe quedarse en **Ready** en la UI (sin procesarse).
3. Ejecuta `resume`: el mensaje se procesa.

### Validación final (según la guía)
- [ ] `orders.queue` aparece en ambos nodos (http://localhost:15672 y http://localhost:15673).
- [ ] Los exchanges y bindings son visibles en ambos nodos.
- [ ] `cluster_status` lista los dos nodos.
- [ ] Los mensajes se procesan y el listener se pausa/reanuda correctamente.

---

## 7. Detener todo

```powershell
# En la terminal de Spring Boot: Ctrl + C
docker compose down        # detiene y elimina contenedores (conserva los datos)
docker compose down -v     # además borra los volúmenes (reinicio limpio)
```

---

## 8. Solución de problemas

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `'mvn' no se reconoce` | Maven no instalado o fuera del PATH | Ver sección 2 y activar el PATH en la terminal actual |
| `curl: (7) Failed to connect to localhost port 8080` | La app Spring Boot no está corriendo | Ejecutar `mvn spring-boot:run` y esperar `Started ...` |
| `Port 8080 was already in use` | Queda otra instancia de la app abierta | Cerrarla con `Ctrl + C`, o `Get-NetTCPConnection -LocalPort 8080 -State Listen` y `Stop-Process -Id <PID> -Force` |
| `ERR_CONNECTION_REFUSED` en 15672/15673 | Contenedores detenidos o Docker Desktop cerrado | Abrir Docker Desktop y `docker compose up -d`; revisar con `docker compose ps -a` |
| `BOOT FAILED ... no_such_vhost` | `definitions.json` sin `vhosts`/`users`/`permissions` | Usar el `definitions.json` de este repo y ejecutar `docker compose down -v` y `docker compose up -d` |
| `EOFException` / `AmqpIOException` al arrancar la app | RabbitMQ aún no estaba listo | Esperar a que `rabbitmq-1` sea `healthy` y reiniciar la app |
| `rabbitmq-2` no se une al cluster | Cookie distinta o nodo 1 no sano | Verificar `RABBITMQ_ERLANG_COOKIE` idéntica en ambos y `docker compose logs rabbitmq-2` |
| Errores de Lombok al compilar | JDK demasiado nuevo | Usar JDK 17 o 21 |
| Los commits aparecen con otro autor en GitHub | Identidad de Git del equipo | `git config user.name` y `git config user.email` con tus datos antes de hacer commit |

---

## 9. Notas

- **Seguridad:** las credenciales (`admin/admin123`, cookie de Erlang) son solo para desarrollo. No las uses en producción.
- **Replicación:** la política `ha-all` replica las colas clásicas en ambos nodos. Es válida para RabbitMQ 3.12; en versiones 4.x se reemplaza por *quorum queues*.
- **Pausar listeners:** `pause` detiene el container del listener (`stop()`) y `resume` lo vuelve a iniciar (`start()`), porque Spring AMQP no ofrece `pause()` nativo.
- **Manejo de errores:** `RabbitResourceManager` registra los errores en el log pero los endpoints responden éxito igualmente. Para confirmar una operación, consulta la cola (`GET /queues/{queueName}`) o la Management UI.
- Primera ejecución de Maven: puede tardar varios minutos mientras descarga dependencias.

# Lab 05 Gestion de Distribucion de Libros y Materiales Academicos para Sedes del ICPNA en el Peru

## Contexto

El ICPNA cuenta con varias sedes en el Peru que requieren de forma periodica libros, workbooks, readers, exam packs y otros materiales academicos para atender a sus estudiantes en cada ciclo. Cuando la coordinacion se realiza por correos dispersos, mensajes de WhatsApp y hojas de calculo aisladas, aparecen problemas frecuentes:

- solicitudes duplicadas para la misma sede y el mismo periodo academico
- aprobaciones sin validar stock real disponible
- despachos usando lotes fuera de vigencia academica
- entregas parciales sin trazabilidad clara
- movimientos de inventario registrados de forma manual o incompleta
- dificultad para saber que materiales fueron solicitados, aprobados, despachados y recibidos

## Alcance

Se trabajara con seis entidades relacionadas:

- SedeIcpna
- MaterialAcademico
- LoteIngreso
- SolicitudDistribucion
- EntregaMaterial
- MovimientoInventario

## Dominio

### Entidad SedeIcpna

#### Campos

- `id` Long autoincrement
- `codigo` String unico
- `nombre` String
- `ciudad` String
- `direccion` String
- `responsableLogistica` String
- `contacto` String
- `estado` String `ACTIVA` `INACTIVA` `SUSPENDIDA`
- `createdAt` Instant
- `updatedAt` Instant

### Entidad MaterialAcademico

#### Campos

- `id` Long autoincrement
- `sku` String unico
- `nombre` String
- `categoria` String `STUDENT_BOOK` `WORKBOOK` `READER` `EXAM_PACK` `DICCIONARIO` `OTROS`
- `nivel` String `BASICO` `INTERMEDIO` `AVANZADO` `KIDS` `JUNIORS` `GENERAL`
- `unidadMedida` String `UNIDAD` `PAQUETE` `CAJA` `KIT`
- `stockMinimo` Integer
- `controlVigencia` boolean
- `activo` boolean
- `createdAt` Instant
- `updatedAt` Instant

### Entidad LoteIngreso

#### Campos

- `id` Long autoincrement
- `codigoLote` String unico
- `materialId` FK a MaterialAcademico
- `fechaIngreso` LocalDate `yyyy-MM-dd`
- `edicion` String
- `fechaFinVigencia` LocalDate nullable
- `cantidadIngresada` Integer
- `cantidadDisponible` Integer
- `proveedor` String
- `estado` String `DISPONIBLE` `AGOTADO` `FUERA_VIGENCIA`
- `createdAt` Instant
- `updatedAt` Instant
- `version` Long para optimistic locking

### Entidad SolicitudDistribucion

#### Campos

- `id` Long autoincrement
- `codigo` String unico
- `sedeId` FK a SedeIcpna
- `periodoAcademico` String formato `YYYY-MM`
- `fechaSolicitud` LocalDate `yyyy-MM-dd`
- `prioridad` String `NORMAL` `ALTA` `URGENTE`
- `estado` String `BORRADOR` `ENVIADA` `OBSERVADA` `APROBADA` `DESPACHADA` `ENTREGADA` `PARCIAL` `CANCELADA`
- `fechaLimiteRevision` LocalDate `yyyy-MM-dd`
- `comentarioSolicitud` String opcional
- `comentarioRevision` String opcional
- `createdAt` Instant
- `updatedAt` Instant

### Entidad EntregaMaterial

#### Campos

- `id` Long autoincrement
- `codigo` String unico
- `solicitudId` FK a SolicitudDistribucion
- `fechaProgramada` LocalDate `yyyy-MM-dd`
- `fechaDespacho` LocalDate nullable
- `fechaEntrega` LocalDate nullable
- `estadoEntrega` String `PROGRAMADA` `DESPACHADA` `EN_RUTA` `ENTREGADA` `CON_INCIDENCIA`
- `responsableAlmacen` String
- `responsableRecepcion` String nullable
- `comentario` String opcional
- `createdAt` Instant
- `updatedAt` Instant

### Entidad MovimientoInventario

#### Campos

- `id` Long autoincrement
- `materialId` FK a MaterialAcademico
- `loteId` FK a LoteIngreso
- `fecha` LocalDate `yyyy-MM-dd`
- `tipoMovimiento` String `INGRESO` `SALIDA` `AJUSTE`
- `cantidad` Integer
- `referenciaTipo` String `LOTE` `SOLICITUD` `ENTREGA` `AJUSTE`
- `referenciaId` Long
- `comentario` String opcional
- `createdAt` Instant

---

## Modelo relacional sugerido

Ademas de las seis entidades principales, se recomienda manejar dos tablas de detalle para modelar correctamente las relaciones de negocio.

### Tabla SolicitudDistribucionDetalle

#### Campos

- `id` Long autoincrement
- `solicitudId` FK a SolicitudDistribucion
- `materialId` FK a MaterialAcademico
- `cantidadSolicitada` Integer
- `cantidadAprobada` Integer
- `comentarioItem` String opcional

### Tabla EntregaMaterialDetalle

#### Campos

- `id` Long autoincrement
- `entregaId` FK a EntregaMaterial
- `materialId` FK a MaterialAcademico
- `loteId` FK a LoteIngreso
- `cantidad` Integer

---

## Reglas de negocio

### Regla de fecha limite de revision

La `fechaLimiteRevision` se calcula automaticamente segun la prioridad:

- `URGENTE`: 1 dia
- `ALTA`: 2 dias
- `NORMAL`: 4 dias

### Regla de solicitud duplicada

No se puede crear una solicitud si ya existe otra para la misma sede y el mismo periodo academico en estado distinto de `CANCELADA`.

### Regla de sede activa

No se puede crear solicitud si la sede esta en estado:

- `INACTIVA`
- `SUSPENDIDA`

### Regla de detalle de solicitud

Toda solicitud debe tener al menos un item en su detalle.

No se permite:

- materiales repetidos en la misma solicitud
- `cantidadSolicitada <= 0`
- `cantidadAprobada < 0`
- `cantidadAprobada > cantidadSolicitada`

### Regla de aprobacion

Una solicitud puede aprobarse solo si:

- esta en estado `ENVIADA` o `OBSERVADA`
- tiene al menos un item con `cantidadAprobada > 0`
- la cantidad aprobada no supera el stock disponible total del material

### Regla de observacion

Una solicitud puede pasar a `OBSERVADA` solo si:

- esta en estado `ENVIADA`
- `comentarioRevision` no esta vacio

### Regla de cancelacion

Una solicitud puede cancelarse solo si:

- no esta en estado `DESPACHADA`
- no esta en estado `ENTREGADA`
- no esta en estado `PARCIAL`

### Regla de creacion de entrega

Una entrega solo puede crearse desde una solicitud en estado `APROBADA`.

Al crear la entrega:

- se genera codigo automatico
- inicia con estado `PROGRAMADA`

### Regla de despacho

Al despachar una entrega se debe:

- seleccionar lotes validos para cada material aprobado
- descontar stock en los lotes usados
- registrar un movimiento de salida por cada lote afectado
- cambiar el estado de la entrega a `DESPACHADA`
- cambiar el estado de la solicitud a `DESPACHADA`

No se permite despachar si:

- el lote esta en `AGOTADO`
- el lote esta en `FUERA_VIGENCIA`
- la cantidad a descontar supera `cantidadDisponible`

### Regla de recepcion final

Al registrar la recepcion final:

- si todo fue entregado correctamente:
  - `estadoEntrega = ENTREGADA`
  - la solicitud pasa a `ENTREGADA`
- si hubo faltantes o incidencias:
  - `estadoEntrega = CON_INCIDENCIA`
  - la solicitud pasa a `PARCIAL`

### Regla de vigencia academica

Si un material tiene `controlVigencia = true`, el lote debe tener `fechaFinVigencia`.

Un lote debe considerarse `FUERA_VIGENCIA` cuando la fecha actual supera `fechaFinVigencia`.

### Regla FEFO adaptada

Para materiales con control de vigencia, al momento del despacho el sistema debe sugerir primero los lotes con fecha de fin de vigencia mas cercana.

### Regla de stock bajo

Un material se considera con stock bajo cuando la suma de `cantidadDisponible` de sus lotes disponibles es menor o igual a `stockMinimo`.

---

## Flujo de estados sugerido

### SolicitudDistribucion

- `BORRADOR` puede pasar a `ENVIADA` o `CANCELADA`
- `ENVIADA` puede pasar a `OBSERVADA`, `APROBADA` o `CANCELADA`
- `OBSERVADA` puede pasar a `ENVIADA` o `CANCELADA`
- `APROBADA` puede pasar a `DESPACHADA`
- `DESPACHADA` puede pasar a `ENTREGADA` o `PARCIAL`
- `ENTREGADA` no puede cambiar
- `PARCIAL` no puede cambiar
- `CANCELADA` no puede cambiar

### EntregaMaterial

- `PROGRAMADA` puede pasar a `DESPACHADA`
- `DESPACHADA` puede pasar a `EN_RUTA`
- `EN_RUTA` puede pasar a `ENTREGADA` o `CON_INCIDENCIA`
- `ENTREGADA` no puede cambiar
- `CON_INCIDENCIA` no puede cambiar

---

## Seguridad

### Autenticacion

Spring Security con JWT

### Roles

- `SEDE` puede crear y ver sus propias solicitudes y consultar sus entregas
- `ALMACEN` puede gestionar materiales, lotes, aprobar solicitudes, despachar entregas y ver movimientos
- `ADMIN` puede gestionar todo

### Reglas de acceso

- `SEDE` solo accede a recursos asociados a su propia sede
- `ALMACEN` puede acceder a toda la operacion logistica
- `ADMIN` accede a todos los recursos y configuraciones

---

## Endpoints sugeridos

### Auth

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register` opcional para pruebas

### Sedes

- `GET /api/v1/sedes`
- `GET /api/v1/sedes/{id}`
- `POST /api/v1/sedes` `ADMIN`
- `PUT /api/v1/sedes/{id}` `ADMIN`

### Materiales academicos

- `GET /api/v1/materiales`
- `GET /api/v1/materiales/{id}`
- `POST /api/v1/materiales` `ALMACEN` `ADMIN`
- `PUT /api/v1/materiales/{id}` `ALMACEN` `ADMIN`

### Lotes de ingreso

- `GET /api/v1/lotes`
- `GET /api/v1/lotes/{id}`
- `POST /api/v1/lotes` `ALMACEN` `ADMIN`
- `PUT /api/v1/lotes/{id}` `ALMACEN` `ADMIN`
- `PATCH /api/v1/lotes/{id}/fuera-vigencia` `ALMACEN` `ADMIN`

### Solicitudes

- `POST /api/v1/solicitudes` `SEDE` `ADMIN`
- `GET /api/v1/solicitudes/{id}`
- `GET /api/v1/solicitudes` con filtros y paginacion
  - `sedeId`
  - `periodoAcademico`
  - `estado`
  - `prioridad`
  - `fechaDesde`
  - `fechaHasta`
  - `page`
  - `size`
  - `sort`
- `PUT /api/v1/solicitudes/{id}`
- `PATCH /api/v1/solicitudes/{id}/enviar`
- `PATCH /api/v1/solicitudes/{id}/aprobar`
- `PATCH /api/v1/solicitudes/{id}/observar`
- `PATCH /api/v1/solicitudes/{id}/cancelar`

### Entregas

- `POST /api/v1/entregas` `ALMACEN` `ADMIN`
- `GET /api/v1/entregas/{id}`
- `GET /api/v1/entregas` con filtros y paginacion
  - `solicitudId`
  - `sedeId`
  - `estadoEntrega`
  - `fechaDesde`
  - `fechaHasta`
  - `page`
  - `size`
  - `sort`
- `PATCH /api/v1/entregas/{id}/despachar`
- `PATCH /api/v1/entregas/{id}/en-ruta`
- `PATCH /api/v1/entregas/{id}/registrar-recepcion`

### Movimientos de inventario

- `GET /api/v1/movimientos`
- `GET /api/v1/movimientos/{id}`
- `GET /api/v1/movimientos` con filtros
  - `materialId`
  - `loteId`
  - `tipoMovimiento`
  - `fechaDesde`
  - `fechaHasta`
  - `page`
  - `size`
  - `sort`
- `POST /api/v1/movimientos/ajuste` `ALMACEN` `ADMIN`

---

## Validaciones minimas

### Validaciones de sede

- `codigo` obligatorio y unico
- `nombre` obligatorio minimo 3 caracteres
- `ciudad` obligatoria
- `direccion` obligatoria minimo 5 caracteres
- `responsableLogistica` obligatorio
- `estado` obligatorio

### Validaciones de material academico

- `sku` obligatorio y unico
- `nombre` obligatorio minimo 3 caracteres
- `categoria` obligatoria
- `nivel` obligatorio
- `unidadMedida` obligatoria
- `stockMinimo` mayor o igual a 0

### Validaciones de lote de ingreso

- `codigoLote` obligatorio y unico
- `materialId` debe existir si no `404 MATERIAL_NOT_FOUND`
- `fechaIngreso` obligatoria
- `cantidadIngresada` mayor a 0
- `cantidadDisponible` mayor o igual a 0
- `cantidadDisponible` no puede ser mayor a `cantidadIngresada`
- `proveedor` obligatorio
- si el material tiene `controlVigencia = true`, `fechaFinVigencia` es obligatoria
- `fechaFinVigencia` debe ser mayor o igual a `fechaIngreso`

### Validaciones de solicitud

- `sedeId` debe existir si no `404 SEDE_NOT_FOUND`
- `periodoAcademico` obligatorio formato `yyyy-MM`
- `fechaSolicitud` obligatoria formato `yyyy-MM-dd`
- `prioridad` obligatoria
- `estado` se define por backend al crear como `BORRADOR`
- debe existir al menos un item en el detalle
- no repetir `materialId` en el detalle
- `cantidadSolicitada` mayor a 0
- `cantidadAprobada` mayor o igual a 0

### Validaciones de entrega

- `solicitudId` debe existir si no `404 SOLICITUD_NOT_FOUND`
- la solicitud debe estar en estado `APROBADA`
- `fechaProgramada` obligatoria
- `responsableAlmacen` obligatorio
- para despachar debe existir al menos un item en el detalle de entrega
- no permitir usar lotes agotados
- no permitir usar lotes fuera de vigencia

### Validaciones de movimiento inventario

- `materialId` debe existir
- `loteId` debe existir
- `fecha` obligatoria
- `tipoMovimiento` obligatorio
- `cantidad` mayor a 0
- `referenciaTipo` obligatorio
- `referenciaId` obligatorio

---

## Concurrencia y consistencia de stock

### Estrategia recomendada

Usar transacciones en las operaciones de negocio mas sensibles:

- aprobar solicitud
- despachar entrega
- registrar ajustes de inventario

### Optimistic locking en lotes

Agregar `@Version` en `LoteIngreso` para detectar actualizaciones concurrentes de stock.

Cuando dos operaciones intenten descontar el mismo lote al mismo tiempo:

- una de ellas debe fallar con conflicto
- el backend debe responder `409 STOCK_CONFLICT`

### Flujo recomendado para despacho dentro de transaccion

1. cargar la entrega y la solicitud relacionada
2. validar estado actual
3. cargar los lotes requeridos
4. validar vigencia y cantidad disponible
5. descontar stock lote por lote
6. actualizar estado del lote si llega a `0`
7. registrar movimientos de salida
8. actualizar estado de entrega y solicitud
9. confirmar transaccion

### Mejora recomendada

Al buscar lotes para despacho, ordenar por:

1. `fechaFinVigencia` ascendente para materiales con control de vigencia
2. `fechaIngreso` ascendente
3. `id` ascendente

Esto ayuda a mantener una salida de stock consistente y mas realista.

---

## Errores estandar JSON

Responder siempre con:

```json
{
  "code": "SOME_CODE",
  "message": "Mensaje humano"
}
```

### Codigos sugeridos

- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`
- `404 SEDE_NOT_FOUND`
- `404 MATERIAL_NOT_FOUND`
- `404 LOTE_NOT_FOUND`
- `404 SOLICITUD_NOT_FOUND`
- `404 ENTREGA_NOT_FOUND`
- `409 SOLICITUD_DUPLICADA`
- `409 ESTADO_INVALIDO`
- `409 STOCK_CONFLICT`
- `422 BUSINESS_RULE_VIOLATION`
- `422 STOCK_INSUFFICIENT`
- `422 LOTE_FUERA_VIGENCIA`
- `422 SEDE_INACTIVA`
- `422 MATERIAL_INACTIVO`

---

## Checklist de aceptacion

- proyecto levanta con `./mvnw spring-boot:run`
- Swagger disponible en `/swagger-ui/index.html`
- MySQL conectado y migraciones aplicadas
- autenticacion JWT funcional
- CRUD de sedes, materiales y lotes operativo
- `POST /api/v1/solicitudes` crea en estado `BORRADOR`
- `PATCH /api/v1/solicitudes/{id}/enviar` cambia correctamente a `ENVIADA`
- no se permite crear solicitud duplicada para la misma sede y periodo academico
- no se puede crear solicitud para una sede inactiva o suspendida
- aprobacion valida stock disponible real por material
- `POST /api/v1/entregas` solo funciona con solicitud aprobada
- al despachar se descuenta stock de los lotes usados
- al agotarse un lote, su estado cambia a `AGOTADO`
- no se pueden usar lotes fuera de vigencia en despacho
- se registran movimientos de salida automaticamente
- la recepcion final actualiza la entrega y la solicitud
- si hay incidencia, la solicitud termina en estado `PARCIAL`
- `SEDE` no puede ver solicitudes de otras sedes
- filtros con paginacion y orden funcionan
- si ocurre choque concurrente en stock, el sistema responde `409 STOCK_CONFLICT`

---

## Recomendaciones

Se recomienda trabajar con:

- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- Validation
- MySQL
- Flyway o Liquibase
- Lombok opcional
- OpenAPI Swagger

### Arquitectura sugerida

Trabajar con una estructura clara por capas:

- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `mapper`
- `config`
- `security`
- `exception`

### Sugerencias de implementacion

- centralizar reglas de negocio en la capa service
- usar DTOs para request y response
- no exponer entidades JPA directamente en los controllers
- usar `@Transactional` en operaciones de aprobacion y despacho
- manejar errores de negocio con una excepcion custom y `@RestControllerAdvice`
- registrar automaticamente `createdAt` y `updatedAt`
- separar validaciones de formato de las validaciones de negocio

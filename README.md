# Blackout Esports — Roster

Microservicio independiente con Java 21, Spring Boot 3.5.0, PostgreSQL y CRUD REST.
Puerto local: **8084**. Base de datos: **roster_db**, puerto **5436**.

## Responsabilidad

Guarda equipos y alineaciones: `name`, `game`, `label`, `number`, `status` y lista ordenada `playerIds`.
Conserva los campos visuales del frontend existente. Cada ID de jugador es positivo y no puede repetirse dentro del mismo roster.
Los IDs referencian el microservicio de Jugadores; no se comparte su base de datos. En esta etapa no se verifica remotamente la existencia de esos jugadores y no se elimina un jugador desde Roster.
Torneos y calendarios pertenecen al microservicio de Torneos y no se duplican aquí.

## Permisos y autenticación

| Operación | Entra ID con Admin/Staff | Cognito |
|---|---|---|
| GET /api/rosters y GET /api/rosters/{id} | Permitido | Permitido |
| POST, PUT y DELETE /api/rosters | Permitido | Denegado |


Todas las rutas de datos requieren un token. Solo la documentación Swagger es pública.
La API comprueba firma RSA, emisor permitido, vencimiento y sujeto del JWT.
Entra requiere la audiencia de esta API, el scope configurado y el rol exacto `Admin` o `Staff`.
Un token de Cognito requiere `token_use=access` y el `client_id` configurado; aunque contenga un claim `roles: [Admin]`, solo obtiene permisos de fan.
Un ID token de Entra sin el scope de API, un ID token de Cognito y los access tokens de Microsoft Graph se rechazan.
Sin configuración de los proveedores, las operaciones protegidas quedan inaccesibles; no hay un modo de producción que omita la seguridad.

## Ejecutar con Docker

Requisitos: Docker con Compose.

1. Copiar `.env.example` a `.env` en la raíz y definir `DB_PASSWORD`.
2. Completar los valores de Entra y Cognito de tu proyecto:
   - `ENTRA_ISSUER=https://login.microsoftonline.com/<tenant-id>/v2.0`
   - `ENTRA_AUDIENCE=<audience de la API registrada en Entra>`
   - `ENTRA_SCOPE=access_as_user`
   - `COGNITO_ISSUER=https://cognito-idp.<region>.amazonaws.com/<user-pool-id>`
   - `COGNITO_CLIENT_ID=<app-client-id>`
3. Ejecutar `docker compose up --build`.
4. Abrir [Swagger](http://localhost:8084/swagger-ui.html) y usar **Authorize** con el access token, sin escribir el prefijo Bearer.

Compose carga `.env`. Para ejecutar directamente con Maven hay que exportar esas variables en la terminal; Spring Boot no carga automáticamente ese archivo.

## Ejecutar con Java

Con Java 21, Maven y PostgreSQL disponibles:
```text
cd blackout-roster
mvn spring-boot:run
```

## Contrato CRUD

- `GET /api/rosters`: lista ordenada por ID.
- `GET /api/rosters/{id}`: detalle.
- `POST /api/rosters`: devuelve 201 y cabecera Location.
- `PUT /api/rosters/{id}`: reemplaza los campos del registro.
- `DELETE /api/rosters/{id}`: devuelve 204.


Ejemplo de cuerpo para POST y PUT:
```json
{
  "name": "Blackout Esports",
  "game": "Valorant",
  "label": "Equipo principal",
  "number": "01",
  "status": "Activo",
  "playerIds": [1, 2, 3]
}
```

Errores: 400 por validación, 401 por token ausente/inválido, 403 por falta de permisos, 404 por registro inexistente y 409 por conflicto de datos.

## Pruebas

```text
cd blackout-roster
mvn verify
```

Las pruebas usan H2 en modo PostgreSQL y JWT firmados con claves RSA generadas solo para pruebas.
Cubren CRUD, persistencia, validación, CORS, documentación y restricciones de permisos. También prueban firma falsa, emisor desconocido, expiración, audience/scope incorrectos y tokens Cognito de tipo ID o de otro cliente.
El perfil `test` no permite omitir autenticación en la aplicación; el validador de claves de prueba existe únicamente en `src/test`.
GitHub Actions ejecuta la misma verificación con Java 21.

## Ramas e integración pendiente

- `main`: base inicial.
- `develop`: base de integración.
- `feature-roster`: implementación CRUD.

La conexión de los frontends y API Gateway/BFF se realizará por separado. El gateway deberá reenviar el access token, y este servicio seguirá validándolo.
Faltan las credenciales/configuración real de los proveedores y la prueba completa con tokens reales.
Para un despliegue productivo, sustituir `ddl-auto: update` por migraciones versionadas, limitar el acceso de red y configurar los orígenes CORS del despliegue.
No se han creado recursos de Azure ni AWS.

Referencias: [JWT en Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) y [verificación de tokens Cognito](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-verifying-a-jwt.html).

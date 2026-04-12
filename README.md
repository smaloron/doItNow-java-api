# DoItNow - API de Gestion de Taches

Application Spring Boot 4 avec MongoDB, authentification JWT et notifications WebSocket en temps reel.

## Stack technique

- Java 17
- Spring Boot 4.0.3
- MongoDB 7
- Spring Security + JWT
- WebSocket (STOMP / SockJS)
- Lombok
- Maven

## Lancement

### Avec Docker (recommande)

```bash
docker compose up --build
```

| Service       | URL                    |
|---------------|------------------------|
| API           | http://localhost:8081   |
| Mongo Express | http://localhost:8082   |
| MongoDB       | localhost:27017         |

### Sans Docker

Pre-requis : Java 17+, MongoDB en local sur le port 27017.

```bash
./mvnw spring-boot:run
```

### Donnees initiales

Au premier demarrage, si la base est vide, un compte admin et 8 taches exemples sont crees automatiquement :

- **Email** : `admin@doitnow.com`
- **Mot de passe** : `admin123`

## Authentification

L'API utilise des tokens JWT (validite : 24h). Les endpoints `/api/auth/**`, `/api/health` et `/api/welcome` sont publics. Tous les autres necessitent un header `Authorization: Bearer <token>`.

### Inscription

```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Reponse `200` :
```json
{
  "token": "eyJhbG..."
}
```

### Connexion

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Reponse `200` :
```json
{
  "token": "eyJhbG..."
}
```

## API Taches

Tous les endpoints ci-dessous necessitent le header `Authorization: Bearer <token>`.
Les taches sont isolees par utilisateur.

### CRUD

| Methode | Endpoint            | Description             |
|---------|---------------------|-------------------------|
| GET     | `/api/tasks`        | Lister les taches (pagine) |
| GET     | `/api/tasks/{id}`   | Detail d'une tache      |
| POST    | `/api/tasks`        | Creer une tache         |
| PUT     | `/api/tasks/{id}`   | Modifier une tache      |
| DELETE  | `/api/tasks/{id}`   | Supprimer une tache     |

### Filtres et recherche

| Methode | Endpoint                       | Description                  |
|---------|--------------------------------|------------------------------|
| GET     | `/api/tasks/search?keyword=x`  | Recherche par titre/description |
| GET     | `/api/tasks/tag/{tag}`         | Filtrer par tag              |
| GET     | `/api/tasks/priority/{priority}` | Filtrer par priorite       |
| GET     | `/api/tasks/overdue`           | Taches en retard             |
| GET     | `/api/tasks/stats`             | Statistiques                 |

### Pagination (GET /api/tasks et /api/tasks/search)

| Parametre   | Defaut      | Description           |
|-------------|-------------|-----------------------|
| `page`      | `0`         | Numero de page        |
| `size`      | `10`        | Taille de page        |
| `sortBy`    | `createdAt` | Champ de tri          |
| `direction` | `desc`      | `asc` ou `desc`       |

### Creer une tache

```
POST /api/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "Ma tache",
  "description": "Description optionnelle",
  "priority": "HIGH",
  "tags": ["urgent", "client"],
  "dueDate": "2026-05-01"
}
```

| Champ         | Type       | Requis | Contraintes                    |
|---------------|------------|--------|--------------------------------|
| `title`       | string     | oui    | 3 a 100 caracteres             |
| `description` | string     | non    | 500 caracteres max             |
| `priority`    | enum       | non    | `LOW`, `MEDIUM`, `HIGH`, `URGENT` (defaut: `MEDIUM`) |
| `tags`        | string[]   | non    |                                |
| `dueDate`     | date       | non    | Format `YYYY-MM-DD`, date future ou presente |

### Reponse tache

```json
{
  "id": "664a...",
  "title": "Ma tache",
  "description": "Description optionnelle",
  "completed": false,
  "userId": "663f...",
  "priority": "HIGH",
  "tags": ["urgent", "client"],
  "dueDate": "2026-05-01",
  "createdAt": "2026-04-12T10:30:00",
  "updatedAt": "2026-04-12T10:30:00"
}
```

### Statistiques

```
GET /api/tasks/stats
```

```json
{
  "total": 8,
  "completed": 2,
  "pending": 6,
  "overdue": 1,
  "completionRate": 25.0,
  "tasksByPriority": {
    "LOW": 2,
    "MEDIUM": 2,
    "HIGH": 3,
    "URGENT": 1
  }
}
```

## Autres endpoints

| Methode | Endpoint        | Auth   | Description    |
|---------|-----------------|--------|----------------|
| GET     | `/api/health`   | non    | `{"status": "UP"}` |
| GET     | `/api/welcome`  | non    | Message de bienvenue |

## WebSocket

Connexion STOMP via SockJS sur `/ws`. Le token JWT doit etre passe dans le header `Authorization` de la frame CONNECT.

**Topic** : `/topic/tasks/{userId}`

Notifications envoyees lors de la creation, modification ou suppression d'une tache :

```json
{
  "type": "CREATED",
  "taskId": "664a...",
  "task": { ... }
}
```

Types : `CREATED`, `UPDATED`, `DELETED` (pour `DELETED`, le champ `task` est `null`).

## Gestion des erreurs

| Code | Cas                       | Format de reponse                              |
|------|---------------------------|------------------------------------------------|
| 400  | Validation echouee        | `{ "champ": "message d'erreur" }`              |
| 404  | Ressource non trouvee     | `{ "error": "Non trouve", "message": "..." }`  |
| 409  | Doublon (email existant)  | `{ "error": "Conflit", "message": "..." }`     |

## Tests

```bash
./mvnw test
```

Les tests d'integration utilisent Testcontainers (MongoDB). Docker doit etre demarre.

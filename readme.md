https://github.com/Krisolut/MediaRatingPlatform

```
Client ──► Router ──► [AuthMiddleware?] ──► Controller ──► Service ──► Repository
                      │
                      └─► JwtService.verify(token)
```

---

### Router
- Registriert Endpunkte (`method + path + handler + requiresAuth`)
- Entscheidet bei jeder Anfrage, ob Auth nötig ist
- Übergibt Request an Controller oder AuthMiddleware

---

### AuthMiddleware
- Prüft Header `Authorization: Bearer`
- Verifiziert Token über `JwtService`
- Schreibt authentifizierte User-ID ins `HttpExchange`-Attribut

---

### Controller
- Liest Request-Body (JSON) → DTO mit `JsonUtil.readJson()`
- Ruft Services auf, um Daten zu verarbeiten
- Wandelt Ergebnisse in JSON um → `JsonUtil.writeJson()`
- **Alle Antworten und Fehlermeldungen werden als JSON gesendet**

> **Jeder `HttpExchange` wird zu JSON übersetzt:**  
> - Eingehende Bodies werden als JSON gelesen und in Objekte umgewandelt  
> - Ausgehende Antworten (Erfolg oder Fehler) werden als JSON serialisiert  
> - Einheitliches Format durch `JsonUtil` (UTF-8, `application/json`)

---

### Service-Schicht
- Enthält Fachlogik (z. B. Registrierung, Login, Rating-Regeln)  
- Hashing über `PasswordHasher` (BCrypt)  
- Token-Erzeugung über `JwtService.generateToken()`  
- Greift nur über Repository-Interfaces auf Daten zu  

---

### Repository
- Aktuell In-Memory (HASHMAP-basiert)  
- Methoden wie `save()`, `findById()`, `findByUsername()`  
- Rückgaben sind `Collections.unmodifiableList()` → read-only  
- Später austauschbar gegen JDBC/PostgreSQL  

---

### Utils
- **JsonUtil** – JSON-(De)Serialisierung & Fehlerausgaben (`writeJson`, `readJson`, `sendError`)  
- **ErrorHandler** – einheitliche Fehlerstruktur `{ error, code, timestamp }`  

---

### 🔐 Authentifizierungs-Workflow

```
POST /api/auth/token
├─ Controller liest JSON (username, password)
├─ AuthService prüft Credentials + BCrypt.checkpw()
├─ JwtService.generateToken(userId)
└─ Response: JSON { "user": {...}, "token": "<jwt>" }

Nachfolgende Requests:
Authorization: Bearer <jwt>
```
- Tokens enthalten:
  - `sub` = User-ID  
  - `iat` = Erstellzeitpunkt  
  - `exp` = Ablaufzeit (+2 h)  
- Bei jedem geschützten Request prüft `AuthMiddleware` Signatur & Ablaufzeit.  

---

### 💡 Design-Entscheidungen

| Konzept | Nutzen |
|----------|---------|
| **JSON-only-Kommunikation** | Einheitliches Format für Requests, Responses und Fehler |
| **Interfaces für Repositories & Hasher** | Lose Kopplung → leicht test- & austauschbar |
| **Middleware statt Inline-Auth** | Klare Trennung: Auth-Logik außerhalb der Controller |
| **Read-only Collections** | Repos bleiben vor externer Manipulation geschützt |
| **SOLID-Prinzipien** | Wartbar, erweiterbar, testfreundlich |

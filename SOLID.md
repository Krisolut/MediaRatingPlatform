# SOLID

Dieses Dokument beschreibt die angewendeten **SOLID-Prinzipien** in der Architektur der Media Rating Platform.

## 1) Single Responsibility Principle (SRP)
**Prinzip:** Jede Klasse hat genau eine Verantwortlichkeit.

**Anwendung:**
- **Controller** kümmern sich ausschließlich um HTTP-Anfragen/Responses und JSON-Parsing.
- **Services** kapseln Geschäftslogik (z. B. Bewertung pro User/Media, Bestätigung von Kommentaren).
- **Repositories** sind allein für Datenzugriff und SQL verantwortlich.

**Nutzen:** Änderungen in HTTP oder Datenbank betreffen nicht automatisch die Business-Logik.

---

## 2) Open/Closed Principle (OCP)
**Prinzip:** Klassen sind offen für Erweiterung, aber geschlossen für Modifikation.

**Anwendung:**
- Neue Features (z. B. neue Endpunkte oder zusätzliche Geschäftsregeln) können als neue Controller/Services ergänzt werden, ohne bestehende Klassen stark zu verändern.
- Bestehende Service-Logik kann durch zusätzliche Methoden erweitert werden.

**Nutzen:** Geringeres Risiko, bestehende Funktionalität zu brechen.

---

## 3) Liskov Substitution Principle (LSP)
**Prinzip:** Subtypen müssen überall dort einsetzbar sein, wo ihre Basistypen erwartet werden.

**Anwendung:**
- Falls Interfaces/Abstraktionen für Repositories oder Services existieren, sollten Implementierungen (z. B. PostgresRepository) problemlos austauschbar sein (z. B. gegen InMemoryRepository für Tests).

**Nutzen:** Austauschbarkeit von Implementierungen ermöglicht Testing und Migrationen.

---

## 4) Interface Segregation Principle (ISP)
**Prinzip:** Clients sollten nicht gezwungen sein, Methoden zu nutzen, die sie nicht benötigen.

**Anwendung:**
- Repository-Interfaces können nach Domänenobjekten getrennt sein (z. B. UserRepository, MediaRepository, RatingRepository).
- Services sollten nur die Methoden enthalten, die für ihren Funktionsbereich notwendig sind.

**Nutzen:** Geringere Kopplung, klarere Verantwortlichkeiten.

---

## 5) Dependency Inversion Principle (DIP)
**Prinzip:** Abstraktionen sollen von Details unabhängig sein; Details hängen von Abstraktionen.

**Anwendung:**
- Services arbeiten idealerweise gegen Repository-Interfaces, nicht gegen konkrete Implementierungen.
- Damit kann die Speicherstrategie (PostgreSQL, InMemory, Mock) ohne Änderung der Service-Logik ausgetauscht werden.

**Nutzen:** Höhere Testbarkeit und Austauschbarkeit, besonders bei Infrastruktur-Komponenten.
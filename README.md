Operation Eignung (Android)

Eine Android-App auf Basis von Jetpack Compose, die dich beim Üben für mehrstufige IT-Eignungstests unterstützt. Fragen werden lokal in einer Room-Datenbank gespeichert und lassen sich per JSON-Import aus KI-generierten Datensätzen einspielen oder manuell pflegen.

## Hauptfunktionen
- **Dashboard** mit Kennzahlen zu Gesamtanzahl, Erfolgsquote und Fehlern sowie einer Kategorietabelle und Schnellaktionen für Quizstart und Import.
- **Quiz-Modus** für Multiple-Choice-, Freitext- und Code-Fragen inklusive Filter nach Modus (Zufall, nur neue, nur falsche Fragen), Fragetyp und Kategorien, Statistik-Anzeige, manueller Bewertung sowie direktem Zugriff auf Bearbeitung oder Löschen der aktuellen Frage.
- **Fragenverwaltung** mit Suchfeld, Filtern, Bottom-Sheet-Editor und Formular für neue oder bestehende Fragen inkl. Maske für Multiple-Choice-Lösungen
- **Kategorienverwaltung** zum Anlegen, Löschen, Aufräumen ungenutzter Kategorien sowie Verschmelzen von zwei Kategorien inklusive automatischer Umhängung der verknüpften Fragen.
- **Aufräumwerkzeuge** für automatische Kategorisierung, Duplikat- und Leerzeilenbereinigung, Statistik-Reset und komplettes Leeren der Fragentabelle.
- **JSON-Import** mit Datei- oder Texteingabe, Konfliktstrategie (überspringen oder aktualisieren), detailliertem Ergebnisbericht und strenger Validierung der Eingabedaten.
- **Prompt-Vorlage** zum Erzeugen kompatibler JSON-Fragen mit gängigen KI-Tools sowie Möglichkeit zum Kopieren und Anpassen direkt in der App.
- **Info-Bereich** mit kompakten Nutzungstipps, Datenschutzhinweisen und Quicklinks zu Import und Quiz.

## Technischer Überblick
- **UI-Framework:** Jetpack Compose mit Material 3 Design-Komponenten und Navigation Compose.
- **Dependency Injection:** Hilt mit `@HiltAndroidApp`-Application und Modul für die Room-Datenbank.
- **Persistenz:** Room-Datenbank mit `questions`- und `categories`-Tabellen, TypeConverter für Fragetypen sowie umfangreichen DAO-Abfragen für Quiz, Statistiken und Verwaltung.
- **Zielplattform:** Android 24+ mit Kotlin 2.1 und Java 21 Toolchain, getestet über Gradle Wrapper.【F:app/build.gradle.kts†L1-L43】


## Datenmodell
- **Fragen (`questions`):** Enthält Fragetext, optionalen Code, bis zu vier Antworten mit Bitmaske für korrekte Optionen, Lösungsvarianten, Kategorie-Referenz sowie Zähler für richtige/falsche Antworten.【F:app/src/main/kotlin/com/example/operationeignung/data/database/entities/Question.kt†L1-L54】
- **Kategorien (`categories`):** Eindeutige Namen, die als Fremdschlüssel bei Fragen referenziert werden.
- **Fragetypen:** Enum `QuestionType` mit Werten `choice`, `text`, `code` und Hilfsmethoden zur Konvertierung.

Viel Erfolg beim Lernen – und Feedback ist willkommen!

## Screenshots
<img width="396" height="890" alt="dashboard" src="https://github.com/user-attachments/assets/00929cca-3fa0-4d76-ad39-1c2df335bf53" />
<img width="401" height="886" alt="import" src="https://github.com/user-attachments/assets/74afd2db-f2c7-44ef-9cde-c61dcbd45db9" />
<img width="400" height="879" alt="quiz" src="https://github.com/user-attachments/assets/ab21905f-e765-4134-bcdd-e65d94b57acb" />
<img width="399" height="888" alt="fragenVerwalten" src="https://github.com/user-attachments/assets/4a61f422-a692-4848-947d-96020a5f3bed" />
<img width="399" height="884" alt="kategorienVerwalten" src="https://github.com/user-attachments/assets/37428968-fbc8-4d69-818b-bed1df95a9d9" />
<img width="397" height="884" alt="DatenBereinigen" src="https://github.com/user-attachments/assets/f6045557-e726-45be-b42e-6848e6d87de2" />
<img width="397" height="888" alt="kiPromt" src="https://github.com/user-attachments/assets/8587abb4-6a35-4be8-ae84-338cba49bc14" />
<img width="397" height="886" alt="info" src="https://github.com/user-attachments/assets/f041d704-d0be-4eb2-809d-ca61d5bcb3af" />

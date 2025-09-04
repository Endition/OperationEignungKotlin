package com.example.operationeignung.ui.prompt

object PromptTemplates {
    // Den bisherigen Prompt-String 1:1 hierher verschieben,
    // aber als Triple-Quoted String statt + Konkatenation.
    val Default: String = """
    Erstelle anspruchsvolle Multiple-Choice- und Programmierfragen für 
    einen Einstellungstest im Bereich IT. 
    Die Fragen sollen aus folgenden Themengebieten stammen:
    -Grundlagen
    -Netzwerk
    -Programmierung
    -Datenbanken
    -Linux
    -Windows
    Format: JSON (Liste von Fragen-Objekten)
    Jede Frage ist ein Objekt mit folgenden Feldern:

    [
      {
        "question_text": "Fragetext",
        "question_code": "Codebeispiel (optional)",
        "answers": ["Antwort A", "Antwort B", "Antwort C", "Antwort D"],
        "correct_mask": 9,
        "type": "choice|text|code",
        "solution_text": "Musterlösung (Text)",
        "solution_code": "Musterlösung (Programmiercode)",
        "category": "Kategorie (Pflichtfeld, z. B. 'Netzwerktechnik')"
      }
    ]

    Hinweise zu den JSON-Feldern.:
    - question: Klar formulierte Frage. Abkürzungen immer mindestens 1× pro Frage erklären (z.B. "JWT (JSON Web Token)").
    - question_code: Enthält den Codeteil einer Frage. Verwende [[PIPE]] statt | im Code, Zeilenumbrüche als \n.
    - answers: Nur bei type=choice: Liste mit 4 Antwortmöglichkeiten als String, sonst leere Strings.
    - correct_mask: Nur bei type=choice: Bitmaske: A=1, B=2, C=4, D=8. Addieren bei mehreren korrekten Antworten. 
    - type:
    -- choice: Multiple-Choice-Fragen
    -- code: Programmierfragen (ohne A–D) 
    -- text: Freitextfragen (ohne A–D)
    - solution_text: Erläuterung der richtigen Antworten bei choice-Fragen, Bei text-Fragen: Musterlösung
    - solution_code: Nur bei type=code mit Musterlösung Programmiercode in der jeweiligen Sprache; sonst leer.
    - category: Kategorie der Frage (Pflichtfeld, z.B. 'Linux',  'Netzwerktechnik') als string.

    Anforderungen:
    - Niveau: Fortgeschritten 
    - Keine reinen Definitionsfragen – immer praxisnah und mit Kontext.
    - Gleichmäßige Verteilung über Themenbereiche.

    Gib nur den JSON-Inhalt ohne zusätzliche Formatierung aus, sodass er in die Zwischenablage kopiert werden kann.
""".trimIndent()
}
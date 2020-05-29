# Sudan statistics plugin for translations

Dies ist eine technische Dokumentation für das Statistik-Plugin für Übersetzungen innerhalb der Metadaten

## Einführung

Die vorliegende Dokumentation beschreibt die Installation, Konfiguration und den Einsatz des Plugins. Mit Hilfe dieses Plugins können die Übersetzungsarbeiten in den Metadatenfeldern ```Title (Arabic)```, ```Title (English)```, ```Description (English)``` und ```Description (Arabic)```.

| Details ||
|--- |--- |
| Version | 1.0.0 |
| Identifier | plugin_statistics_sudan_activity_by_user |
| Source code | - Quellcode noch nicht öffentlich verfügbar -|
| Kompatibilität | Goobi Workflow 2020.05 |
| Dokumentationsdatum | 28.05.2020 |

## Installation

Zur Nutzung des Plugins müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/statistics/plugin_intranda_statistics_sudan.jar
/opt/digiverso/goobi/plugins/GUI/plugin_intranda_statistics_sudan-GUI.jar
```

Zusätzlich muss in der Datenbank folgende Funktion erstellt werden:

```sql
DROP FUNCTION IF EXISTS wordcount;

    DELIMITER $$
    CREATE FUNCTION wordcount(str TEXT CHARSET utf8mb4)
            RETURNS INT
            DETERMINISTIC
            SQL SECURITY INVOKER
            NO SQL
       BEGIN
         DECLARE wordCnt, idx, maxIdx INT DEFAULT 0;
         DECLARE currChar, prevChar BOOL DEFAULT 0;
         SET maxIdx=char_length(str);
         WHILE idx < maxIdx DO
             SET currChar=SUBSTRING(str, idx, 1) RLIKE '[[:alnum:]]';
             IF NOT prevChar AND currChar THEN
                 SET wordCnt=wordCnt+1;
             END IF;
             SET prevChar=currChar;
             SET idx=idx+1;
         END WHILE;
         RETURN wordCnt;
       END
     $$
     DELIMITER ;
```

Dieser Funktion kann ein utf8-codierter Text übergeben werden. Der Text wird Zeichen für Zeichen überprüft. Wenn das aktuelle Zeichen ein alnumerisches Zeichen ist ( Buchstaben, Zahlen, Punkt, Komma, Buchstaben mit Diakritika, Klammern), das vorherige Zeichen jedoch nicht (Nichts, Leerzeichen, Zeilenumbruch, Tabulator), beginnt an dieser Stelle ein neues Wort und der Wortzähler wird erhöht. Am Ende wird der Wortzähler zurück gegeben.

## Konfiguration

Um Zugriff auf die Auswertung zu haben, muss der Nutzer über die Berechtigung ```view_translation_activity``` verfügen.

Anschließend kann der Menüpunkt ```Übersetzungen``` im Bereich ```Controlling``` ausgewält werden.

## Nutzung

Um den Zeitraum der Auswertung zu begrenzen, können die beiden Felder ```Zeitraum von``` und ```Zeitraum bis``` für das Startdatum und Enddatum genutzt werden. Hier kann ein Datum in der Form YYYY-MM-DD angegeben werden, beide Angaben sind optional. Wenn das Startdatum nicht ausgefüllt wurde, gilt das Datum, an dem der erste Schritt abgeschlossen wurde. Fehlt das Enddatum, dann wird der aktuelle Zeitpunkt genutzt.

Im Feld ```Einheit``` wird festgelegt, in welchen Zeiträumen die Auswertung zusammengefasst werden soll. Hier kann aus den Werten ```Tage```, ```Monate```, ```Quartale``` oder ```Jahre``` gewählt werden.

In diesem Plugin können zwei Auswertungen generiert werden.

Die Auswertung ```Überblick``` listet für jeden Zeitraum innerhalb des Start- und Enddatums auf, welcher Nutzer wie viele Schritts ```Translation of Arabic content to English``` oder ```Translation of English content to Arabic``` bearbeitet hat, und wie viele Worte in den Feldern ```Title (Arabic)```, ```Title (English)```, ```Description (English)``` und ```Description (Arabic)``` in diesen Schritten insgesamt enthalten sind.

Die ```Detailierte Anzeige``` listet jeden Schritt ```Translation of Arabic content to English``` oder ```Translation of English content to Arabic```, der innerhalb des Start- und Enddatums abgeschlossen wurde.
Zu jedem Schritt wird außerdem der Nutzer, der zugehörige Vorgang, sowie der Inhalt und die Anzahl der Worte aus den vier Feldern ```Title (Arabic)```, ```Title (English)```, ```Description (English)``` und ```Description (Arabic)``` angezeigt.

Die beiden Auswertungen lassen sich auch jeweils als Excel Datei herunterladen.

## Weitere Informationen

SQL query for overview report:

```sql
    SELECT
    DATE_FORMAT(s.BearbeitungsEnde, '%Y-%m') AS plugin_statistics_sudan_timeRange,
    WORDCOUNT(GROUP_CONCAT(m1.value SEPARATOR ' ')) AS plugin_statistics_sudan_titleCount,
    WORDCOUNT(GROUP_CONCAT(m2.value SEPARATOR ' ')) AS plugin_statistics_sudan_titlearabicCount,
    WORDCOUNT(GROUP_CONCAT(m3.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionCount,
    WORDCOUNT(GROUP_CONCAT(m4.value SEPARATOR ' ')) AS plugin_statistics_sudan_descriptionarabicCount,
    COUNT(s.Titel) AS plugin_statistics_sudan_workflowTitleCount,
    CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName
    FROM
    metadata m1
        JOIN
    metadata m2 ON m1.processid = m2.processid
        JOIN
    metadata m3 ON m1.processid = m3.processid
        JOIN
    metadata m4 ON m1.processid = m4.processid
        JOIN
    schritte s ON m1.processid = s.ProzesseID
        LEFT JOIN
    benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID
    WHERE
    m1.name = 'TitleDocMain'
        AND m2.name = 'TitleDocMainArabic'
        AND m3.name = 'ContentDescription'
        AND m4.name = 'ContentDescriptionArabic'
        AND s.typMetadaten = TRUE
        AND s.Bearbeitungsstatus = 3
        AND s.titel like '%ranslat%'
        AND s.BearbeitungsEnde BETWEEN '2019-01-01' AND '2020-12-31'
    GROUP BY plugin_statistics_sudan_timeRange, plugin_statistics_sudan_userName;
```

SQL query for detailed report:

```sql
    SELECT
    m1.processid,
    m1.value AS plugin_statistics_sudan_title,
    WORDCOUNT(m1.value) AS plugin_statistics_sudan_titleCount,
    m2.value AS plugin_statistics_sudan_titlearabic,
    WORDCOUNT(m2.value) AS plugin_statistics_sudan_titlearabicCount,
    m3.value AS plugin_statistics_sudan_description,
    WORDCOUNT(m3.value) AS plugin_statistics_sudan_descriptionCount,
    m4.value AS plugin_statistics_sudan_descriptionarabic,
    WORDCOUNT(m4.value) AS plugin_statistics_sudan_descriptionarabicCount,
    s.Titel AS plugin_statistics_sudan_workflowTitle,
    p.Titel AS plugin_statistics_sudan_processTitle,
    CONCAT(u.Nachname, ', ', u.Vorname) AS plugin_statistics_sudan_userName
    FROM
    metadata m1
        JOIN
    metadata m2 ON m1.processid = m2.processid
        JOIN
    metadata m3 ON m1.processid = m3.processid
        JOIN
    metadata m4 ON m1.processid = m4.processid
        JOIN
    schritte s ON m1.processid = s.ProzesseID
        LEFT JOIN
    prozesse p ON s.ProzesseID = p.ProzesseID
        LEFT JOIN
    benutzer u ON s.BearbeitungsBenutzerID = u.BenutzerID
    WHERE
    m1.name = 'TitleDocMain'
        AND m2.name = 'TitleDocMainArabic'
        AND m3.name = 'ContentDescription'
        AND m4.name = 'ContentDescriptionArabic'
        AND s.typMetadaten = TRUE
        AND s.titel like '%ranslat%'
        AND s.Bearbeitungsstatus = 3
        AND s.BearbeitungsEnde BETWEEN '2019-01-01' AND '2020-12-31';
```
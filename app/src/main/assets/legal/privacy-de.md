# WageTrack Datenschutzerklärung

Prüfentwurf — 8. Oktober 2026. Noch nicht zur Veröffentlichung geeignet: Anbieterangaben und Freigabeprüfung fehlen. Dokumentversion: 2026-10-08-draft1.

## 1. Verantwortlicher

WageTrack (Android-Paket com.paydaytracker.app) wird von {{PUBLISHER_LEGAL_NAME}}, {{PUBLISHER_POSTAL_ADDRESS}}, {{PUBLISHER_COUNTRY}} angeboten. Kontakt für Datenschutz und Support: {{PRIVACY_EMAIL}}. Falls erforderlich: {{REPRESENTATIVE_OR_DATA_PROTECTION_OFFICER_CONTACT}}.

Diese Erklärung beschreibt lokale Einträge, optionale Google-Anmeldung, Sicherungen und Support. Vertriebsplattformen und Speicheranbieter verarbeiten Daten auch nach ihren eigenen Datenschutzhinweisen.

## 2. Einträge bleiben standardmäßig auf deinem Gerät

WageTrack speichert deine Eingaben im privaten Android-App-Speicher: Arbeitsplätze, Lohnsätze, Schichten, Daten, Arbeits- und Pausenzeiten, Status, Notizen, Vorlagen, Ausgaben, Budgets, Sparziele, Einzahlungen und Vergleichsbeträge aus Lohnabrechnungen. Daraus erstellt die App deinen Verlauf, Schätzungen und die von dir gewählten Funktionen.

Freiwillige Profilfelder umfassen Name, Adresse, E-Mail, Telefonnummer und Steuer-ID. Lohneinstellungen können Steuerklasse, Bundesland, Versicherungsbeitrag, Kinderlosigkeit und Kirchensteuer umfassen. Kirchensteuerangaben können eine Religionszugehörigkeit erkennen lassen. Lass unnötige persönliche Felder leer; nutze manuelle Abzüge, wenn du keine detaillierten Steuerangaben eingeben möchtest. Die App übermittelt diese Felder nicht an Firebase. Gib sensible Angaben anderer Personen nur mit entsprechender Berechtigung ein.

Gespeichert werden außerdem Sprache, Darstellung, Währung, Benachrichtigungseinstellungen, laufende Zeiterfassung und Sicherungseinstellungen. Die lokale Bestätigung der Vereinbarung enthält Dokumentversionen, Sprache und Gerätezeitpunkt. Sie ist keine Werbekennung und kein Konto und wird nicht in exportierte Sicherungen aufgenommen.

Diese Implementierung enthält keine Werbung, Analyse-SDKs oder automatische Absturzübermittlung. WageTrack verkauft keine Informationen und lädt deine Schicht-, Ausgaben- oder Profildatenbank nicht auf einen Anbieterserver hoch. Die Berechnungen sind lokale Schätzungen und treffen keine automatisierten Entscheidungen über Beschäftigung, Kreditwürdigkeit oder rechtliche Ansprüche.

## 3. Optionale Anmeldung

Ist die Google-Anmeldung konfiguriert, initialisieren die Kontoeinstellungen Firebase Authentication. Beim Verbinden werden Google-Anmeldedaten zur Prüfung an Firebase gesendet. Das Konto kann den mit Google verknüpften Namen, die E-Mail-Adresse, Profilfoto-URL, Kennungen und Anmeldemetadaten enthalten. Firebase verarbeitet außerdem Verbindungsinformationen wie IP-Adresse und User-Agent zur Anmeldung und Missbrauchsabwehr. Die App erhält dein Google-Passwort nicht. Bereits das Öffnen der Kontoeinstellungen kann in einem konfigurierten Build den Dienst initialisieren.

Google/Firebase stellt den Anmeldedienst bereit; der Anbieter verwaltet das Firebase-Projekt. Zuständige Vertragseinheit und Auftragsverarbeitungsbedingungen: {{FIREBASE_CONTRACTING_ENTITY_AND_PROCESSOR_DETAILS}}. Die Anmeldung ist freiwillig und von den lokalen Aufzeichnungen getrennt. Sie aktiviert weder automatische Drive-Synchronisierung noch das Hochladen finanzieller Einträge.

Firebase Authentication wird in den USA betrieben. Die für den Vertrag des Anbieters geltenden Garantien für internationale Übermittlungen müssen vor Veröffentlichung ergänzt werden: {{INTERNATIONAL_TRANSFER_SAFEGUARDS_AND_HOW_TO_OBTAIN_COPY}}. Googles aktuelle Dokumentation erläutert Sicherheitsmaßnahmen, Verarbeitungsorte und Übermittlungsregelungen:

https://firebase.google.com/support/privacy

https://policies.google.com/privacy

## 4. Sicherungen, Exporte und Weitergabe

Du entscheidest, ob und wohin du JSON-Sicherungen oder CSV-Daten exportierst, Dateien wiederherstellst oder automatische Ordnersicherungen aktivierst. Die Android-Dateiauswahl kann lokalen Speicher oder Cloud-Anbieter wie Google Drive anbieten. Der gewählte Anbieter erhält die dort gespeicherten Dateien und verarbeitet sie nach seinen eigenen Regeln. WageTrack erhält keinen uneingeschränkten Zugriff auf dein Google-Drive-Konto.

Die automatische Ordnersicherung hält eine aktuelle WageTrack-backup.json und eine vorherige WageTrack-previous.json vor. Beim sicheren Ersetzen entstehen vorübergehende Dateien. Manuelle Exporte können weitere Dateien erzeugen. Speicheranbieter können eigene Versionen und Papierkorbkopien behalten. Die Ordnerberechtigung bleibt bis zum Widerruf oder Entfernen der App bestehen; nach Deaktivierung automatischer Sicherungen enden automatische Schreibvorgänge.

Sicherungen können Finanz- und freiwillige Profildaten enthalten. WageTrack verschlüsselt JSON- und CSV-Exporte nicht. Die App-Sperre schützt keine externen Kopien. Wähle einen vertrauenswürdigen Speicherort und teile Dateien nur mit vorgesehenen Empfängern. Das Abschalten der Sicherung, Löschen von App-Einträgen oder Deinstallieren löscht keine exportierten Dateien. Entferne Dateien, frühere Kopien und Papierkorbinhalte separat. Durch Wiederherstellung können zuvor gelöschte Angaben zurückkehren.

## 5. Gerätefunktionen und Sicherheit

Benachrichtigungen und genaue Alarme dienen deinen ausgewählten Erinnerungen. Android fragt Berechtigungen getrennt von der Vereinbarung ab; du kannst sie in den Android-Einstellungen ändern. Nach Neustart oder Zeitänderung werden Erinnerungen neu geplant. Eine Vordergrundbenachrichtigung unterstützt eine laufende Zeiterfassung. Je nach Geräteeinstellungen können Personen, die deinen Bildschirm sehen, Arbeitsinformationen in Benachrichtigungen und Widgets erkennen.

Android prüft biometrische Merkmale oder die Geräteentsperrung. WageTrack erhält nur das Prüfergebnis, keine Fingerabdruck- oder Gesichtsvorlage. Eine freiwillige App-PIN wird als gesalzener Prüfwert mit Schutz durch einen Android-Keystore-Schlüssel gespeichert, nicht als eingegebene Ziffern. Die Sperre beschränkt den Zugriff; sie verschlüsselt weder die Room-Datenbank noch exportierte Dateien. Für lokale Daten nutzt die App privaten Android-Speicher und Geräteschutz, für Firebase-Verbindungen HTTPS. Ein kompromittiertes Gerät kann dadurch nicht vollständig abgesichert werden.

Automatische Android-App-Sicherungen sind deaktiviert. Die App fordert keinen Zugriff auf Kontakte, Standort, Mikrofon oder Kamera an. Eine unsichtbare lokale Migrationskomponente kann Daten älterer WebView-Versionen lesen; sie ist kein Webanalysedienst.

## 6. Zwecke und Rechtsgrundlagen

Soweit die Verarbeitung durch den Anbieter dem Datenschutzrecht unterliegt, beruhen angeforderte App-/Kontofunktionen und zugehöriger Support auf Art. 6 Abs. 1 Buchst. b DSGVO. Notwendige Sicherheits- und Missbrauchsschutzmaßnahmen beruhen auf Buchst. f; das Interesse ist der Schutz des Dienstes und der Nutzer. Gesetzliche Pflichten beruhen auf Buchst. c. Einwilligungspflichtige optionale Verarbeitung benötigt eine gesonderte, konkrete Entscheidung nach Buchst. a. Die Kenntnisnahme dieser Erklärung ist keine solche Einwilligung. Der Anbieter muss diese Grundlagen und den Umgang mit sensiblen Lohneinstellungen vor Veröffentlichung prüfen.

Die grundlegende Zeiterfassung benötigt weder Konto noch Identitätsangaben. Funktionsbezogene Pflichtfelder sind freiwillig; ohne sie funktioniert die jeweilige Funktion möglicherweise nicht. Der Anbieter kann ausschließlich lokal gespeicherte Einträge nicht aus der Ferne abrufen.

## 7. Aufbewahrung und Löschung

Lokale Einträge bleiben bis zur Löschung erhalten. Einstellungen → Daten löschen entfernt aktive App-Einträge und deaktiviert automatische Sicherungen. Geräteeinstellungen, Bestätigungsnachweis, Anmeldestatus und alte Migrationsdaten können bleiben. Android-Einstellungen → Apps → WageTrack → Speicher → Speicherinhalt löschen oder die Deinstallation entfernt den privaten App-Speicher. Ein Online-Anmeldekonto und externe Dateien werden dadurch nicht gelöscht.

Einstellungen → Google-Konto → Anmeldekonto löschen fordert die Löschung des WageTrack-Firebase-Kontos an. Aus Sicherheitsgründen kann eine erneute Anmeldung nötig sein. Dein Google-Konto wird nicht gelöscht. Die vom Anmeldekonto unabhängigen lokalen Aufzeichnungen bleiben, bis du sie separat löschst. Externe Löschanfragen sind unter {{PUBLIC_ACCOUNT_DELETION_URL}} oder über {{PRIVACY_EMAIL}} möglich; der Anbieter prüft die Kontoinhaberschaft, ohne dein Passwort anzufordern.

Laut Google bleiben IP-Protokolle von Authentication einige Wochen gespeichert; andere Anmeldeinformationen werden nach eingeleiteter Kontolöschung innerhalb von 180 Tagen aus aktiven Systemen und Sicherungen entfernt. Für unabhängig gespeicherte Google-Konto- und Dateianbieterdaten gelten deren Aufbewahrungsregeln.

Bei Supportanfragen erhält der Anbieter die von dir übermittelten Kontaktdaten, Nachrichten und Anhänge. Sende keine unnötigen Lohnabrechnungen, Steuer-IDs oder Sicherungen. Supportanbieter und Aufbewahrungsdauer: {{SUPPORT_PROVIDER_AND_RETENTION_PERIOD}}. Zusätzliche gesetzlich erforderliche Aufbewahrung muss begrenzt und erklärt werden.

## 8. Deine Möglichkeiten und Rechte

Nach anwendbarem Recht kannst du Auskunft, Berichtigung, Löschung, Einschränkung und Datenübertragbarkeit verlangen, einer auf berechtigte Interessen gestützten Verarbeitung widersprechen und Einwilligungen für die Zukunft widerrufen. Nutze den oben genannten Datenschutzkontakt. Beschwerden sind bei einer Aufsichtsbehörde möglich, insbesondere an deinem Wohn- oder Arbeitsort; zuständige Behörde des Anbieters: {{SUPERVISORY_AUTHORITY}}. Für ausschließlich lokale Einträge nutze Bearbeitung, Export und Löschung in der App; der Anbieter kann sie nicht aus der Ferne abrufen.

## 9. Kinder und Änderungen

Zielgruppe und Altersbeschränkungen sind vor Veröffentlichung festzulegen: {{INTENDED_AGE_GROUP_AND_MARKETS}}. Ohne die im jeweiligen Land erforderlichen Schutzmaßnahmen darf die App weder gezielt an Kinder vermarktet noch deren Zustimmung als ausreichende Grundlage optionaler Online-Verarbeitung behandelt werden.

Diese Erklärung ist offline in den Einstellungen verfügbar. Wesentliche Änderungen werden in einer neuen Version kenntlich gemacht; erforderliche Einwilligungen werden gesondert eingeholt. Öffentliche Datenschutz-URL: {{PUBLIC_PRIVACY_POLICY_URL}}. Neue Verarbeitungszwecke werden nicht allein durch eine Textänderung erlaubt.

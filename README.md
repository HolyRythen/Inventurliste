# 📦 InventurListeSwing

Ein einfaches, aber mächtiges **Inventurlisten‑Tool in purem Java Swing**.  
Ideal für kleine Lager, IT‑Geräteverwaltung oder Privatgebrauch – kein Setup, keine Datenbank, läuft überall.  

---

## 🚀 Features

- ✅ **CRUD‑System** (Erstellen, Bearbeiten, Löschen von Einträgen)
- 🔍 **Suche & Filter** (live während der Eingabe)
- 💾 **CSV‑Speicherung** im Benutzerordner (`inventurliste.csv`)
- 📤 **Export/Import** von CSV‑Dateien (Excel‑kompatibel)
- 🕒 **Zeitstempel** („erstellt“, „geändert“) automatisch
- 🧮 Sortieren, Spaltenbreiten anpassbar, zeilenweise Bearbeitung
- 💡 **Offline‑fähig** — kein Server, keine DB, keine Installation nötig

---

## 🧩 Voraussetzungen

- **Java 17 oder neuer**
- Keine zusätzlichen Bibliotheken oder Frameworks erforderlich

---

## ⚙️ Installation & Start

1. Lege die Datei **`InventurListeSwing.java`** in einen Ordner, z. B.:  
   `C:\Users\user\Desktop\java-programms\inventur`

2. Öffne eine PowerShell oder CMD in diesem Ordner.

3. Kompiliere das Programm:

   ```powershell
   javac InventurListeSwing.java
   ```

4. Starte das Programm:

   ```powershell
   java InventurListeSwing
   ```

---

## 🖥️ Nutzung

### 🔹 Neues Gerät / Artikel hinzufügen
1. Klicke **„Neu“**  
2. Fülle die Felder aus (Name ist Pflicht)  
3. Klicke **OK** – der Artikel erscheint sofort in der Liste

### 🔹 Eintrag bearbeiten
- Wähle den gewünschten Artikel aus  
- Klicke **„Bearbeiten“**, ändere die Felder, und bestätige mit **OK**

### 🔹 Eintrag löschen
- Markiere eine Zeile → **„Löschen“**  
- Sicherheitsabfrage verhindert versehentliches Entfernen

### 🔹 Suchen & Filtern
- Tippe in das Suchfeld oben (z. B. „Laptop“ oder „Werkzeug“)  
- Filtert sofort nach Name, Kategorie, Standort oder Notiz

### 🔹 CSV‑Import & Export
- Import: Lade vorhandene Liste (`.csv`) → ersetzt aktuelle Tabelle  
- Export: Speichere deine Liste als **Excel‑kompatible CSV**  

---

## 📁 Speicherort

Standardmäßig speichert das Programm die Daten automatisch unter:  
```
C:\Users\<Benutzername>\inventurliste.csv
```
Beim Schließen des Fensters wird automatisch gesichert (Autosave).

---

## 📊 CSV‑Struktur

| Spalte | Beschreibung |
|:--------|:--------------|
| id | Eindeutige UUID (automatisch) |
| name | Artikelname |
| kategorie | Typ oder Kategorie (z. B. Elektronik, Werkzeug …) |
| standort | Ort oder Raum |
| menge | Anzahl |
| notiz | Freitext (z. B. Zustand, Seriennummer, Zubehör) |
| erstellt | Datum & Uhrzeit (ISO‑Format) |
| geaendert | Letzte Änderung |

---

## 💡 Erweiterungsideen

- [ ] Mehrbenutzer‑Modus mit passwortgeschützter Datei  
- [ ] Auto‑Backup‑Ordner (`inventur_backups/`)  
- [ ] Druckfunktion (PDF‑Export)  
- [ ] Dunkelmodus 🌙  
- [ ] QR‑Code‑Scanner‑Integration  

---

## 📁 Lizenz

MIT License — frei nutzbar & veränderbar.

---

© 2025 Robert Martin

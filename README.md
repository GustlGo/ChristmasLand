# Christmasland (Fabric, Minecraft 26.3)

Neue Dimension **Christmasland** mit nur einem Biom (Snowy Plains).
Das Portal besteht aus Eisblöcken (Eis, Packeis oder Blaueis) statt Obsidian
und wird wie das Netherportal mit Feuerzeug entzündet.

## Portal bauen
- Rahmen wie beim Netherportal: innen mindestens 2 breit x 3 hoch, Ecken sind nicht nötig.
- Mit dem Feuerzeug auf einen Rahmenblock klicken (z. B. auf die Oberseite des unteren Rahmens).
- Im Portal ca. 4 Sekunden stehen bleiben (Kreativ: sofort).
- Rahmenblock abbauen -> Portal verschwindet.

## .jar bauen

### Variante A: GitHub (ohne lokale Einrichtung)
1. Neues Repository auf github.com anlegen und diesen Ordner hochladen (inkl. `.github`).
2. Reiter **Actions** -> Workflow **build** läuft automatisch.
3. Fertigen Lauf öffnen -> unten bei **Artifacts** `christmasland-jar` herunterladen.

### Variante B: lokal
Benötigt JDK 25 und Gradle 9.6:

    gradle build

Die Datei liegt danach in `build/libs/christmasland-1.0.0.jar`.

## Installieren
`christmasland-1.0.0.jar` und die passende **Fabric API für 26.3** in den `mods`-Ordner legen
(Fabric Loader 0.19.5 oder neuer).

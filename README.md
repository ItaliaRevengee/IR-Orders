# IR-Orders

IR-Orders è un plugin per server Minecraft basato su PaperMC che aggiunge un sistema di **buy-order marketplace**: i giocatori piazzano ordini di acquisto bloccando i fondi in escrow (via Vault), e altri giocatori possono soddisfarli consegnando gli item richiesti.

## Requisiti

- Java 21
- PaperMC 1.21.11+
- Vault + un plugin di economia (es. EssentialsX Economy)

## Installazione

1. Compila il plugin con Gradle (vedi [Costruzione](#costruzione)).
2. Copia il file `build/libs/IR-Orders-*.jar` nella cartella `plugins/` del server.
3. Avvia o riavvia il server.
4. Modifica `plugins/IR-Orders/config.yml` per configurare il catalogo e i limiti.

## Comandi

| Comando | Alias | Descrizione |
|---|---|---|
| `/order` | `/market`, `/orders` | Apre il menu principale del marketplace |
| `/order backpack` | — | Apre il backpack virtuale personale |

## Permessi

Nessun permesso custom richiesto — tutti i giocatori possono usare `/order` di default.

## Come funziona

1. Il giocatore apre `/order` e sfoglia il **catalogo** suddiviso per categorie.
2. Seleziona un item, inserisce la quantità e il prezzo offerto: i fondi vengono bloccati in escrow.
3. Gli ordini attivi sono visibili nella schermata **Market** da tutti i giocatori.
4. Un venditore con gli item in inventario clicca sull'ordine per soddisfarlo: gli item vengono trasferiti nel backpack del compratore e il denaro al venditore.
5. Il compratore ritira gli item dal proprio **backpack** virtuale (54 slot, persistente).

### Identità degli item

Gli item sono riconosciuti tramite la chiave PDC `irorders:item_id`, non per nome o lore. Se un item nel catalogo ha la sezione `enchants`, si attiva la **modalità strict**: il venditore deve fornire l'item con esattamente quegli incantesimi.

## Configurazione

```yaml
database:
  file: ir-orders.db          # percorso del file SQLite

economy:
  min-price: 1.0
  max-price: 10000000.0

market:
  max-orders-per-player: 20   # ordini attivi massimi per giocatore

catalog:
  categories:
    blocks:
      display-name: "§aBlocks"
      icon: GRASS_BLOCK
  items:
    - id: diamond              # valore PDC univoco
      material: DIAMOND
      display-name: "§bDiamond"
      category: ores
    - id: diamond_sword_sharp5
      material: DIAMOND_SWORD
      display-name: "§bDiamond Sword §7(Sharpness V)"
      category: weapons
      enchants:                # strict mode: enchant esatti richiesti
        minecraft:sharpness: 5
```

## Costruzione

```bash
gradle --no-daemon build
```

Il JAR finale si trova in `build/libs/`.

## CI / Release

Il workflow GitHub Actions pubblica automaticamente una nuova release ogni volta che `pluginVersion` in `gradle.properties` viene incrementata sul branch principale. È anche possibile creare una release manuale pushando un tag `vX.Y.Z`.

## Dettagli

- **Nome plugin:** `IR-Orders`
- **Main class:** `com.italiarevenge.iROrders.IROrders`
- **Versione attuale:** `1.0.0`
- **API Minecraft:** `1.21`
- **Repository:** https://github.com/ItaliaRevengee/IR-Orders

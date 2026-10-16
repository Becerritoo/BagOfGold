# BagOfGold

BagOfGold is an item-based economy plugin for Minecraft. Money can exist as
physical items in player inventories, be stored in the bank or be managed through
Vault-compatible plugins.

This repository is a maintained fork by
[Becerritoo](https://github.com/Becerritoo). BagOfGold was created by Rocologo,
who remains credited as the original author.

## Maintained version

The current development release is **4.8.0**, targeting Paper 1.21.11 with
Java 21 and CustomItemsLib 1.4.0. Releases use semantic versioning. BagOfGold
registers its physical-currency behavior through the public CustomItemsLib
service contract.

## Features

- Physical item-based currency and bank accounts.
- Vault economy provider support.
- Per-world balances and modern Towny virtual-account resolution.
- Shopkeepers physical-currency trades using the public Shopkeepers API.
- Signed Shopkeepers price templates created from renamed paper such as `bagofgold 3000`.
- Optional bounded player debt. Deposits pay debt before crediting physical cash.
- SQLite and MySQL storage with automatic debt-column migration.
- English, French, Hungarian, Portuguese, Russian and Chinese language files.

## Player debt

Debt is controlled in `plugins/BagOfGold/config.yml`:

```yaml
economy:
  enable-player-debt: true
  maximum-player-debt: 10000.0
```

The limit applies per player, world group and game mode. Set
`enable-player-debt` to `false` to retain the traditional insufficient-funds
behavior.

## Native API

Plugins that declare BagOfGold as a dependency can obtain its native API through
the static accessor:

```java
BagOfGoldAPI api = BagOfGold.getAPI();
double available = api.getAmountInInventory(player);
double stored = api.getAmountInPortableContainers(player);
```

The same instance is registered with Bukkit's `ServicesManager`:

```java
RegisteredServiceProvider<BagOfGoldAPI> registration =
        Bukkit.getServicesManager().getRegistration(BagOfGoldAPI.class);
BagOfGoldAPI api = registration == null ? null : registration.getProvider();
```

BagOfGold also registers the `PhysicalCurrencyService` declared by
CustomItemsLib. This keeps balance accounting, debt behavior and Shopkeepers
currency policy inside BagOfGold while CustomItemsLib manages generic reward
items.

`getAmountInInventory` returns all physical money carried by the player,
including money inside carried shulker boxes and bundles.

## Shopkeepers price templates

An operator with `bagofgold.shopkeepers.template` can place renamed paper such
as `bagofgold 3000` in a regular admin or player trading Shopkeeper editor. When the editor is saved,
BagOfGold replaces it with a signed price template bound to that Shopkeeper.
The template is not currency and cannot increase a player's balance. During a
purchase, BagOfGold withdraws the exact price from physical money and returns
change when required.

See [the maintained fork notes](docs/MAINTAINED-FORK.md) for compatibility and
migration details.

## Original project pages

- [SpigotMC](https://www.spigotmc.org/resources/bagofgold.49332/)
- [Bukkit Dev](https://dev.bukkit.org/projects/bagofgold)

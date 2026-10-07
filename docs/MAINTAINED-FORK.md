# Maintained Fork Notes

This fork is maintained by Becerritoo and is based on Rocologo's original
BagOfGold project. Public releases use semantic versions without a server-specific
suffix. The current release is `4.6.0`.

## Compatibility

- Paper 1.21.11
- Java 21
- CustomItemsLib 1.2.0
- Vault when BagOfGold is the economy provider
- Citizens for banker NPCs
- Towny for town and nation accounts handled through Vault
- Shopkeepers 2.28.1

## Maintained changes

- BagOfGold loads before Towny so Towny can bind to its Vault provider.
- Vault operations resolve modern Towny UUID-backed virtual accounts and legacy
  player-name calls resolve the latest stored UUID.
- Shopkeepers uses its public API and handles physical-currency payments,
  overpayments and change safely.
- Signed reward-token audit, migration, verification and revocation commands are
  available to administrators with the `bagofgold.token` permission.
- MySQL balance changes can be recorded in `mh_balance_ledger` for audits.
- Player and virtual-account names use a 64-character MySQL column.
- Player debt is stored separately from physical cash and is included in Vault
  responses, placeholders and wealth rankings.

## Debt migration

Version 4.6.0 automatically adds the `DEBT` column to existing SQLite and MySQL
`mh_Balance` tables. Existing negative cash balances are converted into positive
debt while cash is reset to zero. New deposits settle debt before crediting cash.

The feature is configured with `economy.enable-player-debt` and
`economy.maximum-player-debt`. The limit applies independently to each player,
world group and game mode.

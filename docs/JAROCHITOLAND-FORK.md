# Jarochitoland Fork Notes

This fork is based on upstream `Rocologo/BagOfGold` and keeps the upstream
version number plus a Jarochitoland suffix:

`4.5.6-JL.9`

The `4.5.6` part identifies the upstream base. The `JL.9` part identifies the
Jarochitoland fork revision.

## Active Branch

`release/paper-1.21.11`

This branch targets the live Paper 1.21.11 server line.

## Runtime Dependencies

- CustomItemsLib 1.1.0-JL.12 or newer in the JL line
- Vault when BagOfGold is used as the server economy provider
- Citizens for banker NPCs
- Towny when town and nation accounts are handled through Vault

## Notable Changes

- BagOfGold loads before Towny so Towny can bind to the BagOfGold Vault
  provider during startup.
- Vault world-balance methods now resolve Towny `town-` and `nation-`
  virtual account names to Towny UUID-backed OfflinePlayer accounts.
- MySQL balance changes are recorded in `mh_balance_ledger` when MySQL storage
  is enabled.
- ItemFrame placement resyncs the player balance after the item is placed,
  avoiding destructive immediate balance subtraction.
- `mh_PlayerSettings.NAME` is created as `VARCHAR(64)` for longer player or
  virtual account names.

## Token Admin Commands

Permission: `bagofgold.token`

- `/bag token audit [player]`
- `/bag token migrate [player]`
- `/bag token verify`
- `/bag token revoke <token-uuid>`

Audit and migration require the target player to be online.

## MySQL Notes

`mh_balance_ledger` is created automatically when MySQL storage is enabled.
It records:

- player UUID
- player name
- delta
- balance before and after
- detected source plugin
- source hint
- timestamp

Existing `mh_PlayerSettings` tables may still need a manual schema migration if
they were created before the `VARCHAR(64)` change.

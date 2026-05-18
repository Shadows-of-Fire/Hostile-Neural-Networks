# Description
Model Attunement allows individual data models to specify their attunement rules. If none are specified, the model attunes to any matching target (entity or block) of the model's declared type.

These rules allow multiple models to exist for the same target, as long as the rules are unique (no two models may match the same entity or block).

**Important:** If more than one model exists for a given entity type or block, model attunements **must** be provided for all models. A parse error will be thrown otherwise.

The shape of the attunement object depends on which kind of data model contains it:

- [Entity Data Models](../DataModel.md) use an **EntityAttunement**, whose `predicate` field is a vanilla [EntityPredicate](https://minecraft.wiki/w/Advancement_definition#minecraft:player_interacted_with_entity).
- [Block Data Models](../BlockDataModel.md) use a **BlockAttunement**, whose `predicate` field is a vanilla [BlockPredicate](https://minecraft.wiki/w/Advancement_definition#minecraft:location).

Both variants share the same outer structure.

# Schema
```js
{
    "attunable": boolean,                  // [Required] || If the model can be attuned at all. If false, attunement for this model is disabled.
    "predicate": EntityPredicate | BlockPredicate // [Required] || Rules selecting which sub-variants of the valid targets will attune this model.
}
```

# Examples

## Entity attunement

Only attune to a charged creeper.

```json
{
    "attunable": true,
    "predicate": {
        "nbt": {
            "powered": true
        }
    }
}
```

## Block attunement

Only attune to a redstone ore block while it is in its lit state.

```json
{
    "attunable": true,
    "predicate": {
        "blocks": [ "minecraft:redstone_ore" ],
        "state": {
            "lit": "true"
        }
    }
}
```

# Description
Model Attunement allows individual data models to specify their attunement rules. If none are specified, the model attunes to any entity matching the model's entity type or variants.  

These attunement rules allow multiple models to exist for the same entity type, as long as the rules are unique (no two models may match the same entity).

**Important:** If more than one model exists for a given entity type, model attunements **must** be provided for all models. A parse error will be thrown otherwise.

# Dependencies
This object references the following objects:
1. [EntityPredicate](https://minecraft.wiki/w/Advancement_definition#minecraft:player_interacted_with_entity)
  a. See "All possible conditions for entities" to understand the structure of EntityPredicate.

# Schema
```js
{
    "attunable": boolean,        // [Required] || If the model can be attuned at all. If false, attunement for this model is disabled.
    "predicate": EntityPredicate // [Required] || Rules that specify which sub-variants of the valid entities will attune this model.
}
```

# Examples

Model attunement rules which only match a charged creeper.

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

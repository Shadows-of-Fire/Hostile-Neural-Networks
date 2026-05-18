# Description
Data Gained allows individual data models to override the `data_gained` values for specific model tiers. The override governs how much progress is added when the player performs the model's world action — killing a matching entity for an [Entity Data Model](../DataModel.md), or breaking a matching block for a [Block Data Model](../BlockDataModel.md).

Takes the form of an unbounded map. Only the specified tier names will be used as overrides; tiers not listed fall back to the value declared on the [ModelTier](../ModelTier.md) itself.

Unfortunately, due to the unbounded nature, there is no validation for incorrect keys.

# Schema
```js
{
    "tier": integer,  // [Optional] || The amount of data per action this model will receive when it is of the specified tier.
}
```

# Examples

The DataGained object from the Ender Dragon data model:
```json
{
    "faulty": 3,
    "basic": 12,
    "advanced": 30,
    "superior": 45
}
```

# Migration

The JSON field on a Data Model was previously named `data_per_kill`; it is now `data_gained`. The legacy field name remains accepted via a deprecated codec, but new models should use `data_gained`.

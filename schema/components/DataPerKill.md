# Description
Data Per Kill allows individual data models to override the `data_per_kill` values for specific model tiers.
As such, it takes the form of an unbounded map. Only the specified tier names will be used as overrides.

Unfortunately, due to the unbounded nature, there is no validation for incorrect keys.

# Schema
```js
{
    "tier": integer,  // [Optional] || The amount of data per kill this model will receive when the model is of the specified tier.
}
```

# Examples

The DataPerKill object from the Ender Dragon data model:
```json
{
    "faulty": 3,
    "basic": 12,
    "advanced": 30,
    "superior": 45
}
```

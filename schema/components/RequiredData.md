# Description
Required Data allows individual data models to override the `required_data` values for specific model tiers.
As such, it takes the form of an unbounded map. Only the specified tier names will be used as overrides.

Unfortunately, due to the unbounded nature, there is no validation for incorrect keys.

Note: Required Data overrides **must** retain the same ordering as the tiers themselves.

# Schema
```js
{
    "tier": integer,  // [Optional] || The data required for a model to be considered at the specified tier.
}
```

# Examples

A Required Data override which makes every tier harder to acquire.
```json
{
    "faulty": 0,
    "basic": 100,
    "advanced": 250,
    "superior": 500,
    "self_aware": 1500
}
```

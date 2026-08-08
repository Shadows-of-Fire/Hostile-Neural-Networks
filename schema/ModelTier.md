# Description
A Model Tier controls the threshold values at which a [Data Model](./DataModel.md) advances, plus the accuracy and per-action data gain it has while at each tier.  
Infinitely many model tiers may be created, allowing total control over the progression system.

# Dependencies
This object references the following objects:
1. [Color](../../../../Placebo/blob/1.21/schema/Color.md)

## Schema
```js
{
    "required_data": integer,  // [Mandatory] || The required data threshold for the model tier. Can be overridden by individual data models.
    "data_gained": integer,    // [Mandatory] || The amount of data received for each world action that targets a model at this tier - killing a matching entity for an Entity Data Model, or breaking a matching block for a Block Data Model. Can be overridden by individual data models.
    "color": Color,            // [Mandatory] || The color of the tier, used when displaying the tier in text form.
    "accuracy": float,         // [Mandatory] || The accuracy (chance a prediction is created) when simulating the model. A value of 1.0 is 100%, and the max value is 64.
    "can_sim": boolean         // [Optional]  || If the model tier is legal for use in a simulation chamber. Defaults to true.
}
```

## Notes
Model Tiers may only be created under the `hostilenetworks` namespace; attempting to register a tier under a different namespace will error.  
As such, tiers are not namespaced when being referenced, as the namespace is implied to be `hostilenetworks`.

A tier with a `required_data` of zero must always exist. The game will refuse to load if no such tier exists.

No two tiers with the same `required_data` may exist. A linear order of tiers must be formed for the data set to be considered legal.

When creating a new tier, you will also need to supply a language key of the form `hostilenetworks.tier.name` for translation.

## Migration
The `data_gained` field was previously named `data_per_kill`. The legacy field name remains accepted via a deprecated codec, but new tier files should use `data_gained`.

## Examples

The Superior model tier.
```json
{
  "required_data": 354,
  "data_gained": 18,
  "color": "light_purple",
  "accuracy": 0.65
}
```

# Description
A Data Model is the primary data object for Hostile Neural Networks, holding all data needed to represent a simulatable mob.

# Dependencies
This object references the following objects:
1. [DisplayData](./components/DisplayData.md)
2. [ItemStack](../../../../Placebo/blob/-/schema/ItemStack.md)
3. [RequiredData](./components/RequiredData.md)
4. [DataPerKill](./components/DataPerKill.md)

The object types `Component` and `Ingredient` are supplied by Vanilla and are not described here or in my own documentation.

## Schema
```js
{
    "entity": "string",             // [Mandatory] || The registry name of the primary entity for the model.
    "variants": [                   // [Optional]  || A list of variant entity registry names, which will also be correlated with this model.
        "string"                    
    ],                              
    "name": Component,              // [Mandatory] || The name of the data model (also used for the prediction). Must have a color set.
    "display": DisplayData,         // [Optional]  || Display data used to adjust the holographic mob shown on the data model.
    "sim_cost": integer,            // [Mandatory] || The cost, in FE/t, to simulate this model in the Simulation Chamber.
    "input": Ingredient,            // [Mandatory] || The input item when simulating this data model.
    "base_drop": ItemStack,         // [Mandatory] || The base drop when simulating the data model. May be an empty item stack.
    "trivia": "string",             // [Mandatory] || The localization key for the trivia string displayed in the Deep Learner.
    "fabricator_drops": [           // [Mandatory] || The list of items that may be obtained from the Loot Fabricator. Empty stacks will be ignored.
        ItemStack                   
    ],                              
    "required_data": RequiredData,  // [Optional]  || Optional overrides for the required data for individual model tiers.
    "data_per_kill": DataPerKill    // [Optional]  || Optional overrides for the data per kill for individual model tiers.
}
```

## Notes
Unknown entity names in the `variants` list will be silently ignored, allowing model authors to supply variant names even if the target mod is not loaded. The slime data model does this by default.

The max value of the `sim_cost` field is `INT_MAX / 20`.

## Examples

The Blaze data model, which includes optional support for Reliquary drops.
```json
{
    "entity": "minecraft:blaze",
    "variants": [],
    "name": {
        "translate": "entity.minecraft.blaze",
        "color": "#FFD528"
    },
    "display": {},
    "sim_cost": 256,
    "input": {
        "item": "hostilenetworks:prediction_matrix"
    },
    "base_drop": {
        "id": "hostilenetworks:nether_prediction"
    },
    "trivia": "hostilenetworks.trivia.blaze",
    "fabricator_drops": [
        {
            "id": "minecraft:blaze_rod",
            "count": 16
        },
        {
            "id": "reliquary:molten_core",
            "optional": true,
            "count": 2
        }
    ]
}
```

# Description
An (Entity) Data Model is the primary data object for Hostile Neural Networks, holding all data needed to represent a simulatable mob. For block-target models, see [BlockDataModel](./BlockDataModel.md).

A `type` field of `hostilenetworks:entity_data_model` selects this codec; it is also the default if `type` is omitted.

# Dependencies
This object references the following objects:
1. [DisplayData](./components/DisplayData.md)
2. [ItemStack](../../../../Placebo/blob/1.21/schema/ItemStack.md)
3. [RequiredData](./components/RequiredData.md)
4. [DataGained](./components/DataGained.md)
5. [ModelAttunement](./components/ModelAttunement.md)

The object types `Component`, `Ingredient`, and `TextColor` are supplied by Vanilla and are not described here.

## Schema
```js
{
    "entity": "string",             // [Mandatory] || The registry name of the primary entity for the model.
    "variants": [                   // [Optional]  || A list of variant entity registry names, which will also be correlated with this model.
        "string"
    ],
    "name": Component,              // [Optional]  || Display name of the model. If omitted, falls back to the entity's vanilla name.
    "name_color": TextColor,        // [Mandatory] || Color used for the model name in tooltips and the prediction item.
    "display": DisplayData,         // [Optional]  || Display data used to adjust the holographic mob shown on the data model.
    "sim_cost": integer,            // [Mandatory] || The cost, in FE/t, to simulate this model in the Simulation Chamber.
    "input": Ingredient,            // [Mandatory] || The input item when simulating this data model.
    "base_drop": ItemStack,         // [Mandatory] || The base drop when simulating the data model. May be an empty item stack.
    "trivia": "string",             // [Mandatory] || The localization key for the trivia string displayed in the Deep Learner.
    "fabricator_drops": [           // [Mandatory] || The list of items that may be obtained from the Loot Fabricator. Empty stacks will be ignored.
        ItemStack
    ],
    "required_data": RequiredData,  // [Optional]  || Optional overrides for the required data for individual model tiers.
    "data_gained": DataGained,      // [Optional]  || Optional overrides for the data gained per kill for individual model tiers. (Legacy name: `data_per_kill`.)
    "attunement": ModelAttunement   // [Optional]  || Optional attunement rules. Required if making multiple models for the same entity type(s).
}
```

## Notes
Unknown entity names in the `variants` list will be silently ignored, allowing model authors to supply variant names even if the target mod is not loaded. The slime data model does this by default.

The max value of the `sim_cost` field is `INT_MAX / 20`.

## Migration

Models authored before the `name_color` refactor used a single `name` field whose embedded text color served as the model's color. That legacy form (`{ "name": { "translate": "...", "color": "..." } }` with no `name_color`) is still accepted via a deprecated codec, but new models should split the color into a dedicated `name_color` field and may omit `name` entirely to inherit the entity's vanilla name.

## Examples

The Blaze data model, which includes optional support for Reliquary drops. `name` is omitted so the model inherits the Blaze's vanilla translation key; only `name_color` is supplied.
```json
{
    "entity": "minecraft:blaze",
    "name_color": "#FFD528",
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

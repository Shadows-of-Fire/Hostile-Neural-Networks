# Description
A Block Data Model is a variant of [Data Model](./DataModel.md) that targets a `Block` instead of an entity. Players attune one by right-clicking the target block with a Model Framework, and grant data by mining the block while a Deep Learner contains the model.

Mechanically, a block model behaves identically to an entity model in the Simulation Chamber and Loot Fabricator. The only differences are the attunement target, the data-gain trigger, how the model's display is rendered (an item form of the block instead of a live entity), and the statistics shown in the Deep Learner (Hardness, Blast Resistance, and Sound Type, in place of an entity's health, armor, and experience).

To declare a block data model, set the `type` field to `hostilenetworks:block_data_model`. (Entity models use `hostilenetworks:entity_data_model`, which is also the default if `type` is omitted.)

# Dependencies
This object references the following objects:
1. [DisplayableBlock](./components/DisplayableBlock.md)
2. [DisplayData](./components/DisplayData.md)
3. [ItemStack](../../../../Placebo/blob/-/schema/ItemStack.md)
4. [RequiredData](./components/RequiredData.md)
5. [DataGained](./components/DataGained.md)
6. [ModelAttunement](./components/ModelAttunement.md)

The object types `Component`, `Ingredient`, `TextColor`, `BlockPredicate`, and `LootItemCondition` are supplied by Vanilla and are not described here.

# Schema
```js
{
    "type": "hostilenetworks:block_data_model", // [Mandatory] || Discriminator selecting the block model codec.
    "block": DisplayableBlock,                  // [Mandatory] || The primary block for the model. May be a registry name string, or a full DisplayableBlock object.
    "variants": [                               // [Optional]  || A list of variant DisplayableBlocks, which will also be correlated with this model.
        DisplayableBlock
    ],
    "name": Component,                          // [Optional]  || Display name of the model. If omitted, falls back to the block's vanilla name.
    "name_color": TextColor,                    // [Mandatory] || Color used for the model name in tooltips and the prediction item.
    "display": DisplayData,                     // [Optional]  || Display data used to scale and offset the rendered block stack.
    "sim_cost": integer,                        // [Mandatory] || The cost, in FE/t, to simulate this model in the Simulation Chamber.
    "input": Ingredient,                        // [Mandatory] || The input item when simulating this data model. Usually the prediction matrix.
    "base_drop": ItemStack,                     // [Mandatory] || The base drop when simulating the data model. May be an empty item stack.
    "trivia": "string",                         // [Mandatory] || The localization key for the trivia string displayed in the Deep Learner.
    "fabricator_drops": [                       // [Mandatory] || The list of items that may be obtained from the Loot Fabricator. Empty stacks will be ignored.
        ItemStack
    ],
    "required_data": RequiredData,              // [Optional]  || Optional overrides for the required data for individual model tiers.
    "data_gained": DataGained,                  // [Optional]  || Optional overrides for the data gained per mine for individual model tiers.
    "attunement": ModelAttunement,              // [Optional]  || Optional attunement rules. Required if multiple models exist for the same block.
    "upgrade_conditions": [                     // [Optional]  || Loot conditions, all of which must pass against the block-break context for mining to grant data. Defaults to empty (mining always grants).
        LootItemCondition
    ]
}
```

## Notes

The `block` and `variants` fields accept either a plain registry-name string (in which case the block's default item form is used as the display stack) or a full `DisplayableBlock` object that lets you override the displayed item — useful for blocks that don't have a corresponding item or that look better when shown as some other stack.

Block models use the `BlockPredicate` form of `attunement` (see [ModelAttunement](./components/ModelAttunement.md)). The predicate matches against the block state at the position the player right-clicked.

The max value of the `sim_cost` field is `INT_MAX / 20`.

The `upgrade_conditions` field accepts standard Vanilla loot conditions (the same objects usable in loot tables).

# Examples

The Redstone Ore data model, which counts deepslate redstone ore as a variant.

```json
{
    "type": "hostilenetworks:block_data_model",
    "block": "minecraft:redstone_ore",
    "variants": [ "minecraft:deepslate_redstone_ore" ],
    "name_color": "#FFD528",
    "display": { "scale": 3 },
    "sim_cost": 256,
    "input": {
        "item": "hostilenetworks:prediction_matrix"
    },
    "base_drop": {
        "id": "hostilenetworks:overworld_prediction"
    },
    "trivia": "hostilenetworks.trivia.redstone_ore",
    "fabricator_drops": [
        { "id": "minecraft:redstone",  "count": 16 },
        { "id": "minecraft:repeater",  "count": 4 },
        { "id": "minecraft:comparator", "count": 4 }
    ],
    "upgrade_conditions": [
        {
            "condition": "minecraft:inverted",
            "term": {
                "condition": "minecraft:match_tool",
                "predicate": {
                    "predicates": {
                        "minecraft:enchantments": [
                            { "enchantments": "minecraft:silk_touch", "levels": { "min": 1 } }
                        ]
                    }
                }
            }
        }
    ]
}
```

# Description
Display Data holds information controlling how the ghost mob is rendered on a data model and in the Deep Learner.  
Every field on this object is optional, meaning that `{}` is a legal definition of a Display Data object.

# Dependencies
This object references the following objects:
1. [CompoundTag](../../../../../Placebo/blob/1.21/schema/CompoundTag.md)

# Schema
```js
{
    "nbt": CompoundTag,  // [Optional] || Extra NBT applied to the rendered entity (e.g. equipment, pose). Defaults to an empty tag.
    "scale": float,      // [Optional] || Scale factor applied to the rendered entity. Range [0, 5]. Defaults to 1.
    "x_offset": float,   // [Optional] || X offset applied to the rendered entity. Range [-5, 5]. Defaults to 0.
    "y_offset": float,   // [Optional] || Y offset applied to the rendered entity. Range [-5, 5]. Defaults to 0.
    "z_offset": float,   // [Optional] || Z offset applied to the rendered entity. Range [-5, 5]. Defaults to 0.
}
```

# Examples

The Slime display data, used to make it larger (since it shows a size 1 slime):
```json
{
    "scale": 2.0
}
```

The Drowned display data, used to show it holding a trident (instead of holding nothing):
```json
{
    "nbt": {
        "HandItems": [
            {
                "id": "minecraft:trident"
            },
            {}
        ]
    }
}
```

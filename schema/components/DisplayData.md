# Description
Display Data holds information controlling how the ghost mob is rendered on a data model and in the Deep Learner.  
Every field on this object is optional, meaning that `{}` is a legal definition of a Display Data object.

# Dependencies
This object references the following objects:
1. [CompoundTag](../../../../Placebo/blob/-/schema/CompoundTag.md)

# Schema
```js
{
    "nbt": CompoundTag,  // [Optional] || 
    "scale": float,      // [Optional] || 
    "x_offset": float,   // [Optional] || 
    "y_offset": float,   // [Optional] || 
    "z_offset": float,   // [Optional] || 
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

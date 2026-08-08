# Description
A DisplayableBlock binds a `Block` (the actual world block matched by attunement and mine events) to an `ItemStack` used to render the model in the GUI and in the player's hand.

It exists because some blocks have no item form, and some blocks look better when displayed as a different stack (i.e. with specific NBT). When the displayed stack would be identical to the block's default item form, the binding can be expressed as a plain registry-name string.

# Dependencies
This object references the following objects:
1. [ItemStack](../../../../../Placebo/blob/1.21/schema/ItemStack.md)

# Schema

Two forms are accepted.

## Short form (string)
A bare block registry name. The display stack defaults to `new ItemStack(block)`.

```json
"minecraft:redstone_ore"
```

## Long form (object)
```js
{
    "block": "string",          // [Mandatory] || Block registry name.
    "display_stack": ItemStack  // [Mandatory] || The item stack rendered for this block.
}
```

# Examples

A short-form binding that uses redstone ore's default item form:
```json
"minecraft:redstone_ore"
```

A long-form binding that displays a redstone ore as a redstone block instead:
```json
{
    "block": "minecraft:redstone_ore",
    "display_stack": { "id": "minecraft:redstone_block" }
}
```

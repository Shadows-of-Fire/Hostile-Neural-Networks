## 6.1.0
* Model Tiers are now fully data-driven! This means that new tiers can be added, and optionally existing tiers could be removed.
  * Tiers must all be declared under the `hostilenetworks` namespace.
  * No two tiers with the same `required_data` value may exist, and one tier with a `required_data` of zero must always exist.
  * Individual tiers can be declared non-simulatable by setting `can_sim` to false.
  * The addition and removal of model tiers is retroactive and will impact all existing data models in the game based on the new data thresholds.
  * Model Tiers may define accuracy values in excess of 100%.
* Updated the item tooltip for the Deep Learner. The tooltip now looks similar to the HUD.
* Added support for Curios API Continuation.
* Added a keybind to open the Deep Learner from the curio slot. Defaults to `u`.
* Made Data Model and Mob Prediction item names always white.
  * Using the name color was a nice bit of flavor, but makes the names too hard to read in many cases.
* Made the Deep Learner HUD position configurable. The `/hnn_client set_hud_pos` command can be used to adjust it in-game.
* Fixed arrow button sprite rendering in the Loot Fabricator and Deep Learner.
* Vizthex: Added Breeze Model, added variant declarations for Hoglin (Zoglin) and Skeleton (Bogged), and reduced slime block output to 3 (was 8).
* Twister: Added Japanese Translation.

## 6.0.1
* Fixed recipes referencing old tag names from 1.20.

## 6.0.0
* Updated to Minecraft 1.21.1.
* Self-Aware model tier accuracy increased to 100% by default (was 99.5%).

## 5.3.1
* Fixed a rare crash that could occur when populating creative tabs.
* MikeyM3thodic4l: Added Enderman Overhaul mobs to the variants list for the Enderman model.
* Quarkrus: Added Russian translation.
* RuyaSavascisi: Added Turkish translation.

## 5.3.0
* Max: Added many new configuration options.
  * Made model tier information loaded from data files.
  * Added the option to disable right-click attunement.
  * Added the option to disable model upgrading on kill.
  * Added the option to disable accuracy interpolation.
  * Added the opton to change how the simulation chamber upgrades models.
* Made the config file synced.
* Fixed a crash that could occur if loot fabricator drops were removed from a model.
* Improved error handling when parsing data models from json.
* Added the `/hostilenetworks give_model` command, allowing for the generation of models with specific tiers.

## 5.2.2
* Fixed a memory leak caused by `ClientEntityCache`.

## 5.2.1
* Updated Guardian data per kill to match other bosses.
* Removed nether star recipe.

## 5.2.0
* Updated to Placebo 8.5.2 and made `DataModel` a record class.
* Changed the Data Model JSON keys `type` and `subtypes` to `entity` and `variants` respectively. Also changed how colors are parsed.
  * Models from the old format can be updated to the new format by running `/hostilenetworks datafix_all` with relevant datapacks loaded.

## 5.1.3
* Removed forge dependency line from the mods.toml and marked as Forge and NeoForge for CF.
  * The dependency will be added back and the Forge marker will be removed once CF supports Neo correctly.

## 5.1.2
* Fixed the Loot Fabricator being an absolute disaster.

## 5.1.1
* Updated to Placebo 8.3.2.

## 5.1.0
* Updated to Placebo 8.3.0 and refactored code to use DynamicHolder instead of keeping live DataModel references.

## 5.0.2
* Updated to placebo 8.2.0.

## 5.0.1
* Fixed the HNN Creative Tab not having a name.

## 5.0.0
* Updated to 1.20.1.

## 4.1.1
* Added support for Apotheosis's Warden Tendril.

## 4.1.0
* Updated to Placebo 7.2.0
* Added the /hostilenetworks command with three subcommands.
  * The first subcommand, generate_model_json, can be used to generate a Data Model JSON by simulating the death of the entity.
  * The second subcommand, update_model_json, can be used to update the fabricator drops of an existing data model based on the current context.
  * The final subcommand, generate_all, will generate a model json for every possible loaded entity.
  * All subcommands use the executing player as the context point, and will use any enchantments or looting that the player has to determine the drops.
* Hovering an item in the Loot Fabricator will now show the item's tooltip.
* Uses of Lapis in the Model Framework and Prediction Matrix has been replaced with Clay Balls, making them fully renewable in most modpacks.
* Some recipes that were incorrectly in recipes/living_matter have been moved to recipes/.
* The Deep Learner will no longer crash if an invalid data model is placed inside it.
* Data Models and Mob Predictions will be sorted in JEI.
* Added native support for Vanilla passive/neutral mobs, Thermal mobs, Twilight Forest mobs, and AllTheModium's Piglich.
* Also added support for reliquary, hexerei, and naturalist drops to relevant models.

## 4.0.2
* Added a missing setChanged call in LootFabTileEntity#setSelection which may have been preventing selection saving.

## 4.0.1
* Added Curios Support!  The Deep Learner now has special Curios slot and will show the HUD when in this slot.
* Fixed Deep Learner GUI not ticking entities (which meant some entities did not perform animations).
* Slightly altered Deep Learner GUI rotations so that mobs start facing forwards instead of backwards.

## 4.0.0
* Updated to 1.19.2

## 3.2.2
* Changed Simulation Chambers so that they start a cycle as soon as they have enough power for the first tick.
  * This should resolve problems where modpacks set a model's sim cost >= 6667 FE/t without changing the chamber's max power.
* Allowed Data Models to specify nothing as their base drop. You must specify minecraft:air to not receive an error.

## 3.2.1
* Updated to Placebo 6.6.0

## 3.2.0
* Added Data Model Subtypes!
  * Subtypes are entities that will count towards the specific type, but are a different entity.
  * Default subtypes include strays counting towards skeletons, and husks/drowned counting towards zombies.
* Fixed version number becoming outdated.
  * Version is now pulled from mod info, which uses the actual version.
* Fixed HNN BE's not marking themselves as changed.
* Fixed Loot Fab and Sim Chamber mining slowly.

## 3.1.1
* Fixed a crash in the simulation chamber.

## 3.1.0
* Made Tier Data and Data Per Kill configurable on a per-model basis.
  * For more information, [see this document](https://gist.github.com/Shadows-of-Fire/2e83a68a6822f9cf9f64b1fb30210b71).
  * Also buffed the Ender Dragon data per kill from 1/4/10/18 to 3/12/30/45.
* Updated to Placebo 6.4.0 to account for menu changes.
* PixVoxel: Updated Korean Translation.

## 3.0.10
* Fixed a memory leak that was happening while the Deep Learner HUD was being rendered.
* Certain entity data models will now be animated properly.
  * Some, like the Guardians and Ender Dragon, don't use tickCount-based animations, and won't work
* gjeodnd12165: Added Korean Translation

## 3.0.9
* Updated to 1.18.2
* Updated to new JEI API.

## 3.0.8
* Fixed a server crash caused by wrong ImmutableMap import.

## 3.0.7
* Upgraded to new Placebo AutoSync and Container Data systems.
  * Should fix power displaying as negative in some cases.

## 3.0.6
* Potential fix for deep learners on servers.

## 3.0.5
* Fixed a dupe bug.

## 3.0.4
* N-Wither: Added Simplified Chinese translation.
* Added the ability to specify optional fabricator drops that will not error if not present.
* Fixed block entities not saving data on world save.

## 3.0.3
* Fixed a typo in skeleton trivia.
* Added config file.
* Fixed Self-Aware model accuracy showing as NaN

## 3.0.2
* Actually apply the first change from 1.0.2

## 3.0.1
* Same changes as 1.0.2

## 3.0.0
* Updated to 1.18.1

## 2.0.2
* Same changes as 1.0.2

## 2.0.1
* Fixed shift-click movement in containers.

## 2.0.0
* Updated to 1.17.1

## 1.0.2
* Fixed certain entities being "spazzy" when rendered on data models.
* Fixed the Loot Fabricator and Sim Chamber not dropping any items.

## 1.0.1
* Fixed a crash caused by a non-obf reflection name.
* Added Elder Guardian, Evoker, Guardian, Magma Cube, Phantom, Shulker, Vindicator, and Zombified Piglin.

## 1.0.0
* Initial Release
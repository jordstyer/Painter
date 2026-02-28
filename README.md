🎨 Painter Mod v2.0

Painter is a professional-grade architectural tool for Minecraft 1.21.11. It transforms the standard Brush into a versatile texturing device, allowing Survival players to detail massive structures with speed, precision, and fairness.

✨ Key Features

|

| Category | Feature | Description |
| Area of Effect | Brush Size | Adjust your reach from 1x1 to 5x5 for rapid texturing. |
|  | Brush Shapes | Choose between Square, Circle, and Diamond patterns. |
| Management | Profiles | Save your favorite block blends (Size + Shape + Palette) to named profiles. |
|  | Persistence | Profiles are stored in config/painter/profiles.json and persist across sessions. |
| UX & UI | Rich Tooltips | Hover over your brush to see the active Profile, Size, Shape, and Palette weights. |
|  | Help System | Built-in /paintbrush help command for instant syntax guidance. |
| Optimization | Batching | Consolidates sounds and inventory updates into single "ticks" to prevent lag. |

⚖️ Survival-First Mechanics

Painter is designed for legitimate survival play. It doesn't just "cheat" blocks into the world; it acts as a smart exchanger.

🛡️ The Fair-Play System

| Interaction | Result | Requirement |
| Standard Blocks | 1:1 Swap (e.g., Stone for Cobble) | None |
| Natural Blocks | Downgrades (Grass $\rightarrow$ Dirt) | Default |
| Natural Blocks | Exact Swap (Grass $\rightarrow$ Grass) | Silk Touch Enchantment |
| Ores | Block Destroyed (Drops Nothing) | Default (Anti-Mining Guard) |
| Ores | Exact Swap (Diamond Ore $\rightarrow$ Ore) | Silk Touch Enchantment |
| Unbreakable | Action Denied (Bedrock, Portals) | N/A (Hardness Guard) |

🧩 Intelligent Safety Guards

Fragile Guard: The brush automatically ignores flowers, grass, crops, water, torches, and containers. You can texture a field without deleting your flowers or a house without replacing your chests.

Shape Matching: Orientation is preserved. Swapping a North-facing Oak Stair for a Stone Stair keeps it facing North.

⌨️ Command Reference

| Command | Usage | Description |
| help | /paintbrush help | Displays the in-game command guide. |
| set | /paintbrush set 50 stone, 50 grass | Sets the block palette with optional weights. |
| size | /paintbrush size <1-5> | Changes the brush radius. |
| shape | /paintbrush shape <type> | Options: square, circle, diamond. |
| save | /paintbrush save <name> | Saves current settings to a permanent profile. |
| load | /paintbrush load <name> | Loads a saved profile onto the brush in hand. |
| clear | /paintbrush clear | Wipes all custom data from the brush. |

📥 Installation

Fabric Loader: Required for Minecraft 1.21.11.

Dependencies: None (Optional: Fabric API).

Setup: Drop the .jar into your mods folder.

Configuration: Profiles are saved at config/painter/profiles.json.

📜 License

This project is licensed under the MIT License.
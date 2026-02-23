🎨 Painter Mod

Painter is a specialized Fabric mod for Minecraft 1.21.11 designed to make texturing and detailing large builds in Survival mode intuitive, fair, and satisfying. Inspired by the command syntax of WorldEdit, this mod turns the standard Brush into a powerful architectural tool.

✨ Features

🛠️ Command System

Dynamic Pattern Parsing: Easily set your palette using [weight] [block] syntax (e.g., /paintbrush set 50 stone, 50 stone_bricks).

Smart Defaults: If you omit the weight, the mod auto-assigns 100% to that block (e.g., /paintbrush set spruce_planks).

Tab-Completion: Full support for block ID suggestions. It intelligently omits the minecraft: prefix to keep your chat history clean and prevent hitting character limits.

⚖️ Survival-First Mechanics

1:1 Item Swap: To keep things balanced, the mod consumes one block from your inventory for every block placed. In return, the replaced block is added back to your inventory.

Inventory Safety: If your inventory is full, replaced blocks are dropped safely at your feet rather than being deleted.

Resource Guard: Durability and items are only consumed if a block is actually changed. Clicking on an identical block or an incompatible shape costs nothing.

🔨 Tool Enhancements

Upgraded Durability: The Brush base durability is increased from 64 to 128.

Enchantment Support: Fully compatible with Unbreaking (to save durability) and Mending (to repair via XP orbs).

UI Tooltips: Hover over your Brush to see your active palette and the percentage chances for each block.

🧩 Intelligent Logic

Shape Matching: The mod respects your architecture. It only swaps compatible classes (Stairs $\rightarrow$ Stairs, Slabs $\rightarrow$ Slabs), ensuring that orientations and "upside-down" states are perfectly preserved.

Weighted Randomization: A robust distribution algorithm ensures your textures look exactly as varied as you intended.

🔊 Aesthetic Polish

Feedback Loops: Every successful paint stroke triggers block-specific particles and placement sounds, providing a "punchy" and satisfying building experience.

🚀 Roadmap: Version 2.0

We are hard at work on the next iteration of the Painter mod. Planned features include:

Palette Profiles: Save and load named palettes (e.g., /paintbrush save castle_wall) via local configs or data components.

Undo Function: A limited history buffer to quickly revert accidental paint strokes.

Area Effect: A toggleable "radius" mode to texture larger surfaces with a single click.

📥 Installation

Ensure you have the Fabric Loader installed for Minecraft 1.21.11.

Download the latest .jar from the releases page.

Place the file in your .minecraft/mods folder.

(Optional) Install the Fabric API for full compatibility.

📜 License

This project is licensed under the MIT License.
# BookO'Knowledge

A complete remake of the iconic "Book O' Knowledge" plugin originally featured in the scripted Minecraft SMP **"Wisp SMP."** This iteration has been entirely re-engineered as a native **Fabric Mod** designed to run seamlessly on modern Minecraft versions.

---

## 🚀 Features

* **Universal Compatibility:** Native architecture supporting both Singleplayer worlds (Integrated Server) and Dedicated Multiplayer Servers out-of-the-box.
* **Custom Registry Integration:** Registers the custom `spellbookmod:spellbook` item using modern registry keys, ensuring proper networking and save-state serialization.
* **Native Permissions Routing:** Implements standard command constraints checking via the internal Mojang `PermissionLevel` ecosystem, maintaining full compatibility with server operator systems and command blocks.
* **Dynamic Item Processing:** Active server-level loop tracking utilizing Fabric Lifecycle Events to grant holders specialized status indicators, such as continuous Haste V when held in the primary hand.

---

## 🛠️ Technical Details

The mod relies on an updated toolchain configuration tailored for modern Java compilation pipelines:

* **Mod Loader:** Fabric Loader
* **Fabric Loom Build Tool:** v1.15.5+
* **Java Version:** Java 25 (Source & Target Compatibility)
* **Minecraft Base Target Mapping:** Modern Registry & Component Structure Layer

---

## 📥 Installation

### For Clients (Singleplayer)
1. Ensure you have installed the **Fabric Loader** launcher profile for your target version.
2. Locate your local Minecraft directory (Press `Win + R`, type `%appdata%\.minecraft`, and press Enter).
3. Drop the compiled `spellbookmod-[version].jar` directly into your `mods` folder.

### For Servers (Multiplayer)
1. Place the exact same `spellbookmod-[version].jar` file into your dedicated server's `mods` directory.
2. Restart the server.
3. *Note: This is a universal mod. Both the server hosting the game and all connecting clients must have the mod installed to prevent missing-identifier registry errors.*

---

## ⌨️ In-Game Usage

### Creative Mode
The book is automatically appended to the vanilla listings. Navigate to your creative inventory screen, select the **Tools & Utilities** tab, and scroll to the bottom of the section to find your custom Spellbook.

### Administrative Command
Authorized operators (Level 2 or higher) or the server console can distribute the item utilizing the registered command node:

```text
/givespellbook <player_name>

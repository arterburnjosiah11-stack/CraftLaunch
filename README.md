# ⬡ CraftLaunch

### A modern, beautiful Minecraft launcher built with Java Swing

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Personal-blue.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-green.svg)]()
[![Status](https://img.shields.io/badge/Status-Active-success.svg)]()

*A free, lightweight Minecraft launcher with mod support, Microsoft sign-in, and a polished emerald theme.*

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Usage](#-how-to-play) • [Mods](#-mods) • [FAQ](#-faq)

</div>

---

## 🎮 About

**CraftLaunch** is a custom Minecraft Java Edition launcher I built as a personal project. It downloads real Minecraft files directly from Mojang, supports the Fabric mod loader with auto-installed Fabric API, and includes a built-in Modrinth mod browser. Everything is wrapped in a clean dark UI with animated emerald accents.

It's designed to be **simple and fast** — type a username, pick a version, click launch. No bloat, no ads, no telemetry.

## ✨ Features

### 🚀 Launching
- **Real Minecraft download** — pulls official files from Mojang's servers
- **40+ versions supported** — every release from 1.8.9 through the latest snapshots
- **Memory control** — adjust JVM heap from 1GB to 16GB with a slider
- **Live launch log** — watch every step of the launch process in real-time
- **Auto-Java detection** — finds your Java install automatically

### 🧩 Mods
- **Fabric mod loader** — auto-installed when you select it
- **Fabric API** — auto-installed every launch (required by 99% of mods)
- **Modrinth browser** — search and install mods directly inside the launcher
- **Mod management** — enable, disable, or delete any installed mod with one click
- **Clear All Mods** — rescue button for when a broken mod crashes the game
- **Version-aware** — searches filter to your selected MC version automatically

### 🔐 Accounts
- **Offline mode** — type a username and play instantly (no setup)
- **Microsoft sign-in** — full OAuth2 browser-redirect flow (requires Mojang approval)
- **Persistent settings** — username, version, RAM, and loader saved between sessions

### 🎨 Design
- **Dark emerald theme** — easy on the eyes, custom-painted Swing components
- **Animated background** — subtle drifting starfield
- **Pulsing diamond logo** — animated gem icon with breathing glow
- **Hero launch button** — gradient + glow + hover bloom
- **Custom UI** — every button, slider, scrollbar, progress bar, tab, and dropdown rebuilt from scratch

### 🛠️ Diagnostics & Recovery
- **Startup diagnostics** — checks Java version, RAM, disk space
- **Crash report viewer** — opens crash-reports folder directly
- **Smart crash detection** — when the game crashes, it suggests common fixes
- **Settings tab** — view all system info at a glance

## 📸 Screenshots

### Main Launch Screen
The PLAY tab with version selector, mod loader picker, RAM slider, and launch log.

### Mods Browser
The MODS tab — installed mods on the left, Modrinth search on the right.

### Settings & Diagnostics
The SETTINGS tab with Java path, game directory, JVM args, and full system info.

## 📦 Installation

### Requirements

- **Java 17 or newer** (64-bit recommended)
  - Download free at [adoptium.net](https://adoptium.net/)

### Quick Start

1. Download the latest `CraftLaunch.zip` from the [Releases](../../releases) page
2. Extract it anywhere on your computer
3. Run the launcher:
   - **Windows:** double-click `launch.bat`
   - **macOS / Linux:** run `./launch.sh` in a terminal *(may need `chmod +x launch.sh` first)*
   - **Any OS:** `java -jar CraftLaunch.jar`

That's it! 🎉

## 🎯 How to Play

### Offline mode (works immediately, no setup)

1. Type a username (3-16 letters/numbers) in the field at the top-right
2. Pick a Minecraft version from the dropdown on the left
3. Choose **Vanilla** for unmodded play or **Fabric** for modded play
4. Adjust the RAM slider if needed (4GB is plenty for vanilla, 6-8GB for heavy modpacks)
5. Click **▶ LAUNCH MINECRAFT**

The first launch downloads ~200MB of game files. Subsequent launches are fast.

**Offline mode works for:**
- ✅ Singleplayer worlds
- ✅ LAN play with friends on your network
- ✅ Most modded servers
- ✅ Self-hosted servers
- ❌ Premium servers like Hypixel (require Microsoft auth)

### Microsoft sign-in *(advanced)*

Microsoft sign-in requires **Mojang's approval** of the launcher's Azure app, which can take ~1 week as of 2024. Click "Sign in with Microsoft" in the launcher for instructions on the approval form.

## 🧩 Mods

CraftLaunch supports the **Fabric** mod loader, which is one of the two most popular ways to mod Minecraft (the other being Forge).

### Installing mods

1. On the **PLAY** tab, set Mod Loader to **Fabric (mods enabled)**
2. Switch to the **MODS** tab
3. Type a mod name in the search field (e.g. "sodium", "iris", "JEI") and hit Enter
4. Click **INSTALL** on any mod you want
5. Switch back to PLAY and click LAUNCH

### Recommended mods

- **[Sodium](https://modrinth.com/mod/sodium)** — massive FPS boost
- **[Iris](https://modrinth.com/mod/iris)** — shader pack support
- **[Lithium](https://modrinth.com/mod/lithium)** — server-side performance
- **[Mod Menu](https://modrinth.com/mod/modmenu)** — in-game mod list

### Managing mods

The MODS tab shows everything in your `mods/` folder:
- **Disable** — temporarily turns off a mod (renames to `.jar.disabled`)
- **Enable** — re-enables a disabled mod
- **✕ (Delete)** — permanently removes a mod
- **Clear All Mods** — wipes the mods folder (great for fixing crashes)

## ⚙️ Configuration

Settings are saved to `~/.craftlaunch/config.properties`:

| Setting | Description |
|---------|-------------|
| `username` | Your offline display name |
| `version` | Last-selected Minecraft version |
| `loader` | Vanilla or Fabric |
| `ram` | Allocated RAM in GB |
| `azureClientId` | Custom Microsoft Azure client ID *(advanced)* |

The Minecraft game files live in the standard location:
- **Windows:** `%APPDATA%\.minecraft`
- **macOS:** `~/Library/Application Support/minecraft`
- **Linux:** `~/.minecraft`

## ❓ FAQ

<details>
<summary><b>Game won't launch / crashes immediately</b></summary>

1. Click **MODS** tab → **Clear All Mods**
2. Try launching with **Vanilla** loader to confirm Minecraft itself works
3. Update your graphics drivers
4. Try Java 21 if you're on Java 17 (some newer MC versions need it)
5. Click **Crash Reports** in the PLAY tab to see the actual error
</details>

<details>
<summary><b>Mod installed but doesn't show up in-game</b></summary>

Make sure your **Mod Loader** is set to **Fabric** in the PLAY tab. Vanilla Minecraft cannot load mods.
</details>

<details>
<summary><b>Mod crashes with "mixin" errors</b></summary>

The mod is built for a different MC version than the one you're running. Either update the mod or switch to the version it supports. MC `1.21.1` is currently the most stable for mods.
</details>

<details>
<summary><b>Can I join Hypixel / online servers?</b></summary>

You'll need Microsoft sign-in working, which requires Mojang to approve the launcher's Azure app. Until then, the official Mojang launcher is the easiest way to join premium servers. CraftLaunch + Offline Mode handles modded play and LAN.
</details>

<details>
<summary><b>"Out of memory" errors</b></summary>

Lower the RAM slider, or install 64-bit Java. 32-bit Java caps you at ~1.5GB regardless of slider value.
</details>

<details>
<summary><b>Where are mods installed?</b></summary>

In your standard Minecraft folder under `mods/`. Click **📁 .minecraft** in the PLAY tab to open it.
</details>

## 🛠️ Building from Source

```bash
# Clone the repo
git clone https://github.com/arterburnjosiah11-stack/CraftLaunch.git
cd CraftLaunch

# Compile
javac -d bin src/Theme.java src/MinecraftLauncher.java

# Package as JAR
echo "Main-Class: MinecraftLauncher" > bin/MANIFEST.MF
jar cfm CraftLaunch.jar bin/MANIFEST.MF -C bin .

# Run
java -jar CraftLaunch.jar
```

Requires **JDK 17+** (Adoptium Temurin recommended).

## 📂 Project Structure

```
CraftLaunch/
├── src/
│   ├── MinecraftLauncher.java    # Main launcher logic (~2000 lines)
│   └── Theme.java                # Custom UI theme & components
├── CraftLaunch.jar               # Built launcher
├── launch.bat                    # Windows launcher script
├── launch.sh                     # macOS/Linux launcher script
├── index.html                    # Project landing page
└── README.md
```

## 🤝 Credits

CraftLaunch builds on top of these excellent open APIs:

- **[Mojang's Piston Meta API](https://piston-meta.mojang.com/)** — version manifest & game files
- **[Fabric Meta](https://meta.fabricmc.net/)** — Fabric loader profiles
- **[Modrinth API](https://docs.modrinth.com/)** — mod search & download

Inspired by [Prism Launcher](https://prismlauncher.org/), [Lunar Client](https://www.lunarclient.com/), and the official Minecraft Launcher.

## ⚖️ Disclaimer

CraftLaunch is **not affiliated with Mojang Studios or Microsoft**. "Minecraft" is a trademark of Mojang Synergies AB. This launcher does not bypass authentication, distribute game files, or enable piracy — it downloads files directly from Mojang's servers and requires you to own a legitimate copy of Minecraft Java Edition for online play.

This is a personal project built for learning purposes and for playing with a small group of friends.

## 📜 License

Personal project. Feel free to fork and modify for your own use. Please don't repackage and sell.

---

<div align="center">

**Built with ☕ Java and 💚 emerald**

*If you find a bug, [open an issue](../../issues) — I'd love to fix it!*

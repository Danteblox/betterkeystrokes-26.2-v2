# PvP Test Client (Fabric, Minecraft 26.2)

Client-side mod with a configurable autoclicker and player ESP, for testing on a server you run.

## Build (from your phone, no PC)

1. Create a GitHub repo and upload the contents of this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab. The "Build mod" workflow runs on every push (or press "Run workflow").
3. When it finishes, open the run and download the `pvptest-jar` artifact. Unzip it to get `pvptest-1.0.0.jar`.

## Build (on a PC)

Needs JDK 25 and Gradle 9.3+: `gradle build`. The jar is in `build/libs/pvptest-1.0.0.jar`.

## Install

1. Install Fabric Loader 0.19.3+ for Minecraft 26.2.
2. Install Fabric API 0.152.2+26.2 (Modrinth) into the same mods folder.
3. Start **Remote Files** in the launcher, open the URL and PIN in a browser, and upload `pvptest-1.0.0.jar` as a mod.

## Controls

| Key | Action |
|-----|--------|
| R   | Toggle autoclicker |
| H   | Toggle ESP |

Rebind both under Options > Controls > PvP Test Client.

## Commands

    /ac                         show current settings
    /ac toggle
    /ac cps <min> <max>         e.g. /ac cps 9 13   (1-20; one click per tick max)
    /ac jitter <0-0.5>          random variation per click
    /ac mode hold|auto          hold = clicks while you hold the button; auto = continuous
    /ac button left|right
    /ac onlyonentity true|false
    /ac clickonblocks true|false
    /ac weapononly true|false
    /ac pausewhileusing true|false

    /esp                        show current settings
    /esp toggle
    /esp color ff3030           hex RGB
    /esp distance <blocks>      0 = unlimited
    /esp invisible true|false   include invisible players

Everything is saved to `config/pvptest.json`, which you can also edit by hand.

## Notes

- ESP uses the vanilla glow outline, which renders through walls. It affects players only.
- Clicks are capped at one per game tick (20 CPS max) because the clicker runs on the client tick. Without a keyboard, use /ac toggle and /esp toggle in chat.
- If a name below fails to compile or a mixin fails to apply on 26.2, it was renamed by Mojang or Fabric:
  `Minecraft#startAttack`, `Minecraft#startUseItem`, `Minecraft#shouldEntityAppearGlowing`,
  `Entity#getTeamColor`, `KeyMapping.Category.register`, `Identifier.fromNamespaceAndPath`,
  `ClientCommandManager`.

# Custom Paintings

![](https://img.shields.io/badge/Loader-Fabric-%23313e51?style=for-the-badge)
![](https://img.shields.io/badge/MC-1.20.*%20|%201.19.*-%23313e51?style=for-the-badge)
![](https://img.shields.io/badge/Side-Client%20+%20Server-%23313e51?style=for-the-badge)

<a href="https://ko-fi.com/roundaround">
  <img alt="kofi-singular-alt" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/donate/kofi-singular-alt_vector.svg">
</a>

-----

Add your own custom paintings to Minecraft! Add as many as you like without being restricted to re-texturing vanilla
paintings! Each painting can have a custom size and texture, and even name and artist labels. Using a custom "Painting
Pack", add custom paintings to any world, even servers!

**TODO: Screenshot of custom paintings on the wall**

Out of the box, you'll also see a built-in pack containing the
four elemental-themed paintings bundled in the game's assets that otherwise are not normally placeable. Enjoy your
stylish Air, Earth, Fire, and Water paintings!

**TODO: Screenshot of unplaceable paintings**

In order to better work with all the paintings in your game, this mod also creates a painting picker menu. Instead of
the game placing a random choice every time, placing a painting will now open up a menu where you can choose a pack and
painting, complete with search and filtering. Now you can place the exact painting you want first time, every time!

**TODO: Screenshot of pack & painting select screens**

**Please note that _no_ new paintings are included in this mod. You need to supply your own!** You can use this mod
without adding and packs, and it'll still give access to the hidden paintings and the fancy new painting picker menu.
However, if you're looking for a painting pack that supports this mod, check out
my [Famous Real Paintings](https://modrinth.com/resourcepack/famous-real-paintings) pack that adds stylized
representations of some of the worlds' most famous paintings from real life!

## Adding a painting pack to your world

The simplest way to add a painting pack to your world is to drop it in the `custompaintings` folder inside the world
save, i.e. `.minecraft/saves/My Awesome World/custompaintings/FamousPaintings-1.0.zip`. The pack can be in zip or folder
form.

```
└── .minecraft/
    ├── config/
    ├── logs/
    ├── resourcepacks/
    ├── saves/
    │   └── My Awesome World/
    │       ├── advancements/
    │       ├── custompaintings/
    │       │   └── FamousPaintings-1.0.zip  ᐊ
    │       ├── data/
    │       ├── datapacks/
    │       ├── DIM1/
    │       ├── DIM-1/
    │       ├── entities/
    │       ├── playerdata/
    │       ├── poi/
    │       ├── region/
    │       ├── stats/
    │       └── level.dat
    ├── options.txt
    └── servers.dat
```

You can also add a pack to a world by open the mod's main menu (see below), navigating to the 'Manage Packs' screen, and
dragging the pack zip file or folder onto your game window, similar to how you would do it with resource packs. The mod
will take care of copying the files over and should automatically refresh the list for you to let you know it worked!

**TODO: Screenshot of packs screen**

## Adding a painting pack to your server

Adding a pack to a server must be done by a server admin with access to the server's filesystem. Similar to installing
on a single player world, simply copy the pack into the `custompaintings` folder inside the server's world save.

```
└── <server root> /
    ├── config/
    ├── logs/
    ├── world/
    │   ├── advancements/
    │   ├── custompaintings/
    │   │   └── FamousPaintings-1.0.zip  ᐊ
    │   ├── data/
    │   ├── datapacks/
    │   ├── DIM1/
    │   ├── DIM-1/
    │   ├── entities/
    │   ├── playerdata/
    │   ├── poi/
    │   ├── region/
    │   ├── stats/
    │   └── level.dat
    ├── banned-ips.json
    ├── banned-players.json
    ├── eula.txt
    ├── ops.json
    ├── server.properties
    ├── usercache.json
    └── whitelist.json
```

## Creating a painting pack

### Pack format

The custom paintings for this mod are powered by painting packs. To create a painting pack for the mod, create a folder
for it somewhere on your computer and give it a helpful name, like "Scott's Custom Paintings". Inside that folder,
create another folder called "images" and copy all of your painting images inside. Make sure they are all PNG image
files. If you have a different format, you can convert it using an online file converter (find one using your favorite
search engine; there are many, many out there). Then again inside the pack's root folder, also create a file named
`custompaintings.json`.

```
.
└── Scott's Custom Paintings/
    ├── images/
    │   ├── castlescape.png
    │   ├── sunsetbeach.png
    │   ├── worldmap.png
    │   └── spookyforest.png
    └── custompaintings.json
```

Open the `custompaintings.json` file and fill it with contents that look something like this:

```json
{
  "id": "scottscustompaintings",
  "name": "Scott's Custom Paintings",
  "description": "Scott's Custom Paintings",
  "paintings": [
    {
      "id": "castlescape",
      "name": "Castle in the Mountains",
      "artist": "Scott Lang",
      "width": 2,
      "height": 3
    },
    {
      "id": "sunsetbeach",
      "name": "Sunset at the Beach",
      "artist": "Claude Monet",
      "width": 2,
      "height": 2
    },
    {
      "id": "worldmap",
      "name": "World Map",
      "artist": "Scott Lang",
      "width": 4,
      "height": 3
    },
    {
      "id": "spookyforest",
      "name": "Nighttime Forest",
      "artist": "Scott Lang",
      "width": 3,
      "height": 1
    }
  ]
}
```

#### Pack properties

`id`: `Text, alphanumeric & underscores only` - The unique ID of the painting pack. This must be different from all
other painting packs (and different from the mod IDs for other fabric mods that add custom paintings).

`name`: `Text (optional)` - The name to show in the painting picker for this pack. If omitted the UI will simply
show the resource pack's filename.

`description`: `Text (optional)` - A short description of the pack to show in the painting picker.

`paintings`: `List[Painting]` - The definitions of all the paintings to pull from in the painting picker UI. Each
painting's id should correspond to the image's filename (without the '.png').

`migrations`: `List[Migration]` - The automatic migrations associated with this pack. It is not likely you'll want to
create one of these by hand, but the standalone painting pack editor _(coming soon)_ can generate them for you when you
split large packs into multiple smaller ones.

#### Painting properties

Each painting will need its own properties defined to tell the mod how the paintings should be displayed in game.

`id`: `Text, alphanumeric & underscores only` - The unique ID of the painting. This should correspond to the name of the
painting's PNG image file in the pack's "images" folder (without the '.png').

`name`: `Text (optional)` - The name of the painting.

`artist`: `Text (optional)` - The artist of the painting.

`width`: `Integer` - The number of blocks wide this painting should occupy. For example, the vanilla Donkey Kong
painting would have a value of 4.

`height`: `Integer` - The number of blocks high this painting should occupy. For example, the vanilla Donkey Kong
painting would have a value of 3.

## Mod main menu (config, pack management, cache management, etc.)

The mod ships with a main menu to access a suite of utilities for working with the mod. If you
have [Mod Menu](https://modrinth.com/mod/modmenu) installed, you'll find the main menu from the mod's configure button
in the mods list. Otherwise, you can configure a keybind (`U` key by default) in Minecraft's vanilla keybinds menu which
will open the Custom Paintings main menu on key press.

**TODO: Mod Menu screenshot**
**TODO: Keybinds screenshot**
**TODO: Main menu screenshot**

### Mod configuration

The mod has multiple configuration files to tweaking mod behavior. There is some client-side only configuration hosted
in the main config folder inside your Minecraft's installation folder as well as world-specific configuration inside the
save file (`world` on servers) folder.

#### Client-side/global configuration

There is a "global" config file for your client that applies to all your single player worlds. This will be found in
`<minecraft directory>/config/custompaintings.toml`. You can also modify it through the config screen found from the
main menu.

**Override vanilla render distance** (`overrideRenderDistance`): `true|false` - Whether to override the game's default
painting rendering distance threshold. By default, paintings are rendered at the same distance as all other entities,
but enabling this will allow you to force the game to render them from further away. Default is `false`.

**Render distance scale** (`renderDistanceScale`): `Integer; 1-64` - The number with which to scale the render distance
threshold for paintings. Higher numbers means rendering from farther away. Only applies when `overrideRenderDistance` is
`true`. Default is `16`.

**Cache images from the server locally** (`cacheImages`): `true|false` - Whether to cache painting images that have been
downloaded from the server. Doing this can reduce loading time and save on network/server resources, but means storing
the images permanently on disk, which can potentially take up a lot of space. Default is `true`.

**Number of days to retain cached images** (`cacheTtl`): `Integer; 1-1000` - How long cached painting images should be
stored in the cache before being marked as stale and getting automatically deleted. Default is `14`.

**Silence all legacy pack conversion prompts** (`silenceAllConvertPrompts`): `true|false` - Whether to silence prompts
about converting legacy (pre-3.0.0) resource pack style painting packs to the new format. After joining a world, the mod
will check if you have any of the old style painting packs in your resourcepacks directory and prompt you to convert
them from the mod's main menu. Set this to `true` to silence them globally. Defaults to `false`.

#### Single player per-world configuration

There is also a config file in each world's save folder, i.e.
`<minecraft directory>/saves/My World/config/custompaintings.toml`. If you are in a single player world, you can also
modify these from the same in-game menu as the client-side config.

#### Server-side configuration

Finally there is a config file on the server, inside the world's save folder, i.e.
`<server root>/world/config/custompaintings.toml`. You'll need access to the server's filesystem to change any of these
settings.

**Throttle image transfers to clients** (`throttleImageDownloads`): `true|false` - Whether to throttle the rate at which
custom painting images are sent to clients when they join the server. Highly recommended to keep this enabled,
especially if you have a moderate-to-large scale server (10+ players). Defaults to `true`.

**Maximum number of image packets per second** (`maxImagePacketsPerSecond`): `Integer; 0-∞` - The maximum number of
image network packets to send per second to connected clients. Only applies when `throttleImageDownloads` is `true`. Set
to `0` to disable throttling by packet count. Defaults to `40`.

**Maximum number of image packets per second per client** (`maxPerClientImagePacketsPerSecond`): `Integer; 0-∞` - The
maximum number of image network packets to send per second to connected clients on a per-client basis. Only applies when
`throttleImageDownloads` is `true`. Set to `0` to disable throttling by packet count on a per-client basis.

**Maximum size for each image packet in KB** (`maxImagePacketSize`): `Integer; 0-∞`: The maximum size for each painting
image network packet that can be sent to clients, in KB. Only applies when `throttleImageDownloads` is `true`. Set to
`0` (not recommended) to send all images in their entirety in single network packets. Defaults to `256`.

### Enabling or disabling packs, or adding a pack to your world

### Converting old (pre-3.0.0) packs to the new format

## Uninstalling this mod and data integrity

-----

<details>
<summary>Pre-3.0.0 mod listing</summary>

Add your own custom paintings to Minecraft. No longer are you bound to the number (or sizes) of paintings in vanilla!
Based on resource packs, you can define as many paintings as you like! The server will store the painting's size and
name and any clients with a painting of the same name will be able to render it!

**Please note that _no_ new paintings are included in this mod. You need to supply your own!** If you're looking for a
painting pack that supports this mod, check out
my [Famous Real Paintings](https://modrinth.com/resourcepack/famous-real-paintings) resource pack!

This mod comes with a painting picker UI, allowing you to specifically choose which painting you'd like to pick. Even if
you don't install any painting packs, the new picker UI can help you place exactly the painting you're looking for!

![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/pack-select.png)
![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/painting-select.png)
![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/filter.png)

## Resource packs

### Image files

Insert your custom painting image files in a very similar place as vanilla paintings! The primary difference will be the
`minecraft` folder should be changed to your own custom, unique id, and should match the one listed in your
`custompaintings.json` file (see below).

`Famous Paintings.zip/assets/famouspaintings/textures/painting/davinci_mona_lisa.png`

Similar to vanilla paintings, the files need to be in .png format. While there are technically no minimum or maximum
size, it is really recommended to stick to an image resolution of 16 pixels per block (or scaled up to match your
current resource pack). Anything over 160 pixels per block is probably going to make the image file larger than it
really needs to be and is likely to cause performance issues.

### Resource pack format

The custom paintings for this mod are powered by resource packs. To create a resource pack compatible with the mod,
simply add a new file `custompaintings.json` to the root of the resource pack that might look something like this:

```json
{
  "id": "famouspaintings",
  "name": "Famous real paintings",
  "paintings": [
    {
      "id": "davinci_mona_lisa",
      "name": "Mona Lisa",
      "artist": "Leonardo DaVinci",
      "width": 1,
      "height": 2
    },
    {
      "id": "monet_water_lilies",
      "name": "Water Lilies",
      "artist": "Claude Monet",
      "width": 2,
      "height": 2
    },
    {
      "id": "vangogh_starry_night",
      "name": "Starry Night",
      "artist": "Vincent van Gogh",
      "width": 4,
      "height": 3
    }
  ]
}
```

#### Pack properties

`id`: `Text, alphanumeric & underscores only` - The unique ID of the painting pack. This must be different from all
other painting packs (and different from the mod IDs for other fabric mods that add custom paintings). Additionally this
ID determines the directory in which you should place the painting textures. For example, with the ID "famouspaintings",
the textures should go in `assets/famouspaintings/textures/painting`.

`name`: `Text (optional)` - The name to show in the painting picker for this collection! If omitted the UI will simply
show the resource pack's filename.

`paintings`: `List[Painting]` - The definitions of all the paintings to pull from in the painting picker UI!

`migrations`: `List[Migration]` - The large-scale migrations associated with this pack. It is not likely you'll want to
create one of these by hand, but the standalone painting resource pack editor _(coming soon)_ can generate them for you
when you split large packs into multiple smaller ones.

#### Painting properties

Each painting will need its own properties defined. By default, any painting textures within your resource pack's
directory will default to the ID of the filename, with a block height & width of 1. Define entries in your
custompaintings.json to overwrite these dimensions!

`id`: `Text, alphanumeric & underscores only` - The unique ID of the painting. This is what will be stored on the server
and will determine which texture to render. For example, the ID of `starry_night` in the pack `famouspaintings` will
render the texture located at `assets/famouspaintings/textures/painting/starry_night.png`.

`name`: `Text (optional)` - The name of the painting. Introduced in v2.0.0.

`artist`: `Text (optional)` - The artist of the painting. Introduced in v2.0.0.

`width`: `Integer` - The number of blocks wide this painting should occupy. For example, the vanilla Donkey Kong
painting would have a value of 4.

`height`: `Integer` - The number of blocks high this painting should occupy. For example, the vanilla Donkey Kong
painting would have a value of 3.

#### Migration properties

As mentioned above, it is unlikely you'll want to create a migration by hand. However, if you would like to do so, a
migration is composed of a few key parts.

`id`: `Text` - Together with the pack id should be globally unique across all migrations in all packs.

`description`: `Text (optional)` - A human-readable description of the migration, i.e. "Split from 'Famous Real
Paintings'"

`pairs`: `List[List[ID]]` - The list of painting ID pairs this migration is responsible for. For each entry here,
running the migration will reassign any painting with the first ID to instead be the painting with the second ID.

## Management UIs

To assist in managing your custom paintings through resource pack updates (because sometimes things can get changed or
removed, so the paintings in your world might get out of date), there are a few included UIs designed to facilitate some
common cleanup tasks. These screens can be accessed through a keybinding (no assignment by default, located in the
Miscellaneous section of the keybinds menu) or by using the `/custompaintings manage` command.

![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/manage.png)

### Unknown paintings

Identify any paintings in your world whose IDs no longer exist in any of your resource packs, then decide whether you'd
like to reassign those paintings to something new or simply remove them from your world.

![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/unknown.png)

### Mismatched paintings

Identify any paintings in your world that have metadata that does not match what is currently specified in your enabled
resource packs, and easily update them from the resource pack with the click of a button.

![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/mismatched.png)

### Run a migration

When updating the custom painting resource packs, sometimes it is necessary to do large scale updates (such as splitting
packs into multiple smaller ones). If any migrations are specified in any of your painting resource packs, you can run
them here to automatically update any paintings affected.

![](https://raw.githubusercontent.com/Roundaround/mc-fabric-custom-paintings/refs/heads/main/assets/screenshots/2_0_0/migration.png)

## Commands

The mod comes some utility commands for managing your custom paintings as well. To access them, open your Minecraft chat
and enter `/custompaintings` followed by the appropriate command. For example, you can access the management UIs by
entering `/custompaintings manage` in the Minecraft chat box. If a command references a "targeted" painting, it is
referring to any painting your character is currently looking at.

`manage` - Opens the management screens

`identify` - Prints some information in chat about the targeted painting

`count` - Prints the number of copies of the targeted painting currently in the world

`count minecraft:kebab` - Prints the number of copies of the kebab painting currently in the world

`fix` - Attempts to update the targeted painting from the data in your installed resource packs

`fix all` - Attempts to update all paintings that have incorrect data

`fix famouspaintings:davinci_mona_lisa` - Attempts to update all copies of the famouspaintings:davinci_mona_lisa
painting throughout the world

`move up 2` - Moves the targeted painting up 2 blocks

`reassign minecraft:aztec` - Changes the targeted painting to be the aztec painting

`reassign all minecraft:kebab minecraft:aztec` - Changes all copies of the kebab painting into the aztec painting

`remove` - Removes the targeted painting

`remove unknown` - Removes all paintings in the world that aren't specified in any resource packs

`remove minecraft:kebab` - Removes all copies of kebab in the world

## Warning

This mod was designed and developed with single player in mind! While the mod _might_ work on a multiplayer server,
everyone would need to have the mod and all custom painting resource packs installed (and all the same version) or
things can get messy quick.

</details>

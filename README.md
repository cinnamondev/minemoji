# minemoji

Use emotes in minecraft! Does not interfere with resource packs as it uses the new Object components :)

Animated sprites supported, will work with DiscordSRV (translate the discord emotes -> minemoji emotes and v.v)

(Supports Paper 1.21.9/10+!)

https://github.com/user-attachments/assets/404500c4-36d6-4563-a049-5e0031ad3cd0

### Note on DiscordSRV integration

I don't think this would play very nice with other plugins that hook into
the post-process events of DiscordSRV (at the moment). Todo for ways to inserting objectcomponents into discordsrv messages.

## Config

```
unicode-emojis:
  enabled: true # default option, set to false to disable default emojis...
  uri: "https://cinnamondev.github.io/minemoji/packs/twemoji-latest.zip"

custom-packs:
  # download json files from sources before loading.
  download: false # place jsons in plugins/minemoji/packs/*.json otherwise
  packs:
    - "https://cinnamondev.github.io/minemoji/packs/minemoji-latest.json" # Minemoji example pack
enforce-pack: true
# use minimessage!  :) remove entry to disable prompt
join-prompt: "This server uses resource pack to allow players to use emotes in gamechat :) <click:open_url:'https://github.com/cinnamondev/minemoji'><u><b><aqua>About</aqua></b></u></click>"
```

Default config. Add json files served by websites containing pack information to download emote packs.
The uri specified in `unicode-emojis` should point to a resource pack, however.


## PackMaker

Make emote resource packs + json files out of input directories. Example command:

```
java -cp minemoji.jar com.github.cinnamondev.minemoji.PackMaker.PackMaker 
  --input-directory ./sample_pack/
  --output-directory ./minemoji-latest
  --width 32
  --prefix minemoji
  --pack-url https://cinnamondev.github.io/minemoji/packs/minemoji-latest.zip
  --zip-pack --delete-directory
```
The corresponding emoji lookup files will be put in the root of the resource pack and
in `<JAR LOCATION|plugins>/minemoji/packs/*.json`.

If you generate a unicode emote pack, use prefix `unicode` and argument `--skip-packgen`, which will
disable generating a `unicode.json` file (as the `unicode` emote set is 'pre-baked')

Supports GIF, Svg, Png, etc... Anything Java ImageIO can process...
File names will be taken as sprite names.

In the input directory, if you put files ending with `.info` corresponding to the
emotes, i.e.: `ralsei_spin.gif` and `ralsei_spin.gif.info`, you can change:
- The max frame rate of an animated emote (the default and **absolute** max is 20)

# license

licensed under apache 2.0 license

import argparse
import emoji
import cairosvg
import os
import glob
import json

from wand.image import Image

def dir_path(string: str):
    if os.path.isdir(string):
        return string
    else:
        print(string)
        raise NotADirectoryError(string)

def dir_make_path(string:str):
    if os.path.isdir(string):
        return string
    else:
        os.makedirs(string)
        return string
def arg_parse():
    parser = argparse.ArgumentParser(description="resource pack machine")
    parser.add_argument("--twemoji", action='store_true', help="ignores .info files and uses own knowledge for lut")
    parser.add_argument("--emoji_set_name", default="minemoji", help="emoji set's name")
    parser.add_argument("--in_dir", type=dir_path, default="./pack_images", help="directory containing this emoji set images and corresponding .info files if needed")
    parser.add_argument("--out_dir", type=dir_make_path, default="./pack_output", help="root directory for resource pack to be generated ")
    parser.add_argument("--default_max_frames", type=int, default=10,  help="limit amount of frames kept in gif file types.")

    return parser.parse_args()

def main():
    args = arg_parse()
    if args.twemoji: 
        args.emoji_set_name = "twemoji"


    emojis = None
    if not os.path.isfile(args.out_dir + "/emojis-" + args.emoji_set_name +"" + ".json"):
        emojis = {
            "prefix": args.emoji_set_name
            "packVersion": 1
            "emojis": []
        }
    else:
        with open(args.out_dir + "/emojis.json", 'r', encoding='utf-8') as f:
            emojis = json.load(f)
            emojis.packVersion += 1
    out_dir = args.out_dir + "/assets/minecraft/textures/" + args.emoji_set_name +"/"
    dir_make_path(out_dir)
    # make image
    for image in (glob.glob(args.in_dir + "/*.png") + glob.glob(args.in_dir + "/*.jpg") + glob.glob(args.in_dir + "/*.jpeg") + glob.glob(args.in_dir + "/*.gif") + glob.glob(args.in_dir + "/*.svg")):
        spriteName = None
        maxFrames = None
        hexCode = None
        if args.twemoji:
            spriteName = os.path.basename(image).split(".")[0]
        elif not os.path.exists(image + ".info"):
            spriteName = os.path.basename(image).split(".")[0]
            maxFrames = args.default_max_frames
        else:
            with open(image + ".info") as info:
                spriteName = info.readline().strip().replace(" ","_")
                try:
                    maxFrames = int(info.readline())
                    if maxFrames == -1: maxFrames = args.default_max_frames
                except:
                    print("malformed option defaulting to script default")
                hexCode = info.readline().strip()

        if image.endswith(".svg"):
            cairosvg.svg2png(url=image,
                             write_to=(out_dir + spriteName + ".png"),
                             output_width=32,
                             output_height=32)
        elif image.endswith(".gif"):
            mcmeta = {
                "animation": {
                    "frametime": 20,
                    "interpolate": True,
                    "frames": [] # [{"index":0,"time":ticks}]
                }
            }
            with (Image() as newImg):
                #newImg.format = "png"
                with Image(filename=image) as inImage:
                    len(image)
                    if maxFrames == -2:
                        prevDelay = None
                        computeFrameDelay = False
                        for i, frame in enumerate(inImage.sequence):
                            if frame.delay != prevDelay or computeFrameDelay:
                                computeFrameDelay = True # if we see the first instance of weird frame delay then we stop updating prevDelay and that is used for frametime
                                mcmeta['animation']['frames'].push({
                                    "index": i,
                                    "time": frame.delay / 20
                                })
                            else: prevDelay = frame.delay
                            frame.resize(32,32)
                            newImg.sequence.append(frame)
                        mcmeta['animation']['frametime'] = prevDelay / 20
                    for i in range(0, len(image), (int) (len(image)/maxFrames)):
                            print(i)
                            inImage.sequence[i].resize(32,32)
                            newImg.sequence.append(inImage.sequence[i])
                newImg.smush(stacked=True)
                newImg.save(filename=out_dir + spriteName + ".png")
                # save frames details...
                with open(out_dir + spriteName + ".png.mcmeta", "w+", encoding="utf-8") as f:
                    json.dump(mcmeta, f, ensure_ascii=False)
        else:
            with Image(filename=image) as inImage:
                inImage.format = "png"
                inImage.resize(32,32)
                inImage.save(filename = out_dir + spriteName + ".png")
        output = {
            "emojiText": spriteName,
            "resource": args.emoji_set_name + "/" + spriteName,
        }
        if hexCode != None: output["hex"] = hexCode
        emojis['emojis'].append(output)
    if not args.twemoji:
        print("hellow")
        with open(args.out_dir + "/emojis.json", 'w+', encoding='utf-8') as f:
            json.dump(emojis, f)
    with open(args.out_dir + "/pack.mcmeta", 'w+', encoding='utf-8') as pack_meta:
        json.dump({
            "pack": {
                "description": "Minemoji Emoji Pack",
                "min_format": 69,
                "max_format": 69
            }
        }, pack_meta)
    dir_make_path(args.out_dir + "/assets/minecraft/atlases/")
    with open(args.out_dir + "/assets/minecraft/atlases/paintings.json", 'w+', encoding='utf-8') as atlas:
        json.dump({
            "sources": [
                {
                    "type": "directory",
                    "source": args.emoji_set_name,
                    "prefix": args.emoji_set_name + "/"
                }
            ]
        }, atlas)
    print("gifs!")



if __name__ == "__main__":
    main()

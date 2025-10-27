package com.github.cinnamondev.minemoji.PackMaker;

import com.github.cinnamondev.minemoji.EmojiSet;
import com.google.gson.Gson;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


///  half finished resource pack generator
/// java -cp jar com.github.cinnamondev.minemoji.PackMaker.PackMaker (...args)
public class PackMaker {
    private static final Options cliOptions;
    static {
        cliOptions = new Options();

        var inDir = new Option("i", "input-directory",  true, "location containing images + .info files");
        inDir.setRequired(true);
        inDir.setType(File.class);
        cliOptions.addOption(inDir);

        var outDir = new Option("o", "output-directory",  true, "resource pack root. if this is zipped, the filename will be this + .zip.");
        outDir.setRequired(true);
        outDir.setType(File.class);
        cliOptions.addOption(outDir);

        var prefix = new Option("p", "prefix", true, "prefix used for recognizing and in sprite folder");
        prefix.setRequired(true);
        cliOptions.addOption(prefix);

        var packUrl = new Option("u", "pack-url", true, "url used to refer to pack for download");
        packUrl.setRequired(true);
        packUrl.setType(URL.class);
        cliOptions.addOption(packUrl);

        var formatMaxVersion = new Option("max-version", true, "max version");
        formatMaxVersion.setRequired(false);
        formatMaxVersion.setType(Number.class);
        cliOptions.addOption(formatMaxVersion);

        var emoteKeyWidth = new Option("w", "width", true, "width of emote key");
        emoteKeyWidth.setRequired(false);
        emoteKeyWidth.setType(Number.class);
        cliOptions.addOption(emoteKeyWidth);

        var packgen = new Option("s", "skip-packgen", false, "skips generation of .json file");
        packgen.setRequired(false);
        cliOptions.addOption(packgen);

        var createZip = new Option("z", "zip-pack", false, "zips up back");
        packgen.setRequired(false);
        cliOptions.addOption(createZip);

        var zipDeleteDirectory = new Option("d", "delete-directory", false, "delete directory");
        zipDeleteDirectory.setRequired(false);
        cliOptions.addOption(zipDeleteDirectory);

        cliOptions.addOption("t", "atlas", true, "atlas key used. not rec to change.");
        cliOptions.addOption("v", "verbose", false, "verbose");

        cliOptions.addOption("c", "dontServe", false, "dont serve pack to client?");
        cliOptions.addOption("a", "append", false, "append to pack");
    }
    protected record CLIArgs(File inputDirectory, File outputDirectory, URL packUrl, String prefix,
                             String atlas,
                             boolean createPackInfo, boolean verbose, int maxVersion,
                             int keyWidth, boolean createZip, boolean deleteDirectory,
                             boolean serveToClient, boolean appendToPack) {
        public static CLIArgs fromOpts(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(cliOptions, args);

            return new CLIArgs(
                    commandLine.getParsedOptionValue("input-directory"),
                    commandLine.getParsedOptionValue("output-directory"),
                    commandLine.getParsedOptionValue("pack-url"),
                    commandLine.getParsedOptionValue("prefix"),
                    commandLine.getOptionValue("atlas", "paintings"),
                    !commandLine.hasOption("skip-packgen"),
                    commandLine.hasOption("verbose"),
                    commandLine.hasOption("max-version")
                            ? ((Number)commandLine.getParsedOptionValue("max-version")).intValue()
                            : MAX_RP_VERSION,
                    commandLine.hasOption("width")
                            ? ((Number) commandLine.getParsedOptionValue("width")).intValue()
                            : 32,
                    commandLine.hasOption("zip-pack"),
                    commandLine.hasOption("delete-directory") && commandLine.hasOption("zip-pack"),
                    !commandLine.hasOption("serve"),
                    commandLine.hasOption("append")
            );
        }
    }

    protected static int MIN_RP_VERSION = 69;
    protected static int MAX_RP_VERSION = 69;
    protected static int DEFAULT_FRAMERATE = 5;

    protected static void generatePackMCMeta(CLIArgs args) throws IOException {
        File file = args.outputDirectory.toPath().resolve("pack.mcmeta").toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        bw.write("{\"pack\": {\"description\": \"Minemoji Emoji Pack\",\"min_format\":" + MIN_RP_VERSION  +",\"max_format\":" + args.maxVersion + "}}");
        bw.close();
    }
    protected static Path texturesFolder(CLIArgs args) throws IOException {
        Path basePath = args.outputDirectory.toPath();

        Path path = basePath.resolve("assets/minecraft/textures/" + args.prefix);
        path.toFile().mkdirs();
        return path;
    }
    protected static void generateAtlas(CLIArgs args) throws IOException {
        File file = args.outputDirectory.toPath().resolve("assets/minecraft/atlases/" + args.atlas +".json").toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        bw.write("{\"sources\": [{\"type\": \"directory\", \"source\": \"" + args.prefix + "\", \"prefix\": \"" + args.prefix + "/\"}]}");
        bw.close();
    }

    protected static int getFramerateOrDefault(@Nullable File infoFile) throws IOException {
        if (!infoFile.exists()) { return DEFAULT_FRAMERATE; }
        try (BufferedReader bw = new BufferedReader(new FileReader(infoFile))) {
            String unparsedLine = bw.readLine();
            if (NumberUtils.isCreatable(unparsedLine)) {
                return Integer.parseInt(unparsedLine);
            } else {
                return DEFAULT_FRAMERATE;
            }
        }
    }
    protected static File resolveInfoFile(File file) {
        return file.getParentFile().toPath().resolve(file.getName() + ".info").toFile();
    }

    ///  transcodes and saves SVGs using args specified.
    public static void processSvg(CLIArgs args, File file, File outFile) throws IOException, TranscoderException {
        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) args.keyWidth);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) args.keyWidth);
        TranscoderInput input = new TranscoderInput(new FileInputStream(file));

        OutputStream out = new FileOutputStream(outFile);
        TranscoderOutput output = new TranscoderOutput(out);

        t.transcode(input, output);
        out.flush();
        out.close();
    }

    ///  [from stackoverflow :)](https://stackoverflow.com/questions/20077913/read-delay-between-frames-in-animated-gif)
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)== 0) {
                return((IIOMetadataNode) rootNode.item(i));
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return(node);
    }

    protected static int getDelayTimeFromCurrentStream(ImageReader gifReader, int frame) throws IOException {
        //if (frame == 0) { return -1; }
        IIOMetadata meta = gifReader.getImageMetadata(frame);
        String formatName = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(formatName);
        IIOMetadataNode graphicsController = getNode(root, "GraphicControlExtension");
        String unparsedTime = graphicsController.getAttribute("delayTime");

        int time = Integer.parseInt(unparsedTime);
        return time != 0 ? time : 10; // 10 / 5 -> 2 frametime
    }

    protected static int modeOfList(List<Integer> list) {
        if (list == null || list.isEmpty()) { throw new IllegalArgumentException("list is empty"); }
        return list.stream()
                .filter(f -> f != -1)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    protected static void processGif(CLIArgs args, ImageReader gifReader, File file, File outFile, File mcmetaFile) throws IOException {
        int maxFramerate = getFramerateOrDefault(resolveInfoFile(file));
        if (maxFramerate > 20) { maxFramerate = 20; } // framerate is capped by the game! 20 fps -> 1 fpt

        ArrayList<Integer> frameTimes =  new ArrayList<>(); // the most common entry will be turned into frameTime.
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        gifReader.setInput(stream);

        int n = gifReader.getNumImages(true);
        BufferedImage output = new BufferedImage(args.keyWidth,args.keyWidth*n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();
        g2d.setBackground(new Color(0,0,0,1f));
        for (int i = 0; i < n; i++) {
            BufferedImage frame = gifReader.read(i);
            var newFrame = frame.getScaledInstance(args.keyWidth, args.keyWidth, Image.SCALE_SMOOTH);
            g2d.drawImage(newFrame, 0, args.keyWidth*i, null);

            frameTimes.add(getDelayTimeFromCurrentStream(gifReader, i));
        }
        g2d.dispose();
        ImageIO.write(output, "png", outFile);

        // make the texture .mcmeta, most common delay will be the 'frame time'
        // and delays too short will be increased to maintain the max framerate.
        int mode = modeOfList(frameTimes);
        int minDelay = (10000 / maxFramerate);  // hundreths of a second. at 20 fps it should be '5'
        int minDelayTicks = minDelay / 5; // at 20 fps it should be '1'
        ArrayList<String> frames = new ArrayList<>(frameTimes.size());
        boolean hasNonStandardFrames = false;
        for (int i = 0; i < frameTimes.size(); i++) {
            int currentFrame = frameTimes.get(i);
            String frameOutput;
            if (currentFrame == mode || currentFrame == -1) {
                frameOutput = String.valueOf(i);
            } else {
                hasNonStandardFrames = true;
                int currentFrameTicks;
                if (currentFrame < minDelayTicks) {
                    currentFrameTicks = minDelayTicks;
                } else {
                    currentFrameTicks = currentFrame / 5;
                }
                frameOutput = "{\"index\": " + i +",\"time\":"+ currentFrameTicks +"}";
            }
            frames.add(frameOutput);
        }


        // TODO writer with frametime do when home!!
        if (!mcmetaFile.exists()) { mcmetaFile.createNewFile(); }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(mcmetaFile))) {
            int frametime = mode/5;
            bw.write("{\"animation\": {\"frametime\":" + (frametime != 0 ? frametime : 1) +
                    (hasNonStandardFrames // we only add the array we made if we have different frame delays.
                            ? ",\"frames\": [" + String.join(",", frames) +"]"
                            : ""
                    ) + "}}");
        }

        stream.close();
    }

    protected static void genericTranscodeResize(CLIArgs args, File file, File outFile) throws IOException {
        ImageInputStream s = ImageIO.createImageInputStream(file);
        BufferedImage image = ImageIO.read(s);
        Image scaledImage = image.getScaledInstance(args.keyWidth, args.keyWidth, Image.SCALE_SMOOTH);
        BufferedImage newImage = new BufferedImage(args.keyWidth, args.keyWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        ImageIO.write(newImage, "png", outFile);
    }

    /// https://stackoverflow.com/a/32052016
    public static void zipFiles(File sourceFolder, File zipFile) throws IOException {
        if (!sourceFolder.exists()) { throw new IllegalArgumentException("source doesnt exist"); }
        if (!sourceFolder.isDirectory()) { throw new IllegalArgumentException("source is not a directory :("); }

        if (zipFile.exists()) { zipFile.delete(); }
        zipFile.createNewFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Files.walk(sourceFolder.toPath())
                    .filter(p -> p.toFile().isFile()) // only files
                    .forEach(p -> {
                        ZipEntry ze = new ZipEntry(sourceFolder.toPath().relativize(p).toString());
                        try {
                            out.putNextEntry(ze);
                            Files.copy(p, out);
                            out.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public static void deleteDirectory(File directory) throws IOException {
        Files.walk(directory.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    public static void main(String[] __args) throws ParseException, URISyntaxException, IOException, TranscoderException {
        var args = CLIArgs.fromOpts(__args);
        System.out.println(args);

        Path packJsonDirectory = Paths.get(PackMaker.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent()
                .resolve("minemoji/packs/");

        if (!packJsonDirectory.toFile().exists() && args.createPackInfo) { packJsonDirectory.toFile().mkdirs(); }

        Gson gson = new Gson();
        EmojiSet set;
        File jsonFile = packJsonDirectory.resolve(args.prefix + ".json").toFile();
        if (jsonFile.exists() && args.createPackInfo) {
            try (BufferedReader br = new BufferedReader(new FileReader(jsonFile))) {
                set = gson.fromJson(br, EmojiSet.class);
                set.packVersion += 1;
            }
        } else {
            set = new EmojiSet();
            set.packVersion = 1;
        }
        set.prefix = args.prefix;
        set.url = args.packUrl.toURI();
        set.serveToClient = args.serveToClient;

        System.out.println(set.prefix);

        if (!args.outputDirectory.exists()) { args.outputDirectory.mkdirs(); }
        if (!args.inputDirectory.isDirectory() || !args.outputDirectory.isDirectory()) {
            throw new ParseException("invalid input / output directory");
        }

        var gifReader = ImageIO.getImageReadersByFormatName("gif").next();

        List<String> endings = Arrays.stream(ImageIO.getReaderFileSuffixes()).toList();
        ArrayList<EmojiSet.SpriteMeta> emojis = new ArrayList<>();
        for (File file : args.inputDirectory.listFiles()) {
            String baseString = file.getName().substring(0, file.getName().lastIndexOf('.')).toLowerCase();
            String resourceName = baseString.replace('~', '-');
            String fileEnding = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            File outputFile = texturesFolder(args).resolve(resourceName + ".png").toFile();
            switch (fileEnding) {
                case "svg" -> processSvg(args, file, outputFile);
                case "gif" -> processGif(args, gifReader, file, outputFile, texturesFolder(args).resolve(resourceName + ".png.mcmeta").toFile());
                default -> {
                    if (endings.contains(fileEnding)) {
                        genericTranscodeResize(args, file, outputFile);
                    }
                }
            }

            String spriteName = baseString.replace('.', '-');
            if (args.createPackInfo) {
                emojis.add(new EmojiSet.SpriteMeta(
                        baseString.replace('.', '-'),
                        args.prefix + "/" + resourceName
                ));
            }
        }

        if (args.appendToPack) {
            set.emojis.addAll(emojis);
        } else { set.emojis = emojis; }

        if (args.createPackInfo) {
            if (!jsonFile.exists()) { jsonFile.createNewFile(); }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile, false))) {
                gson.toJson(set, bw);
            }
            Files.copy(jsonFile.toPath(), new File(args.outputDirectory + "/" + args.prefix + ".json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        generateAtlas(args);
        if (!args.appendToPack) {
            generatePackMCMeta(args);
        }

        if (args.createZip) {
            String zipName = args.outputDirectory.getName() + ".zip";
            System.out.println(args.outputDirectory.getParentFile().toPath().resolve(zipName).toFile());
            zipFiles(args.outputDirectory, args.outputDirectory.getParentFile().toPath().resolve(zipName).toFile());
        }

        if (args.deleteDirectory && args.outputDirectory.exists()) {
            deleteDirectory(args.outputDirectory);
        }
    }
}

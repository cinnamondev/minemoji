package com.github.cinnamondev.minemoji.PackMaker;

import com.github.cinnamondev.minemoji.EmojiSet;
import com.google.gson.Gson;
import net.kyori.adventure.key.Key;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


///  half finished resource pack generator
/// java -cp jar com.github.cinnamondev.minemoji.PackMaker.PackMaker (...args)
public class PackMaker {
    private static final Logger LOG = LogManager.getLogger(PackMaker.class);
    private static final Options cliOptions;
    static {
        cliOptions = new Options();

        var inDir = new Option("i", "input-directory",  true, "location containing images + .info files");
        inDir.setRequired(true);
        inDir.setType(File.class);
        cliOptions.addOption(inDir);

        var outDir = new Option("o", "output-directory",  true, "resource pack root");
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

        var packgen = new Option("s", "skip-packgen", false, "skips generation of .json file");
        packgen.setRequired(false);
        cliOptions.addOption(packgen);

        cliOptions.addOption("a", "atlas", true, "atlas key used. not rec to change.");
        cliOptions.addOption("v", "verbose", false, "verbose");
    }
    protected record CLIArgs(File inputDirectory, File outputDirectory, URL packUrl, String prefix, Key atlas, boolean createPackInfo, boolean verbose, int maxVersion) {
        public static CLIArgs fromOpts(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(cliOptions, args);

            return new CLIArgs(
                    commandLine.getParsedOptionValue("input-directory"),
                    commandLine.getParsedOptionValue("output-directory"),
                    commandLine.getParsedOptionValue("pack-url"),
                    commandLine.getParsedOptionValue("prefix"),
                    Key.key(commandLine.getOptionValue("atlas", "paintings")),
                    !commandLine.hasOption("skip-packgen"),
                    commandLine.hasOption("verbose"),
                    commandLine.hasOption("max-version")
                            ? ((Number)commandLine.getParsedOptionValue("max-version")).intValue()
                            : MAX_RP_VERSION
            );
        }
    }

    protected static int MIN_RP_VERSION = 1;
    protected static int MAX_RP_VERSION = 2;
    protected static int DEFAULT_FRAMERATE = 5;

    protected static void generatePackMCMeta(CLIArgs args) throws IOException {
        File file = args.outputDirectory.toPath().resolve("pack.mcmeta").toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        bw.write("{\"pack\": {\"description\": \"Minemoji Emoji Pack\", \"min_format\": 69, \"max_format\":" + args.maxVersion + "}}");
        bw.close();
    }
    protected static Path texturesFolder(CLIArgs args) throws IOException {
        Path basePath = args.outputDirectory.toPath();

        Path path = basePath.resolve("assets/minecraft/textures/" + args.prefix);
        path.toFile().mkdirs();
        return path;
    }
    protected static void generateAtlas(CLIArgs args) throws IOException {
        File file = args.outputDirectory.toPath().resolve("assets/minecraft/atlases/" + args.atlas.value() +".json").toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        bw.write("{\"sources\": [{\"type\": \"directory\", \"source\": \"" + args.prefix + "\", \"prefix\": \"" + args.prefix + "/\"}]}");
        bw.close();
    }

    protected static int getFramerateOrDefault(@Nullable File infoFile) throws IOException {
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
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 32.0f);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 32.0f);
        TranscoderInput input = new TranscoderInput(new FileInputStream(file));

        OutputStream out = new FileOutputStream(outFile);
        TranscoderOutput output = new TranscoderOutput(out);

        t.transcode(input, output);
        out.flush();
        out.close();
    }

    protected static void processGif(CLIArgs args, ImageReader gifReader, File file, File outFile) throws IOException {
        int framerate = getFramerateOrDefault(resolveInfoFile(file));

        ImageInputStream stream = ImageIO.createImageInputStream(file);
        gifReader.setInput(stream);
        int n = gifReader.getNumImages(true);
        BufferedImage output = new BufferedImage(32,32*n, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();
        for (int i = 0; i < n; i++) {
            BufferedImage frame = gifReader.read(i);
            var newFrame = frame.getScaledInstance(32, 32, Image.SCALE_DEFAULT);
            g2d.drawImage(newFrame, 0, 32*i, null);
        }
        g2d.dispose();
        ImageIO.write(output, "png", outFile);
    }

    protected static void genericTranscodeResize(CLIArgs args, File file, File outFile) throws IOException {
        try (ImageInputStream s = ImageIO.createImageInputStream(file)) {
            BufferedImage image = ImageIO.read(s);
            BufferedImage newImage = new BufferedImage(32,32, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            ImageIO.write(newImage, "png", outFile);
        }
    }

    protected static String filenameToUsableSpriteName(String filename) { // low effort
        String base = filename.substring(0, filename.lastIndexOf('.') + 1);
        return base.replace('.', '-');
    }
    public static void main(String[] __args) throws ParseException, URISyntaxException, IOException, TranscoderException {
        var args = CLIArgs.fromOpts(__args);
        if (args.verbose) { LOG.atLevel(Level.ALL); } else { LOG.atLevel(Level.WARN); }
        System.out.println(args);
        LOG.info(args);


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
                set.prefix = args.prefix;
                set.url = args.packUrl.toURI();
            }
        } else {
            set = new EmojiSet();
            set.prefix = args.prefix;
            set.packVersion = 1;
            set.url = args.packUrl.toURI();
        }


        if (!args.outputDirectory.exists()) { args.outputDirectory.mkdirs(); }
        if (!args.inputDirectory.isDirectory() || !args.outputDirectory.isDirectory()) {
            throw new ParseException("invalid input / output directory");
        }

        var gifReader = ImageIO.getImageReadersByFormatName("gif").next();

        List<String> endings = Arrays.stream(ImageIO.getReaderFileSuffixes()).toList();
        ArrayList<EmojiSet.SpriteMeta> emojis = new ArrayList<>();
        for (File file : args.inputDirectory.listFiles()) {
            String baseString = file.getName().substring(0, file.getName().lastIndexOf('.'));
            String fileEnding = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            File outputFile = texturesFolder(args).resolve(baseString + ".png").toFile();
            switch (fileEnding) {
                case "svg" -> processSvg(args, file, outputFile);
                case "gif" -> processGif(args, gifReader, file, outputFile);
                default -> {
                    if (endings.contains(fileEnding)) {
                        genericTranscodeResize(args, file, outputFile);
                    }
                }
            }
            if (args.createPackInfo) {
                emojis.add(new EmojiSet.SpriteMeta(
                        filenameToUsableSpriteName(baseString),
                        filenameToUsableSpriteName(baseString)
                ));
            }
        }

        set.emojis = emojis;
        if (!jsonFile.exists()) { jsonFile.createNewFile(); }
        if (args.createPackInfo) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile, false))) {
                gson.toJson(set, bw);
            }
            Files.copy(jsonFile.toPath(), new File(args.outputDirectory + "/pack.json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        generateAtlas(args);
        generatePackMCMeta(args);
    }
}

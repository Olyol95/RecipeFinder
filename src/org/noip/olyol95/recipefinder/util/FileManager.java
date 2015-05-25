package org.noip.olyol95.recipefinder.util;

import org.noip.olyol95.recipefinder.RecipeFinder;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.logging.Level;

/**
 * Recipe Finder plugin for Bukkit/Spigot
 * Copyright (C) 2015 Oliver Youle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Oliver Youle
 */
public class FileManager {

    public static String PLUGIN_DIR,CONFIG_DIR,LANG_DIR,CONFIG_FILE = "config.txt", DEFAULT_LANG_FILE = "en_US.lang", JAR_LANG_FILE=File.separator+"lang"+File.separator+DEFAULT_LANG_FILE;

    private static String DEFAULT_LANG_URL = "https://raw.githubusercontent.com/Olyol95/RecipeFinder/master/lang/"+DEFAULT_LANG_FILE;

    public static boolean onEnable() {

        try {

            PLUGIN_DIR = Paths.get(RecipeFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent() + File.separator + "RecipeFinder";

            CONFIG_DIR = PLUGIN_DIR + File.separator + "config";
            LANG_DIR = PLUGIN_DIR + File.separator + "lang";

            File configDir = new File(CONFIG_DIR);

            if (!configDir.exists()) {

                configDir.mkdirs();

            }

            File configFile = new File(CONFIG_DIR+File.separator+CONFIG_FILE);

            if (!configFile.exists()) {

                generateDefaultConfig();

            }

            File langDir = new File(LANG_DIR);

            if (!langDir.exists()) {

                langDir.mkdirs();

            }

            File langFile = new File(LANG_DIR+File.separator+ DEFAULT_LANG_FILE);

            checkForNewLangFile();

            if (!langFile.exists()) {

                generateDefaultLang();

            }

            parseLangFromConfig();

        } catch (Exception e) {

            return false;

        }

        return true;

    }

    public static Hashtable<String,String> parseLangToSynonyms() {

        Hashtable<String,String> synonyms = new Hashtable<>();

        File langFile = new File(LANG_DIR+File.separator+RecipeFinder.getPlugin().getLanguage());

        try {

            BufferedReader reader = new BufferedReader(new FileReader(langFile));

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.startsWith("item.") || line.startsWith("tile.")) {

                    String[] split = line.split("=");
                    synonyms.put(split[0].replaceAll("\\.name",""),split[1].toLowerCase());

                }

            }

            reader.close();

        } catch (Exception e) {

            if (RecipeFinder.getPlugin().getLanguage().equals(DEFAULT_LANG_FILE)) {

                RecipeFinder.getPlugin().getLogger().log(Level.SEVERE,"No language file found! Lookup will likely be affected!");

                RecipeFinder.getPlugin().setLanguageEnabled(false);

            } else {

                RecipeFinder.getPlugin().getLogger().log(Level.WARNING, "Language file "+RecipeFinder.getPlugin().getLanguage()+" not found! Defaulting language to " + DEFAULT_LANG_FILE);

                RecipeFinder.getPlugin().setLanguage(DEFAULT_LANG_FILE);

                return parseLangToSynonyms();

            }

        }

        return synonyms;

    }

    private static void generateDefaultConfig() {

        File configFile = new File(CONFIG_DIR+File.separator+CONFIG_FILE);

        RecipeFinder.getPlugin().getLogger().log(Level.INFO,"Generating config file...");

        try {

            if (!configFile.exists()) configFile.createNewFile();

            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(configFile)));

            writer.println("          RecipeFinder Configuration File");
            writer.println("---------------------------------------------------");
            writer.println("languages enabled: true");
            writer.println("language file: "+DEFAULT_LANG_FILE);
            writer.println("---------------------------------------------------");
            writer.println("http://dev.bukkit.org/bukkit-plugins/recipe-finder/");
            writer.println("          Copyright (C) 2015 Oliver Youle          ");

            writer.flush();
            writer.close();

            RecipeFinder.getPlugin().getLogger().log(Level.INFO,"Config file generated successfully!");

        } catch (Exception e) {

            RecipeFinder.getPlugin().getLogger().log(Level.SEVERE,"Failed to generate config file!");

            e.printStackTrace();

        }

    }

    private static void generateDefaultLang() {

        try {

            RecipeFinder.getPlugin().getLogger().log(Level.INFO,"Default lang file could not be downloaded.");
            RecipeFinder.getPlugin().getLogger().log(Level.INFO,"Copying default lang file from jar");

            InputStream stream = RecipeFinder.class.getResourceAsStream(JAR_LANG_FILE);
            File langFile = new File(LANG_DIR+File.separator+DEFAULT_LANG_FILE);

            if (!langFile.exists()) langFile.createNewFile();

            if (stream == null) {

                RecipeFinder.getPlugin().getLogger().log(Level.SEVERE,"Default lang file not found in jar! Please nag Olyol95!");

            } else {

                FileOutputStream fileOutputStream = new FileOutputStream(langFile);

                int readBytes;
                byte[] buffer = new byte[4096];

                while ((readBytes = stream.read(buffer)) > 0) {

                    fileOutputStream.write(buffer, 0, readBytes);

                }

                fileOutputStream.flush();
                fileOutputStream.close();
                stream.close();

            }

        } catch (Exception e1) {

            RecipeFinder.getPlugin().getLogger().log(Level.SEVERE,"Error copying default lang file from jar!");

        }

    }

    private static void checkForNewLangFile() {

        try {

            RecipeFinder.getPlugin().getLogger().log(Level.INFO,"Checking for language update...");

            URL website = new URL(DEFAULT_LANG_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(LANG_DIR+File.separator+"temp.lang");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            rbc.close();
            fos.close();

            File newLangFile = new File(LANG_DIR+File.separator+"temp.lang");
            File langFile = new File(LANG_DIR+File.separator+DEFAULT_LANG_FILE);

            int newVersion = 0;
            int langVersion = 0;

            BufferedReader reader = new BufferedReader(new FileReader(newLangFile));

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.startsWith("recipe.lang.version")) {

                    String[] split = line.split("=");
                    newVersion = Integer.parseInt(split[1].replace(".", ""));

                }

            }

            reader.close();

            if (langFile.exists()) {

                reader = new BufferedReader(new FileReader(langFile));

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("recipe.lang.version")) {

                        String[] split = line.split("=");
                        langVersion = Integer.parseInt(split[1].replace(".", ""));

                    }

                }

                reader.close();

            }

            if (newVersion > langVersion) {

                if (langFile.exists()) {

                    langFile.delete();

                }

                langFile.createNewFile();

                FileInputStream fileInputStream = new FileInputStream(newLangFile);
                FileOutputStream fileOutputStream = new FileOutputStream(langFile);

                int readBytes;
                byte[] buffer = new byte[4096];

                while ((readBytes = fileInputStream.read(buffer)) > 0) {

                    fileOutputStream.write(buffer, 0, readBytes);

                }

                fileOutputStream.flush();
                fileOutputStream.close();
                fileInputStream.close();

            }

            newLangFile.delete();

        } catch (Exception e) {

            RecipeFinder.getPlugin().getLogger().log(Level.WARNING,"Error fetching language update.");

            e.printStackTrace();

        }

    }

    private static void parseLangFromConfig() {

        File configFile = new File(CONFIG_DIR+File.separator+CONFIG_FILE);

        if (configFile.exists()) {

            try {

                BufferedReader reader = new BufferedReader(new FileReader(configFile));

                String line;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("language file:")) {

                        RecipeFinder.getPlugin().setLanguage(line.split(":")[1].trim());

                    } else if (line.startsWith("languages enabled:")) {

                        RecipeFinder.getPlugin().setLanguageEnabled(Boolean.parseBoolean(line.split(":")[1].trim()));

                    }

                }

                reader.close();

            } catch (Exception e) {

                RecipeFinder.getPlugin().getLogger().log(Level.WARNING,"Error parsing config file! Defaulting language to "+DEFAULT_LANG_FILE);

            }

        } else {

            RecipeFinder.getPlugin().getLogger().log(Level.WARNING,"Config file not found! Defaulting language to "+DEFAULT_LANG_FILE);

        }

    }

}
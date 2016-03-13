package org.noip.olyol95.recipefinder.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.noip.olyol95.recipefinder.RecipeFinder;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

/**
 * Recipe Finder plugin for Bukkit/Spigot
 * Copyright (C) 2016 Oliver Youle
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Oliver Youle
 */
public class FileManager {

    public static String LANG_DIR, DEFAULT_LANG_DIR;
    public static final String MANIFEST_FILE_NAME = "manifest.yml";
    private static final String REMOTE_DIR = "https://raw.githubusercontent.com/Olyol95/RecipeFinder/development";
    private static final String MANIFEST_URL = REMOTE_DIR + "/lang/" + MANIFEST_FILE_NAME;

    public static boolean onEnable() {

        try {

            RecipeFinder.getPlugin().saveDefaultConfig();

            LANG_DIR = RecipeFinder.getPlugin().getDataFolder().getPath() + File.separator + "lang";
            DEFAULT_LANG_DIR = LANG_DIR + File.separator + "default";

            File defaultLangDir = new File(DEFAULT_LANG_DIR);

            if (!defaultLangDir.exists()) {

                if (!defaultLangDir.mkdirs()) {

                    RecipeFinder.getPlugin().getLogger().severe("Could not create the required directory: " +
                            DEFAULT_LANG_DIR);
                    return false;

                }

            }

            if (!synchroniseLangDirectory(false)) return false;

            RecipeFinder.getPlugin().setLanguageEnabled(
                    RecipeFinder.getPlugin().getConfig().getBoolean("languages-enabled")
            );
            RecipeFinder.getPlugin().setServerLocale(
                    RecipeFinder.getPlugin().getConfig().getString("server-locale")
            );

        } catch (Exception e) {

            RecipeFinder.getPlugin().getLogger().severe(e.getLocalizedMessage());
            return false;

        }

        return true;

    }

    public static boolean synchroniseLangDirectory(boolean fromJar) {

        File installedManifest = new File(LANG_DIR + File.separator + MANIFEST_FILE_NAME);
        YamlConfiguration sourceConfiguration;
        YamlConfiguration installedConfiguration;

        InputStream sourceManifestStream;

        double installedVersion = 0, sourceVersion;

        if (fromJar) {

            RecipeFinder.getPlugin().getLogger().info("Copying default lang files from jar");

            sourceManifestStream = RecipeFinder.class.getResourceAsStream(
                    File.separator + "lang" + File.separator + MANIFEST_FILE_NAME
            );

        } else {

            RecipeFinder.getPlugin().getLogger().info("Checking for a language update...");

            try {
                sourceManifestStream = new URL(MANIFEST_URL).openStream();
            } catch (Exception e) {
                RecipeFinder.getPlugin().getLogger().warning(
                        "failed to fetch remote manifest! " + e.getLocalizedMessage()
                );
                return synchroniseLangDirectory(true);
            }

        }

        if (installedManifest.exists()) {
            try {
                installedConfiguration = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(new FileInputStream(installedManifest))
                );
                installedVersion = installedConfiguration.getDouble("version");
            } catch (Exception e) {
                RecipeFinder.getPlugin().getLogger().severe(
                        "Failed to read from local manifest! " + e.getLocalizedMessage()
                );
                return false;
            }
        }

        sourceConfiguration = YamlConfiguration.loadConfiguration(
                new InputStreamReader(sourceManifestStream)
        );
        sourceVersion = sourceConfiguration.getDouble("version");

        boolean hardUpdate = sourceVersion > installedVersion;

        if (hardUpdate) {
            try {
                sourceConfiguration.save(installedManifest);
            } catch (Exception e) {
                RecipeFinder.getPlugin().getLogger().severe("Error installing manifest! " + e.getLocalizedMessage());
                return false;
            }
            RecipeFinder.getPlugin().getLogger().info("Clearing lang directory...");
            File defaultLangDir = new File(DEFAULT_LANG_DIR);
            for (File file : defaultLangDir.listFiles()) {
                file.delete();
            }
        }

        installedConfiguration = YamlConfiguration.loadConfiguration(installedManifest);
        List<String> languageFiles = installedConfiguration.getStringList("files");

        for (String langFileName : languageFiles) {

            File installedLangFile = new File(DEFAULT_LANG_DIR + File.separator + langFileName);

            if (!installedLangFile.exists()) {

                InputStream sourceStream;

                if (fromJar) {

                    RecipeFinder.getPlugin().getLogger().info("Copying " + langFileName + " from jar...");
                    sourceStream = RecipeFinder.class.getResourceAsStream(
                            File.separator + "lang" + File.separator + langFileName
                    );

                } else {

                    RecipeFinder.getPlugin().getLogger().info("Downloading " + langFileName + "...");
                    try {
                        sourceStream = new URL(REMOTE_DIR + "/lang/" + langFileName).openStream();
                    } catch (Exception e) {
                        RecipeFinder.getPlugin().getLogger().warning(
                                "Error fetching file " + langFileName + ": " + e.getLocalizedMessage()
                        );
                        return synchroniseLangDirectory(true);
                    }

                }

                try {
                    Files.copy(sourceStream, installedLangFile.toPath());
                } catch (Exception e) {
                    RecipeFinder.getPlugin().getLogger().severe(
                            "Error writing file " + langFileName + ": " + e.getLocalizedMessage()
                    );
                    return false;
                }

            }

        }

        return true;

    }

    public static Hashtable<String, Hashtable<String, List<String>>> loadTranslations() {

        Hashtable<String, Hashtable<String, List<String>>> translations =
                new Hashtable<String, Hashtable<String, List<String>>>();

        File manifest = new File(LANG_DIR + File.separator + MANIFEST_FILE_NAME);

        if (manifest.exists()) {

            YamlConfiguration localConfiguration;

            try {
                localConfiguration = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(new FileInputStream(manifest))
                );
            } catch (Exception e) {
                RecipeFinder.getPlugin().getLogger().severe(
                        "Error loading translations, could not read from manifest: " + e.getLocalizedMessage()
                );
                return translations;
            }

            List<String> defaultLangFiles = localConfiguration.getStringList("files");

            for (String langFileName: defaultLangFiles) {

                Hashtable<String, List<String>> langTranslations = parseTranslationsFromFile(
                        new File(DEFAULT_LANG_DIR + File.separator + langFileName)
                );
                if (langTranslations != null) translations.put(langFileName.substring(0, langFileName.length()-5), langTranslations);

            }

        }

        List<String> userLangFiles = RecipeFinder.getPlugin().getConfig().getStringList("language-files");

        for (String langFileName: userLangFiles) {

            Hashtable<String, List<String>> langTranslations = parseTranslationsFromFile(
                    new File(LANG_DIR + File.separator + langFileName)
            );
            if (langTranslations != null) translations.put(langFileName.substring(0, langFileName.length()-5), langTranslations);

        }

        return translations;

    }

    private static Hashtable<String, List<String>> parseTranslationsFromFile(File inputFile) {

        try {

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            Hashtable<String, List<String>> translations = new Hashtable<String, List<String>>();

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.startsWith("item.") || line.startsWith("tile.")) {

                    String[] split = line.split("=");

                    if (split.length == 2) {
                        String itemKey = split[0].replaceAll("\\.name", "");
                        String itemValue = split[1].toLowerCase();

                        if (!translations.containsKey(itemKey)) translations.put(itemKey, new ArrayList<String>());

                        translations.get(itemKey).add(itemValue);
                    }

                }

            }

            reader.close();

            return translations;

        } catch (Exception e) {
            RecipeFinder.getPlugin().getLogger().warning(
                    "Error loading translations for language file " + inputFile.getPath() + ": " +
                            e.getLocalizedMessage()
            );
            e.printStackTrace();
            return null;
        }

    }

    public static Hashtable<UUID, List<String>> loadPlayerLanguages() {

        Hashtable<UUID, List<String>> playerLanguages = new Hashtable<UUID, List<String>>();

        return playerLanguages;

    }

}

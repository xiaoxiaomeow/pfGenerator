package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Translate {
    private static final String[] cats = new String[]{
            "source",
            "school",
            "subSchools",
            "descriptors",
            "castingTime",
            "range",
            "area",
            "targetOrArea",
            "duration",
            "savingThrow",
            "spellResistance",
            "mythicSource"
    };
    private static final Map<String, SortedSet<String>> tags = new HashMap();
    private static void register (String cat, String tag) {
        if (tag == null) {
            return;
        }
        if (!tags.containsKey(cat)) {
            tags.put(cat, new TreeSet<>());
        }
        tags.get(cat).add(tag);
    }
    private static void registerAll (String cat, JSONObject spell) {
        Object object = spell.get(cat);
        if (object == null) {
            return;
        }
        if (object instanceof String) {
            register(cat, object.toString());
        }
        if (object instanceof JSONArray) {
            for (Object child : (JSONArray) object) {
                if (cat.toLowerCase().contains("source")) {
                    register(cat, child.toString().split(" pg\\.")[0]);
                } else {
                    register(cat, child.toString());
                }
            }
        }
    }
    public static void exportTags () throws IOException {
        File source = new File("spells/json");
        File target = new File("tags.json");

        for (File spellFile : source.listFiles()) {
            JSONObject spell = JSONObject.fromObject(Tools.readFile(spellFile));

            for (String cat : cats) {
                registerAll(cat, spell);
            }

            JSONObject levels = (JSONObject) spell.get("levels");
            if (levels != null) {
                for (Object key : levels.keySet()) {
                    register("clazz", key.toString());
                }
            }
        }

        JSONObject tagsObject = new JSONObject();
        for (Map.Entry<String, SortedSet<String>> entry : tags.entrySet()) {
            JSONObject catTags = new JSONObject();
            for (String tag : entry.getValue()) {
                catTags.put(tag, tag);
            }
            tagsObject.put(entry.getKey() + "Translations", catTags);
        }

        Tools.writeFile(target, tagsObject.toString(4));
    }

    private static JSONObject translations;
    private static String translateItem (String cat, String zh, JSONObject spell) {
        String en = (String) spell.get(cat);
        if (en == null) {
            if (zh == null) {
                return null;
            } else {
                // System.err.println(cat + "_zh but not en found in spell " + spell.getString("key"));
                return zh;
            }
        } else {
            JSONObject dict = translations.getJSONObject(cat + "Translations");
            if (zh == null) {
                // System.err.println(cat + "_en but not zh found in spell " + spell.getString("key"));
                if (dict.containsKey(en)) {
                    if (dict.getString(en).isEmpty()) {
                        return en;
                    } else{
                        return dict.getString(en);
                    }
                } else {
                    dict.put(en, "");
                    return en;
                }
            } else {
                if (dict.containsKey(en)) {
                    return dict.getString(en);
                } else {
                    dict.put(en, zh);
                    return zh;
                }

            }
        }
    }
    private static void translateSpell (JSONObject spell, String chmText) {
        spell.put("name_zh", Tools.reg(chmText, "^(.*?)( \\(|（)"));

        spell.put("race_zh", translateItem("race", Tools.reg(chmText, "^[^\\n]*? \\[(.*?)\\]"), spell));
        spell.put("castingTime_zh", translateItem("castingTime", Tools.reg(chmText, "\n施法时间\\s*(\\S.*?)\n"), spell));
        spell.put("components_zh", Tools.reg(chmText, "\n成分\\s*(\\S.*?)\n"));
        spell.put("range_zh", translateItem("range", Tools.reg(chmText, "\n距离\\s*(\\S.*?)\n"), spell));
        spell.put("effect_zh", Tools.reg(chmText, "\n效果\\s*(\\S.*?)\n"));
        spell.put("targetOrArea_zh", translateItem("targetOrArea", Tools.reg(chmText, "\n目标或区域\\s*(\\S.*?)\n"), spell));
        if (spell.get("targetOrArea_zh") == null) {
            spell.put("target_zh", Tools.reg(chmText, "\n目标\\s*(\\S.*?)\n"));
            spell.put("area_zh", translateItem("area", Tools.reg(chmText, "\n区域\\s*(\\S.*?)\n"), spell));
        }
        spell.put("duration_zh", translateItem("duration", Tools.reg(chmText, "\n持续时间\\s*(\\S.*?)\n"), spell));
        spell.put("savingThrow_zh", translateItem("savingThrow", Tools.reg(chmText, "\n豁免\\s*(\\S.*?)\n"), spell));
        spell.put("spellResistance_zh", translateItem("spellResistance", Tools.reg(chmText, "\n法术抗力\\s*(\\S.*?)\n"), spell));

        spell.put("text_zh", htmlText(chmText));
    }

    private static String htmlText(String chmText) {
        int index = chmText.lastIndexOf("       ");
        if (index == -1) return null;
        index = chmText.indexOf("\n", index);
        if (index == -1) return null;
        chmText = chmText.substring(index);

        return analyzeText(chmText);
    }

    private static String analyzeText (String chmText) {
        chmText = chmText.replaceAll("‘([^‘’]*?) \\(([^‘’]*?)\\)’", "‘<a href=\"spell.html?spell=$2\">$1 \\($2\\)</a>’");
        StringBuilder text = new StringBuilder();
        text.append("<p>");
        boolean newParagraph = true;
        for (String paragraph : chmText.split("[\\s|　]*\n[\\s|　]*")) {
            if (paragraph.startsWith("<div") || paragraph.startsWith("<table") || paragraph.startsWith("<tr")) {
                text.append(paragraph);
            } else if (paragraph.startsWith("<b>")) {
                if (newParagraph) {
                    text.append("</p><p>" + paragraph);
                    newParagraph = false;
                } else {
                    text.append("<br>" + paragraph);
                }
            } else {
                text.append("</p><p>" + paragraph);
                newParagraph = true;
            }
        }
        text.append("</p>");
        return text.toString().replaceAll("<p><\\/p>", "");
    }

    public static void translateSpells () throws IOException {
        File ref = new File("spells/json");
        File source = new File("spells/chm");
        File target = new File("spells/translated");
        File translationFile = new File("translations.json");
        Tools.clearPath(target);
        target.mkdirs();

        // load Translations
        translations = JSONObject.fromObject(Tools.readFile(translationFile));

        File[] files = source.listFiles();
        ArrayList<File> sort = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().contains("-")) {
                sort.add(file);
            }
        }
        for (File file : files) {
            if (file.getName().contains("-")) {
                sort.add(file);
            }
        }
        for (File chmFile : sort) {
            String content = Tools.readFile(chmFile).trim();
            String[] chmTexts = content.split("\n(?=[^\n\\(\\)（）]*? *[\\(（][^\n]*[\\)）] *(|\\[[^\\[\\]\n]*\\])\n学派)");

            for (String chmText : chmTexts) {
                chmText = chmText.trim();
                String key = Tools.reg(chmText, "^[^\n\\(\\)（）]* *[\\(（]([^\n]*)[\\)）] *(?:|\\[[^\\[\\]\n]*\\])\n学派");
                if (key == null || key.isEmpty()) {
                    System.out.println("Can't find key in spell: ");
                    System.out.println(chmText);
                    System.exit(0);
                }
                System.out.println("Translating spell " + key);
                key = key.toLowerCase().replaceAll("/", "-");
                File spellFile = new File(ref, key + ".json");
                if (!spellFile.exists()) {
                    key = key.replaceAll(" i$", " 1")
                            .replaceAll(" ii$", " 2")
                            .replaceAll(" iii$", " 3")
                            .replaceAll(" iv$", " 4")
                            .replaceAll(" v$", " 5")
                            .replaceAll(" vi$", " 6")
                            .replaceAll(" vii$", " 7")
                            .replaceAll(" viii$", " 8")
                            .replaceAll(" ix$", " 9");
                    spellFile = new File(ref, key + ".json");
                }
                if (!spellFile.exists()) {
                    System.out.println("Can't find spell " + key);
                    System.exit(0);
                }
                JSONObject spell = JSONObject.fromObject(Tools.readFile(spellFile));
                try {
                    translateSpell(spell, chmText);
                } catch (Exception e) {
                    System.out.println(chmText);
                    StringSelection stringSelection = new StringSelection(chmText);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
                    e.printStackTrace();
                    System.exit(0);
                }
                File spellTarget = new File(target, key + ".json");
                if (spellTarget.exists()) {
                    System.err.println("Duplicate Spell " + key);
                } else {
                    Tools.writeFile(spellTarget, spell.toString(4));
                }
            }
        }

        // save Translations
        Tools.writeFile(translationFile, translations.toString(4));
    }

    public static void copyRawSpells () throws IOException {
        File ref = new File("spells/json");
        File target = new File("spells/translated");

        for (File spellFile : ref.listFiles()) {
            JSONObject spell = JSONObject.fromObject(Tools.readFile(spellFile));
            String key = spell.getString("key");
            File spellTarget = new File(target, key + ".json");
            if (!spellTarget.exists()) {
                translateSpell(spell, "");
                Tools.writeFile(spellTarget, spell.toString(4));
            }
        }
    }

    public static void mythicSpells () throws IOException {
        File source = new File("spells/mythic.txt");
        File ref = new File("spells/translated");

        String content = Tools.readFile(source);
        String[] spells = content.split("\n(?=[^\n\\(\\)（）]*? *[\\(（][^\n]*[\\)）] *(|\\[[^\\[\\]\n]*\\])\n来源：)");
        for (String chmText : spells) {
            chmText = chmText.trim();
            if (chmText.isEmpty()) continue;
            String key = Tools.reg(chmText, "^[^\n\\(\\)（）]* *[\\(（]([^\n]*)[\\)）] *(?:|\\[[^\\[\\]\n]*\\])\n来源：");
            int index = chmText.indexOf("来源：");
            index = chmText.indexOf("\n", index);
            String mythicText = chmText.substring(index + 1).trim();

            mythicText = analyzeText(mythicText.trim());

            File spellFile = new File(ref, key.replaceAll("/", "-") + ".json");
            if (!spellFile.exists()) {
                System.out.println("Can't find spell " + key);
                System.exit(0);
            }
            JSONObject spell = JSONObject.fromObject(Tools.readFile(spellFile));
            spell.put("mythicText_zh", mythicText);
            Tools.writeFile(spellFile, spell.toString(4));
        }
    }


    public static void main(String[] args) throws IOException {
        // exportTags();
        translateSpells();
        copyRawSpells();

        mythicSpells();

        Deploy.main(null);
    }
}

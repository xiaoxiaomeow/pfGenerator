package com.bluebear;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class Deploy {


    public static void generateIndex () throws IOException {
        File source = new File("spells/translated");
        File target = new File("../../pathfinder/spellsIndex.js");

        JSONArray spells = new JSONArray();
        for (File spellFile : source.listFiles()) {
            JSONObject spell = JSONObject.fromObject(Tools.readFile(spellFile));
            JSONObject index = new JSONObject();

            String[] keys = new String[] {"key", "name", "name_zh", "school", "subSchools", "subSchoolsOperator", "descriptors", "levels", "descriptorsOperator",
                    "source", "race", "castingTime_zh", "duration_zh", "range_zh", "spellResistance_zh", "savingThrow_zh"};
            for (String key : keys) {
                index.put(key, spell.get(key));
            }

            if (spell.containsKey("mythicText")) {
                index.put("mythic", true);
            }

            spells.add(index);
        }
        Tools.writeFile(target, "var spellsIndex = " + spells.toString(4));
    }

    public static void copyFiles () throws IOException {
        File source = new File("spells/translated");
        File target = new File("../../pathfinder/spells");
        Tools.clearPath(target);
        target.mkdirs();

        for (File file : source.listFiles()) {
            Tools.writeFile(new File(target, file.getName()), Tools.readFile(file));
        }
    }

    public static void main(String[] args) throws IOException {
        generateIndex();
        copyFiles();
    }
}

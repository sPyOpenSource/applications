package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.junit.Test;
import org.json.JSONObject;

/*
 * Copyright (C) 2026 xuyi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 * @author xuyi
 */
public class ClassTest {
    @Test
    public void y2j(){
        try {
            String code = Files.readString(Paths.get("ai/NormalAI.yaml"));
            code = code.replace("\r", "");
            code = code.replace("\n", ",]},");
            code = code.replace("]},    ", "],");
            code = code.replace(": ", ":[");
            code = code.replace(":,],", ":{");
            code = code.replace(",],", "],");
            code = code.replace(",,", ",");
            code = code.replace("{,", "{");
            code = code.replaceFirst(",]},", "],");
            code = code.replace(",]", "]");
            code = "{"+code;
            //code = code.replace("],}", "]}");
            System.out.println(code);
            JSONObject object = new JSONObject(code);
            System.out.println(object.toString(4));
        } catch (IOException ex) {
            System.getLogger(ClassTest.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        //new JSON();
    }
}

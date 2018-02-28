package com.jsoniter;

import java.util.HashMap;

public class GlobalData {
    public static HashMap<String, MethodData> dictionary;
    static {
        dictionary = new HashMap<String, MethodData>();

        MethodData methodData10 =  new MethodData(19);
        dictionary.put("IterImplSkip - skip", methodData10);

        MethodData methodData9 =  new MethodData(26);
        dictionary.put("Codegen - chooseImpl", methodData9);

        MethodData methodData8 =  new MethodData(18);
        dictionary.put("Config - updateBindings", methodData8);

        MethodData methodData7 =  new MethodData(24);
        dictionary.put("IterImplForStreaming - readNumber", methodData7);

        MethodData methodData6 =  new MethodData(40);
        dictionary.put("IterImpl - readStringSlowPath", methodData6);
    }
}
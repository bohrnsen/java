package com.jsoniter;

import java.util.HashMap;

public class GlobalData {
    public static HashMap<String, MethodData> dictionary;
    static {
        dictionary = new HashMap<String, MethodData>();

        MethodData methodData =  new MethodData(40);
        dictionary.put("IterImpl - readStringSlowPath", methodData);

        MethodData methodData2 =  new MethodData(43);
        dictionary.put("IterImplForStreaming - readStringSlowPath", methodData2);

        MethodData methodData3 =  new MethodData(29);
        dictionary.put("CodegenImplObjectStrict - genObjectUsingStrict", methodData3);

        MethodData methodData4 =  new MethodData(31);
        dictionary.put("extra/GsonCompatibilityMode - createDecoder", methodData4);

        MethodData methodData5 =  new MethodData(42);
        dictionary.put("CodegenImplNative - genReadOp", methodData5);

        MethodData methodData6 =  new MethodData(20);
        dictionary.put("OmitValue - parse", methodData6);
    }
}
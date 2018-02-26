package com.jsoniter.suite;

import com.jsoniter.*;
import com.jsoniter.TestFloat;
import com.jsoniter.TestGenerics;
import com.jsoniter.TestGson;
import com.jsoniter.TestNested;
import com.jsoniter.TestObject;
import com.jsoniter.TestString;
import com.jsoniter.any.TestList;
import com.jsoniter.output.*;
import com.jsoniter.output.TestInteger;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static com.jsoniter.GlobalData.dictionary;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        com.jsoniter.TestAnnotationJsonIgnore.class,
        com.jsoniter.output.TestAnnotationJsonIgnore.class,
        com.jsoniter.TestAnnotationJsonProperty.class,
        com.jsoniter.output.TestAnnotationJsonProperty.class,
        TestAnnotationJsonWrapper.class,
        TestAnnotationJsonUnwrapper.class,
        TestAnnotation.class,
        com.jsoniter.output.TestGenerics.class,
        TestCustomizeType.class, TestDemo.class,
        TestExisting.class, TestGenerics.class, TestGenerics.class, TestIO.class,
        TestNested.class,
        com.jsoniter.output.TestNested.class,
        TestObject.class,
        com.jsoniter.output.TestObject.class,
        TestReadAny.class, TestSkip.class, TestSlice.class,
        TestString.class,
        com.jsoniter.output.TestString.class,
        TestWhatIsNext.class,
        TestAny.class,
        com.jsoniter.output.TestArray.class,
        com.jsoniter.any.TestArray.class,
        com.jsoniter.TestArray.class,
        TestSpiPropertyEncoder.class,
        com.jsoniter.TestMap.class,
        com.jsoniter.output.TestMap.class,
        TestNative.class,
        TestBoolean.class, TestFloat.class, com.jsoniter.output.TestFloat.class,
        TestList.class, TestInteger.class, com.jsoniter.output.TestInteger.class,
        com.jsoniter.output.TestJackson.class,
        com.jsoniter.TestJackson.class,
        TestSpiTypeEncoder.class,
        TestSpiTypeDecoder.class,
        TestSpiPropertyDecoder.class,
        TestGson.class,
        com.jsoniter.output.TestGson.class,
        TestStreamBuffer.class,
        TestCollection.class,
        TestList.class,
        TestAnnotationJsonObject.class})

public abstract class AllTestCases {
    @AfterClass
    public static void tearDown() {
        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("AD-HOC BRANCH COVERAGE");
        System.out.println("------------------------------------------------------------------------");
        System.out.println();

        NumberFormat formatter = new DecimalFormat("#00.00");

        for (String key: dictionary.keySet()) {
            MethodData methodData = dictionary.get(key);
            System.out.println(key + " (" + formatter.format(methodData.getCoverage() * 100) + "%): " + methodData);
        }

        System.out.println();
        System.out.println("------------------------------------------------------------------------");
        System.out.println();
    }
}


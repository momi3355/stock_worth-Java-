package com.momi3355.stockworth;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.junit.Before;
import org.junit.Test;

public class PythonTest {
    private Python py;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(appContext));
        }
        py = Python.getInstance();
    }

    @Test
    public void testAdd() {
        // 파이썬 함수 호출
        String temp = py.getModule("hello").callAttr("say_hello").toString();

        // 결과 확인
        assertEquals(temp, "Hello from Python!");
    }
}

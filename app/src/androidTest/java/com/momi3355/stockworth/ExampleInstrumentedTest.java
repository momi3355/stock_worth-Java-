package com.momi3355.stockworth;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private Python py;
    private Context appContext;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(appContext));
        }
        py = Python.getInstance();
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        assertEquals("com.momi3355.stockworth", appContext.getPackageName());
    }
}
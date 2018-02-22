/*
 * Copyright 2018 Jaroslav Groman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.jtek.popularmovies.utilities;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class MockDataUtils {

    private static final String TAG = MockDataUtils.class.getSimpleName();

    private static String readFile(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        int readLen;

        byte[] buffer = new byte[1024];

        try {
            while ((readLen = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, readLen));
            }
            inputStream.close();
        } catch (IOException ex) {
            Log.e(TAG, "IOException reading file.");
            return "";
        }

        return sb.toString();
    }

    public static String getMockJson(Context context, String fileName) {
        InputStream inputStream = context.getResources().openRawResource(
                context.getResources().getIdentifier(fileName, "raw", context.getPackageName())
        );

        return readFile(inputStream);
    }
}

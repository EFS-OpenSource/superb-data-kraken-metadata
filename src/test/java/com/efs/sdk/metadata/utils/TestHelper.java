/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.efs.sdk.metadata.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

public class TestHelper {

    public static final String RESULT_PATH = "results";

    public static String getInputContent(String prefix, String filepath) throws IOException {
        return IOUtils.toString(getInputStream(format("/%s/%s", prefix, filepath)), StandardCharsets.UTF_8);
    }

    private static InputStream getInputStream(String filePath) throws IOException {
        InputStream in = TestHelper.class.getResourceAsStream(filePath);
        if (in == null) {
            throw new IOException(format("Could not load file '%s'", filePath));
        }
        return in;
    }


    public static Integer findRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

}

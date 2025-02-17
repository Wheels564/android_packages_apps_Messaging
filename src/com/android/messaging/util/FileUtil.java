/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import com.android.messaging.Factory;
import com.android.messaging.R;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {
    /** Returns a new file name, ensuring that such a file does not already exist. */
    private static synchronized File getNewFile(File directory, String extension,
            String fileNameFormat) throws IOException {
        final Date date = new Date(System.currentTimeMillis());
        final SimpleDateFormat dateFormat = new SimpleDateFormat(fileNameFormat,
                Locale.getDefault(Locale.Category.FORMAT));
        final String numberedFileNameFormat = dateFormat.format(date) + "_%02d" + "." + extension;
        for (int i = 1; i <= 99; i++) { // Only save 99 of the same file name.
            final String newName = String.format(Locale.US, numberedFileNameFormat, i);
            File testFile = new File(directory, newName);
            if (!testFile.exists()) {
                testFile.createNewFile();
                return testFile;
            }
        }
        LogUtil.e(LogUtil.BUGLE_TAG, "Too many duplicate file names: " + numberedFileNameFormat);
        return null;
    }

    /**
     * Creates an unused name to use for creating a new file. The format happens to be similar
     * to that used by the Android camera application.
     *
     * @param directory directory that the file should be saved to
     * @param contentType of the media being saved
     * @return file name to be used for creating the new file. The caller is responsible for
     *   actually creating the file.
     */
    public static File getNewFile(File directory, String contentType) throws IOException {
        String fileExtension = ContentType.getExtensionFromMimeType(contentType);
        final Context context = Factory.get().getApplicationContext();
        String fileNameFormat = context.getString(ContentType.isImageType(contentType)
                ? R.string.new_image_file_name_format : R.string.new_file_name_format);
        return getNewFile(directory, fileExtension, fileNameFormat);
    }

    // Checks if the file is in /data, and don't allow any app to send personal information.
    // We're told it's possible to create world readable hardlinks to other apps private data
    // so we ban all /data file uris.
    public static boolean isInPrivateDir(Uri uri) {
        return isFileUriInPrivateDir(uri) || isContentUriInPrivateDir(uri);
    }

    private static boolean isFileUriInPrivateDir(Uri uri) {
        if (!UriUtil.isFileUri(uri)) {
            return false;
        }
        final File file = new File(uri.getPath());
        return FileUtil.isSameOrSubDirectory(Environment.getDataDirectory(), file);
    }

    private static boolean isContentUriInPrivateDir(Uri uri) {
        if (!uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            return false;
        }
        try {
            Context context = Factory.get().getApplicationContext();
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            int fd = pfd.getFd();
            // Use the file descriptor to find out the read file path through symbolic link.
            Path fdPath = Paths.get("/proc/self/fd/" + fd);
            Path filePath = java.nio.file.Files.readSymbolicLink(fdPath);
            pfd.close();
            return FileUtil.isSameOrSubDirectory(Environment.getDataDirectory(), filePath.toFile());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks, whether the child directory is the same as, or a sub-directory of the base
     * directory.
     */
    private static boolean isSameOrSubDirectory(File base, File child) {
        try {
            base = base.getCanonicalFile();
            child = child.getCanonicalFile();
            File parentFile = child;
            while (parentFile != null) {
                if (base.equals(parentFile)) {
                    return true;
                }
                parentFile = parentFile.getParentFile();
            }
            return false;
        } catch (IOException ex) {
            LogUtil.e(LogUtil.BUGLE_TAG, "Error while accessing file", ex);
            return false;
        }
    }
}

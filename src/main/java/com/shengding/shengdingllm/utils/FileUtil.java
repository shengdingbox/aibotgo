package com.shengding.shengdingllm.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * 文件操作工具类deleteFiles
 *
 * @version 1.0
 * @remark 2019年7月16日
 * @since 1.0
 */
@Slf4j
public class FileUtil {
    /**
     * 删除目录，返回删除的文件数
     *
     * @param rootPath 待删除的目录
     * @param fileNum  已删除的文件个数
     * @return 已删除的文件个数
     */
    public static int deleteFiles(String rootPath, int fileNum) {
        File file = new File(rootPath);
        if (!file.exists()) {
            return -1;
        }
        if (file.isDirectory()) {
            File[] sonFiles = file.listFiles();
            if (sonFiles != null && sonFiles.length > 0) {
                for (File sonFile : sonFiles) {
                    if (sonFile.isDirectory()) {
                        fileNum = deleteFiles(sonFile.getAbsolutePath(), fileNum);
                    } else {
                        sonFile.delete();
                        fileNum++;
                    }
                }
            }
        } else {
            file.delete();
        }
        return fileNum;
    }
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException var4) {
                    log.info("Ignore failure in closing the Closeable", var4);
                }
            }
        }

    }
    private static OutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file);
    }
    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            byte[] b = new byte[4096];
            boolean var3 = false;

            int n;
            while((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } finally {
            output.close();
        }
    }
}

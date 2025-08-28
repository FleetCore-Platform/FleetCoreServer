package io.levysworks.Utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MissionZipBuilder implements Closeable {
    private final ZipOutputStream zipStream;
    private final File archive;

    public MissionZipBuilder(String missionUUID) throws IOException {
        String fileName = "mission_" + missionUUID;
        String suffix = ".bundle.zip";

        this.archive = File.createTempFile(fileName, suffix);
        archive.deleteOnExit();
        this.zipStream = new ZipOutputStream(new FileOutputStream(archive));
    }

    public MissionZipBuilder mission(String thingName, InputStream missionInputStream) throws IOException {
        ZipEntry zipEntry = new ZipEntry(thingName);
        this.zipStream.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while((length = missionInputStream.read(bytes)) >= 0) {
            this.zipStream.write(bytes, 0, length);
        }

        return this;
    }

    public File build() throws IOException {
        this.zipStream.close();
        return archive;
    }

    @Override
    public void close() {
        try {
            if (zipStream != null) {
                zipStream.close();
            }
        } catch (IOException ignored) {
        }
        if (archive != null && archive.exists()) {
            archive.delete();
        }
    }
}

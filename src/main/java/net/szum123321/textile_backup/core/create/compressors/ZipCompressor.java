/*
 * A simple backup mod for Fabric
 * Copyright (C) 2020  Szum123321
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.szum123321.textile_backup.core.create.compressors;

import net.szum123321.textile_backup.config.ConfigHelper;
import net.szum123321.textile_backup.core.Utilities;
import net.szum123321.textile_backup.core.create.BackupContext;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ZipCompressor extends AbstractCompressor {
    private final static ConfigHelper config = ConfigHelper.INSTANCE;

    public static ZipCompressor getInstance() {
        return new ZipCompressor();
    }

    @Override
    protected OutputStream createArchiveOutputStream(OutputStream stream, BackupContext ctx, int coreLimit) {
        ZipArchiveOutputStream arc =  new ZipArchiveOutputStream(stream);

        arc.setMethod(ZipArchiveOutputStream.DEFLATED);
        arc.setUseZip64(Zip64Mode.AsNeeded);
        arc.setLevel(config.get().compression);
        arc.setComment("Created on: " + Utilities.getDateTimeFormatter().format(LocalDateTime.now()));

        return arc;
    }

    @Override
    protected void addEntry(File file, String entryName, OutputStream arc) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)){
            ZipArchiveEntry entry = (ZipArchiveEntry)((ZipArchiveOutputStream)arc).createArchiveEntry(file, entryName);

            if(isDotDat(file.getName())) {
                entry.setMethod(ZipArchiveOutputStream.STORED);
                entry.setSize(file.length());
                entry.setCompressedSize(file.length());
                entry.setCrc(getCRC(file));
            }

            ((ZipArchiveOutputStream)arc).putArchiveEntry(entry);

            IOUtils.copy(fileInputStream, arc);

            ((ZipArchiveOutputStream)arc).closeArchiveEntry();
        }
    }

    //*.dat files are already compressed with gzip which uses the same algorithm as zip so there's no point in compressing it again
    protected static boolean isDotDat(String filename) {
        String[] arr = filename.split("\\.");
        return arr[arr.length - 1].contains("dat"); //includes dat_old
    }

    protected static long getCRC(File file) throws IOException {
        Checksum sum = new CRC32();
        byte[] buffer = new byte[8192];
        int len;

        try (InputStream stream = new FileInputStream(file)) {
            while ((len = stream.read(buffer)) != -1) sum.update(buffer, 0, len);
        } catch (IOException e) {
            throw new IOException("Error while calculating CRC of: " + file.getAbsolutePath(), e);
        }

        return sum.getValue();
    }
}

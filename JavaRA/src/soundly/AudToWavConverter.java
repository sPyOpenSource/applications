/*
 * Copyright (C) 2026 xuyi
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package soundly;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.Arrays;

public class AudToWavConverter {
    
    // Westwood AUD header (simplified)
    private static class AudHeader {
        int sampleRate;      // Usually 22050
        short channels;      // 1 = mono
        short bitsPerSample; // 8 (ADPCM) or 16 (PCM)
        int dataSize;
        boolean isAdpcm;
    }
    
    public static void convertAudToWav(Path audFile, Path wavFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(audFile.toFile(), "r")) {
            // Read AUD header (Westwood format)
            byte[] sig = new byte[4];
            raf.readFully(sig);
            if (!Arrays.equals(sig, new byte[]{'A','U','D','\0'})) {
                throw new IOException("Not a valid AUD file: " + audFile);
            }
            
            AudHeader header = new AudHeader();
            header.sampleRate = readInt(raf);
            header.channels = readShort(raf);
            header.bitsPerSample = readShort(raf);
            header.dataSize = readInt(raf);
            header.isAdpcm = (header.bitsPerSample == 8);
            
            // Read audio data
            byte[] audioData = new byte[header.dataSize];
            raf.readFully(audioData);
            
            // Decode if ADPCM
            byte[] pcmData;
            if (header.isAdpcm) {
                pcmData = decodeImaAdpcm(audioData, header.channels);
            } else {
                pcmData = audioData; // Already PCM
            }
            
            // Write WAV
            writeWav(wavFile, pcmData, header.sampleRate, header.channels, 16);
        }
    }
    
    // IMA ADPCM Decoder (Westwood variant)
    private static byte[] decodeImaAdpcm(byte[] adpcmData, int channels) {
        // IMA ADPCM step table
        int[] stepTable = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31,
            34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143,
            157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
            724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
            3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
        };
        
        int[] indexTable = {
            -1, -1, -1, -1, 2, 4, 6, 8,
            -1, -1, -1, -1, 2, 4, 6, 8
        };
        
        int totalSamples = adpcmData.length * 2 / channels;
        byte[] pcmData = new byte[totalSamples * 2]; // 16-bit output
        ByteBuffer out = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        
        for (int ch = 0; ch < channels; ch++) {
            int predictor = 0;
            int stepIndex = 0;
            int step = stepTable[0];
            
            int sampleIndex = ch;
            int byteIndex = ch;
            
            while (byteIndex < adpcmData.length) {
                byte b = adpcmData[byteIndex];
                byteIndex += channels;
                
                // Two nibbles per byte
                for (int nibble = 0; nibble < 2; nibble++) {
                    int delta = (nibble == 0) ? (b & 0x0F) : ((b >> 4) & 0x0F);
                    
                    // Compute difference
                    int diff = step >> 3;
                    if ((delta & 4) != 0) diff += step;
                    if ((delta & 2) != 0) diff += step >> 1;
                    if ((delta & 1) != 0) diff += step >> 2;
                    
                    if ((delta & 8) != 0) predictor -= diff;
                    else predictor += diff;
                    
                    // Clamp
                    if (predictor > 32767) predictor = 32767;
                    else if (predictor < -32768) predictor = -32768;
                    
                    // Update step index
                    stepIndex += indexTable[delta];
                    if (stepIndex < 0) stepIndex = 0;
                    else if (stepIndex > 88) stepIndex = 88;
                    step = stepTable[stepIndex];
                    
                    // Write 16-bit PCM
                    out.putShort((short) predictor);
                }
            }
            
            // Interleave channels if stereo
            if (channels == 2) {
                // Re-interleave: we decoded per-channel, need to interleave
                // This is simplified - real implementation needs proper deinterleaving
            }
        }
        
        return pcmData;
    }
    
    private static void writeWav(Path file, byte[] pcmData, int sampleRate, int channels, int bitsPerSample) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            int byteRate = sampleRate * channels * (bitsPerSample / 8);
            int blockAlign = channels * (bitsPerSample / 8);
            int dataSize = pcmData.length;
            int chunkSize = 36 + dataSize;
            
            // RIFF header
            fos.write("RIFF".getBytes());
            writeInt(fos, chunkSize);
            fos.write("WAVE".getBytes());
            
            // fmt chunk
            fos.write("fmt ".getBytes());
            writeInt(fos, 16); // chunk size
            writeShort(fos, (short) 1); // PCM
            writeShort(fos, (short) channels);
            writeInt(fos, sampleRate);
            writeInt(fos, byteRate);
            writeShort(fos, (short) blockAlign);
            writeShort(fos, (short) bitsPerSample);
            
            // data chunk
            fos.write("data".getBytes());
            writeInt(fos, dataSize);
            fos.write(pcmData);
        }
    }
    
    private static int readInt(RandomAccessFile raf) throws IOException {
        byte[] b = new byte[4];
        raf.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    private static short readShort(RandomAccessFile raf) throws IOException {
        byte[] b = new byte[2];
        raf.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
    
    private static void writeInt(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
    
    private static void writeShort(OutputStream os, short v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }
}

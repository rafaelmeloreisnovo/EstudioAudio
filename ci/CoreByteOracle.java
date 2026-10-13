/* SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1 */
package io.rafaelia.audiostudio;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
final class CoreByteOracle {
    public static void main(String[] args) throws Exception {
        byte[] data=new byte[65];
        for(int i=0;i<data.length;++i)data[i]=(byte)(i*73+19);
        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        RfaStoredZip zip=new RfaStoredZip(stream);
        zip.add("metadata/proof.bin",data);zip.add("empty.txt",new byte[0]);zip.finish();
        Files.write(Paths.get(args[0],"java.zip"),stream.toByteArray());
        LowSha256 hash=new LowSha256();hash.update(data,0,data.length);
        StringBuilder hex=new StringBuilder();
        for(byte b:hash.finish())hex.append(String.format("%02x",b&255));
        Files.write(Paths.get(args[0],"java.sha256"),hex.toString().getBytes("US-ASCII"));
    }
}


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matthew Titmus <matthew.titmus@gmail.com>
 */
public class Dedupe {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static void main(String[] args) throws Exception {
        File pwd = new File(".");
        File targetDirectory = args.length > 0 ? new File(pwd, args[0]) : pwd;
        println targetDirectory.directory

        if (targetDirectory.isDirectory()) {
            Map<String, List<File>> md5sumFileLookup = new HashMap<>();
            List<File> fileList;

            for (File file : targetDirectory.listFiles()) {
                if (file.isFile()) {
                    String md5sum = calculateMD5Sum(file);

                    if (null == (fileList = md5sumFileLookup.get(md5sum))) {
                        md5sumFileLookup.put(md5sum, fileList = new ArrayList<>());
                    }

                    fileList.add(file);
                }
            }
        } else {
            System.err.println("Target must be a directory");
            System.exit(1);
        }
    }

    public static String calculateMD5Sum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = new byte[256];
        int count;

        InputStream is = Files.newInputStream(Paths.get(file.toURI()));
        DigestInputStream dis = new DigestInputStream(is, md);

        try {
            while (-1 != (count = dis.read(bytes))) {
                md.update(bytes, 0, count);
            }
        } finally {
            is.close();
        }

        byte[] digest = md.digest();
        String base64 = ENCODER.encodeToString(digest);

        return base64;
    }
}


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple utility that receives a directory and looks for duplicate files (such as multiple of the
 * same image) in a directory.
 *
 * TODO: Add support for flags
 * <ol>
 * <li>-r : Recursive; match only within directories</li>
 * <li>-R : Recursive; match among all directories</li>
 * <li>-XX : Automatically remove matching files (except the first)</li>
 * <li>-sA : Sort matches alphabetically (default; current behavior)</li>
 * <li>-sT : Sort matches by timestamp</li>
 * <li>-v : Verbose mode (mostly for exceptions atm).
 * </ol>
 *
 * TODO: Options to be provided for deletion:
 * <ol>
 * <li>1, 2, 3 : Index of file to keep.</li>
 * <li>XX : Delete all in group</li>
 * <li>N : None; keep all in group</li>
 * </ol>
 *
 * @author Matthew Titmus <matthew.titmus@gmail.com>
 */
public class DedupeJava {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static void main(String[] args) throws Exception {
        File pwd = new File(".");
        File targetDirectory = args.length > 0 ? new File(pwd, args[0]) : pwd;

        if (!targetDirectory.isDirectory()) {
            System.err.println("Target must be a directory");
            System.exit(1);
        } else {
            try {
                DedupeJava dedupe = new DedupeJava();
                dedupe.doDedupe(targetDirectory);
            } catch (NoSuchAlgorithmException | IOException e) {
                System.err.println("An unexpected error has occured: " + e.toString());
            }
        }
    }

    /**
     * Kicks off the de-dupe process. Called from main().
     *
     * @param targetDirectory The directory to identify duplicates in.
     * @throws IOException If there is an underlying error in I/O.
     * @throws NoSuchAlgorithmException If the MD5 sum algorithm isn't known. Should basically never happen.
     */
    public void doDedupe(File targetDirectory) throws IOException, NoSuchAlgorithmException {
        List<List<File>> duplicateGroups = getduplicateGroups(targetDirectory);

        for (List<File> group : duplicateGroups) {
            List<File> toDelete = null;

            do {
                toDelete = translateInput(group);
            } while (null == toDelete);

            for (File f : toDelete) {
                System.out.println("Deleting: " + f.getCanonicalPath());
            }
        }
    }

    /**
     * Validates and standardizes the input against the group array.
     *
     * @param input
     * @return A string representing a number, or a capital letter; or null if invalid
     */
    private List<File> translateInput(List<File> group) throws IOException {
        System.out.println(generateQueryOutput(group));

        List<File> toDelete = null;
        String input = new BufferedReader(new InputStreamReader(System.in)).readLine();

        if (input != null && !input.isEmpty()) {
            if (input.matches("[0-9]+")) {
                int index = Integer.parseInt(input) - 1;

                if (index >= 0 && index < group.size()) {
                    toDelete = new ArrayList(group.subList(index, index));
                }
            } else {
                input = input.toUpperCase();

                switch (input) {
                    // Delete all
                    case "XX":
                        toDelete = new ArrayList<>(group);
                        break;

                    // Delete none/keep all
                    case "N":
                        toDelete = new ArrayList<>();
                        break;

                    // Delete none/keep all
                    case "Q":
                        toDelete = null;
                        throw new InterruptedIOException("Break by user");

                    // Invalid input. Return a null list.
                    default:
                        toDelete = null;
                        break;
                }
            }
        }

        return toDelete;
    }

    /**
     * Receives a list of {@link File Files} and generates the "what do you want to do" output.
     *
     * @param files A {@link List} of {@link File} objects. Files are output in the order listed.
     */
    private String generateQueryOutput(List<File> files) {
        return "";
    }

    /**
     * Identifies all groups of duplicate files and returns them as a {@link List} of Lists.
     *
     * Only files with duplicates are included! That is, all lists are guaranteed to have 2 or more
     * elements
     *
     * @param targetDirectory The directory to scan for duplicates.
     * @return A {@link List} of Lists of duplicate {@link File Files}.
     */
    private List<List<File>> getduplicateGroups(File targetDirectory) throws IOException, NoSuchAlgorithmException {
        Map<String, List<File>> md5sumFileLookup = new HashMap<>();
        List<File> fileList;

        // Loop through all children and associate files with md5sums
        for (File file : targetDirectory.listFiles()) {
            if (file.isFile()) {
                String md5sum = calculateMD5Sum(file);

                if (null == (fileList = md5sumFileLookup.get(md5sum))) {
                    md5sumFileLookup.put(md5sum, fileList = new ArrayList<>());
                }

                fileList.add(file);
            }
        }

        // Create a list of lists, containing only the duplicate groups
        List<List<File>> duplicateGroups = new ArrayList<>();
        for (List<File> group : md5sumFileLookup.values()) {
            if (group.size() > 1) {
                Collections.sort(group);
                duplicateGroups.add(group);
            }
        }

        System.out.println("Duplicate groups found: " + duplicateGroups.size());

        // Purge the map, for memory's sake.
        md5sumFileLookup.clear();

        // Sort the list of lists by the name of the each list's first file
        Collections.sort(
                duplicateGroups,
                (a, b) -> a.get(0).getName().compareTo(b.get(0).getName())
        );

        return duplicateGroups;
    }

    public static String calculateMD5Sum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = new byte[256];
        int count;

        try (
                InputStream is = Files.newInputStream(Paths.get(file.toURI()));
                DigestInputStream dis = new DigestInputStream(is, md)) {

            while (-1 != (count = dis.read(bytes))) {
                md.update(bytes, 0, count);
            }
        }

        byte[] digest = md.digest();
        String base64 = ENCODER.encodeToString(digest);

        return base64;
    }
}

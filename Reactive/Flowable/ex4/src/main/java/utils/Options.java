package utils;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.net.URL;

/**
 * This class implements the Singleton pattern to handle command-line
 * option processing.
 */
public class Options {
    /**
     * Logging tag.
     */
    private static final String TAG = Options.class.getName();

    /** 
     * The singleton {@link Options} instance. 
     */
    private static Options mUniqueInstance = null;

    /**
     * The path to the image directory.
     */
    private static final String sIMAGE_DIRECTORY_PATH =
        "DownloadImages";

    /**
     * Default image names to use for testing.
     */
    private final String[] mDefaultImageNames = new String[] {
        "1.png,"
        + "9.jpg,"
        + "10.png,"
        + "11.jpg,"
        + "16.jpg,"
        + "19.png,"
        + "12.jpg",
        "13.png,"
        + "14.jpg,"
        + "15.jpg,"
        + "17.jpg,"
        + "2.jpg,"
        + "20.jpg,"
        + "3.png",
        "4.png,"
        + "5.jpg,"
        + "21.jpg,"
        + "6.jpg,"
        + "7.png,"
        + "18.jpg,"
        + "8.jpg",
        "1.png,"
        + "9.jpg,"
        + "10.png,"
        + "11.jpg,"
        + "16.jpg,"
        + "19.png,"
        + "12.jpg",
        "13.png,"
        + "14.jpg,"
        + "15.jpg,"
        + "17.jpg,"
        + "2.jpg,"
        + "20.jpg,"
        + "3.png",
        "4.png,"
        + "5.jpg,"
        + "21.jpg,"
        + "6.jpg,"
        + "7.png,"
        + "18.jpg,"
        + "8.jpg"
    };

    /**
     * Prefix for all the URLs.
     */
    private static final String sURL_PREFIX =
        "http://www.dre.vanderbilt.edu/~schmidt/images/";

    /**
     * Controls whether debugging output will be generated (defaults
     * to false).
     */
    private boolean mDiagnosticsEnabled = false;

    /**
     * Controls how many entries are generated.
     */
    private int mMAX_SIZE = Integer.MAX_VALUE;

    /**
     * Controls whether logging is enabled
     */
    private boolean mLoggingEnabled;

    /**
     * Method to return the one and only singleton uniqueInstance.
     */
    public static Options instance() {
        if (mUniqueInstance == null)
            mUniqueInstance = new Options();

        return mUniqueInstance;
    }

    /**
     * Return the path for the directory where images are stored.
     */
    public String getDirectoryPath() {
        return new File(sIMAGE_DIRECTORY_PATH).getAbsolutePath();
    }

    /**
     * @return A {@link Flowable} of {@link URL} objects
     */
    public Flowable<URL> getUrlFlowable() {
        return Flowable
            // Convert the array of strings into a stream of strings.
            .fromArray(mDefaultImageNames)

            // Map each string in the list into a list of URLs.
            .flatMap(this::convertStringToFlowable)

            // Limit the number of entries generated.
            .take(mMAX_SIZE);
    }

    /**
     * Create a new {@link Flowable} of {@link URL} objects from a
     * {@code stringOfUrls} that contains the {@code sURL_PREFIX} names
     * separated by commas and add them to {@link Flowable} that's returned.
     */
    private Flowable<URL> convertStringToFlowable(String stringOfNames) {
        // Create a Function that returns a new URL object when
        // applied and which converts checked URL exceptions into
        // runtime exceptions.
        Function<String, URL> urlFactory =
            ExceptionUtils.rethrowFunction(URL::new);

        return Flowable
            // Spit the string into an array based on "," separator.
            .fromArray(stringOfNames.split(","))

            // Concatenate the url prefix with each name.
            .map(name -> sURL_PREFIX + name)

            // Convert each string in the Flowable to a URL.
            .map(urlFactory);
    }

    /**
     * Returns whether debugging output is generated.
     */
    public boolean diagnosticsEnabled() {
        return mDiagnosticsEnabled;
    }

    /**
     * Returns whether logging is enabled or not.
     */
    public boolean loggingEnabled() {
        return mLoggingEnabled;
    }

    /**
     * Parse command-line arguments and set the appropriate values.
     */
    public void parseArgs(String[] argv) {
        if (argv != null) {
            for (int argc = 0; argc < argv.length; argc += 2)
                switch (argv[argc]) {
                case "-d":
                    mDiagnosticsEnabled = argv[argc + 1].equals("true");
                    break;
                case "-l":
                    mLoggingEnabled = argv[argc + 1].equals("true");
                        break;
                case "-s":
                    mMAX_SIZE = Integer.parseInt(argv[argc + 1]);
                    break;
                default:
                    printUsage();
                    return;
                }
        }
    }

    /**
     * Print out usage and default values.
     */
    private void printUsage() {
        System.out.println("Usage: ");
        System.out.println("-d [true|false] -l [true|false] -s [n]");
    }

    /**
     * Display the statistics about the test.
     */
    public static void printStats(String testName,
                                  int imageCount) {
        if (!testName.equals("warmup"))
            System.out.println("["
                               + Thread.currentThread().getId()
                               + "] "
                               + testName
                               + " downloaded and stored "
                               + imageCount
                               + " images using "
                               + (((IoScheduler) Schedulers.io()).size())
                               + " threads in the pool");
    }

    /**
     * Print the {@code element} and the {@code operation} along with
     * the current thread name to aid debugging and comprehension.
     *
     * @param element The given element
     * @param operation The Reactor operation being performed
     * @return The element parameter
     */
    public static <T> T logIdentity(T element, String operation) {
        System.out.println("["
                           + Thread.currentThread().getName()
                           + "] "
                           + operation
                           + " -- " 
                           + element);
        return element;
    }

    /**
     * Make the constructor private for a singleton.
     */
    private Options() {
        // Create the path to the image directory.
        File imageDirectory =
            new File(getDirectoryPath());

        // Create the image directory.
        imageDirectory.mkdirs();
    }
}

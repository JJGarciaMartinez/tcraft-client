package service;

/**
 * Service class for retrieving information about the system environment.
 */
public class SystemInfoService {

    /**
     * Retrieves the name of the operating system running the Java virtual machine.
     *
     * @return the name of the operating system as a string, as specified by the "os.name" system property.
     */
    public static String getOS() {
        return System.getProperty("os.name");
    }
}

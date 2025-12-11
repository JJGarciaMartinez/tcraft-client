package service;

public class SystemInfoService {

    public static String getOS() {
        return System.getProperty("os.name");
    }
}

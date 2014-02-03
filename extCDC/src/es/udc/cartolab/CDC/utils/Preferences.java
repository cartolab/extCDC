package es.udc.cartolab.CDC.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Preferences {
    // esta clase gestiona el archivo de configuraciï¿½n, no comprueba si los
    // cambios son vï¿½lidos.

    private static final String preferencesFile = "gvSIG" + File.separator
			+ "extensiones" + File.separator + "es.udc.cartolab.CDC"
	    + File.separator + ".config";
    private static Preferences instance = null;

    private String latestCsv = null;

    private static synchronized void createInstance() throws IOException {
        if (instance == null) {
            instance = new Preferences();
        }
    }

    public static Preferences getPreferences() throws IOException {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }

    private Preferences() throws IOException {
        String line;
        File configFile = new File(preferencesFile);

        try {
            // leer el archivo en busca de los datos
            FileReader r = new FileReader(configFile);
            BufferedReader fileReader = new BufferedReader(r);
            while ((line = fileReader.readLine()) != null) {
                if (line.charAt(0) != '#') {
                    int spacePos = line.indexOf(" ");
                    if (spacePos > 0) {
                        String configWord = line.substring(0, spacePos);
                        if (configWord.compareTo("latestCsv") == 0) {
                            latestCsv = line.substring(line.indexOf('"'));
                            latestCsv = latestCsv.substring(1);
                            latestCsv = latestCsv.substring(0, latestCsv
                                    .indexOf('"'));
                        }
                    }
                }
            }
            fileReader.close();
        } catch (FileNotFoundException fnfe) {
            configFile.createNewFile();
            System.out.println("Escribiendo en el archivo de nueva creación");
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write("directory");
            fileWriter.flush();
            fileWriter.close();
        }
    }

    public String getLatestCsv() {
        return latestCsv;
    }

    public int setLatestCsv(String latestCsv) throws IOException {
        // return 0 if everything is OK.
        // return -1 if there's an error saving the preferences file.
        File configFile = new File(preferencesFile);
        int error = 0;
        if (!configFile.canWrite()) {
            return -1;
        }
        FileWriter fileWriter = new FileWriter(configFile);
        String line = "latestCsv \"" + latestCsv + "\"" + "\n";
        fileWriter.write(line);

        fileWriter.flush();
        this.latestCsv = latestCsv;
        return error;

    }

    public boolean isConfigured() {
        return true;
    }
}

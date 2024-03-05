/*
 * (c) Copyright IBM Corp. 2024
 * (c) Copyright Instana Inc.
 */

package com.instana.dc.rdb.impl.informix;

import com.instana.dc.rdb.impl.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnstatCommandExecutor {

    private static final Logger LOGGER = Logger.getLogger(OnstatCommandExecutor.class.getName());
    public static final String PATH_TO_SCRIPT = "/rdb/src/main/java/com/instana/dc/rdb/impl/informix/scripts/";
    public static final String USER_DIR = "user.dir";
    public static final String SHELL_SCRIPT = "sh";
    public static final String LINE_BREAK = "\n";
    private final String dbPath;
    private final String binPath;
    private final String serverName;
    private final String configFile;
    private final String sqlHostFile;

    public OnstatCommandExecutor(String dbPath, String serverName) {
        this.dbPath = dbPath;
        this.binPath = dbPath + Constants.BIN;
        this.serverName = serverName;
        this.configFile = Constants.ONCONFIG + serverName;
        this.sqlHostFile = Constants.SQL_HOSTS + serverName;
    }

    /**
     * Public Util Method to execute the command
     *
     * @param fileName : Name of the file which you want to execute
     * @param index    : Index of the Result array where intended output is present
     * @return : Value as String
     */
    public String executeCommand(String fileName, int index) {
        String currentDirectory = System.getProperty(USER_DIR);
        String absoluteFilePath = currentDirectory + PATH_TO_SCRIPT + fileName;

        String[] command = new String[]{
                SHELL_SCRIPT,
                absoluteFilePath,
                binPath,
                dbPath,
                configFile,
                serverName,
                sqlHostFile
        };

        Optional<String[]> result = commandExecutorAndExtractor(command);
        if (result.isPresent()) {
            return result.get()[index];
        }
        LOGGER.log(Level.WARNING, "No result present for executable: {}", fileName);
        return null;
    }


    /**
     * Private Util Method to execute and extract the result of the given onstat command
     *
     * @param commands : commands to execute
     * @return String[] : Output of the command
     */
    private Optional<String[]> commandExecutorAndExtractor(String[] commands) {
        StringBuilder output = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line).append(LINE_BREAK);
            }

            String[] outputPart = output.toString().split(LINE_BREAK);
            String[] metricValue = outputPart[outputPart.length - 1].split(Constants.BLANK_SPACE);
            return Optional.of(metricValue);
        } catch (IOException exp) {
            LOGGER.log(Level.SEVERE, "Unable to extract the value: ", exp);
        }

        return Optional.empty();
    }
}

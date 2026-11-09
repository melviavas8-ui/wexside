package ru.wexside.config;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ConfigSerializable {
    public String getConfigId();

    public void writeConfig(DataOutputStream var1) throws IOException;

    public void readConfig(DataInputStream var1) throws IOException;
}


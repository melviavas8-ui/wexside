package ru.wexside.misc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class ShaderIncludeProcessor {
    private static final String INCLUDE_DIRECTIVE = "#include";

    private static String resolveIncludePath(String includePath, String currentDirectory) {
        if (includePath.startsWith("/") || includePath.startsWith("assets/")) {
            return includePath.startsWith("/") ? includePath : "/" + includePath;
        }
        if (currentDirectory == null || currentDirectory.isEmpty()) {
            return includePath;
        }
        if (currentDirectory.endsWith("/")) {
            return currentDirectory + includePath;
        }
        return currentDirectory + "/" + includePath;
    }

    private static String expandIncludes(String source, String currentDirectory, ClassLoader classLoader, Set<String> includeStack, int depth) {
        if (depth > 64) {
            throw new IllegalStateException("Include depth too large (possible cycle)");
        }
        StringBuilder result = new StringBuilder(source.length() + 1024);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith(INCLUDE_DIRECTIVE)) {
                    Object absolutePath;
                    int openingQuote = trimmedLine.indexOf(34);
                    int closingQuote = trimmedLine.lastIndexOf(34);
                    if (openingQuote < 0 || closingQuote <= openingQuote) {
                        throw new IllegalStateException("Malformed include: " + line);
                    }
                    String requestedPath = trimmedLine.substring(openingQuote + 1, closingQuote).trim();
                    String resolvedPath = ShaderIncludeProcessor.resolveIncludePath(requestedPath, currentDirectory);
                    Object object = absolutePath = resolvedPath.startsWith("/") ? resolvedPath : "/" + resolvedPath;
                    if (!includeStack.add((String)absolutePath)) {
                        throw new IllegalStateException("Include cycle detected: " + (String)absolutePath);
                    }
                    String includedSource = ShaderIncludeProcessor.readResource(classLoader, (String)absolutePath);
                    String expandedSource = ShaderIncludeProcessor.expandIncludes(includedSource, ShaderIncludeProcessor.parentDirectory((String)absolutePath), classLoader, includeStack, depth + 1);
                    includeStack.remove(absolutePath);
                    result.append(expandedSource).append('\n');
                    continue;
                }
                result.append(line).append('\n');
            }
        }
        catch (Exception exception) {
            throw new RuntimeException("Preprocess failed (in " + currentDirectory + "): " + exception.getMessage(), exception);
        }
        return result.toString();
    }

    private static String readResource(ClassLoader classLoader, String path) {
        String resource = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream inputStream = classLoader.getResourceAsStream(resource);){
            if (inputStream == null) {
                throw new IllegalStateException("Include not found on classpath: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception exception) {
            throw new RuntimeException("Read failed: " + path, exception);
        }
    }

    private static String parentDirectory(String path) {
        int separator = path.lastIndexOf(47);
        if (separator <= 0) {
            return "/";
        }
        return path.substring(0, separator);
    }

    public static String preprocess(String source, String sourceDirectory, ClassLoader classLoader) {
        return ShaderIncludeProcessor.expandIncludes(source, ShaderIncludeProcessor.normalizeDirectory(sourceDirectory), classLoader, new HashSet<String>(), 0);
    }

    private static String normalizeDirectory(String directory) {
        if (directory == null || directory.isEmpty()) {
            return "/";
        }
        return directory.endsWith("/") ? directory : directory + "/";
    }
}


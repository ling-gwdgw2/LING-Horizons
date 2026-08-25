package me.ling.horizons.client.core.gl.shader;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Self-contained NeoForge-compatible shader loader for Voxy.
 *
 * This loader uses Voxy's isolated classloader and internal recursive #import
 * resolver, eliminating direct dependencies on Sodium's internal ShaderParser/ShaderConstants
 * APIs. This ensures rock-solid compatibility across Sodium 0.6.x, Sodium 0.8.12+, and Iris 1.8.14+.
 *
 * Upstream reference: https://github.com/MCRcortex/voxy
 */
public class ShaderLoader {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<namespace>.*):(?<path>.*)>");

    /**
     * Parse and load a shader, resolving all recursive #import directives
     * and ensuring valid GLSL 460 core versioning.
     */
    public static String parse(String id) {
        // Load shader source using Voxy's classloader (NeoForge classloader isolation fix)
        String shaderSource = getShaderSource(id);

        // Process any nested #import directives recursively using Voxy's internal parser
        String processed = processImports(shaderSource);

        // Normalize line endings
        processed = processed.replaceAll("\r\n", "\n");

        // Strip any embedded #version directive so we can supply uniform target version
        processed = processed.replaceFirst("^#version .+\n", "");
        processed = processed.replaceFirst("\n#version .+\n", "\n");

        // Prepend target GLSL 460 core version
        return "#version 460 core\n\n" + processed + "\n//beans\n";
    }

    /**
     * Load shader source using Voxy's classloader.
     * Path format: "namespace:path" -> "/assets/{namespace}/shaders/{path}"
     */
    private static String getShaderSource(String id) {
        String[] parts = id.split(":", 2);
        String namespace = parts.length > 1 ? parts[0] : "ling_horizons";
        String path = parts.length > 1 ? parts[1] : parts[0];

        String resourcePath = String.format("/assets/%s/shaders/%s", namespace, path);

        try (InputStream in = ShaderLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Shader not found: " + resourcePath + " (id=" + id + ")");
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source: " + resourcePath, e);
        }
    }

    /**
     * Process #import directives recursively, loading from Voxy's resources.
     */
    private static String processImports(String source) {
        StringBuilder result = new StringBuilder();
        for (String line : source.split("\n")) {
            if (line.trim().startsWith("#import")) {
                Matcher matcher = IMPORT_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    String namespace = matcher.group("namespace");
                    String path = matcher.group("path");
                    String importId = namespace + ":" + path;
                    String importedSource = getShaderSource(importId);
                    result.append(processImports(importedSource));
                } else {
                    result.append(line);
                }
            } else {
                result.append(line);
            }
            result.append("\n");
        }
        return result.toString();
    }
}

/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jenkins.pluginhealth.scoring.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.config.ApplicationConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PluginDocumentationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginDocumentationService.class);

    private final ObjectMapper objectMapper;
    private final ApplicationConfiguration configuration;

    public PluginDocumentationService(ObjectMapper objectMapper, ApplicationConfiguration configuration) {
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    public Map<String, String> fetchPluginDocumentationUrl() {
        try {
            final Map<String, Link> documentationUrlsMap = objectMapper.readValue(
                    URI.create(configuration.jenkins().documentationUrls()).toURL(), new TypeReference<>() {});
            return documentationUrlsMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue() == null || e.getValue().url() == null
                                    ? ""
                                    : e.getValue().url()));
        } catch (MalformedURLException e) {
            LOGGER.error("URL to documentation link is incorrect.", e);
            return Map.of();
        } catch (IOException e) {
            LOGGER.error("Could not fetch plugin documentation.", e);
            return Map.of();
        }
    }

    record Link(String url) {}
}

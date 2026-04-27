package com.zhangyichuang.medicine.common.core.validation;

import com.zhangyichuang.medicine.common.core.annotation.TrustedResource;
import com.zhangyichuang.medicine.common.core.config.TrustedResourceProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class TrustedResourceValidator implements ConstraintValidator<TrustedResource, Object> {

    private List<String> trustedDomains = List.of();
    private List<String> allowedFileNames = List.of();
    private Pattern fileNamePattern;
    private boolean allowSubdomains;
    private boolean allowRelative;
    private boolean allowBlank;

    @Autowired(required = false)
    private TrustedResourceProperties properties;

    @Override
    public void initialize(TrustedResource constraint) {
        this.allowSubdomains = constraint.allowSubdomains();
        this.allowRelative = constraint.allowRelative();
        this.allowBlank = constraint.allowBlank();

        List<String> domainOverrides = normalizeDomains(constraint.trustedDomains());
        if (!domainOverrides.isEmpty()) {
            this.trustedDomains = domainOverrides;
        } else if (properties != null) {
            this.trustedDomains = normalizeDomains(properties.getDomains());
        }

        List<String> fileNameOverrides = normalizeFileNames(constraint.allowedFileNames());
        if (!fileNameOverrides.isEmpty()) {
            this.allowedFileNames = fileNameOverrides;
        } else if (properties != null) {
            this.allowedFileNames = normalizeFileNames(properties.getFileNames());
        }

        String pattern = constraint.fileNamePattern();
        if (!StringUtils.hasText(pattern) && properties != null) {
            pattern = properties.getFileNamePattern();
        }
        if (StringUtils.hasText(pattern)) {
            this.fileNamePattern = Pattern.compile(pattern);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        switch (value) {
            case null -> {
                return allowBlank;
            }
            case CharSequence sequence -> {
                return validateValue(sequence.toString());
            }
            case Collection<?> collection -> {
                for (Object item : collection) {
                    if (item == null) {
                        if (!allowBlank) {
                            return false;
                        }
                        continue;
                    }
                    if (!(item instanceof CharSequence)) {
                        return false;
                    }
                    if (!validateValue(item.toString())) {
                        return false;
                    }
                }
                return true;
            }
            case Object[] array when value.getClass().isArray() -> {
                for (Object item : array) {
                    if (item == null) {
                        if (!allowBlank) {
                            return false;
                        }
                        continue;
                    }
                    if (!(item instanceof CharSequence)) {
                        return false;
                    }
                    if (!validateValue(item.toString())) {
                        return false;
                    }
                }
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    private boolean validateValue(String value) {
        if (!StringUtils.hasText(value)) {
            return allowBlank;
        }
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            return false;
        }

        String scheme = uri.getScheme();
        if (!StringUtils.hasText(scheme)) {
            if (!allowRelative) {
                return false;
            }
        } else if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }

        if (StringUtils.hasText(scheme)) {
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            if (!isTrustedHost(host)) {
                return false;
            }
        }

        if (!allowedFileNames.isEmpty() || fileNamePattern != null) {
            String fileName = extractFileName(uri.getPath());
            if (!StringUtils.hasText(fileName)) {
                return false;
            }
            if (!allowedFileNames.isEmpty() && !allowedFileNames.contains(fileName)) {
                return false;
            }
            if (fileNamePattern != null && !fileNamePattern.matcher(fileName).matches()) {
                return false;
            }
        }

        return true;
    }

    private boolean isTrustedHost(String host) {
        if (trustedDomains.isEmpty()) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (String domain : trustedDomains) {
            if (normalizedHost.equals(domain)) {
                return true;
            }
            if (allowSubdomains && normalizedHost.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private String extractFileName(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash >= path.length() - 1) {
            return "";
        }
        return path.substring(lastSlash + 1);
    }

    private List<String> normalizeDomains(String[] domains) {
        if (domains == null || domains.length == 0) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String domain : domains) {
            String normalized = normalizeDomain(domain);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private List<String> normalizeDomains(List<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String domain : domains) {
            String normalized = normalizeDomain(domain);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        String trimmed = domain.trim().toLowerCase(Locale.ROOT);
        try {
            URI uri = URI.create(trimmed.contains("://") ? trimmed : "http://" + trimmed);
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
        }
        int colonIndex = trimmed.lastIndexOf(':');
        if (colonIndex > 0 && !trimmed.contains("]")) {
            return trimmed.substring(0, colonIndex);
        }
        return trimmed;
    }

    private List<String> normalizeFileNames(String[] fileNames) {
        if (fileNames == null || fileNames.length == 0) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String fileName : fileNames) {
            if (StringUtils.hasText(fileName)) {
                result.add(fileName.trim());
            }
        }
        return List.copyOf(result);
    }

    private List<String> normalizeFileNames(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String fileName : fileNames) {
            if (StringUtils.hasText(fileName)) {
                result.add(fileName.trim());
            }
        }
        return List.copyOf(result);
    }
}

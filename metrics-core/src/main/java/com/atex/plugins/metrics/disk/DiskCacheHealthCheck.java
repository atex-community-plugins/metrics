package com.atex.plugins.metrics.disk;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

import javax.servlet.ServletContext;

import com.atex.plugins.metrics.AbstractHealthCheck;
import com.atex.plugins.metrics.HealthCheckProvider;
import com.atex.plugins.metrics.Parameters;
import com.polopoly.application.Application;
import com.polopoly.application.servlet.ApplicationServletUtil;
import com.polopoly.cm.client.CmClientBase;
import com.polopoly.cm.client.DiskCacheSettings;

/**
 * DiskCacheHealthCheck
 *
 * @author mnova
 */
@HealthCheckProvider(name = DiskCacheHealthCheck.NAME)
public class DiskCacheHealthCheck extends AbstractHealthCheck {

    final static String NAME = "diskcache";

    private CacheSettings cacheSettings;

    private ServletContext servletContext;
    private long minFreeSpace;
    private double minPct;

    @Override
    protected void init(final ServletContext servletContext) {
        this.servletContext = checkNotNull(servletContext);
        final Parameters params = new Parameters(servletContext);

        // 512Mb

        this.minFreeSpace = params.getLong(
                NAME + ".minFreeSpace",
                512000 * 1000);

        // 1%

        this.minPct = params.getDouble(
                NAME + ".minPct",
                1.0);
    }

    @Override
    protected Result check() throws Exception {
        String warning = null;
        final CacheSettings cacheSettings = getCacheSettings();
        final File files = cacheSettings.getFiles();
        if (files != null) {
            final long total = files.getTotalSpace();
            final long free = files.getUsableSpace();

            if (free <= minFreeSpace) {
                return Result.unhealthy("free space on " + files.getAbsolutePath() + " is " + free + " which is less than " + minFreeSpace);
            }
            final double pct = (free * 100 / total);
            if (pct <= minPct) {
                warning = "free space on " + files.getAbsolutePath() + " is " + pct + "%\n";
            }
        }
        final File contents = cacheSettings.getContents();
        if (contents != null) {
            final long total = contents.getTotalSpace();
            final long free = contents.getUsableSpace();

            if (free <= minFreeSpace) {
                return Result.unhealthy("free space on " + contents.getAbsolutePath() + " is " + free + " which is less than " + minFreeSpace);
            }
            final double pct = (free * 100 / total);
            if (pct <= minPct) {
                if (warning == null) {
                    warning = "";
                }
                warning += "free space on " + contents.getAbsolutePath() + " is " + pct + "%\n";
            }
        }
        return Result.healthy(warning);
    }

    private synchronized CacheSettings getCacheSettings() {
        if (cacheSettings == null) {
            final Application application = ApplicationServletUtil.getApplication(servletContext);
            final CmClientBase cmClient = (CmClientBase) application.getApplicationComponent(CmClientBase.DEFAULT_COMPOUND_NAME);
            final DiskCacheSettings settings = cmClient.getDiskCacheSettings();
            final File files = settings.getFilesCacheDirectoryAbsolute();
            final File contents = settings.getContentCacheDirectoryAbsolute();
            cacheSettings = new CacheSettings(files, contents);
        }
        return cacheSettings;
    }

    private class CacheSettings {
        private final File files;
        private final File contents;

        private CacheSettings(final File files, final File contents) {
            this.files = files;
            this.contents = contents;
        }

        public File getFiles() {
            return files;
        }

        public File getContents() {
            return contents;
        }
    }

}

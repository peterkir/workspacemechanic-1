/*******************************************************************************
 * Copyright (C) 2011, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.mechanic.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.eclipse.mechanic.ICollector;
import com.google.eclipse.mechanic.IResourceTaskReference;
import com.google.eclipse.mechanic.SuffixFileFilter;
import com.google.eclipse.mechanic.plugin.core.ResourceTaskProvider;

public final class FileTaskProvider extends ResourceTaskProvider {
  private final File dir;

  /**
   * This is only public for ClassFileTaskScanner. See if we can make this private.
   */
  public final class TaskReference implements IResourceTaskReference {
    private final File file;

    public TaskReference(File file) {
      this.file = file;
    }

    public ResourceTaskProvider getProvider() {
      return FileTaskProvider.this;
    }

    public InputStream newInputStream() throws FileNotFoundException {
      return new FileInputStream(file);
    }

    public String getName() {
      return file.getName();
    }

    public String getPath() {
      return file.getPath();
    }

    @Override
    public String toString() {
      return file.getPath();
    }

    public long getLastModified() {
      return file.lastModified();
    }

    public File asFile() {
      return file;
    }

    public long computeMD5() throws IOException {
      return Files.hash(file, Hashing.md5()).asLong();
    }
  }

  private static final String SEPARATOR = System.getProperty("file.separator");

  private FileTaskProvider(File dir) {
    String path = dir.getPath();
    if (path.startsWith("~" + SEPARATOR)) {
      dir = new File(System.getProperty("user.home"), path.substring(1));
    } else if (path.startsWith(".."  + SEPARATOR)) {
      dir = new File(new File(System.getProperty("user.dir"), ".."), path.substring(2));
    } else if (path.startsWith("." + SEPARATOR)) {
      dir = new File(System.getProperty("user.dir"), path.substring(1));
    }
    this.dir = dir;
  }

  public static FileTaskProvider newInstance(File dir) throws IOException {
    Preconditions.checkNotNull(dir);
    FileTaskProvider instance = new FileTaskProvider(dir);
    instance.validateInitialization();
    return instance;
  }

  /**
   * REMOVE as we move things along.
   */
  public File getFile() {
    return dir;
  }

  private void validateInitialization() throws IOException {
    if (!dir.exists()) {
      throw new IOException(
          String.format("Directory '%s' does not exist.", dir));
    }
    if (!dir.canRead()) {
      throw new IOException(
          String.format("Directory '%s' is not readable.", dir));
    }
  }

  public void collectTaskReferences(
      String localPath, String filter, ICollector<IResourceTaskReference> collector) {
    // dir points to the root of the task directory, we
    // add the PACKAGE PATH to point to the dir with classes.
    File localDir = new File(dir.getAbsolutePath() + File.separator + localPath);
    new FileTaskProvider(localDir).collectTaskReferences(filter, collector);
  }

  public void collectTaskReferences(String filterText, ICollector<IResourceTaskReference> collector) {
    SuffixFileFilter filter = new SuffixFileFilter(filterText);

    File[] filesInDir = dir.listFiles(filter);
    if (filesInDir != null) {
      for (File file : filesInDir) {
        collector.collect(new TaskReference(file));
      }
    }
  }

  @Override
  public String toString() {
    return "File provider: " + dir.getPath();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof FileTaskProvider)) {
      return false;
    }

    return ((FileTaskProvider) obj).dir.equals(dir);
  }

  @Override
  public int hashCode() {
    return dir.hashCode();
  }
}

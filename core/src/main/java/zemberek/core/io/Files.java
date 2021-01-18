package zemberek.core.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helper methods for File operations. File operations throws RuntimeException instead of
 * IOException.
 */
public class Files {

  public static final Comparator<File> FILE_MODIFICATION_TIME_COMPARATOR_ASC = new FileModificationTimeComparatorAsc();
  public static final Comparator<File> FILE_MODIFICATION_TIME_COMPARATOR_DESC = new FileModificationTimeComparatorDesc();

  private Files() {
  }

  public static Comparator<File> getNameSortingComparator(final Locale locale) {
    return (file, file1) -> {
      Collator coll = Collator.getInstance(locale);
      return coll.compare(file.getName(), file1.getName());
    };
  }

  public static Comparator<File> getAbsolutePathSortingComparator(final Locale locale) {
    return (file, file1) -> {
      Collator coll = Collator.getInstance(locale);
      return coll.compare(file.getAbsolutePath(), file1.getAbsolutePath());
    };
  }

  public static Comparator<File> getNameSortingComparator() {
    return (file, file1) -> file.getName().compareToIgnoreCase(file1.getName());
  }

  public static Comparator<File> getAbsolutePathSortingComparator() {
    return (file, file1) -> file.getAbsolutePath().compareToIgnoreCase(file1.getAbsolutePath());
  }

  public static FileFilter extensionFilter(String... extensions) {
    if (extensions.length == 0) {
      return new AcceptAllFilter();
    }
    return new ExtensionFilter(extensions);
  }

  /**
   * This deletes files in a directory. it does not go into sub directories, and it does not delete
   * directories.
   *
   * @param files : zero or more files.
   */
  public static void deleteFiles(File... files) {
    for (File s : files) {
      if (s.exists() && !s.isDirectory()) {
        Files.deleteFiles(s);
      }
    }
  }

  /**
   * This deletes files and directories and all child directories and files.
   *
   * @param files : zero or more file or dorectory.
   */
  public static void deleteFilesAndDirs(File... files) {
    for (File file : files) {
      if (file.exists()) {
        if (file.isDirectory()) {
          deleteFilesAndDirs(file.listFiles());
        } else {
          file.delete();
        }
      }
    }
  }

  /**
   * Crawls into a directory and retrieves all the files in it and its sub directories.
   *
   * @param dir a File representing a directory
   * @return all the files in the
   */
  public static List<File> crawlDirectory(File dir) {
    return crawlDirectory(dir, new AcceptAllFilter());
  }

  /**
   * Crawls into a directory and retrieves all the files in it and its sub directories.
   *
   * @param dir a File representing a directory
   * @param comparator comparator to apply.
   * @return all the files in the
   */
  public static List<File> getFilesSorted(File dir, Comparator<File> comparator) {
    checkExistingDirectory(dir);
    List<File> files = Arrays.asList(dir.listFiles());
    files.sort(comparator);
    return files;
  }

  /**
   * Crawls into a directory and retrieves all the files in it and its sub directories.
   *
   * @param dir a File representing a directory
   * @param comparator comparator to apply.
   * @param filters filters to apply.
   * @return all the files in the
   */
  public static List<File> getFilesSorted(File dir, Comparator<File> comparator,
      FileFilter... filters) {
    checkExistingDirectory(dir);
    List<File> files = new ArrayList<>();
    for (File file : dir.listFiles()) {
      if (filters.length == 0) {
        files.add(file);
      } else {
        for (FileFilter filter : filters) {
          if (filter.accept(file)) {
            files.add(file);
            break;
          }
        }
      }
    }
    files.sort(comparator);
    return files;
  }

  /**
   * Crawls into a directory and retrieves all the files in it and its sub directories. Only the
   * files matching to the filter will be included.
   *
   * @param dir a File representing a directory
   * @param filters filter
   * @return all the files in the
   */
  public static List<File> crawlDirectory(File dir, FileFilter... filters) {
    return crawlDirectory(dir, true, filters);
  }

  /**
   * Crawls into a directory and retrieves all the files in it and its sub directories. Only the
   * files matching to the filter will be included.
   *
   * @param dir a File representing a directory
   * @param recurseSubDirs determines if it will recurse to the sub directories.
   * @param filters filter
   * @return all the files in the
   */
  public static List<File> crawlDirectory(File dir, boolean recurseSubDirs, FileFilter... filters) {
    checkNotNull(dir, "File is null!");
    checkExistingDirectory(dir);
    List<File> files = new ArrayList<>();
    for (File file : dir.listFiles()) {
      if (file.isDirectory() && recurseSubDirs) {
        files.addAll(crawlDirectory(file, true, filters));
      } else if (!file.isDirectory()) {
        if (filters.length == 0) {
          files.add(file);
        } else {
          for (FileFilter filter : filters) {
            if (filter.accept(file)) {
              files.add(file);
              break;
            }
          }
        }
      }
    }
    return files;
  }

  private static void checkExistingDirectory(File dir) {
    checkNotNull(dir, "Dir is null!");
    checkArgument(dir.exists(), "Directory does not exist! : " + dir);
    checkArgument(dir.isDirectory(), "i was expecting a directory : " + dir);
  }

  /**
   * get all directories under root dir.
   *
   * @param rootDir root dir to scan
   * @param recurseSubDirs if true, sub directories are also scanned.
   * @return List of directories.
   */
  public static List<File> getDirectories(File rootDir, boolean recurseSubDirs) {
    checkNotNull(rootDir, "File is null!");
    checkArgument(rootDir.isDirectory(), "i was expecting a directory..");
    checkArgument(rootDir.exists(), "Directory does not exist!.");
    List<File> dirs = new ArrayList<>();
    for (File dir : rootDir.listFiles()) {
      if (dir.isDirectory()) {
        if (recurseSubDirs) {
          dirs.addAll(getDirectories(dir, true));
        }
        dirs.add(dir);
      }
    }
    return dirs;
  }


  private static class FileModificationTimeComparatorDesc implements Comparator<File> {

    public int compare(File f1, File f2) {
      if (f1.lastModified() > f2.lastModified()) {
        return 1;
      } else {
        return f1.lastModified() == f2.lastModified() ? 0 : -1;
      }
    }
  }

  private static class FileModificationTimeComparatorAsc implements Comparator<File> {

    public int compare(File f1, File f2) {
      if (f1.lastModified() < f2.lastModified()) {
        return 1;
      } else {
        return f1.lastModified() == f2.lastModified() ? 0 : -1;
      }
    }
  }

  /**
   * This is a file filter using regular expressions. if file path/name matches with regexp it will
   * accept.
   */
  public static class RegexpFilter implements FileFilter {

    final Pattern regexp;

    public RegexpFilter(String regExp) {
      checkNotNull(regExp, "regexp String cannot be null.");
      checkArgument(!Strings.isNullOrEmpty(regExp), "regexp String cannot be empty");
      this.regexp = Pattern.compile(regExp);
    }

    public RegexpFilter(Pattern regExp) {
      checkNotNull(regExp, "regexp Pattern cannot be null.");
      this.regexp = regExp;
    }

    public boolean accept(File pathname) {
      return regexp.matcher(pathname.getPath()).find();
    }
  }

  public static class ExtensionFilter implements FileFilter {

    private final String[] extensions;

    public ExtensionFilter(String... extensions) {
      this.extensions = extensions;
    }

    public boolean accept(File pathname) {
      if (extensions == null || extensions.length == 0) {
        return true;
      }
      for (String extension : extensions) {
        if (pathname.getName().endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class AcceptAllFilter implements FileFilter {

    public boolean accept(File pathname) {
      return true;
    }
  }
}


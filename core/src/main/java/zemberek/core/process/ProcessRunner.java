package zemberek.core.process;

import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProcessRunner {

  File processRoot;

  public ProcessRunner(File processRoot) {
    this.processRoot = processRoot;
  }

  public void execute(ProcessBuilder pb) throws IOException, InterruptedException {
    System.out.println(Joiner.on(" ").join(pb.command()));
    Process process = pb.redirectErrorStream(true).directory(processRoot).start();
    new AsyncPipe(process.getErrorStream(), System.err).start();
    new AsyncPipe(process.getInputStream(), System.out).start();
    process.waitFor();
  }

  public void execute(ProcessBuilder pb, InputStream is, OutputStream os)
      throws IOException, InterruptedException {
    System.out.println(Joiner.on(" ").join(pb.command()));
    execute(pb.directory(processRoot).start(), is, os);
  }

  public void execute(ProcessBuilder pb, OutputStream os) throws IOException, InterruptedException {
    System.out.println(Joiner.on(" ").join(pb.command()));
    execute(pb.directory(processRoot).start(), os);
  }

  public void execute(Process process) throws IOException, InterruptedException {
    new AsyncPipe(process.getErrorStream(), System.err).start();
    new AsyncPipe(process.getInputStream(), System.out).start();
    process.waitFor();
  }

  public void execute(Process process, InputStream is, OutputStream os)
      throws IOException, InterruptedException {
    new AsyncPipe(process.getInputStream(), os).start();
    new AsyncPipe(is, process.getOutputStream()).start();
    new AsyncPipe(process.getErrorStream(), System.err, false).start();
    process.waitFor();
  }

  public void execute(Process process, OutputStream os) throws IOException, InterruptedException {
    new AsyncPipe(process.getInputStream(), os).start();
    new AsyncPipe(process.getErrorStream(), System.err, false).start();
    process.waitFor();
  }

  /**
   * A thread copies an input stream to an output stream.
   */
  class AsyncPipe extends Thread {

    InputStream is;
    OutputStream os;
    boolean closeStreams;

    AsyncPipe(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
      closeStreams = true;
    }

    AsyncPipe(InputStream is, OutputStream os, boolean closeStreams) {
      this.is = is;
      this.os = os;
      this.closeStreams = closeStreams;
    }

    @Override
    public void run() {
      try {
        synchronized (this) {
          byte[] buf = new byte[4096];
          int i;
          while ((i = is.read(buf)) != -1) {
            os.write(buf, 0, i);
          }
          if (closeStreams) {
            os.close();
            is.close();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * A thread copies an input stream to an output stream.
   */
  class SyncPipe {

    InputStream is;
    OutputStream os;
    boolean closeStreams;

    SyncPipe(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
      closeStreams = true;
    }

    SyncPipe(InputStream is, OutputStream os, boolean closeStreams) {
      this.is = is;
      this.os = os;
      this.closeStreams = closeStreams;
    }

    public void pipe() {
      try {
        synchronized (this) {
          byte[] buf = new byte[4096];
          int i;
          while ((i = is.read(buf)) != -1) {
            os.write(buf, 0, i);
          }
          if (closeStreams) {
            os.close();
            is.close();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

package cn.kk.autovpn;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class DynamicInputStream extends InputStream {
  private final Semaphore      lock  = new Semaphore(1);
  protected byte[]             buf;
  // 0: pos, 1: mark, 2: count
  protected AtomicIntegerArray marks = new AtomicIntegerArray(3);

  public DynamicInputStream() {
    this(new byte[0]);
  }

  public DynamicInputStream(byte[] buf) {
    this(buf, 0, buf.length);
  }

  public DynamicInputStream(byte[] buf, int offset, int length) {
    this.init(buf, offset, length);
  }

  public void setData(byte[] b) {
    this.setData(b, 0, b.length);
  }

  public void setData(byte[] b, int offset, int length) {
    this.init(b, offset, length);
  }

  private void init(byte[] b, int offset, int length) {
    if (b != null) {
      this.buf = b;
      this.marks.set(0, offset);
      this.marks.set(2, Math.min(offset + length, b.length));
      this.marks.set(1, offset);

      if (b.length == 0) {
        this.lock.tryAcquire();
      } else {
        this.lock.release();
      }
    } else {
      this.buf = null;
      this.lock.release();
    }
  }

  @Override
  public int read() {
    try {
      this.lock.acquire();
      if (this.buf == null) {
        return -1;
      }
      final int val = (this.buf[this.marks.get(0)] & 0xff);
      this.marks.set(0, this.marks.get(0) + 1);
      return val;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return -1;
    } finally {
      this.lock.release();
      if (this.marks.get(0) == this.marks.get(2)) {
        this.lock.tryAcquire();
      }
    }
  }

  @Override
  public int read(byte b[], int off, int l) {
    try {
      this.lock.acquire();
      if (this.buf == null) {
        return -1;
      }
      int len = l;
      if (b == null) {
        throw new NullPointerException();
      } else if ((off < 0) || (len < 0) || (len > (b.length - off))) {
        throw new IndexOutOfBoundsException();
      }
      len = Math.min(len, this.marks.get(2) - this.marks.get(0));
      if (len <= 0) {
        return 0;
      }
      System.out.println("len: " + len);
      System.arraycopy(this.buf, this.marks.get(0), b, off, len);
      this.marks.set(0, (this.marks.get(0) + len));
      return len;
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    } finally {
      if (this.marks.get(0) >= this.marks.get(2)) {
        this.lock.tryAcquire();
      } else {
        this.lock.release();
      }
    }
  }

  @Override
  public long skip(long n) {
    long k = this.marks.get(2) - this.marks.get(0);
    if (n < k) {
      k = n < 0 ? 0 : n;
    }

    this.marks.set(0, (int) (this.marks.get(0) + k));
    return k;
  }

  @Override
  public int available() {
    return this.marks.get(2) - this.marks.get(0);
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @SuppressWarnings("sync-override")
  @Override
  public void mark(int readAheadLimit) {
    this.marks.set(1, this.marks.get(0));
  }

  @SuppressWarnings("sync-override")
  @Override
  public void reset() {
    this.marks.set(0, this.marks.get(1));
  }

  @Override
  public void close() throws IOException {
    // silent
  }
}

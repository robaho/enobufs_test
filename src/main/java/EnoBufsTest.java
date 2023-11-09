
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import javax.net.ServerSocketFactory;

public class EnoBufsTest {
    private static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
    // private static final Executor executor = Executors.newCachedThreadPool();

    private static class Listener implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket s = ServerSocketFactory.getDefault().createServerSocket();
                s.bind(new InetSocketAddress("localhost",8888),200);
                int count=0;
                while(true) {
                    Socket ns = s.accept();
                    // ns.setSendBufferSize(1024*128);
                    count++;
                    System.out.println("accepted connection "+count);
                    executor.execute(new Sender(ns));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    private static class Sender implements Runnable {
        private final Socket s;

        private Sender(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                OutputStream os = new BufferedOutputStream(s.getOutputStream());
                while (true) { 
                    while (true) { 
                        String line = r.readLine();
                        if(line==null) {
                            // System.out.println("end of stream");
                            return;
                        }
                        // System.out.println("line = "+line);
                        if(line.contains("/devnull")) {
                            break;
                        }
                    }
                    long size = 1_000_000;
                    os.write("HTTP/1.1 200 OK\r\n".getBytes());
                    os.write("Connection: keep-alive\r\n".getBytes());
                    os.write(("Content-Length: "+size+"\r\n").getBytes());
                    os.write("\r\n".getBytes());
                    byte[] buffer = new byte[1024 * 1024];
                    while (size > 0) {
                        long len = Math.min(size, buffer.length);
                        os.write(buffer, 0, (int) len);
                        size -= len;
                    }
                    os.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("java version = "+System.getProperty("java.version"));
        executor.execute(new Listener());
        LockSupport.park();
    }
}
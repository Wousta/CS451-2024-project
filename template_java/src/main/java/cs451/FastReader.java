package cs451;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

// Fast reader class that I used years ago in a competitive programming contest
// I do not remember the origin exactly, one of my team partners made it.
public class FastReader {
    BufferedReader in = null;
    StringTokenizer st;

    public FastReader() {
        in = new BufferedReader(new InputStreamReader(System.in));
    }

    public FastReader(InputStream is) {
        in = new BufferedReader(new InputStreamReader(is));
    }

    public boolean hasNext() {
        if (st != null && st.hasMoreTokens()) {
            return true;
        }
        String tmp;
        try {
            in.mark(20);
            tmp = in.readLine();
            if (tmp == null) {
                return false;
            }
            in.reset();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public String next() {
        try {
            while (st == null || !st.hasMoreTokens()) {
                st = new StringTokenizer(in.readLine());
            }
        } catch (Exception e) {
            return null;
        }
        return st.nextToken();
    }

    public int nextInt() {
        return Integer.parseInt(next());
    }
    
    public long nextLong() {
        return Long.parseLong(next());
    }
    
    public double nextDouble() {
        return Double.parseDouble(next());
    }
    
    public String nextLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            System.err.println("Error readline");
        }
        return null;
    }

    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            
        }
    }
}

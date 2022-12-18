package ru.netology;

public class Request {
    private String method;
    private String title;
    private String body;
    private String path;
    private String protocol;


    public Request(String method, String path, String protocol) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request rec = (Request) o;
        return rec.method.equals(this.method) && rec.path.equals(this.path) && rec.protocol.equals(this.protocol);
    }

    @Override
    public int hashCode() {
        int result = method == null ? 0 : method.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + protocol.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "method" + method + " path" + path + " protocol" + protocol;
    }
}

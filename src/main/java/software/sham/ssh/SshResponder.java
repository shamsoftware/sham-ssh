package software.sham.ssh;

public class SshResponder {
    public static SshResponder NULL = new SshResponder();

    private String response = "";

    public String respond() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}

public class clientTest {
    public static void main(String[] args) {
        TemporaryNode tn = new TemporaryNode();

        if(tn.start("arber.isufi@city.ac.uk:clientnode","127.0.0.1:2513")) {
            //tn.store("CopperKey","Lock");
            //tn.get("CopperKey");
            tn.echo();
            tn.terminateConnection("test complete!");
        }
    }
}

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pipeline;

public class Sender {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Gimme a frequency");
			System.exit(-1);
		}

		String[] params = { "--gst-debug=2", "--gst-debug-no-color" };
		Gst.init("Sender", params);

		Pipeline pipeline = new Pipeline("pipeline");

		Element source = ElementFactory.make("audiotestsrc", "source");
		source.set("freq", new Long(args[0]));

		Element tee = ElementFactory.make("tee", null);
		Element room = new RoomSender("room10", "224.1.42.10", 5000);

		pipeline.addMany(source, tee, room);
		System.out.println("link " + Element.linkMany(source, tee));
		tee.getRequestPad("src%d").link(room.getStaticPad("sink"));

		pipeline.play();

		Gst.main();
	}
}

package sender;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
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

		Element convert = ElementFactory.make("audioconvert", "convert");
		Element rtpPayload = ElementFactory.make("rtpL16pay", "rtpPayload");
		// System.out
		// .println(rtpPayload
		// .getSinkPads()
		// .get(0)
		// .setCaps(
		// Caps.fromString("audio/x-raw-int, endianness=(int)1234,"
		// + " signed=(boolean)true, width=(int)16, depth=(int)16,"
		// + " rate=(int)44100, channels=(int)1")));
		Element rtpBin = ElementFactory.make("gstrtpbin", "rtpbin");
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
			}
		});
		rtpBin.getRequestPad("send_rtp_sink_0");

		Element sink = ElementFactory.make("udpsink", "sink");
		sink.set("port", 5002);

		pipeline.addMany(source, convert, rtpPayload, rtpBin, sink);
		System.out.println(Element.linkMany(source, convert, rtpPayload));

		System.out.println(Element.linkPads(rtpPayload, "src", rtpBin,
				"send_rtp_sink_0"));
		System.out.println(Element.linkPads(rtpBin, "send_rtp_src_0", sink,
				"sink"));

		pipeline.play();

		System.out.println("Src caps = "
				+ source.getSrcPads().get(0).getNegotiatedCaps());

		Gst.main();
	}
}

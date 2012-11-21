package sender;

import org.gstreamer.Bin;
import org.gstreamer.Closure;
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
		Bin rtpBin = (Bin) ElementFactory.make("gstrtpbin", "rtpbin");
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
			}
		});
		rtpBin.getRequestPad("send_rtp_sink_0");
		rtpBin.connect("on-ssrc-validated", new Closure() {
			@SuppressWarnings("unused")
			public void invoke() {
				System.out.println("lol");
			}
		});
		System.out.println("Caps (check out SSRC) "
				+ rtpBin.getElementByName("rtpsession0").getSinkPads().get(0)
						.getCaps());

		Element rtpsink = ElementFactory.make("udpsink", "rtpsink");
		rtpsink.set("port", 5002);
		Element rtcpsink = ElementFactory.make("udpsink", "rtcpsink");
		rtcpsink.set("port", 5003);
		rtcpsink.set("async", false);
		rtcpsink.set("sync", false);

		pipeline.addMany(source, convert, rtpPayload, rtpBin, rtpsink, rtcpsink);
		System.out.println(Element.linkMany(source, convert, rtpPayload));

		System.out.println(Element.linkPads(rtpPayload, null, rtpBin,
				"send_rtp_sink_0"));
		System.out.println(Element.linkPads(rtpBin, "send_rtp_src_0", rtpsink,
				"sink"));

		System.out.println(Element.linkPads(rtpBin, "send_rtcp_src_0",
				rtcpsink, null));

		pipeline.play();

		System.out.println(rtpBin.getSrcPads().get(1).getCaps());

		System.out.println("Src caps = "
				+ source.getSrcPads().get(0).getNegotiatedCaps());

		Gst.main();
	}
}

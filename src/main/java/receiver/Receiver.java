package receiver;

import org.gstreamer.Caps;
import org.gstreamer.Closure;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;

public class Receiver {
	public static void main(String[] args) {
		String[] params = { "--gst-debug=2", "--gst-debug-no-color" };
		Gst.init("Receiver", params);

		Pipeline pipeline = new Pipeline("pipeline");

		Element rtpSource = ElementFactory.make("udpsrc", "rtpSource");
		rtpSource.set("port", 5002);
		System.out.println("caps "
				+ rtpSource
						.getSrcPads()
						.get(0)
						.setCaps(
								Caps.fromString("application/x-rtp, "
										+ "media=(string)audio, "
										+ "clock-rate=(int)44100, "
										+ "encoding-name=(string)L16, "
										+ "encoding-params=(string)1, "
										+ "channels=(int)1, "
										+ "payload=(int)96")));
		Element rtcpSource = ElementFactory.make("udpsrc", "rtcpSource");
		rtcpSource.set("port", 5003);

		Element convert = ElementFactory.make("audioconvert", "convert");
		final Element rtpDepayload = ElementFactory.make("rtpL16depay",
				"rtpDepayload");
		final Element rtpBin = ElementFactory.make("gstrtpbin", "rtpbin");
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
				if (pad.getName().startsWith("recv_rtp_src")) {
					System.out.println("Got new sound input pad: " + pad);
					System.out.println("bin-payload "
							+ Element.linkPads(rtpBin, pad.getName(),
									rtpDepayload, null));
				}
			}
		});
		rtpBin.getRequestPad("recv_rtp_sink_0");
		rtpBin.getRequestPad("recv_rtcp_sink_0");
		rtpBin.connect("on-new-ssrc", new Closure() {
			@SuppressWarnings("unused")
			public void invoke(Element element, int session, int ssrc) {
				System.out.println("New SSRC " + String.format("%X", ssrc));
			}
		});

		Element sink = ElementFactory.make("autoaudiosink", "sink");

		pipeline.addMany(rtpSource, rtcpSource, rtpBin, rtpDepayload, convert,
				sink);

		System.out.println("big "
				+ Element.linkMany(rtpDepayload, convert, sink));

		System.out.println("rtp "
				+ Element.linkPads(rtpSource, null, rtpBin, "recv_rtp_sink_0"));
		System.out
				.println("rtcp "
						+ Element.linkPads(rtcpSource, null, rtpBin,
								"recv_rtcp_sink_0"));

		pipeline.play();

		Gst.main();
	}
}

import org.gstreamer.Caps;
import org.gstreamer.Closure;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;

public class Receiver {
	public static void main(String[] args) {
		Gst.init("Receiver", new String[] { "--gst-debug-level=2",
				"--gst-debug=adder:4", "--gst-debug-no-color" });

		final Pipeline pipeline = new Pipeline("pipeline");

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

		final Element adder = ElementFactory.make("adder", "adder");
		adder.set("caps", Caps.fromString("audio/x-raw, "
				+ "format=(string)S16LE, rate=(int)44100, "
				+ "channels=(int)1, layout=(string)interleaved"));
		System.out.println("adder caps " + adder.get("caps"));

		final Element rtpBin = ElementFactory.make("gstrtpbin", "rtpbin");
		rtpBin.set("buffer-mode", 0);
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
				if (pad.getName().startsWith("recv_rtp_src")) {
					System.out.println("\nGot new sound input pad: " + pad);

					final Element rtpDepayload = ElementFactory.make(
							"rtpL16depay", null);
					final Element convert = ElementFactory.make("audioconvert",
							null);
					pipeline.add(rtpDepayload);
					rtpDepayload.syncStateWithParent();
					pipeline.add(convert);
					convert.syncStateWithParent();

					System.out.println("bin-payload "
							+ Element.linkPads(rtpBin, pad.getName(),
									rtpDepayload, null));

					Pad adderPad = adder.getRequestPad("sink%d");

					System.out.println("depayload-convert "
							+ Element.linkMany(rtpDepayload, convert));
					System.out.println(convert + "-" + adderPad + " "
							+ convert.getStaticPad("src").link(adderPad)
							+ convert.getSrcPads().get(0).getCaps() + " -- "
							+ adderPad.getCaps());
				}
			}
		});
		rtpBin.connect(new Element.PAD_REMOVED() {
			@Override
			public void padRemoved(Element element, Pad pad) {
				System.out.println("bye bye");
			}
		});
		rtpBin.connect("on-bye-timeout", new Closure() {
			@SuppressWarnings("unused")
			public void invoke() {
				System.out.println("on-bye-timeout");
			}
		});
		rtpBin.connect("on-timeout", new Closure() {
			@SuppressWarnings("unused")
			public void invoke() {
				System.out.println("on-timeout");
			}
		});
		rtpBin.connect("on-bye-ssrc", new Closure() {
			@SuppressWarnings("unused")
			public void invoke() {
				System.out.println("on-bye-ssrc");
			}
		});
		rtpBin.set("autoremove", true);
		rtpBin.set("use-pipeline-clock", true);
		rtpBin.set("ntp-sync", true);
		rtpBin.getRequestPad("recv_rtp_sink_0");
		rtpBin.getRequestPad("recv_rtcp_sink_0");
		rtpBin.connect("on-new-ssrc", new Closure() {
			@SuppressWarnings("unused")
			public void invoke(Element element, int session, int ssrc) {
				System.out.println("New SSRC (uint)" + ssrc + "/"
						+ String.format("%X", ssrc));
			}
		});

		Element sink = ElementFactory.make("autoaudiosink", "sink");

		pipeline.addMany(rtpSource, rtcpSource, rtpBin, adder, sink);

		System.out.println("final elts of stream "
				+ Element.linkMany(adder, sink));

		System.out.println("rtp "
				+ Element.linkPads(rtpSource, null, rtpBin, "recv_rtp_sink_0"));
		System.out
				.println("rtcp "
						+ Element.linkPads(rtcpSource, null, rtpBin,
								"recv_rtcp_sink_0"));

		pipeline.play();

		System.out.println("Reactivate ?");
		new java.util.Scanner(System.in).nextLine();
		pipeline.play();
		System.out.println("Bye ?");
		new java.util.Scanner(System.in).nextLine();
		System.out.println("Bye");
	}
}

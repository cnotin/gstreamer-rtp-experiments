import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.Pipeline;

public class Receiver {
	public static void main(String[] args) {
		Gst.init("Receiver", new String[] { "--gst-debug-level=2",
				"--gst-debug=liveadder:4", "--gst-debug=basesrc:4",
				"--gst-debug-no-color" });

		// ################# CREATE AND CONFIGURE ELEMENTS ##################
		final Pipeline pipeline = new Pipeline("pipeline");

		final Element udpSource = ElementFactory.make("udpsrc", null);
		udpSource.set("port", 5002);
		successOrDie(
				"caps",
				udpSource
						.getSrcPads()
						.get(0)
						.setCaps(
								Caps.fromString("application/x-rtp, media=audio, "
										+ "clock-rate=8000, channel=1, payload=0, "
										+ "encoding-name=PCMU")));

		final Element rtpBin = ElementFactory.make("gstrtpbin", null);
		final Element sinkQueue = ElementFactory.make("queue", null);
		final Element sink = ElementFactory.make("autoaudiosink", null);

		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
				if (pad.getName().startsWith("recv_rtp_src")) {
					Element fake = ElementFactory.make("fdsink", null);
					pipeline.add(fake);
					pad.link(fake.getStaticPad("sink"));

					// System.out.println("\nGot new sound input pad: " + pad);
					//
					// // create elements
					// DecodeBin decoder = new DecodeBin();
					//
					// // add them
					// pipeline.add(decoder);
					//
					// // sync them
					// decoder.syncStateWithParent();
					//
					// // link them
					// successOrDie(
					// "bin-decoder",
					// pad.link(decoder.getStaticPad("sink")).equals(
					// PadLinkReturn.OK));
					//
					// successOrDie(
					// "decoder-sinkqueue",
					// decoder.getStaticPad("src")
					// .link(sinkQueue.getStaticPad("sink"))
					// .equals(PadLinkReturn.OK));
				}
			}
		});

		// ############## ADD THEM TO PIPELINE ####################
		pipeline.addMany(udpSource, rtpBin, sinkQueue, sink);

		// ###################### LINK THEM ##########################
		Pad pad = rtpBin.getRequestPad("recv_rtp_sink_0");
		successOrDie("udpSource-rtpbin", udpSource.getStaticPad("src")
				.link(pad).equals(PadLinkReturn.OK));

		successOrDie("adder-queue-sink ", Element.linkMany(sinkQueue, sink));
		// ################## ROCK'n'ROLL #############################
		pipeline.play();

		System.out.println("Reactivate ?");
		new java.util.Scanner(System.in).nextLine();
		pipeline.play();
		System.out.println("Bye ?");
		new java.util.Scanner(System.in).nextLine();
		System.out.println("Bye");
	}

	private static void successOrDie(String message, boolean result) {
		if (!result) {
			System.err.println("Die because of " + message);
			System.exit(-1);
		}
	}
}

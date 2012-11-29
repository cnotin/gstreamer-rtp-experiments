import org.gstreamer.Caps;
import org.gstreamer.Closure;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.Pipeline;

public class Receiver {
	public static void main(String[] args) {
		Gst.init("Receiver", new String[] { "--gst-debug-level=2",
				"--gst-debug-no-color", "--gst-debug=liveadder:4" });

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
		rtpBin.connect("request-pt-map", new Closure() {
			@SuppressWarnings("unused")
			public void invoke() {
				System.err.println("LOL");
			}
		});
		final Element adder = ElementFactory.make("liveadder", null);
		final Element sink = ElementFactory.make("autoaudiosink", null);

		// ####################### CONNECT EVENTS ######################"
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
				if (pad.getName().startsWith("recv_rtp_src")) {
					System.out.println("\nGot new sound input pad: " + pad);

					// create elements
					RtpMulawDecodeBin decoder = new RtpMulawDecodeBin();

					// add them
					pipeline.add(decoder);

					// sync them
					decoder.syncStateWithParent();

					// link them
					successOrDie(
							"bin-decoder",
							pad.link(decoder.getStaticPad("sink")).equals(
									PadLinkReturn.OK));

					Pad adderPad = adder.getRequestPad("sink%d");
					successOrDie("decoder-adder", decoder.getStaticPad("src")
							.link(adderPad).equals(PadLinkReturn.OK));
					inspect(pipeline);
				}
			}
		});

		// ############## ADD THEM TO PIPELINE ####################
		pipeline.addMany(udpSource, rtpBin, adder, sink);

		// ###################### LINK THEM ##########################
		Pad pad = rtpBin.getRequestPad("recv_rtp_sink_0");
		successOrDie("udpSource-rtpbin", udpSource.getStaticPad("src")
				.link(pad).equals(PadLinkReturn.OK));

		successOrDie("adder-sink", Element.linkMany(adder, sink));
		// ################## ROCK'n'ROLL #############################

		pipeline.play();

		inspect(pipeline);
		System.out.println("Stop ?");
		new java.util.Scanner(System.in).nextLine();
		pipeline.stop();

		inspect(pipeline);
		System.out.println("Play ?");
		new java.util.Scanner(System.in).nextLine();
		pipeline.play();

		inspect(pipeline);
		System.out.println("Stop ?");
		new java.util.Scanner(System.in).nextLine();
		pipeline.stop();

		inspect(pipeline);
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

	private static void inspect(Pipeline pipe) {
		for (Element elt : pipe.getElements()) {
			System.out.println(elt.getName());
			for (Pad pad : elt.getSinkPads()) {
				if (pad.getPeer() == null) {
					System.out.println("\tSink pad: " + pad.getName()
							+ " DISCONNECTED");
				} else {
					System.out
							.println("\tSink pad: " + pad.getName()
									+ " connected to peer parent="
									+ pad.getPeer().getParent() + " / "
									+ pad.getPeer());
				}
			}
			for (Pad pad : elt.getSrcPads()) {
				if (pad.getPeer() == null) {
					System.out.println("\tSink pad: " + pad.getName()
							+ " DISCONNECTED");
				} else {
					System.out
							.println("\tSrc pad: " + pad.getName()
									+ " connected to peer parent="
									+ pad.getPeer().getParent() + " / "
									+ pad.getPeer());
				}
			}
		}

		pipe.debugToDotFile(Pipeline.DEBUG_GRAPH_SHOW_ALL, "test");
	}
}

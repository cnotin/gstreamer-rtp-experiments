import lombok.Getter;

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.elements.good.RTPBin;

public class RoomSender extends Bin {
	private final Pad sink;
	@Getter
	private final RtpMulawEncodeBin encoder;
	private final Element udpSink;
	private final RTPBin rtpBin;

	public RoomSender(String name, String ip, int port) {
		super(name);

		encoder = new RtpMulawEncodeBin();
		encoder.syncStateWithParent();
		rtpBin = new RTPBin((String) null);
		Pad rtpSink0 = rtpBin.getRequestPad("send_rtp_sink_0");

		udpSink = ElementFactory.make("udpsink", null);
		udpSink.set("host", ip);
		udpSink.set("port", port);
		udpSink.set("auto-multicast", true);

		// ############## ADD THEM TO PIPELINE ####################
		addMany(encoder, rtpBin, udpSink);

		// ###################### LINK THEM ##########################
		sink = new GhostPad("sink", encoder.getStaticPad("sink"));
		sink.setActive(true);
		addPad(sink);

		System.out.println("encoder-rtpBin "
				+ encoder.getStaticPad("src").link(rtpSink0)
						.equals(PadLinkReturn.OK));
		System.out.println("rtpbin-udpSink "
				+ rtpBin.getStaticPad("send_rtp_src_0")
						.link(udpSink.getStaticPad("sink"))
						.equals(PadLinkReturn.OK));

		// how to find SSRC (pretty dirty)
		String caps = rtpBin.getElementByName("rtpsession0").getSinkPads()
				.get(0).getCaps().toString();
		System.out.println(caps);
		int ssrcBegin = caps.indexOf("ssrc=(uint)") + 11;
		int ssrcEnd = caps.indexOf(";", ssrcBegin);
		System.out.println(caps.substring(ssrcBegin, ssrcEnd));
	}
}

import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;

public class DecodeBin extends Bin {
	private Element queue;
	private Element rtpDepay;
	private Element decoder;
	private Element convert;

	private Pad sink;
	private Pad src;

	public DecodeBin() {
		super("decodebin");

		queue = ElementFactory.make("queue", null);
		rtpDepay = ElementFactory.make("rtppcmudepay", null);
		decoder = ElementFactory.make("mulawdec", null);
		convert = ElementFactory.make("audioconvert", null);

		sink = new GhostPad("sink", rtpDepay.getStaticPad("sink"));
		src = new GhostPad("src", convert.getStaticPad("src"));

		this.addMany(queue, rtpDepay, decoder, convert);
		Bin.linkMany(queue, rtpDepay, decoder, convert);

		this.addPad(sink);
		this.addPad(src);
	}
}

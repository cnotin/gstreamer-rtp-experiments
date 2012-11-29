import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;

public class RtpMulawDecodeBin extends Bin {
	private Element rtpDepay;
	private Element decoder;
	private Element convert;

	private Pad sink;
	private Pad src;

	public RtpMulawDecodeBin() {
		super();

		rtpDepay = ElementFactory.make("rtppcmudepay", null);
		decoder = ElementFactory.make("mulawdec", null);
		convert = ElementFactory.make("audioconvert", null);

		this.addMany(rtpDepay, decoder, convert);
		Bin.linkMany(rtpDepay, decoder, convert);

		sink = new GhostPad("sink", rtpDepay.getStaticPad("sink"));
		src = new GhostPad("src", convert.getStaticPad("src"));

		this.addPad(sink);
		this.addPad(src);

		this.sink.connect(new OnPadUnlinked());
		this.sink.connect(new OnPadLinked());
	}

	private class OnPadUnlinked implements Pad.UNLINKED {
		@Override
		public void unlinked(Pad complainer, Pad gonePad) {
			System.err.println("I, " + complainer + ", have lost my dear pad "
					+ gonePad + " :(");
		}
	}

	private class OnPadLinked implements Pad.LINKED {
		@Override
		public void linked(Pad greeter, Pad newPad) {
			System.err.println("I, " + greeter + ", have a new friend "
					+ newPad + " :)");
		}
	}
}

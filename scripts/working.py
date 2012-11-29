#!/usr/bin/python

import gobject;
gobject.threads_init()
import gst;

if __name__ == "__main__":
    # First create our pipeline
    pipe = gst.Pipeline("mypipe")

    # Create a software mixer with "Adder"
    adder = gst.element_factory_make("adder","audiomixer")
    pipe.add(adder)

    # Gather a request sink pad on the mixer
    sinkpad1=adder.get_request_pad("sink%d")

    # Create the first buzzer..
    buzzer1 = gst.element_factory_make("audiotestsrc","buzzer1")
    buzzer1.set_property("freq",1000)
    pipe.add(buzzer1)
    # .. and connect it's source pad to the previously gathered request pad
    buzzersrc1=buzzer1.get_pad("src")
    buzzersrc1.link(sinkpad1)

    # Add some output
    output = gst.element_factory_make("autoaudiosink", "audio_out")
    pipe.add(output)
    adder.link(output)

    # Start the playback
    pipe.set_state(gst.STATE_PLAYING)

    raw_input("1kHz test sound. Press <ENTER> to continue.")

    # Get an another request sink pad on the mixer
    sinkpad2=adder.get_request_pad("sink%d")

    # Create an another buzzer and connect it the same way
    buzzer2 = gst.element_factory_make("audiotestsrc","buzzer2")
    buzzer2.set_property("freq",500)
    pipe.add(buzzer2)

    buzzersrc2=buzzer2.get_pad("src")
    buzzersrc2.link(sinkpad2)

    # Start the second buzzer (other ways streaming stops because of starvation)
    buzzer2.set_state(gst.STATE_PLAYING)

    raw_input("1kHz + 500Hz test sound playing simoultenously. Press <ENTER> to continue.")

    # Before removing a source, we must use pad blocking to prevent state changes
    buzzersrc1.set_blocked(True)
    # Stop the first buzzer
    buzzer1.set_state(gst.STATE_NULL)
    # Unlink from the mixer
    buzzersrc1.unlink(sinkpad2)
    # Release the mixers first sink pad
    adder.release_request_pad(sinkpad1)
    # Because here none of the Adder's sink pads block, streaming continues

    raw_input("Only 500Hz test sound. Press <ENTER> to stop.")

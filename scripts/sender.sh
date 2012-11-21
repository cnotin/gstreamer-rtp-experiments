#!/bin/sh

if [ $1 ]
then
	FREQ=$1
else
	FREQ=440;
fi;

gst-launch --gst-debug=2 --gst-debug-no-color gstrtpbin name=rtpbin \
    audiotestsrc freq=$FREQ ! audioconvert ! audio/x-raw-int,channels=1,depth=16,width=16,rate=44100 ! \
    rtpL16pay ! rtpbin.send_rtp_sink_1 \
          rtpbin.send_rtp_src_1 ! udpsink port=5002 \
          rtpbin.send_rtcp_src_1 ! udpsink port=5003 sync=false async=false
#    udpsrc port=5007 ! rtpbin.recv_rtcp_sink_0

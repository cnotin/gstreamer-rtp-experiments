#!/bin/sh

if [ $1 ]
then
	FREQ=$1
else
	FREQ=440;
fi;

gst-launch -v --gst-debug=2 gstrtpbin name=rtpbin \
    audiotestsrc freq=$FREQ ! audioconvert ! \
    mulawenc ! rtppcmupay ! rtpbin.send_rtp_sink_0 \
          rtpbin.send_rtp_src_0 ! udpsink port=5002
#          rtpbin.send_rtcp_src_0 ! udpsink port=5003 sync=false async=false
#    udpsrc port=5007 ! rtpbin.recv_rtcp_sink_0
